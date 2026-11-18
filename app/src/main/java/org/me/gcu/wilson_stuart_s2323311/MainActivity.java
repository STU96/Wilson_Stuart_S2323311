/*  Starter project for Mobile Platform Development - 1st diet 26/27
    You should use this project as the starting point for your assignment.
    This project simply reads the data from the required source and displays the
    raw data in a TextField
*/

//
// Name                 Stuart Wilson
// Student ID           S2323311
// Programme of Study   Software Development
//

package org.me.gcu.wilson_stuart_s2323311;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.view.View.OnClickListener;
import android.content.pm.PackageManager;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.me.gcu.wilson_stuart_s2323311.data.TrafficXmlParser;
import org.me.gcu.wilson_stuart_s2323311.model.FeedType;
import org.me.gcu.wilson_stuart_s2323311.model.Situation;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Base64;


public class MainActivity extends AppCompatActivity implements OnClickListener {
    private TextView rawDataDisplay;
    private Button startButton;

    //DOWNLOADING FROM TRAFFIC SCOTLAND
    private String clientId = "";
    private String clientKey = "";
    final String unplannedEventsURL = "https://datex2.trafficscotland.org/rest/2.3/publications/UnplannedEvents/Content.xml";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        // Set up the raw links to the graphical components
        rawDataDisplay = (TextView)findViewById(R.id.rawDataDisplay);
        startButton = (Button)findViewById(R.id.startButton);
        startButton.setOnClickListener(this);

        // More Code goes here
        try {
            Bundle metaData = getPackageManager()
                    .getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA).metaData;
            clientId = String.valueOf(metaData.get(getPackageName() + ".TS_CLIENT_ID"));
            clientKey = String.valueOf(metaData.get(getPackageName() + ".TS_CLIENT_KEY"));
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }
        Log.d("Keys", "Traffic Scotland keys loaded: " + !clientId.isEmpty());

        // TEMPORARY parser test with the sample files
        new Thread(() -> {
            int[] files = { R.raw.sample_unplanned, R.raw.sample_roadworks };
            for (int file : files) {
                StringBuilder xml = new StringBuilder();
                try (BufferedReader in = new BufferedReader(
                        new InputStreamReader(getResources().openRawResource(file)))) {
                    String line;
                    while ((line = in.readLine()) != null) {
                        xml.append(line).append('\n');
                    }
                } catch (IOException e) {
                    Log.e("ParserTest", "Could not read sample: " + e);
                }
                FeedType type = (file == R.raw.sample_unplanned) ? FeedType.INCIDENT : FeedType.ROADWORKS;
                ArrayList<Situation> list = new TrafficXmlParser().parse(xml.toString(), type);
                Log.d("ParserTest", type.getLabel() + ": " + list.size() + " situations in the collection");
                for (Situation s : list) {
                    Log.d("ParserTest", s.toString());   // iterate through the collection (Lab 3)
                }
            }
        }).start();


    }

    public void onClick(View aview)
    {
        startProgress();
    }

    public void startProgress()
    {
        // Run network access on a separate thread;
        new Thread(new Task(unplannedEventsURL)).start();
    } //

    // Need separate thread to access the internet resource over network
    // Other neater solutions should be adopted in later iterations.
    private class Task implements Runnable
    {
        private String result="";
        private String myurl;
        BufferedReader in = null;
        String inputLine = "";
        public Task(String aurl){
            myurl = aurl;
        }
        @Override
        public void run(){
            Log.d("MyTask","in run");
            try {
                URL url = new URL(myurl);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                //ADD AUTHORISATION (string with clientIID:clientKey)
                urlConnection.setRequestProperty("Authorization",Base64.getEncoder().encodeToString((clientId + ":" + clientKey).getBytes()));
                try {
                    in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    while ((inputLine = in.readLine()) != null){
                        result = result + inputLine;
                    }
                    in.close();
                } catch (IOException e) {
                    String error = urlConnection.getErrorStream().toString();
                    Log.e("urlConnection", "ioexception");
                }
                finally {
                    urlConnection.disconnect();
                }
            } catch (IOException e) {
                Log.e("URL", "ioexception");
            }

            //Clean up any leading garbage characters
            int i = result.indexOf("<?"); //initial tag
            result = result.substring(i);

            // Now that you have the xml data into result, you can parse it
            try {
                XmlPullParserFactory factory =
                        XmlPullParserFactory.newInstance();
                factory.setNamespaceAware(true);
                XmlPullParser xpp = factory.newPullParser();
                xpp.setInput( new StringReader( result ) );

                //YOUR PARSING HERE!!!


            } catch (XmlPullParserException e) {
                Log.e("Parsing","EXCEPTION" + e);
                //throw new RuntimeException(e);
            }

            // Now update the TextView to display raw XML data
            // Probably not the best way to update TextView
            // but we are just getting started !

            MainActivity.this.runOnUiThread(new Runnable()
            {
                public void run() {
                    Log.d("UI thread", "I am the UI thread");
                    rawDataDisplay.setText(result);
                }
            });
        }

    }

}