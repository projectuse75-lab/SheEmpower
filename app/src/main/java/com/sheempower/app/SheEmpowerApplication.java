package com.sheempower.app;

import android.app.Application;
import android.util.Log;

import androidx.appcompat.app.AppCompatDelegate;

import com.sheempower.app.utils.PreferencesManager;

public class SheEmpowerApplication extends Application {

    private static final String TAG = "SheEmpowerApplication";

    @Override
    public void onCreate() {
        super.onCreate();

        setupUncaughtExceptionHandler();

        PreferencesManager preferencesManager =
                new PreferencesManager(this);

        applySavedTheme(preferencesManager);
    }

    private void setupUncaughtExceptionHandler() {
        final Thread.UncaughtExceptionHandler defaultHandler =
                Thread.getDefaultUncaughtExceptionHandler();

        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            if (isSettingsProviderSecurityException(throwable)) {
                Log.w(TAG, "Suppressed framework SecurityException for user 999 settings provider: "
                        + throwable.getMessage());
                return;
            }

            if (defaultHandler != null) {
                defaultHandler.uncaughtException(thread, throwable);
            }
        });
    }

    private boolean isSettingsProviderSecurityException(Throwable throwable) {
        if (throwable == null) {
            return false;
        }
        if (throwable instanceof SecurityException) {
            String msg = throwable.getMessage();
            if (msg != null && (
                    msg.contains("Failed to find provider settings for user") ||
                    msg.contains("accessibility_enabled") ||
                    msg.contains("SettingsProvider") ||
                    msg.contains("user 999")
            )) {
                return true;
            }
        }
        return isSettingsProviderSecurityException(throwable.getCause());
    }

    private void applySavedTheme(
            PreferencesManager preferencesManager) {

        if (preferencesManager.isDarkThemeEnabled()) {

            AppCompatDelegate.setDefaultNightMode(
                    AppCompatDelegate.MODE_NIGHT_YES
            );

        } else {

            AppCompatDelegate.setDefaultNightMode(
                    AppCompatDelegate.MODE_NIGHT_NO
            );
        }
    }
}