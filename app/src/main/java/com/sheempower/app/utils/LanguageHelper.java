package com.sheempower.app.utils;

import android.content.Context;
import androidx.appcompat.app.AlertDialog;

public class LanguageHelper {

    public interface OnLanguageSelectedListener {
        void onLanguageSelected(String langMode);
    }

    public static String getFormattedText(Context ctx, String urdu, String english) {
        if (ctx == null) return urdu + " / " + english;
        PreferencesManager prefs = new PreferencesManager(ctx);
        String mode = prefs.getAppLanguage();

        if (PreferencesManager.LANG_URDU.equals(mode)) {
            return urdu != null ? urdu : "";
        } else if (PreferencesManager.LANG_ENGLISH.equals(mode)) {
            return english != null ? english : "";
        } else {
            if (urdu != null && !urdu.isEmpty() && english != null && !english.isEmpty()) {
                return urdu + " / " + english;
            } else if (urdu != null && !urdu.isEmpty()) {
                return urdu;
            } else {
                return english != null ? english : "";
            }
        }
    }

    public static String processDualString(Context ctx, String combinedText) {
        if (combinedText == null) return "";
        if (!combinedText.contains(" / ")) {
            return combinedText;
        }
        String[] parts = combinedText.split(" / ", 2);
        String urdu = parts[0].trim();
        String english = parts.length > 1 ? parts[1].trim() : "";
        return getFormattedText(ctx, urdu, english);
    }

    public static String getLanguageDisplayName(String mode) {
        if (PreferencesManager.LANG_URDU.equals(mode)) {
            return "🇵🇰 اردو (Urdu Only)";
        } else if (PreferencesManager.LANG_ENGLISH.equals(mode)) {
            return "🇬🇧 English (English Only)";
        } else {
            return "🇵🇰🇬🇧 اردو + English (Mix)";
        }
    }

    public static void showLanguageSelectionDialog(Context ctx, OnLanguageSelectedListener listener) {
        if (ctx == null) return;
        PreferencesManager prefs = new PreferencesManager(ctx);
        String currentMode = prefs.getAppLanguage();

        String[] options = {
                "🇵🇰 اردو (Urdu Only)",
                "🇬🇧 English (English Only)",
                "🇵🇰🇬🇧 اردو + English (Mix)"
        };

        int selectedIndex = 2;
        if (PreferencesManager.LANG_URDU.equals(currentMode)) selectedIndex = 0;
        else if (PreferencesManager.LANG_ENGLISH.equals(currentMode)) selectedIndex = 1;

        new AlertDialog.Builder(ctx)
                .setTitle("🌐 Select App Language / زبان منتخب کریں")
                .setSingleChoiceItems(options, selectedIndex, (dialog, which) -> {
                    String chosenMode = PreferencesManager.LANG_MIX;
                    if (which == 0) chosenMode = PreferencesManager.LANG_URDU;
                    else if (which == 1) chosenMode = PreferencesManager.LANG_ENGLISH;

                    prefs.setAppLanguage(chosenMode);
                    prefs.setHasSelectedLanguage(true);
                    dialog.dismiss();

                    if (listener != null) {
                        listener.onLanguageSelected(chosenMode);
                    }
                })
                .setNegativeButton("منسوخ کریں / Cancel", null)
                .show();
    }
}