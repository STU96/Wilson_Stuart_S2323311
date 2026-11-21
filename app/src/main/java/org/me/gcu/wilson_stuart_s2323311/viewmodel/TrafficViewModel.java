package org.me.gcu.wilson_stuart_s2323311.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import org.me.gcu.wilson_stuart_s2323311.data.FeedResult;
import org.me.gcu.wilson_stuart_s2323311.data.TrafficRepository;
import org.me.gcu.wilson_stuart_s2323311.model.FeedType;
import org.me.gcu.wilson_stuart_s2323311.model.Situation;
import org.me.gcu.wilson_stuart_s2323311.model.TextHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

// ViewModel: holds the data for the screens, survives rotation, and runs the downloads on a background thread.
// The fragments observe the LiveData here and never touch the network or the parser.
public class TrafficViewModel extends AndroidViewModel {

    private static final String TAG = "TrafficViewModel";

    // How often the data updates automatically, in minutes
    public static final long UPDATE_INTERVAL_MINUTES = 60;

    public enum MapMode { ALL, SINGLE, SEARCH }

    private final TrafficRepository repository;
    // One background thread for the start-up download, the automatic updates and manual refreshes
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    // ----- data observed by the UI -----
    private final MutableLiveData<List<Situation>> incidents = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Situation>> roadworks = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> incidentsInfo = new MutableLiveData<>("");
    private final MutableLiveData<String> roadworksInfo = new MutableLiveData<>("");
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> status = new MutableLiveData<>("Loading traffic data…");
    private final MutableLiveData<Boolean> offline = new MutableLiveData<>(false);
    private final MutableLiveData<Situation> selected = new MutableLiveData<>();
    private final MutableLiveData<List<Situation>> searchResults = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> searchSummary = new MutableLiveData<>("No search yet");
    private final MutableLiveData<List<String>> roadNames = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Situation>> mapSituations = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> mapTitle = new MutableLiveData<>("All situations");

    // ----- working data (written by the background thread, read by searches) -----
    private volatile List<Situation> currentIncidents = new ArrayList<>();
    private volatile List<Situation> currentRoadworks = new ArrayList<>();
    private volatile HashMap<String, ArrayList<Situation>> roadIndex = new HashMap<>();

    private volatile MapMode mapMode = MapMode.ALL;
    private volatile boolean mapShowIncidents = true;
    private volatile boolean mapShowRoadworks = true;

    // The last search, so it can be repeated automatically after each update
    private volatile String lastRoadQuery = null;
    private volatile boolean lastSearchWasDistance = false;
    private volatile double lastLatitude, lastLongitude, lastRadiusKm;
    private volatile String lastPlaceName = "";

    public TrafficViewModel(@NonNull Application application) {
        super(application);
        repository = new TrafficRepository(application);
        // Update when the app starts (delay 0) and then every UPDATE_INTERVAL_MINUTES
        executor.scheduleWithFixedDelay(this::updateData, 0, UPDATE_INTERVAL_MINUTES, TimeUnit.MINUTES);
    }

    // Manual refresh (toolbar button) - queued on the same background thread
    public void refresh() {
        executor.execute(this::updateData);
    }

    // ===================== background work =====================

    // Runs on the background thread. postValue() sends each result back to the UI thread.
    private void updateData() {
        Log.d(TAG, "Updating data on thread: " + Thread.currentThread().getName());
        loading.postValue(true);
        try {
            loadBothFeeds();
        } catch (Exception e) {
            // catch everything, otherwise one error would stop all the future updates
            Log.e(TAG, "Update failed: " + e);
            status.postValue("Update failed: " + e.getMessage());
        } finally {
            loading.postValue(false);
        }
    }

    private void loadBothFeeds() {
        FeedResult incidentResult = repository.loadFeed(FeedType.INCIDENT);
        FeedResult roadworksResult = repository.loadFeed(FeedType.ROADWORKS);

        // Only replace what we have if the new result actually contains data
        if (incidentResult.hasData()) {
            currentIncidents = incidentResult.getSituations();
            incidents.postValue(currentIncidents);
            incidentsInfo.postValue(describe(incidentResult));
        }
        if (roadworksResult.hasData()) {
            currentRoadworks = roadworksResult.getSituations();
            roadworks.postValue(currentRoadworks);
            roadworksInfo.postValue(describe(roadworksResult));
        }
        buildRoadIndex();

        // Status line shown at the top of the app
        boolean live = incidentResult.getSource() == FeedResult.Source.LIVE
                && roadworksResult.getSource() == FeedResult.Source.LIVE;
        offline.postValue(!live);
        status.postValue(buildStatus(incidentResult, roadworksResult, live));

        // Keep everything else the user is looking at up to date
        refreshSelected();
        if (mapMode == MapMode.ALL) {
            mapSituations.postValue(buildAllForMap());
        }
        if (lastRoadQuery != null || lastSearchWasDistance) {
            ArrayList<Situation> results = lastSearchWasDistance
                    ? computeDistanceSearch(lastLatitude, lastLongitude, lastRadiusKm)
                    : computeRoadSearch(lastRoadQuery);
            searchResults.postValue(results);
            searchSummary.postValue(buildSearchSummary(results.size()));
            if (mapMode == MapMode.SEARCH) {
                mapSituations.postValue(results);
            }
        }
    }

    private String describe(FeedResult result) {
        String published = result.getPublicationTime() == null
                ? "unknown" : TextHelper.formatDate(result.getPublicationTime());
        return result.getSituations().size() + " " + result.getFeedType().getLabel().toLowerCase(Locale.UK)
                + (result.getFeedType() == FeedType.INCIDENT ? "s" : "")
                + " · feed published " + published;
    }

    private String buildStatus(FeedResult incidentResult, FeedResult roadworksResult, boolean live) {
        SimpleDateFormat time = new SimpleDateFormat("HH:mm", Locale.UK);
        if (live) {
            Date next = new Date(System.currentTimeMillis() + UPDATE_INTERVAL_MINUTES * 60_000L);
            return "Live data · updated " + time.format(new Date()) + " · next update " + time.format(next);
        }
        if (incidentResult.getSource() == FeedResult.Source.SAMPLE) {
            return "Sample data (development mode)";
        }
        String error = incidentResult.getErrorMessage().isEmpty()
                ? roadworksResult.getErrorMessage() : incidentResult.getErrorMessage();
        if (incidentResult.hasData() || roadworksResult.hasData()) {
            long saved = Math.min(incidentResult.getDataTimeMillis(), roadworksResult.getDataTimeMillis());
            if (saved == 0) {
                saved = Math.max(incidentResult.getDataTimeMillis(), roadworksResult.getDataTimeMillis());
            }
            return "Offline (" + error + ") · showing saved data from " + TextHelper.formatDate(new Date(saved));
        }
        return "No data: " + error + ". Check your connection and tap Refresh.";
    }

    // HashMap of road -> situations on that road, also used for the road name suggestions
    private void buildRoadIndex() {
        HashMap<String, ArrayList<Situation>> index = new HashMap<>();
        for (Situation s : getAllSituations()) {
            String road = TextHelper.normaliseRoad(s.getRoadName());
            if (road.isEmpty()) continue;
            ArrayList<Situation> list = index.get(road);
            if (list == null) {
                list = new ArrayList<>();
                index.put(road, list);
            }
            list.add(s);
        }
        roadIndex = index;
        ArrayList<String> names = new ArrayList<>(index.keySet());
        Collections.sort(names, ROAD_ORDER);
        roadNames.postValue(names);
    }

    // sorts roads as A1, A7, A9, A77, A90 instead of A1, A77, A9
    private static final Comparator<String> ROAD_ORDER = (a, b) -> {
        if (a.charAt(0) != b.charAt(0)) {
            return Character.compare(a.charAt(0), b.charAt(0));
        }
        int numberA = TextHelper.parseIntSafe(a.replaceAll("[^0-9]", ""));
        int numberB = TextHelper.parseIntSafe(b.replaceAll("[^0-9]", ""));
        return numberA != numberB ? Integer.compare(numberA, numberB) : a.compareTo(b);
    };

    private void refreshSelected() {
        Situation current = selected.getValue();
        if (current == null) return;
        for (Situation s : getAllSituations()) {
            if (s.getSituationId().equals(current.getSituationId())) {
                selected.postValue(s);
                return;
            }
        }
    }

    // ===================== used by the fragments =====================

    public List<Situation> getAllSituations() {
        ArrayList<Situation> all = new ArrayList<>(currentIncidents);
        all.addAll(currentRoadworks);
        return all;
    }

    public void select(Situation situation) {
        selected.setValue(situation);
    }

    // ----- search by road -----
    public void searchByRoad(String query) {
        lastRoadQuery = query;
        lastSearchWasDistance = false;
        ArrayList<Situation> results = computeRoadSearch(query);
        searchResults.setValue(results);
        searchSummary.setValue(buildSearchSummary(results.size()));
    }

    private ArrayList<Situation> computeRoadSearch(String query) {
        LinkedHashSet<Situation> found = new LinkedHashSet<>();
        String road = TextHelper.normaliseRoad(query);
        ArrayList<Situation> indexed = roadIndex.get(road);
        if (indexed != null) {
            found.addAll(indexed);                  // fast lookup by road
        }
        for (Situation s : getAllSituations()) {    // plus situations that mention the road elsewhere
            if (!found.contains(s) && s.matchesRoad(query)) {
                found.add(s);
            }
        }
        ArrayList<Situation> results = new ArrayList<>(found);
        for (Situation s : results) {
            s.setDistanceKm(-1);
        }
        // Incidents first, then roadworks with the longest delays
        Collections.sort(results, (a, b) -> {
            if (a.isRoadworks() != b.isRoadworks()) {
                return a.isRoadworks() ? 1 : -1;
            }
            return Integer.compare(b.getDelayLevel().ordinal(), a.getDelayLevel().ordinal());
        });
        return results;
    }

    // ----- search by distance -----
    public void searchByDistance(double latitude, double longitude, double radiusKm, String placeName) {
        lastRoadQuery = null;
        lastSearchWasDistance = true;
        lastLatitude = latitude;
        lastLongitude = longitude;
        lastRadiusKm = radiusKm;
        lastPlaceName = placeName;
        ArrayList<Situation> results = computeDistanceSearch(latitude, longitude, radiusKm);
        searchResults.setValue(results);
        searchSummary.setValue(buildSearchSummary(results.size()));
    }

    private ArrayList<Situation> computeDistanceSearch(double latitude, double longitude, double radiusKm) {
        ArrayList<Situation> results = new ArrayList<>();
        for (Situation s : getAllSituations()) {
            double distance = s.distanceFrom(latitude, longitude);
            if (distance <= radiusKm) {
                s.setDistanceKm(distance);
                results.add(s);
            }
        }
        Collections.sort(results, (a, b) -> Double.compare(a.getDistanceKm(), b.getDistanceKm()));
        return results;
    }

    private String buildSearchSummary(int count) {
        if (lastSearchWasDistance) {
            return count + " situation(s) within " + Math.round(lastRadiusKm) + " km of " + lastPlaceName;
        }
        return count + " situation(s) on or mentioning " + TextHelper.normaliseRoad(lastRoadQuery);
    }

    // ----- map -----
    public void showAllOnMap() {
        mapMode = MapMode.ALL;
        mapTitle.setValue("All situations");
        mapSituations.setValue(buildAllForMap());
    }

    public void setMapLayers(boolean showIncidents, boolean showRoadworks) {
        mapShowIncidents = showIncidents;
        mapShowRoadworks = showRoadworks;
        if (mapMode == MapMode.ALL) {
            mapSituations.setValue(buildAllForMap());
        }
    }

    public void showSelectedOnMap() {
        Situation s = selected.getValue();
        if (s == null) return;
        mapMode = MapMode.SINGLE;
        ArrayList<Situation> one = new ArrayList<>();
        one.add(s);
        mapTitle.setValue(s.getTypeLabel() + " · " + s.getRoadName());
        mapSituations.setValue(one);
    }

    public void showSearchResultsOnMap() {
        mapMode = MapMode.SEARCH;
        mapTitle.setValue(searchSummary.getValue());
        List<Situation> results = searchResults.getValue();
        mapSituations.setValue(results == null ? new ArrayList<Situation>() : new ArrayList<Situation>(results));
    }

    private ArrayList<Situation> buildAllForMap() {
        ArrayList<Situation> list = new ArrayList<>();
        if (mapShowIncidents) list.addAll(currentIncidents);
        if (mapShowRoadworks) list.addAll(currentRoadworks);
        return list;
    }

    public MapMode getMapMode() { return mapMode; }
    public boolean isMapShowIncidents() { return mapShowIncidents; }
    public boolean isMapShowRoadworks() { return mapShowRoadworks; }

    // ----- LiveData getters -----
    public LiveData<List<Situation>> getIncidents() { return incidents; }
    public LiveData<List<Situation>> getRoadworks() { return roadworks; }
    public LiveData<String> getIncidentsInfo() { return incidentsInfo; }
    public LiveData<String> getRoadworksInfo() { return roadworksInfo; }
    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<String> getStatus() { return status; }
    public LiveData<Boolean> getOffline() { return offline; }
    public LiveData<Situation> getSelected() { return selected; }
    public LiveData<List<Situation>> getSearchResults() { return searchResults; }
    public LiveData<String> getSearchSummary() { return searchSummary; }
    public LiveData<List<String>> getRoadNames() { return roadNames; }
    public LiveData<List<Situation>> getMapSituations() { return mapSituations; }
    public LiveData<String> getMapTitle() { return mapTitle; }

    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdownNow();   // stop the background thread when the app is closed
    }
}
