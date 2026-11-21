package org.me.gcu.wilson_stuart_s2323311.ui;

// MainActivity implements this so the fragments can ask it to change screen
public interface NavigationHost {
    int SCREEN_INCIDENTS = 0;
    int SCREEN_ROADWORKS = 1;
    int SCREEN_SEARCH = 2;
    int SCREEN_MAP = 3;
    int SCREEN_DETAIL = 4;

    void openDetail();                 // show the selected situation
    void openMap();                    // show the map (keeps the Back button working)
    void setCurrentScreen(int screen); // highlights the matching navigation button
}
