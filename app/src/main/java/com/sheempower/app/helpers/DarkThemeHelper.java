package com.sheempower.app.helpers;

import android.app.Activity;

import com.sheempower.app.R;
import com.sheempower.app.utils.PreferencesManager;

public class DarkThemeHelper {

    public static void applyTheme(Activity activity) {
        if (activity == null) return;
        PreferencesManager prefs = new PreferencesManager(activity);
        if (prefs.isDarkThemeEnabled()) {
            activity.setTheme(R.style.Theme_SheEmpower_Dark);
        } else {
            activity.setTheme(R.style.Theme_SheEmpower);
        }
    }
}
