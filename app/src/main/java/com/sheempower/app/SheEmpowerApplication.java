package com.sheempower.app;

import android.app.Application;
import com.google.firebase.database.FirebaseDatabase;

public class SheEmpowerApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        try {
            // Enable Firebase persistence for offline caching
            FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        } catch (Exception e) {
            // Already enabled or error - ignore
        }
    }
}