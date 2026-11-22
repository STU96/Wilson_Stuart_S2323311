/*  Mobile Platform Development coursework 2026/27 - Traffic Scotland app
    Single Activity app using Fragments and MVVM.
*/

//
// Name                 Stuart Wilson
// Student ID           S2323311
// Programme of Study   Software Development
//

package org.me.gcu.wilson_stuart_s2323311;

import android.app.AlertDialog;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import org.me.gcu.wilson_stuart_s2323311.model.FeedType;
import org.me.gcu.wilson_stuart_s2323311.ui.DetailFragment;
import org.me.gcu.wilson_stuart_s2323311.ui.NavigationHost;
import org.me.gcu.wilson_stuart_s2323311.ui.SearchFragment;
import org.me.gcu.wilson_stuart_s2323311.ui.SituationListFragment;
import org.me.gcu.wilson_stuart_s2323311.ui.SituationMapFragment;
import org.me.gcu.wilson_stuart_s2323311.viewmodel.TrafficViewModel;

// The only Activity in the app. It holds the toolbar, the navigation buttons and the fragment container.
public class MainActivity extends AppCompatActivity implements OnClickListener, NavigationHost {

    private TrafficViewModel viewModel;
    private MaterialButton btnIncidents;
    private MaterialButton btnRoadworks;
    private MaterialButton btnSearch;
    private MaterialButton btnMap;
    private TextView tvStatus;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);   // Android picks the portrait or landscape version
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        MaterialToolbar toolbar = (MaterialToolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle(R.string.app_title);   // show the app title instead of the project name

        // Link up the views (both layouts use the same ids)
        tvStatus = (TextView) findViewById(R.id.tvStatus);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        btnIncidents = (MaterialButton) findViewById(R.id.btnIncidents);
        btnRoadworks = (MaterialButton) findViewById(R.id.btnRoadworks);
        btnSearch = (MaterialButton) findViewById(R.id.btnSearch);
        btnMap = (MaterialButton) findViewById(R.id.btnMap);
        btnIncidents.setOnClickListener(this);
        btnRoadworks.setOnClickListener(this);
        btnSearch.setOnClickListener(this);
        btnMap.setOnClickListener(this);

        // The ViewModel keeps the data when the screen rotates, so it isn't downloaded again
        viewModel = new ViewModelProvider(this).get(TrafficViewModel.class);
        viewModel.getStatus().observe(this, text -> tvStatus.setText(text));
        viewModel.getOffline().observe(this, offline -> tvStatus.setTextColor(
                ContextCompat.getColor(this, offline ? R.color.offline_text : R.color.status_text)));
        viewModel.getLoading().observe(this, busy ->
                progressBar.setVisibility(busy ? View.VISIBLE : View.GONE));

        // Only add the first screen on a fresh start - after a rotation the current fragment comes back by itself
        if (savedInstanceState == null) {
            showTopLevel(SituationListFragment.newInstance(FeedType.INCIDENT));
        }
    }

    @Override
    public void onClick(View v) {
        if (v == btnIncidents) {
            showTopLevel(SituationListFragment.newInstance(FeedType.INCIDENT));
        } else if (v == btnRoadworks) {
            showTopLevel(SituationListFragment.newInstance(FeedType.ROADWORKS));
        } else if (v == btnSearch) {
            showTopLevel(new SearchFragment());
        } else if (v == btnMap) {
            viewModel.showAllOnMap();
            showTopLevel(new SituationMapFragment());
        }
    }

    // Shows one of the main screens and clears the back stack
    private void showTopLevel(Fragment fragment) {
        FragmentManager fm = getSupportFragmentManager();
        fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        fm.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }

    // Opens a screen on top of the current one so Back goes to the previous screen
    private void pushScreen(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void openDetail() {
        pushScreen(new DetailFragment());
    }

    @Override
    public void openMap() {
        pushScreen(new SituationMapFragment());
    }

    // Each fragment calls this when it is shown so the right button is highlighted
    @Override
    public void setCurrentScreen(int screen) {
        if (screen == SCREEN_DETAIL) {
            return;   // keep the highlight from the screen we came from
        }
        highlight(btnIncidents, screen == SCREEN_INCIDENTS);
        highlight(btnRoadworks, screen == SCREEN_ROADWORKS);
        highlight(btnSearch, screen == SCREEN_SEARCH);
        highlight(btnMap, screen == SCREEN_MAP);
    }

    private void highlight(MaterialButton button, boolean selected) {
        int background = ContextCompat.getColor(this, selected ? R.color.primary : R.color.nav_unselected);
        int foreground = ContextCompat.getColor(this, selected ? R.color.white : R.color.primary);
        button.setBackgroundTintList(ColorStateList.valueOf(background));
        button.setTextColor(foreground);
        button.setIconTint(ColorStateList.valueOf(foreground));
    }

    // ----- toolbar menu -----
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_refresh) {
            viewModel.refresh();
            Toast.makeText(this, R.string.refreshing, Toast.LENGTH_SHORT).show();
            return true;
        } else if (item.getItemId() == R.id.action_about) {
            showAboutDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Simple About dialog
    private void showAboutDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.about);
        builder.setMessage(R.string.about_text);
        builder.setPositiveButton("OK", (dialog, id) -> dialog.dismiss());
        builder.create().show();
    }
}
