package org.me.gcu.wilson_stuart_s2323311.data;

import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.util.Log;

import org.me.gcu.wilson_stuart_s2323311.R;
import org.me.gcu.wilson_stuart_s2323311.model.FeedType;
import org.me.gcu.wilson_stuart_s2323311.model.Situation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;

// Downloads the two feeds, saves a copy for when there's no internet, and parses them.
// This does network and file work, so it is only called from the background thread.
public class TrafficRepository {

    private static final String TAG = "Repository";

    public static final String UNPLANNED_URL =
            "https://datex2.trafficscotland.org/rest/2.3/publications/UnplannedEvents/Content.xml";
    public static final String ROADWORKS_URL =
            "https://datex2.trafficscotland.org/rest/2.3/publications/CurrentRoadworks/Content.xml";

    // true = load the sample XML files from res/raw instead of downloading (used for testing)
    public static final boolean USE_SAMPLE_DATA = true;

    private final Context appContext;
    private final String clientId;
    private final String clientKey;
    private final TrafficXmlParser parser = new TrafficXmlParser();

    public TrafficRepository(Context context) {
        appContext = context.getApplicationContext();
        clientId = readMetaData("TS_CLIENT_ID");
        clientKey = readMetaData("TS_CLIENT_KEY");
    }

    // Reads one of the API keys from the manifest meta-data (the values come from local.properties)
    private String readMetaData(String name) {
        try {
            Bundle metaData = appContext.getPackageManager()
                    .getApplicationInfo(appContext.getPackageName(), PackageManager.GET_META_DATA).metaData;
            Object value = (metaData == null) ? null : metaData.get(appContext.getPackageName() + "." + name);
            return value == null ? "" : String.valueOf(value);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Could not read meta-data " + name);
            return "";
        }
    }

    // Gets one feed (live if possible, otherwise the saved copy) and parses it
    public synchronized FeedResult loadFeed(FeedType feedType) {
        boolean incidents = (feedType == FeedType.INCIDENT);
        File cacheFile = new File(appContext.getFilesDir(), incidents ? "unplanned.xml" : "roadworks.xml");

        if (USE_SAMPLE_DATA) {
            String xml = readRawResource(incidents ? R.raw.sample_unplanned : R.raw.sample_roadworks);
            return parseResult(feedType, xml, FeedResult.Source.SAMPLE, System.currentTimeMillis(), "");
        }

        String error;
        if (clientId.isEmpty() || clientKey.isEmpty()) {
            error = "Traffic Scotland keys missing (check local.properties)";
        } else if (!isNetworkAvailable()) {
            error = "No internet connection";
        } else {
            try {
                String xml = download(incidents ? UNPLANNED_URL : ROADWORKS_URL);
                writeFile(cacheFile, xml);   // save a copy for when there's no internet
                return parseResult(feedType, xml, FeedResult.Source.LIVE, System.currentTimeMillis(), "");
            } catch (IOException e) {
                error = (e.getMessage() == null) ? "Download failed" : e.getMessage();
            }
        }
        Log.e(TAG, feedType.getLabel() + " live update failed: " + error);

        // no live data, so use the last saved copy if there is one
        if (cacheFile.exists()) {
            String xml = readFile(cacheFile);
            if (!xml.isEmpty()) {
                return parseResult(feedType, xml, FeedResult.Source.CACHE, cacheFile.lastModified(), error);
            }
        }
        return new FeedResult(feedType, new ArrayList<>(), FeedResult.Source.NONE, 0, null, error);
    }

    private FeedResult parseResult(FeedType feedType, String xml, FeedResult.Source source,
                                   long time, String error) {
        ArrayList<Situation> situations = parser.parse(xml, feedType);
        return new FeedResult(feedType, situations, source, time, parser.getPublicationTime(), error);
    }

    // Downloads the XML with HttpURLConnection
    private String download(String address) throws IOException {
        Log.d(TAG, "Downloading " + address);
        HttpURLConnection connection = (HttpURLConnection) new URL(address).openConnection();
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(30000);
        // Authorisation header: Base64 of "clientId:clientKey"
        String credentials = clientId + ":" + clientKey;
        connection.setRequestProperty("Authorization",
                Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8)));
        try {
            int status = connection.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) {
                throw new IOException("Traffic Scotland replied with HTTP " + status);
            }
            StringBuilder result = new StringBuilder();
            try (BufferedReader in = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    result.append(inputLine).append('\n');
                }
            }
            String xml = result.toString();
            if (xml.toLowerCase().contains("<html")) {
                // a web page instead of XML usually means the keys weren't accepted
                throw new IOException("Traffic Scotland sent a web page, not XML (check your keys)");
            }
            int start = xml.indexOf("<?");          // skip anything before the XML starts
            if (start < 0) {
                start = xml.indexOf("<");           // some replies have no <?xml ...?> line
            }
            if (start < 0) {
                throw new IOException("The reply did not contain XML");
            }
            return xml.substring(start);
        } finally {
            connection.disconnect();
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm =
                (ConnectivityManager) appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            return false;
        }
        Network network = cm.getActiveNetwork();
        if (network == null) {
            return false;
        }
        NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
        return capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    private void writeFile(File file, String text) {
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            writer.write(text);
        } catch (IOException e) {
            Log.e(TAG, "Could not save " + file.getName());
        }
    }

    private String readFile(File file) {
        try {
            return readStream(new FileInputStream(file));
        } catch (IOException e) {
            Log.e(TAG, "Could not read " + file.getName());
            return "";
        }
    }

    private String readRawResource(int resId) {
        try {
            return readStream(appContext.getResources().openRawResource(resId));
        } catch (IOException e) {
            Log.e(TAG, "Could not read sample data");
            return "";
        }
    }

    private String readStream(InputStream stream) throws IOException {
        StringBuilder text = new StringBuilder();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = in.readLine()) != null) {
                text.append(line).append('\n');
            }
        }
        return text.toString();
    }
}
