package com.sheempower.app.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferencesManager {

    private static final String PREFS_NAME = "she_empower_settings";
    private static final String KEY_NOTIFICATIONS = "notifications_enabled";
    private static final String KEY_DARK_THEME = "dark_theme_enabled";

    private SharedPreferences sharedPreferences;
    private Context context;

    public PreferencesManager(Context context) {
        this.context = context;
        this.sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // ============ NOTIFICATIONS ============
    public void setNotificationsEnabled(boolean enabled) {
        sharedPreferences.edit().putBoolean(KEY_NOTIFICATIONS, enabled).apply();
    }

    public boolean areNotificationsEnabled() {
        return sharedPreferences.getBoolean(KEY_NOTIFICATIONS, true); // Default: true
    }

    // ============ DARK THEME ============
    public void setDarkThemeEnabled(boolean enabled) {
        sharedPreferences.edit().putBoolean(KEY_DARK_THEME, enabled).apply();
    }

    public boolean isDarkThemeEnabled() {
        return sharedPreferences.getBoolean(KEY_DARK_THEME, false); // Default: false (light theme)
    }

    // ============ CLEAR ALL SETTINGS ============
    public void clear() {
        sharedPreferences.edit().clear().apply();
    }
}