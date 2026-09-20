package com.sheempower.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;
import com.google.firebase.auth.FirebaseAuth;
import com.sheempower.app.R;
import com.sheempower.app.utils.DemoDataSeeder;
import com.sheempower.app.utils.SessionManager;
import com.sheempower.app.utils.PreferencesManager;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyTheme();
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // ✅ Seed only if no real data yet
        // DemoDataSeeder.forceSeed(this);

        try {
            FirebaseAuth auth = FirebaseAuth.getInstance();
            if (auth.getCurrentUser() == null) {
                auth.signInAnonymously().addOnCompleteListener(task -> {
                    navigate();
                });
            } else {
                navigate();
            }
        } catch (Exception e) {
            Log.e("Splash", "Auth error", e);
            navigate();
        }
    }

    private void navigate() {
        new Handler().postDelayed(() -> {
            try {
                SessionManager session = new SessionManager(this);
                Intent intent;
                if (session.isLoggedIn()) {
                    if (session.isSeller()) {
                        intent = new Intent(this, SellerDashboardActivity.class);
                    } else if (session.isAdmin()) {
                        intent = new Intent(this, AdminDashboardActivity.class);
                    } else {
                        intent = new Intent(this, MainActivity.class);
                    }
                } else {
                    intent = new Intent(this, MainActivity.class);
                }
                startActivity(intent);
                finish();
            } catch (Exception e) {
                Log.e("Splash", "Navigation error", e);
                startActivity(new Intent(this, MainActivity.class));
                finish();
            }
        }, 1000);
    }

    // ✅ DARK THEME - ADD AT END OF CLASS
    private void applyTheme() {
        PreferencesManager prefs = new PreferencesManager(this);
        if (prefs.isDarkThemeEnabled()) {
            setTheme(R.style.Theme_SheEmpower_Dark);
        } else {
            setTheme(R.style.Theme_SheEmpower);
        }
    }
}