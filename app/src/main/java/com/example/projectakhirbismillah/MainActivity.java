package com.example.projectakhirbismillah;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.example.projectakhirbismillah.fragment.CartFragment;
import com.example.projectakhirbismillah.fragment.FavoriteFragment;
import com.example.projectakhirbismillah.fragment.HomeFragment;
import com.example.projectakhirbismillah.fragment.ProfileFragment;
import com.example.projectakhirbismillah.util.CartManager;
import com.example.projectakhirbismillah.util.FavoriteManager;
import com.example.projectakhirbismillah.util.NetworkStateManager;
import com.example.projectakhirbismillah.util.SessionManager;
import com.example.projectakhirbismillah.util.ThemeHelper;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

public class MainActivity extends AppCompatActivity implements NetworkStateManager.NetworkStateListener {
    private static final String TAG = "MainActivity";
    private BottomNavigationView bottomNavigationView;
    private SessionManager sessionManager;
    private View networkErrorLayout;
    private FrameLayout fragmentContainer;
    private Button buttonRefresh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Terapkan tema sebelum super.onCreate() dan setContentView()
        Log.d(TAG, "Applying saved theme from preferences before content view");
        int savedThemeMode = ThemeHelper.getThemeMode(this);
        ThemeHelper.applyTheme(savedThemeMode);
        Log.d(TAG, "Saved theme mode: " + savedThemeMode);

        // Panggil super.onCreate() hanya SEKALI
        super.onCreate(savedInstanceState);

        // Atur window insets
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        // Set layout - hanya SEKALI
        setContentView(R.layout.activity_main);

        Log.d(TAG, "MainActivity created with theme: " +
                (ThemeHelper.isDarkMode(this) ? "Dark" : "Light"));

        // Initialize session manager
        sessionManager = new SessionManager(this);

        // Initialize network state manager
        NetworkStateManager.getInstance().initialize(getApplicationContext());

        // Initialize CartManager dan FavoriteManager singleton with application context
        CartManager.getInstance(getApplicationContext());
        FavoriteManager.getInstance().initialize(getApplicationContext());

        // Find views
        fragmentContainer = findViewById(R.id.fragment_container);
        networkErrorLayout = findViewById(R.id.network_error_layout);
        buttonRefresh = findViewById(R.id.button_refresh);

        // Set refresh button click listener
        buttonRefresh.setOnClickListener(v -> checkNetworkAndRefresh());

        // Check if user is logged in
        if (!sessionManager.isLoggedIn()) {
            // Redirect to login screen if not logged in
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        // Setup edge-to-edge display
        setupEdgeToEdge();

        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Setup bottom navigation
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.navigation_home) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.navigation_favorites) {
                // Reload favorite manager data sebelum membuat fragment
                FavoriteManager.getInstance().initialize(getApplicationContext());
                selectedFragment = new FavoriteFragment();
            } else if (itemId == R.id.navigation_cart) {
                selectedFragment = new CartFragment();
            } else if (itemId == R.id.navigation_profile) {
                selectedFragment = new ProfileFragment();
            }

            if (selectedFragment != null && NetworkStateManager.getInstance().isNetworkAvailable()) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
                hideNetworkError();
                return true;
            } else if (!NetworkStateManager.getInstance().isNetworkAvailable()) {
                // Show network error if there's no connection
                showNetworkError();
                return true;
            }

            return false;
        });

        // Set default fragment if no network issues
        if (savedInstanceState == null) {
            if (NetworkStateManager.getInstance().isNetworkAvailable()) {
                bottomNavigationView.setSelectedItemId(R.id.navigation_home);
            } else {
                showNetworkError();
            }
        }

        // Add this activity as network listener
        NetworkStateManager.getInstance().addListener(this);
    }

    private void setupEdgeToEdge() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            // Adjust the bottom padding of fragmentContainer to account for navigation bar
            if (fragmentContainer != null) {
                fragmentContainer.setPadding(
                        fragmentContainer.getPaddingLeft(),
                        fragmentContainer.getPaddingTop(),
                        fragmentContainer.getPaddingRight(),
                        systemBars.bottom
                );
            }

            return insets;
        });
    }

    @Override
    protected void onDestroy() {
        // Remove network listener to prevent memory leaks
        NetworkStateManager.getInstance().removeListener(this);
        super.onDestroy();
    }

    @Override
    public void onNetworkAvailable() {
        runOnUiThread(() -> {
            hideNetworkError();
            // Refresh the current fragment
            int selectedItemId = bottomNavigationView.getSelectedItemId();
            bottomNavigationView.setSelectedItemId(selectedItemId);
        });
    }

    @Override
    public void onNetworkUnavailable() {
        runOnUiThread(this::showNetworkError);
    }

    private void showNetworkError() {
        if (fragmentContainer != null) {
            fragmentContainer.setVisibility(View.GONE);
        }
        if (networkErrorLayout != null) {
            networkErrorLayout.setVisibility(View.VISIBLE);
        }
    }

    private void hideNetworkError() {
        if (networkErrorLayout != null) {
            networkErrorLayout.setVisibility(View.GONE);
        }
        if (fragmentContainer != null) {
            fragmentContainer.setVisibility(View.VISIBLE);
        }
    }

    private void checkNetworkAndRefresh() {
        if (NetworkStateManager.getInstance().checkNetworkAvailability()) {
            hideNetworkError();
            // Refresh current fragment
            int selectedItemId = bottomNavigationView.getSelectedItemId();
            bottomNavigationView.setSelectedItemId(selectedItemId);
        } else {
            // Network still unavailable, show a toast or some feedback
            android.widget.Toast.makeText(this,
                    "Koneksi internet masih tidak tersedia",
                    android.widget.Toast.LENGTH_SHORT).show();
        }
    }
}