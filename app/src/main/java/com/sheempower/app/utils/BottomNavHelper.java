package com.sheempower.app.utils;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import com.sheempower.app.R;
import com.sheempower.app.activities.*;

public class BottomNavHelper {

    public static void setupForBuyerOrVisitor(Activity activity, View navBar, int activeIndex) {
        SessionManager session = new SessionManager(activity);
        boolean loggedIn = session.isLoggedIn();

        setItem(navBar, 1, "🏠", "براؤز", activeIndex == 1, v -> {
            if (!(activity instanceof MainActivity)) activity.startActivity(new Intent(activity, MainActivity.class));
        });
        setItem(navBar, 2, "📦", "آرڈرز", activeIndex == 2, v -> {
            if (loggedIn) activity.startActivity(new Intent(activity, MyOrdersActivity.class));
            else { Toast.makeText(activity, "پہلے لاگ ان کریں", Toast.LENGTH_SHORT).show(); activity.startActivity(new Intent(activity, LoginActivity.class)); }
        });
        setItem(navBar, 3, "👤", "پروفائل", activeIndex == 3, v -> {
            if (loggedIn) activity.startActivity(new Intent(activity, ProfileActivity.class));
            else { Toast.makeText(activity, "پہلے لاگ ان کریں", Toast.LENGTH_SHORT).show(); activity.startActivity(new Intent(activity, LoginActivity.class)); }
        });
    }

    public static void setupForSeller(Activity activity, View navBar, int activeIndex) {
        setItem(navBar, 1, "📊", "ڈیش بورڈ", activeIndex == 1, v -> {
            if (!(activity instanceof SellerDashboardActivity)) activity.startActivity(new Intent(activity, SellerDashboardActivity.class));
        });
        setItem(navBar, 2, "📋", "آرڈرز", activeIndex == 2, v -> {
            if (!(activity instanceof SellerOrdersActivity)) activity.startActivity(new Intent(activity, SellerOrdersActivity.class));
        });
        setItem(navBar, 3, "👤", "پروفائل", activeIndex == 3, v -> activity.startActivity(new Intent(activity, ProfileActivity.class)));
    }

    public static void setupForAdmin(Activity activity, View navBar, int activeIndex) {
        setItem(navBar, 1, "✅", "منظوریاں", activeIndex == 1, v -> {
            if (!(activity instanceof AdminDashboardActivity)) activity.startActivity(new Intent(activity, AdminDashboardActivity.class));
        });
        setItem(navBar, 2, "📦", "آرڈرز", activeIndex == 2, v -> {
            if (!(activity instanceof AdminOrdersActivity)) activity.startActivity(new Intent(activity, AdminOrdersActivity.class));
        });
        setItem(navBar, 3, "🚪", "لاگ آؤٹ", activeIndex == 3, v -> {
            new SessionManager(activity).logout();
            activity.startActivity(new Intent(activity, MainActivity.class));
            activity.finish();
        });
    }

    private static void setItem(View navBar, int index, String icon, String label, boolean active, View.OnClickListener listener) {
        int iconId, labelId, itemId;
        switch (index) {
            case 1: iconId = R.id.navIcon1; labelId = R.id.navLabel1; itemId = R.id.navItem1; break;
            case 2: iconId = R.id.navIcon2; labelId = R.id.navLabel2; itemId = R.id.navItem2; break;
            default: iconId = R.id.navIcon3; labelId = R.id.navLabel3; itemId = R.id.navItem3; break;
        }
        TextView iconView = navBar.findViewById(iconId);
        TextView labelView = navBar.findViewById(labelId);
        View item = navBar.findViewById(itemId);
        iconView.setText(icon);
        labelView.setText(label);
        labelView.setTextColor(active ? 0xFFFF4081 : 0xFF888888);
        item.setOnClickListener(listener);
    }
}