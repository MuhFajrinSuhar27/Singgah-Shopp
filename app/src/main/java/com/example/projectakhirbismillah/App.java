package com.example.projectakhirbismillah;

import android.app.Application;
import android.util.Log;

import com.example.projectakhirbismillah.util.ThemeHelper;

public class App extends Application {
    private static final String TAG = "App";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Application onCreate - Initializing app components");

        // Inisialisasi tema dari SharedPreferences
        ThemeHelper.initTheme(this);
        Log.d(TAG, "Theme initialized from preferences");
    }
}