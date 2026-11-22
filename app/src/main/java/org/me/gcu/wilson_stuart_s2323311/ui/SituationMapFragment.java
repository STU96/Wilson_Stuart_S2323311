package org.me.gcu.wilson_stuart_s2323311.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;

import org.me.gcu.wilson_stuart_s2323311.R;
import org.me.gcu.wilson_stuart_s2323311.model.Situation;
import org.me.gcu.wilson_stuart_s2323311.viewmodel.TrafficViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// Map showing all situations, one selected situation, or the search results
public class SituationMapFragment extends Fragment implements OnMapReadyCallback,
        GoogleMap.OnInfoWindowClickListener, View.OnClickListener {

    private TrafficViewModel viewModel;
    private GoogleMap map;
    private MapHelper mapHelper;
    private List<Situation> situations = new ArrayList<>();
    private boolean needsCameraFit = true;   // only re-centre the map when something new is picked

    private TextView tvMapCount;
    private View layersPanel;
    private CheckBox cbIncidents;
    private CheckBox cbRoadworks;
    private CheckBox cbSatellite;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        TextView tvMapTitle = view.findViewById(R.id.tvMapTitle);
        tvMapCount = view.findViewById(R.id.tvMapCount);
        layersPanel = view.findViewById(R.id.layersPanel);
        cbIncidents = view.findViewById(R.id.cbIncidents);
        cbRoadworks = view.findViewById(R.id.cbRoadworks);
        cbSatellite = view.findViewById(R.id.cbSatellite);
        cbIncidents.setOnClickListener(this);
        cbRoadworks.setOnClickListener(this);
        cbSatellite.setOnClickListener(this);

        mapHelper = new MapHelper(requireContext());
        viewModel = new ViewModelProvider(requireActivity()).get(TrafficViewModel.class);
        cbIncidents.setChecked(viewModel.isMapShowIncidents());
        cbRoadworks.setChecked(viewModel.isMapShowRoadworks());

        SupportMapFragment mapFragment =
                (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        viewModel.getMapTitle().observe(getViewLifecycleOwner(), title -> {
            tvMapTitle.setText(title);
            needsCameraFit = true;   // new selection, so zoom to it
        });
        viewModel.getMapSituations().observe(getViewLifecycleOwner(), list -> {
            situations = (list == null) ? new ArrayList<>() : list;
            // only show the incidents/roadworks tick boxes when everything is on the map
            layersPanel.setVisibility(viewModel.getMapMode() == TrafficViewModel.MapMode.ALL
                    ? View.VISIBLE : View.GONE);
            drawSituations();
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        ((NavigationHost) requireActivity()).setCurrentScreen(NavigationHost.SCREEN_MAP);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;
        map.getUiSettings().setZoomControlsEnabled(true);
        map.setOnInfoWindowClickListener(this);
        map.setMapType(cbSatellite.isChecked() ? GoogleMap.MAP_TYPE_HYBRID : GoogleMap.MAP_TYPE_NORMAL);
        drawSituations();
    }

    private void drawSituations() {
        if (map == null) {
            return;   // map not ready yet; onMapReady will call this again
        }
        map.clear();
        LatLngBounds.Builder bounds = new LatLngBounds.Builder();
        int drawn = 0;
        for (Situation s : situations) {
            if (mapHelper.addSituation(map, s, bounds) != null) {
                drawn++;
            }
        }
        tvMapCount.setText(String.format(Locale.UK, "%d situation(s) on the map · tap a marker, then its label for details",
                drawn));
        if (needsCameraFit) {
            mapHelper.fitCamera(map, bounds, drawn);
            needsCameraFit = false;
        }
    }

    // Tapping a marker's info window opens the full details
    @Override
    public void onInfoWindowClick(@NonNull Marker marker) {
        Object tag = marker.getTag();
        if (tag instanceof Situation) {
            viewModel.select((Situation) tag);
            ((NavigationHost) requireActivity()).openDetail();
        }
    }

    @Override
    public void onClick(View v) {
        if (v == cbSatellite) {
            if (map != null) {
                map.setMapType(cbSatellite.isChecked() ? GoogleMap.MAP_TYPE_HYBRID : GoogleMap.MAP_TYPE_NORMAL);
            }
        } else {
            viewModel.setMapLayers(cbIncidents.isChecked(), cbRoadworks.isChecked());
        }
    }
}
