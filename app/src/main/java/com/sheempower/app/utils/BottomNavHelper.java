package com.sheempower.app.utils;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.sheempower.app.R;
import com.sheempower.app.activities.*;

public class BottomNavHelper {

    public static void setupBottomNav(Activity activity, SessionManager session) {
        LinearLayout navHome = activity.findViewById(R.id.navHome);
        LinearLayout navCenter = activity.findViewById(R.id.navCenter);
        LinearLayout navProfile = activity.findViewById(R.id.navProfile);

        TextView navIcon2 = activity.findViewById(R.id.navIcon2);
        TextView navLabel2 = activity.findViewById(R.id.navLabel2);

        // ============================================================
        // ✅ ROLE-BASED CONFIGURATION FOR CENTER TAB
        // ============================================================
        String role = session.getRole();
        boolean isLoggedIn = session.isLoggedIn();

        if (isLoggedIn && Constants.ROLE_SELLER.equals(role)) {
            navIcon2.setText("📊");
            navLabel2.setText("ڈیش بورڈ");
        } else if (isLoggedIn && Constants.ROLE_ADMIN.equals(role)) {
            navIcon2.setText("👥");
            navLabel2.setText("بیچنے والے");
        } else {
            navIcon2.setText("📦");
            navLabel2.setText("آرڈرز");
        }

        // ============================================================
        // ✅ TAB 1: HOME / BROWSE
        // ============================================================
        navHome.setOnClickListener(v -> {
            if (!(activity instanceof MainActivity)) {
                activity.startActivity(new Intent(activity, MainActivity.class));
                activity.finish();
            }
        });

        // ============================================================
        // ✅ TAB 2: CENTER (Dynamic)
        // ============================================================
        navCenter.setOnClickListener(v -> {
            if (!isLoggedIn) {
                Toast.makeText(activity, "پہلے لاگ ان کریں / Please login first", Toast.LENGTH_SHORT).show();
                activity.startActivity(new Intent(activity, LoginActivity.class));
                return;
            }

            if (Constants.ROLE_SELLER.equals(role)) {
                if (!(activity instanceof SellerDashboardActivity)) {
                    activity.startActivity(new Intent(activity, SellerDashboardActivity.class));
                    activity.finish();
                }
            } else if (Constants.ROLE_ADMIN.equals(role)) {
                if (!(activity instanceof AdminDashboardActivity)) {
                    activity.startActivity(new Intent(activity, AdminDashboardActivity.class));
                    activity.finish();
                }
            } else {
                // Buyer or Visitor
                if (!(activity instanceof MyOrdersActivity)) {
                    activity.startActivity(new Intent(activity, MyOrdersActivity.class));
                    activity.finish();
                }
            }
        });

        // ============================================================
        // ✅ TAB 3: PROFILE
        // ============================================================
        navProfile.setOnClickListener(v -> {
            if (!isLoggedIn) {
                Toast.makeText(activity, "پہلے لاگ ان کریں / Please login first", Toast.LENGTH_SHORT).show();
                activity.startActivity(new Intent(activity, LoginActivity.class));
                return;
            }

            if (!(activity instanceof ProfileActivity)) {
                activity.startActivity(new Intent(activity, ProfileActivity.class));
                activity.finish();
            }
        });
    }

    // ============================================================
    // ✅ HELPER: SET SELECTED TAB (HIGHLIGHT)
    // ============================================================
    public static void setSelectedTab(Activity activity, int selected) {
        // 1 = Home, 2 = Center, 3 = Profile
        TextView label1 = activity.findViewById(R.id.navLabel1);
        TextView label2 = activity.findViewById(R.id.navLabel2);
        TextView label3 = activity.findViewById(R.id.navLabel3);

        // Reset all
        label1.setTextColor(0xFF888888);
        label2.setTextColor(0xFF888888);
        label3.setTextColor(0xFF888888);

        // Highlight selected
        if (selected == 1) label1.setTextColor(0xFFFF4081);
        else if (selected == 2) label2.setTextColor(0xFFFF4081);
        else if (selected == 3) label3.setTextColor(0xFFFF4081);
    }
}