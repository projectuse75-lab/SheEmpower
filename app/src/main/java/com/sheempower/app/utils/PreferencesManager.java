package com.sheempower.app.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

public class PreferencesManager {

    private static final String PREF_NAME = "she_empower_settings";

    private static final String KEY_NOTIFICATIONS = "notifications_enabled";
    private static final String KEY_DARK_THEME = "dark_theme_enabled";

    private final SharedPreferences sharedPreferences;

    public PreferencesManager(Context context) {
        sharedPreferences = context.getSharedPreferences(
                PREF_NAME,
                Context.MODE_PRIVATE
        );
    }

    // =========================
    // NOTIFICATIONS
    // =========================

    public void setNotificationsEnabled(boolean enabled) {
        sharedPreferences.edit()
                .putBoolean(KEY_NOTIFICATIONS, enabled)
                .apply();
    }

    public boolean areNotificationsEnabled() {
        return sharedPreferences.getBoolean(KEY_NOTIFICATIONS, true);
    }

    // =========================
    // DARK THEME
    // =========================

    public void setDarkThemeEnabled(boolean enabled) {
        sharedPreferences.edit()
                .putBoolean(KEY_DARK_THEME, enabled)
                .apply();
    }

    public boolean isDarkThemeEnabled() {
        return sharedPreferences.getBoolean(KEY_DARK_THEME, false);
    }

    // =========================
    // APPLY THEME
    // =========================

    public void applyTheme() {

        if (isDarkThemeEnabled()) {
            AppCompatDelegate.setDefaultNightMode(
                    AppCompatDelegate.MODE_NIGHT_YES
            );
        } else {
            AppCompatDelegate.setDefaultNightMode(
                    AppCompatDelegate.MODE_NIGHT_NO
            );
        }
    }

    // =========================
    // CLEAR SETTINGS
    // =========================

    public void clear() {
        sharedPreferences.edit()
                .clear()
                .apply();
    }
}