package org.me.gcu.wilson_stuart_s2323311.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;

import org.me.gcu.wilson_stuart_s2323311.R;
import org.me.gcu.wilson_stuart_s2323311.model.FeedType;
import org.me.gcu.wilson_stuart_s2323311.model.Situation;
import org.me.gcu.wilson_stuart_s2323311.viewmodel.TrafficViewModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

// Shows the list of incidents or roadworks, with a filter box and sort options
public class SituationListFragment extends Fragment implements AdapterView.OnItemClickListener {

    private static final String ARG_FEED = "feed";

    private FeedType feedType;
    private TrafficViewModel viewModel;
    private SituationAdapter adapter;
    private List<Situation> allItems = new ArrayList<>();

    private TextView tvListTitle;
    private EditText etFilter;
    private Spinner spSort;

    // Creates the fragment for one feed - the feed type is kept in the arguments so it survives rotation
    public static SituationListFragment newInstance(FeedType feedType) {
        SituationListFragment fragment = new SituationListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_FEED, feedType.name());
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_situation_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        feedType = FeedType.valueOf(requireArguments().getString(ARG_FEED, FeedType.INCIDENT.name()));
        boolean roadworks = (feedType == FeedType.ROADWORKS);

        tvListTitle = view.findViewById(R.id.tvListTitle);
        TextView tvListInfo = view.findViewById(R.id.tvListInfo);
        etFilter = view.findViewById(R.id.etFilter);
        spSort = view.findViewById(R.id.spSort);
        ListView listView = view.findViewById(R.id.listView);
        TextView tvEmpty = view.findViewById(R.id.tvEmpty);

        // The colour legend only applies to roadworks
        view.findViewById(R.id.legend).setVisibility(roadworks ? View.VISIBLE : View.GONE);
        view.findViewById(R.id.legendIncident).setVisibility(View.GONE);

        adapter = new SituationAdapter(requireContext(), new ArrayList<>(), false);
        listView.setAdapter(adapter);
        listView.setEmptyView(tvEmpty);
        listView.setOnItemClickListener(this);

        ArrayAdapter<CharSequence> sortAdapter = ArrayAdapter.createFromResource(requireContext(),
                R.array.sort_options, android.R.layout.simple_spinner_item);
        sortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spSort.setAdapter(sortAdapter);
        spSort.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                applyFilterAndSort();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        etFilter.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                applyFilterAndSort();
            }
        });

        // Same ViewModel that MainActivity and the other fragments use
        viewModel = new ViewModelProvider(requireActivity()).get(TrafficViewModel.class);
        LiveData<List<Situation>> data = roadworks ? viewModel.getRoadworks() : viewModel.getIncidents();
        data.observe(getViewLifecycleOwner(), list -> {
            allItems = (list == null) ? new ArrayList<>() : list;
            applyFilterAndSort();
        });
        LiveData<String> info = roadworks ? viewModel.getRoadworksInfo() : viewModel.getIncidentsInfo();
        info.observe(getViewLifecycleOwner(), tvListInfo::setText);
    }

    @Override
    public void onResume() {
        super.onResume();
        ((NavigationHost) requireActivity()).setCurrentScreen(
                feedType == FeedType.ROADWORKS ? NavigationHost.SCREEN_ROADWORKS : NavigationHost.SCREEN_INCIDENTS);
    }

    // Applies the filter text and sort order, then updates the ListView
    private void applyFilterAndSort() {
        if (adapter == null) return;
        String filter = etFilter.getText().toString().trim().toLowerCase(Locale.UK);
        ArrayList<Situation> shown = new ArrayList<>();
        for (Situation s : allItems) {
            if (filter.isEmpty()
                    || s.getDescription().toLowerCase(Locale.UK).contains(filter)
                    || s.getRoadName().toLowerCase(Locale.UK).contains(filter)
                    || s.getTypeLabel().toLowerCase(Locale.UK).contains(filter)) {
                shown.add(s);
            }
        }
        switch (spSort.getSelectedItemPosition()) {
            case 1: // Road (A-Z)
                Collections.sort(shown, (a, b) -> a.getRoadName().compareTo(b.getRoadName()));
                break;
            case 2: // Longest delays first
                Collections.sort(shown, (a, b) ->
                        Integer.compare(b.getDelayLevel().ordinal(), a.getDelayLevel().ordinal()));
                break;
            default: // Newest first
                Collections.sort(shown, (a, b) -> {
                    if (a.getStartTime() == null) return (b.getStartTime() == null) ? 0 : 1;  // no date: last
                    if (b.getStartTime() == null) return -1;
                    return b.getStartTime().compareTo(a.getStartTime());
                });
        }
        adapter.clear();
        adapter.addAll(shown);
        String title = (feedType == FeedType.ROADWORKS) ? "Roadworks" : "Incidents (unplanned events)";
        tvListTitle.setText(String.format(Locale.UK, "%s · showing %d of %d", title, shown.size(), allItems.size()));
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Situation situation = adapter.getItem(position);
        if (situation != null) {
            viewModel.select(situation);
            ((NavigationHost) requireActivity()).openDetail();
        }
    }
}
