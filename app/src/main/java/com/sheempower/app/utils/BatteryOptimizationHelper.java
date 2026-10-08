package com.sheempower.app.utils;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;

public class BatteryOptimizationHelper {

    private static final String PREFS_NAME = "battery_opt_prefs";
    private static final String KEY_PROMPTED = "battery_opt_prompted";
    private static final String TAG = "BatteryOptHelper";

    /**
     * Checks if battery optimization is disabled. If not and user hasn't been prompted yet,
     * shows a friendly Urdu dialog explaining why it's needed for order notifications.
     */
    public static void checkAndPrompt(Activity activity) {
        if (activity == null || activity.isFinishing()) return;

        Context context = activity.getApplicationContext();

        // Check if battery optimization is already ignored
        if (isIgnoringBatteryOptimizations(context)) {
            Log.d(TAG, "Battery optimization is already disabled.");
            return;
        }

        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (prefs.getBoolean(KEY_PROMPTED, false)) {
            return;
        }

        prefs.edit().putBoolean(KEY_PROMPTED, true).apply();

        new AlertDialog.Builder(activity)
                .setTitle("🔔 آرڈر کی اطلاعات بروقت حاصل کریں / Order Notifications")
                .setMessage("بیچنے والی کے لیے ضروری اطلاع:\n\n" +
                        "بعض موبائلز (Infinix, Tecno, Vivo, Oppo, Xiaomi) آرڈر کی اطلاعات کو بیک گراؤنڈ میں روک دیتے ہیں۔\n\n" +
                        "نئے آرڈر کی نوٹیفکیشن بروقت حاصل کرنے کے لیے 'بیٹری آپٹیمائزیشن' (Battery Optimization) کو بند کرنا ضروری ہے۔\n\n" +
                        "کیا آپ ابھی اس کی اجازت دینا چاہتی ہیں؟")
                .setPositiveButton("ہاں، اجازت دیں / Allow", (dialog, which) -> {
                    requestIgnoreBatteryOptimization(activity);
                    promptOemAutostartIfNeeded(activity);
                })
                .setNegativeButton("بعد میں / Later", null)
                .setCancelable(false)
                .show();
    }

    public static boolean isIgnoringBatteryOptimizations(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (pm != null) {
                return pm.isIgnoringBatteryOptimizations(context.getPackageName());
            }
        }
        return true;
    }

    public static void requestIgnoreBatteryOptimization(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                Intent intent = new Intent();
                String packageName = activity.getPackageName();
                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + packageName));
                activity.startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Failed to launch battery optimization intent: " + e.getMessage());
                try {
                    Intent fallback = new Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS);
                    activity.startActivity(fallback);
                } catch (Exception ignored) {}
            }
        }
    }

    /**
     * Shows OEM specific autostart / background setting guide for Xiaomi, Vivo, Oppo, Infinix, Tecno, Realme.
     */
    public static void promptOemAutostartIfNeeded(Activity activity) {
        if (activity == null || activity.isFinishing()) return;

        String manufacturer = Build.MANUFACTURER != null ? Build.MANUFACTURER.toLowerCase() : "";
        String title = "⚙️ آٹو سٹارٹ سیٹنگز / Background Settings";
        String message = getOemInstructionMessage(manufacturer);

        if (message == null || message.isEmpty()) return;

        new AlertDialog.Builder(activity)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("سیٹنگز کھولیں / Open Settings", (dialog, which) -> openOemAutostartSettings(activity))
                .setNegativeButton("ٹھیک ہے / OK", null)
                .show();
    }

    public static String getOemInstructionMessage(String manufacturer) {
        if (manufacturer == null) return null;
        manufacturer = manufacturer.toLowerCase();

        if (manufacturer.contains("xiaomi") || manufacturer.contains("redmi")) {
            return "شاومی (Xiaomi/Redmi) موبائل گائیڈ:\n\n" +
                    "1. سیٹنگز کھولیں اور 'Autostart' کو آن کریں\n" +
                    "2. Battery Saver میں جا کر 'No restrictions' منتخب کریں";
        } else if (manufacturer.contains("vivo")) {
            return "ویوو (Vivo) موبائل گائیڈ:\n\n" +
                    "1. 'Autostart' یا 'High background power consumption' آن کریں\n" +
                    "2. App Manager میں جا کر 'Background app refresh' کو اجازت دیں";
        } else if (manufacturer.contains("oppo") || manufacturer.contains("realme")) {
            return "اوپو / ریئل می (Oppo/Realme) موبائل گائیڈ:\n\n" +
                    "1. App Management میں جا کر 'Allow Auto Startup' آن کریں\n" +
                    "2. Recent Apps میں ایپ کو لاک (Lock) کریں";
        } else if (manufacturer.contains("infinix") || manufacturer.contains("tecno") || manufacturer.contains("itel")) {
            return "انفینکس / ٹیکنو (Infinix/Tecno) موبائل گائیڈ:\n\n" +
                    "1. Phone Master ایپ کھولیں\n" +
                    "2. Auto-start Management میں She Empower کو آن کریں";
        } else if (manufacturer.contains("samsung")) {
            return "سیمسنگ (Samsung) موبائل گائیڈ:\n\n" +
                    "1. Settings -> Device Care -> Battery میں جائیں\n" +
                    "2. Never sleeping apps میں She Empower کو شامل کریں";
        } else if (manufacturer.contains("huawei") || manufacturer.contains("honor")) {
            return "ہواوے (Huawei) موبائل گائیڈ:\n\n" +
                    "1. Settings -> Battery -> App Launch میں جائیں\n" +
                    "2. She Empower کی پرمیشنز کو Manual کریں";
        }
        return null;
    }

    public static void openOemAutostartSettings(Activity activity) {
        if (activity == null) return;
        String manufacturer = Build.MANUFACTURER != null ? Build.MANUFACTURER.toLowerCase() : "";
        Intent intent = new Intent();

        try {
            if (manufacturer.contains("xiaomi") || manufacturer.contains("redmi")) {
                intent.setComponent(new ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"));
            } else if (manufacturer.contains("oppo")) {
                intent.setComponent(new ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"));
            } else if (manufacturer.contains("realme")) {
                intent.setComponent(new ComponentName("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity"));
            } else if (manufacturer.contains("vivo")) {
                intent.setComponent(new ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"));
            } else if (manufacturer.contains("huawei") || manufacturer.contains("honor")) {
                intent.setComponent(new ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"));
            } else if (manufacturer.contains("infinix") || manufacturer.contains("tecno")) {
                intent.setComponent(new ComponentName("com.transsion.phonemaster", "com.transsion.phonemaster.autostart.AutoStartActivity"));
            } else {
                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + activity.getPackageName()));
            }
            activity.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Failed to launch OEM intent: " + e.getMessage());
            try {
                Intent fallback = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                fallback.setData(Uri.parse("package:" + activity.getPackageName()));
                activity.startActivity(fallback);
            } catch (Exception ignored) {}
        }
    }
}