package com.sheempower.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import com.sheempower.app.R;
import com.sheempower.app.utils.DemoDataSeeder;
import com.sheempower.app.utils.SessionManager;
import com.sheempower.app.utils.VoiceGuide;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        try {
            DemoDataSeeder.seedIfNeeded(this);
        } catch (Exception e) {
            Log.e("SplashActivity", "Demo seeding failed: " + e.getMessage(), e);
        }

        VoiceGuide.announceScreen(this, "شی ایمپاور میں خوش آمدید");

        SessionManager session = new SessionManager(this);

        new Handler().postDelayed(() -> {
            try {
                Intent intent;
                if (session.isLoggedIn()) {
                    if (session.isSeller()) intent = new Intent(this, SellerDashboardActivity.class);
                    else if (session.isAdmin()) intent = new Intent(this, AdminDashboardActivity.class);
                    else intent = new Intent(this, MainActivity.class);
                } else {
                    intent = new Intent(this, MainActivity.class);
                }
                startActivity(intent);
            } catch (Exception e) {
                Log.e("SplashActivity", "Navigation failed: " + e.getMessage(), e);
                startActivity(new Intent(this, MainActivity.class));
            } finally {
                finish();
            }
        }, 800);
    }
}