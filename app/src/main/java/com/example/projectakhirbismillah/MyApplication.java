package com.example.projectakhirbismillah;

import android.app.Application;
import com.example.projectakhirbismillah.util.NetworkStateManager;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize NetworkStateManager at application startup
        NetworkStateManager.getInstance().initialize(getApplicationContext());
    }

    @Override
    public void onTerminate() {
        // Clean up NetworkStateManager when application terminates
        NetworkStateManager.getInstance().unregisterNetworkCallback();
        super.onTerminate();
    }
}