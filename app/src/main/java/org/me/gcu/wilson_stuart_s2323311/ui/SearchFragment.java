package org.me.gcu.wilson_stuart_s2323311.ui;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import org.me.gcu.wilson_stuart_s2323311.R;
import org.me.gcu.wilson_stuart_s2323311.model.Situation;
import org.me.gcu.wilson_stuart_s2323311.viewmodel.TrafficViewModel;

import java.util.ArrayList;
import java.util.Locale;

// Search by road name, or by distance from the phone's location or a fixed place
public class SearchFragment extends Fragment implements View.OnClickListener,
        AdapterView.OnItemClickListener, SeekBar.OnSeekBarChangeListener, RadioGroup.OnCheckedChangeListener {

    // Places offered for the distance search. Position 0 = the phone's own (coarse) location.
    private static final String[] PLACE_NAMES = {
            "My location (phone)", "GCU, Glasgow", "Edinburgh", "Aberdeen", "Dundee", "Inverness", "Stirling"};
    private static final double[][] PLACE_COORDINATES = {
            {0, 0}, {55.8666, -4.2504}, {55.9533, -3.1883}, {57.1497, -2.0943},
            {56.4620, -2.9707}, {57.4778, -4.2247}, {56.1165, -3.9369}};
    private static final int DEFAULT_PLACE = 1;   // GCU, Glasgow
    private static final long LOCATION_TIMEOUT_MS = 10000;

    private TrafficViewModel viewModel;
    private SituationAdapter resultsAdapter;
    private ArrayAdapter<String> roadAdapter;

    private RadioGroup rgSearchMode;
    private View panelRoad;
    private View panelDistance;
    private AutoCompleteTextView etRoad;
    private Spinner spPlace;
    private SeekBar sbRadius;
    private TextView tvRadius;
    private Button btnResultsMap;

    private ActivityResultLauncher<String> permissionLauncher;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private LocationManager locationManager;
    private LocationListener locationListener;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Ask for location permission when it is needed
        permissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            if (granted) {
                searchFromPhoneLocation();
            } else {
                useDefaultPlace("Location permission refused");
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rgSearchMode = view.findViewById(R.id.rgSearchMode);
        panelRoad = view.findViewById(R.id.panelRoad);
        panelDistance = view.findViewById(R.id.panelDistance);
        etRoad = view.findViewById(R.id.etRoad);
        spPlace = view.findViewById(R.id.spPlace);
        sbRadius = view.findViewById(R.id.sbRadius);
        tvRadius = view.findViewById(R.id.tvRadius);
        btnResultsMap = view.findViewById(R.id.btnResultsMap);
        TextView tvSearchSummary = view.findViewById(R.id.tvSearchSummary);
        ListView lvResults = view.findViewById(R.id.lvResults);

        rgSearchMode.setOnCheckedChangeListener(this);
        view.findViewById(R.id.btnRoadSearch).setOnClickListener(this);
        view.findViewById(R.id.btnDistanceSearch).setOnClickListener(this);
        btnResultsMap.setOnClickListener(this);
        sbRadius.setOnSeekBarChangeListener(this);

        // Road suggestions come from the roads that are actually in the feeds
        roadAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line,
                new ArrayList<>());
        etRoad.setAdapter(roadAdapter);
        etRoad.setThreshold(1);
        etRoad.setOnItemClickListener((parent, v, position, id) -> runRoadSearch());
        etRoad.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                runRoadSearch();
                return true;
            }
            return false;
        });

        ArrayAdapter<String> placeAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, PLACE_NAMES);
        placeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spPlace.setAdapter(placeAdapter);
        if (savedInstanceState == null) {
            spPlace.setSelection(DEFAULT_PLACE);
        }

        resultsAdapter = new SituationAdapter(requireContext(), new ArrayList<>(), true);
        lvResults.setAdapter(resultsAdapter);
        lvResults.setEmptyView(view.findViewById(R.id.tvNoResults));
        lvResults.setOnItemClickListener(this);

        viewModel = new ViewModelProvider(requireActivity()).get(TrafficViewModel.class);
        viewModel.getRoadNames().observe(getViewLifecycleOwner(), names -> {
            roadAdapter.clear();
            roadAdapter.addAll(names);
        });
        viewModel.getSearchResults().observe(getViewLifecycleOwner(), results -> {
            resultsAdapter.clear();
            resultsAdapter.addAll(results);
            btnResultsMap.setEnabled(!results.isEmpty());
        });
        viewModel.getSearchSummary().observe(getViewLifecycleOwner(), tvSearchSummary::setText);

        updateRadiusText();
        updatePanels();
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        // After a rotation, show the right panel for the selected search type
        updatePanels();
        updateRadiusText();
    }

    @Override
    public void onResume() {
        super.onResume();
        ((NavigationHost) requireActivity()).setCurrentScreen(NavigationHost.SCREEN_SEARCH);
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        updatePanels();
    }

    private void updatePanels() {
        boolean byRoad = rgSearchMode.getCheckedRadioButtonId() != R.id.rbByDistance;
        panelRoad.setVisibility(byRoad ? View.VISIBLE : View.GONE);
        panelDistance.setVisibility(byRoad ? View.GONE : View.VISIBLE);
    }

    private void updateRadiusText() {
        tvRadius.setText(String.format(Locale.UK, "Within %d km", sbRadius.getProgress()));
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btnRoadSearch) {
            runRoadSearch();
        } else if (id == R.id.btnDistanceSearch) {
            runDistanceSearch();
        } else if (id == R.id.btnResultsMap) {
            viewModel.showSearchResultsOnMap();       // show all the results on the map
            ((NavigationHost) requireActivity()).openMap();
        }
    }

    private void runRoadSearch() {
        String road = etRoad.getText().toString().trim();
        if (road.isEmpty()) {
            etRoad.setError(getString(R.string.enter_road));
            return;
        }
        hideKeyboard();
        etRoad.dismissDropDown();
        viewModel.searchByRoad(road);
    }

    private void runDistanceSearch() {
        int place = spPlace.getSelectedItemPosition();
        if (place == 0) {
            if (hasLocationPermission()) {
                searchFromPhoneLocation();
            } else {
                permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION);
            }
        } else {
            viewModel.searchByDistance(PLACE_COORDINATES[place][0], PLACE_COORDINATES[place][1],
                    sbRadius.getProgress(), PLACE_NAMES[place]);
        }
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    // Uses the phone's last known location, or asks for a new one if there isn't one
    private void searchFromPhoneLocation() {
        if (!hasLocationPermission()) {
            return;
        }
        locationManager = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);
        Location best = null;
        for (String provider : locationManager.getProviders(true)) {
            try {
                Location last = locationManager.getLastKnownLocation(provider);
                if (last != null && (best == null || last.getTime() > best.getTime())) {
                    best = last;
                }
            } catch (SecurityException e) {
                // this provider isn't allowed, so skip it
            }
        }
        if (best != null) {
            viewModel.searchByDistance(best.getLatitude(), best.getLongitude(), sbRadius.getProgress(),
                    "your location");
            return;
        }

        String provider = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
                ? LocationManager.NETWORK_PROVIDER : LocationManager.GPS_PROVIDER;
        if (!locationManager.isProviderEnabled(provider)) {
            useDefaultPlace("Location is switched off");
            return;
        }
        Toast.makeText(requireContext(), R.string.finding_location, Toast.LENGTH_SHORT).show();
        // Anonymous class rather than a lambda so it also works on older Android versions
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                stopLocationUpdates();
                if (isAdded()) {
                    viewModel.searchByDistance(location.getLatitude(), location.getLongitude(),
                            sbRadius.getProgress(), "your location");
                }
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) { }

            @Override
            public void onProviderEnabled(@NonNull String provider) { }

            @Override
            public void onProviderDisabled(@NonNull String provider) { }
        };
        try {
            locationManager.requestLocationUpdates(provider, 0, 0f, locationListener, Looper.getMainLooper());
            handler.postDelayed(timeout, LOCATION_TIMEOUT_MS);
        } catch (SecurityException e) {
            useDefaultPlace("Location not available");
        }
    }

    private final Runnable timeout = () -> {
        stopLocationUpdates();
        if (isAdded()) {
            useDefaultPlace("Could not get your location");
        }
    };

    private void stopLocationUpdates() {
        handler.removeCallbacks(timeout);
        if (locationManager != null && locationListener != null) {
            locationManager.removeUpdates(locationListener);
            locationListener = null;
        }
    }

    private void useDefaultPlace(String reason) {
        Toast.makeText(requireContext(), reason + " – using " + PLACE_NAMES[DEFAULT_PLACE],
                Toast.LENGTH_LONG).show();
        spPlace.setSelection(DEFAULT_PLACE);
        viewModel.searchByDistance(PLACE_COORDINATES[DEFAULT_PLACE][0], PLACE_COORDINATES[DEFAULT_PLACE][1],
                sbRadius.getProgress(), PLACE_NAMES[DEFAULT_PLACE]);
    }

    private void hideKeyboard() {
        InputMethodManager imm =
                (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(etRoad.getWindowToken(), 0);
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        updateRadiusText();
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) { }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) { }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Situation situation = resultsAdapter.getItem(position);
        if (situation != null) {
            viewModel.select(situation);
            ((NavigationHost) requireActivity()).openDetail();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopLocationUpdates();
    }
}
