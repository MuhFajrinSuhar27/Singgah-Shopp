package com.example.projectakhirbismillah.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class NetworkStateManager {
    private static final String TAG = "NetworkStateManager";
    private static NetworkStateManager instance;
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    private boolean isNetworkAvailable = false;
    private List<NetworkStateListener> listeners = new ArrayList<>();

    public interface NetworkStateListener {
        void onNetworkAvailable();
        void onNetworkUnavailable();
    }

    private NetworkStateManager() {
        // Private constructor for singleton
    }

    public static synchronized NetworkStateManager getInstance() {
        if (instance == null) {
            instance = new NetworkStateManager();
        }
        return instance;
    }

    public void initialize(Context context) {
        if (connectivityManager == null) {
            connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            registerNetworkCallback();
            // Check initial state
            isNetworkAvailable = checkNetworkAvailability();
            Log.d(TAG, "Initial network state: " + (isNetworkAvailable ? "Available" : "Unavailable"));
        }
    }

    private void registerNetworkCallback() {
        if (connectivityManager == null) return;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                networkCallback = new ConnectivityManager.NetworkCallback() {
                    @Override
                    public void onAvailable(@NonNull Network network) {
                        isNetworkAvailable = true;
                        Log.d(TAG, "Network available");
                        notifyNetworkAvailable();
                    }

                    @Override
                    public void onLost(@NonNull Network network) {
                        isNetworkAvailable = false;
                        Log.d(TAG, "Network unavailable");
                        notifyNetworkUnavailable();
                    }
                };

                connectivityManager.registerDefaultNetworkCallback(networkCallback);
            } else {
                // For older Android versions
                NetworkRequest networkRequest = new NetworkRequest.Builder()
                        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        .build();

                networkCallback = new ConnectivityManager.NetworkCallback() {
                    @Override
                    public void onAvailable(@NonNull Network network) {
                        isNetworkAvailable = true;
                        Log.d(TAG, "Network available");
                        notifyNetworkAvailable();
                    }

                    @Override
                    public void onLost(@NonNull Network network) {
                        isNetworkAvailable = false;
                        Log.d(TAG, "Network unavailable");
                        notifyNetworkUnavailable();
                    }
                };

                connectivityManager.registerNetworkCallback(networkRequest, networkCallback);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error registering network callback: " + e.getMessage());
        }
    }

    public void unregisterNetworkCallback() {
        if (connectivityManager != null && networkCallback != null) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            } catch (Exception e) {
                Log.e(TAG, "Error unregistering network callback: " + e.getMessage());
            }
        }
    }

    public boolean isNetworkAvailable() {
        return isNetworkAvailable;
    }

    public boolean checkNetworkAvailability() {
        if (connectivityManager == null) return false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = connectivityManager.getActiveNetwork();
            if (network == null) return false;

            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
            return capabilities != null &&
                    (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
        } else {
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            return networkInfo != null && networkInfo.isConnected();
        }
    }

    public void addListener(NetworkStateListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
            // Notify based on current state immediately
            if (isNetworkAvailable) {
                listener.onNetworkAvailable();
            } else {
                listener.onNetworkUnavailable();
            }
        }
    }

    public void removeListener(NetworkStateListener listener) {
        listeners.remove(listener);
    }

    private void notifyNetworkAvailable() {
        for (NetworkStateListener listener : listeners) {
            listener.onNetworkAvailable();
        }
    }

    private void notifyNetworkUnavailable() {
        for (NetworkStateListener listener : listeners) {
            listener.onNetworkUnavailable();
        }
    }
}