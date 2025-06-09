package com.example.projectakhirbismillah.util;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

public class ThemeHelper {
    private static final String TAG = "ThemeHelper";
    private static final String PREFERENCES_NAME = "theme_preferences";
    private static final String KEY_THEME_MODE = "theme_mode";

    // Tema modes
    public static final int MODE_SYSTEM = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
    public static final int MODE_LIGHT = AppCompatDelegate.MODE_NIGHT_NO;
    public static final int MODE_DARK = AppCompatDelegate.MODE_NIGHT_YES;

    /**
     * Apply the specified theme mode
     */
    public static void applyTheme(int themeMode) {
        Log.d(TAG, "Applying theme mode: " + themeMode);
        
        // Check current mode before changing
        int currentMode = AppCompatDelegate.getDefaultNightMode();
        Log.d(TAG, "Previous theme mode was: " + currentMode);
        
        // Force theme mode with more persistence
        AppCompatDelegate.setDefaultNightMode(themeMode);
        
        // Log untuk verifikasi
        Log.d(TAG, "Theme mode after set: " + AppCompatDelegate.getDefaultNightMode());
    }

    /**
     * Save theme mode preference
     */
    public static void saveThemeMode(Context context, int themeMode) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(
                PREFERENCES_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(KEY_THEME_MODE, themeMode);
        boolean success = editor.commit(); // Gunakan commit() untuk memastikan perubahan langsung disimpan
        
        Log.d(TAG, "Saved theme mode: " + themeMode + ", success: " + success);
        
        // Verifikasi penyimpanan
        int savedMode = sharedPreferences.getInt(KEY_THEME_MODE, -1);
        Log.d(TAG, "Verified saved mode: " + savedMode);
    }

    /**
     * Get saved theme mode preference
     */
    public static int getThemeMode(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(
                PREFERENCES_NAME, Context.MODE_PRIVATE);

        // Default ke system mode jika belum ada preferensi
        int defaultMode = MODE_LIGHT; // Selalu default ke light

        int currentMode = sharedPreferences.getInt(KEY_THEME_MODE, defaultMode);
        Log.d(TAG, "Retrieved theme mode: " + currentMode);
        return currentMode;
    }

    /**
     * Check if dark mode is active
     */
    public static boolean isDarkMode(Context context) {
        int currentMode = getThemeMode(context);

        Log.d(TAG, "Checking if dark mode. Current mode: " + currentMode);

        if (currentMode == MODE_DARK) {
            return true;
        } else if (currentMode == MODE_LIGHT) {
            return false;
        } else {
            // Jika menggunakan MODE_SYSTEM, cek konfigurasi sistem
            boolean isSystemDark = (context.getResources().getConfiguration().uiMode &
                    android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                    android.content.res.Configuration.UI_MODE_NIGHT_YES;

            Log.d(TAG, "Using system setting. System dark mode: " + isSystemDark);
            return isSystemDark;
        }
    }

    /**
     * Toggle between light and dark themes
     */
    public static void toggleTheme(Context context) {
        boolean isDark = isDarkMode(context);
        Log.d(TAG, "Toggling theme. Currently dark mode: " + isDark);
        
        int newMode = isDark ? MODE_LIGHT : MODE_DARK;
        Log.d(TAG, "Switching to mode: " + newMode);
        
        // Force immediate apply
        saveThemeMode(context, newMode);
        
        // Paksa tema berubah segera
        applyTheme(newMode);
        
        // Log untuk debugging
        isDark = isDarkMode(context);
        Log.d(TAG, "After toggle, is dark mode: " + isDark);
    }
    

    /**
     * Initialize theme from saved preferences
     */
    public static void initTheme(Context context) {
        int themeMode = getThemeMode(context);
        Log.d(TAG, "Initializing theme with mode: " + themeMode);
        applyTheme(themeMode);
    }

    /**
     * Reset theme to default (light)
     */
    public static void resetTheme(Context context) {
        saveThemeMode(context, MODE_LIGHT);
        applyTheme(MODE_LIGHT);
    }
}