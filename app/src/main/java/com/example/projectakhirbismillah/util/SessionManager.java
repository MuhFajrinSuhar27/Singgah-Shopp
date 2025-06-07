package com.example.projectakhirbismillah.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class SessionManager {
    // SharedPreferences file name
    private static final String PREF_NAME = "ECommerceLogin";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_NAME = "name";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_ID = "id";
    private static final int PRIVATE_MODE = 0;

    private SharedPreferences pref;
    private Editor editor;
    private Context context;

    public SessionManager(Context context) {
        this.context = context;
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    /**
     * Create login session
     */
    public void createLoginSession(String id, String name, String email) {
        // Storing login value as TRUE
        editor.putBoolean(KEY_IS_LOGGED_IN, true);

        // Storing user data in pref
        editor.putString(KEY_ID, id);
        editor.putString(KEY_NAME, name);
        editor.putString(KEY_EMAIL, email);

        // commit changes
        editor.apply();
    }

    /**
     * Get stored session data
     */
    public String getUserId() {
        return pref.getString(KEY_ID, "");
    }

    public String getUserName() {
        return pref.getString(KEY_NAME, "");
    }

    public String getUserEmail() {
        return pref.getString(KEY_EMAIL, "");
    }

    /**
     * Check login method will check user login status
     * If false it will redirect user to login page
     * Else won't do anything
     * */
    public boolean isLoggedIn() {
        return pref.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    /**
     * Clear session details
     */
    public void logoutUser() {
        // Clearing all data from SharedPreferences
        editor.clear();
        editor.apply();
    }
}