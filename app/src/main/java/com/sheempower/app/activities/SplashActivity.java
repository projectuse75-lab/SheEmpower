package com.sheempower.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.sheempower.app.R;
import com.sheempower.app.utils.DemoDataSeeder;
import com.sheempower.app.utils.SessionManager;
import com.sheempower.app.utils.TTSHelper;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Seed demo data if needed
        DemoDataSeeder.seedIfNeeded(this);

        SessionManager session = new SessionManager(this);

        // ✅ TTS ready hone tak wait karein (2 second delay)
        new Handler().postDelayed(() -> {
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
        }, 2000); // ✅ 2 second delay for smooth transition
    }
}