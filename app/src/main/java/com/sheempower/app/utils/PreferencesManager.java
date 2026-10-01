package com.sheempower.app.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferencesManager {

    private static final String PREFS_NAME = "she_empower_settings";
    private static final String KEY_NOTIFICATIONS = "notifications_enabled";
    private static final String KEY_DARK_THEME = "dark_theme_enabled";
    private static final String KEY_VOICE_MUTED = "voice_muted";

    public static final String LANG_URDU = "ur";
    public static final String LANG_ENGLISH = "en";
    public static final String LANG_MIX = "mix";

    private final SharedPreferences sharedPreferences;

    public PreferencesManager(Context context) {
        this.sharedPreferences = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // ============ LANGUAGE (PERMANENT BILINGUAL DEFAULT) ============
    public void setAppLanguage(String langMode) {
        // App is permanently bilingual (English + Urdu)
    }

    public String getAppLanguage() {
        return LANG_MIX; // Always English + Urdu
    }

    public boolean hasSelectedLanguage() {
        return true; // Permanently configured
    }

    public void setHasSelectedLanguage(boolean selected) {
        // Permanently configured
    }

    // ============ NOTIFICATIONS ============
    public void setNotificationsEnabled(boolean enabled) {
        sharedPreferences.edit().putBoolean(KEY_NOTIFICATIONS, enabled).apply();
    }

    public boolean areNotificationsEnabled() {
        return sharedPreferences.getBoolean(KEY_NOTIFICATIONS, true);
    }

    // ============ DARK THEME ============
    public void setDarkThemeEnabled(boolean enabled) {
        sharedPreferences.edit().putBoolean(KEY_DARK_THEME, enabled).apply();
    }

    public boolean isDarkThemeEnabled() {
        return sharedPreferences.getBoolean(KEY_DARK_THEME, false);
    }

    // ============ VOICE GUIDE MUTE ============
    public void setVoiceMuted(boolean muted) {
        sharedPreferences.edit().putBoolean(KEY_VOICE_MUTED, muted).apply();
    }

    public boolean isVoiceMuted() {
        return sharedPreferences.getBoolean(KEY_VOICE_MUTED, false);
    }

    // ============ CLEAR ALL SETTINGS ============
    public void clear() {
        sharedPreferences.edit().clear().apply();
    }
}