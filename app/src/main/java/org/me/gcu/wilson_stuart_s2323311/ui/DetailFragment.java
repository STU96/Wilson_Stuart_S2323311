package org.me.gcu.wilson_stuart_s2323311.ui;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLngBounds;

import org.me.gcu.wilson_stuart_s2323311.R;
import org.me.gcu.wilson_stuart_s2323311.model.Impact;
import org.me.gcu.wilson_stuart_s2323311.model.LocationPoint;
import org.me.gcu.wilson_stuart_s2323311.model.Situation;
import org.me.gcu.wilson_stuart_s2323311.model.TextHelper;
import org.me.gcu.wilson_stuart_s2323311.viewmodel.TrafficViewModel;

// Shows all the details of the selected situation plus a small map
public class DetailFragment extends Fragment implements View.OnClickListener, OnMapReadyCallback {

    private TrafficViewModel viewModel;
    private Situation situation;
    private GoogleMap miniMap;
    private MapHelper mapHelper;

    private ImageView ivDetailIcon;
    private TextView tvDetailType;
    private TextView tvDetailSubtitle;
    private TextView tvDetailDescription;
    private TextView tvDetailBadge;
    private LinearLayout detailRows;
    private LayoutInflater inflater;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        this.inflater = inflater;
        return inflater.inflate(R.layout.fragment_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ivDetailIcon = view.findViewById(R.id.ivDetailIcon);
        tvDetailType = view.findViewById(R.id.tvDetailType);
        tvDetailSubtitle = view.findViewById(R.id.tvDetailSubtitle);
        tvDetailDescription = view.findViewById(R.id.tvDetailDescription);
        tvDetailBadge = view.findViewById(R.id.tvDetailBadge);
        detailRows = view.findViewById(R.id.detailRows);
        Button btnShowOnMap = view.findViewById(R.id.btnShowOnMap);
        btnShowOnMap.setOnClickListener(this);

        mapHelper = new MapHelper(requireContext());
        SupportMapFragment mapFragment =
                (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.miniMap);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        viewModel = new ViewModelProvider(requireActivity()).get(TrafficViewModel.class);
        viewModel.getSelected().observe(getViewLifecycleOwner(), selected -> {
            situation = selected;
            showSituation();
            drawMiniMap();
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        ((NavigationHost) requireActivity()).setCurrentScreen(NavigationHost.SCREEN_DETAIL);
    }

    private void showSituation() {
        if (situation == null) {
            tvDetailType.setText(R.string.nothing_selected);
            return;
        }
        int colour = SituationStyle.getColour(requireContext(), situation);
        ivDetailIcon.setImageResource(SituationStyle.getIconRes(situation.getKind()));
        ivDetailIcon.setBackgroundTintList(ColorStateList.valueOf(colour));
        tvDetailType.setText(situation.getTypeLabel());
        tvDetailSubtitle.setText(situation.getFeedType().getLabel() + " · " + situation.getRoadName()
                + " · " + situation.getLocation().getDirectionText());
        tvDetailDescription.setText(situation.getDescription());
        tvDetailBadge.setText(SituationStyle.getBadgeText(situation));
        tvDetailBadge.setBackgroundTintList(ColorStateList.valueOf(colour));

        detailRows.removeAllViews();

        addSection("When");
        addRow("Started", TextHelper.formatDate(situation.getStartTime()));
        if (situation.getEndTime() != null) {
            addRow("Expected end", TextHelper.formatDate(situation.getEndTime()));
            long days = situation.getDurationDays();
            addRow("Duration", days + (days == 1 ? " day" : " days")
                    + (days <= 7 ? " (short term)" : " (long term)"));
        }
        addRow("First reported", TextHelper.formatDate(situation.getCreationTime()));
        addRow("Last updated", TextHelper.formatDate(situation.getVersionTime()));

        addSection("Impact");
        Impact impact = situation.getImpact();
        addRow("Summary", impact.getSummary());
        if (impact.getNumberOfOperationalLanes() >= 0) {
            addRow("Lanes open", String.valueOf(impact.getNumberOfOperationalLanes()));
        }
        if (impact.getNumberOfLanesRestricted() >= 0) {
            addRow("Lanes restricted", String.valueOf(impact.getNumberOfLanesRestricted()));
        }
        if (situation.isRoadworks()) {
            addRow("Delays", situation.getDelayLevel().getLabel());
            if (!situation.getWorksType().isEmpty()) {
                addRow("Works", situation.getWorksTypeText());
            }
            addRow("Urgent roadworks", situation.isUrgentRoadworks() ? "Yes" : "No");
        }

        addSection("Location");
        addRow("Road", situation.getRoadName());
        addRow("Direction", situation.getLocation().getDirectionText());
        addPoint("From", situation.getLocation().getFrom());
        addPoint("To", situation.getLocation().getTo());

        addSection("Record");
        addRow("Situation type", situation.getRecordTypeText());
        addRow("Detail type", situation.getDetailTypeText());
        addRow("Probability", TextHelper.camelCaseToWords(situation.getProbability()));
        if (!situation.getComplianceOption().isEmpty()) {
            addRow("Compliance", TextHelper.camelCaseToWords(situation.getComplianceOption()));
        }
        addRow("Source", situation.getSource());
        addRow("Situation ID", situation.getSituationId());
        addRow("Record version", situation.getRecordId() + " (v" + situation.getRecordVersion() + ")");
    }

    private void addPoint(String label, LocationPoint point) {
        if (point == null) {
            return;
        }
        addRow(label, point.getDisplayName());
        addRow(label + " lat/long", point.getCoordinatesText());
    }

    private void addSection(String title) {
        TextView section = (TextView) inflater.inflate(R.layout.item_detail_section, detailRows, false);
        section.setText(title);
        detailRows.addView(section);
    }

    private void addRow(String label, String value) {
        View row = inflater.inflate(R.layout.item_detail_row, detailRows, false);
        ((TextView) row.findViewById(R.id.tvRowLabel)).setText(label);
        ((TextView) row.findViewById(R.id.tvRowValue)).setText(
                (value == null || value.trim().isEmpty()) ? "Not given" : value);
        detailRows.addView(row);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        miniMap = googleMap;
        miniMap.getUiSettings().setMapToolbarEnabled(false);
        drawMiniMap();
    }

    private void drawMiniMap() {
        if (miniMap == null || situation == null) {
            return;
        }
        miniMap.clear();
        LatLngBounds.Builder bounds = new LatLngBounds.Builder();
        int drawn = (mapHelper.addSituation(miniMap, situation, bounds) != null) ? 1 : 0;
        mapHelper.fitCamera(miniMap, bounds, drawn);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btnShowOnMap && situation != null) {
            viewModel.showSelectedOnMap();
            ((NavigationHost) requireActivity()).openMap();
        }
    }
}
