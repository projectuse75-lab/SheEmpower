package com.sheempower.app.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferencesManager {

    private static final String PREFS_NAME = "she_empower_settings";
    private static final String KEY_NOTIFICATIONS = "notifications_enabled";
    private static final String KEY_DARK_THEME = "dark_theme_enabled";
    private static final String KEY_VOICE_MUTED = "voice_muted";
    private static final String KEY_APP_LANGUAGE = "app_language";
    private static final String KEY_HAS_SELECTED_LANG = "has_selected_lang";

    public static final String LANG_URDU = "ur";
    public static final String LANG_ENGLISH = "en";
    public static final String LANG_MIX = "mix";

    private final SharedPreferences sharedPreferences;

    public PreferencesManager(Context context) {
        this.sharedPreferences = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // ============ LANGUAGE ============
    public void setAppLanguage(String langMode) {
        sharedPreferences.edit().putString(KEY_APP_LANGUAGE, langMode).apply();
    }

    public String getAppLanguage() {
        return sharedPreferences.getString(KEY_APP_LANGUAGE, LANG_MIX); // Default: Mix
    }

    public boolean hasSelectedLanguage() {
        return sharedPreferences.getBoolean(KEY_HAS_SELECTED_LANG, false);
    }

    public void setHasSelectedLanguage(boolean selected) {
        sharedPreferences.edit().putBoolean(KEY_HAS_SELECTED_LANG, selected).apply();
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