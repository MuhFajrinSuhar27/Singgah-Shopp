package com.example.projectakhirbismillah;

import android.app.Application;
import android.util.Log;

import com.example.projectakhirbismillah.util.NetworkStateManager;
import com.example.projectakhirbismillah.util.ThemeHelper;

public class MyApplication extends Application {
    private static final String TAG = "MyApplication";
    
    @Override
    public void onCreate() {
        super.onCreate();

        // Inisialisasi tema dari SharedPreferences
        Log.d(TAG, "Initializing app theme from preferences");
        ThemeHelper.initTheme(getApplicationContext());

        // Initialize NetworkStateManager at application startup
        NetworkStateManager.getInstance().initialize(getApplicationContext());
        
        // Inisialisasi komponen aplikasi lainnya jika diperlukan
        Log.d(TAG, "Application components initialized");
    }

    @Override
    public void onTerminate() {
        // Clean up NetworkStateManager when application terminates
        NetworkStateManager.getInstance().unregisterNetworkCallback();
        super.onTerminate();
    }
}