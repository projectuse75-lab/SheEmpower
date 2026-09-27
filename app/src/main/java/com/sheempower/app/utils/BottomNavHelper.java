package com.sheempower.app.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.sheempower.app.R;
import com.sheempower.app.activities.AdminDashboardActivity;
import com.sheempower.app.activities.AdminOrdersActivity;
import com.sheempower.app.activities.MainActivity;
import com.sheempower.app.activities.MyOrdersActivity;
import com.sheempower.app.activities.ProfileActivity;
import com.sheempower.app.activities.SettingsActivity;
import com.sheempower.app.activities.SellerDashboardActivity;
import com.sheempower.app.activities.SellerOrdersActivity;

public class BottomNavHelper {

    public static void setupForBuyerOrVisitor(
            Context context,
            View view,
            int selectedIndex) {

        if (!(view instanceof LinearLayout)) return;

        LinearLayout navBar = (LinearLayout) view;

        setupLabels(navBar, false);
        setSelectedItem(navBar, selectedIndex);

        setupClickListeners(
                context,
                navBar,
                selectedIndex,
                false
        );
    }


    public static void setupForSeller(
            Context context,
            View view,
            int selectedIndex) {

        if (!(view instanceof LinearLayout)) return;

        LinearLayout navBar = (LinearLayout) view;

        setupLabels(navBar, true);
        setSelectedItem(navBar, selectedIndex);

        setupClickListeners(
                context,
                navBar,
                selectedIndex,
                true
        );
    }


    public static void setupForAdmin(
            Context context,
            View view,
            int selectedIndex) {

        if (!(view instanceof LinearLayout)) return;

        LinearLayout navBar = (LinearLayout) view;

        setupLabels(navBar, false);
        setSelectedItem(navBar, selectedIndex);

        setupAdminClickListeners(
                context,
                navBar,
                selectedIndex
        );
    }


    private static void setupLabels(
            LinearLayout navBar,
            boolean sellerMode) {

        TextView icon1 = navBar.findViewById(R.id.navIcon1);
        TextView label1 = navBar.findViewById(R.id.navLabel1);

        TextView icon2 = navBar.findViewById(R.id.navIcon2);
        TextView label2 = navBar.findViewById(R.id.navLabel2);

        TextView icon3 = navBar.findViewById(R.id.navIcon3);
        TextView label3 = navBar.findViewById(R.id.navLabel3);

        TextView icon4 = navBar.findViewById(R.id.navIcon4);
        TextView label4 = navBar.findViewById(R.id.navLabel4);

        icon1.setText("🏠");
        label1.setText(sellerMode ? "ہوم" : "براؤز");

        icon2.setText("📦");
        label2.setText("آرڈرز");

        icon3.setText("👤");
        label3.setText("پروفائل");

        icon4.setText("⚙️");
        label4.setText("سیٹنگز");

        int normalColor =
                ContextCompat.getColor(
                        navBar.getContext(),
                        R.color.screen_text
                );

        label1.setTextColor(normalColor);
        label2.setTextColor(normalColor);
        label3.setTextColor(normalColor);
        label4.setTextColor(normalColor);
    }


    private static void setSelectedItem(
            LinearLayout navBar,
            int selectedIndex) {

        View item1 = navBar.findViewById(R.id.navItem1);
        View item2 = navBar.findViewById(R.id.navItem2);
        View item3 = navBar.findViewById(R.id.navItem3);
        View item4 = navBar.findViewById(R.id.navItem4);

        item1.setAlpha(0.65f);
        item2.setAlpha(0.65f);
        item3.setAlpha(0.65f);
        item4.setAlpha(0.65f);

        TextView label1 = navBar.findViewById(R.id.navLabel1);
        TextView label2 = navBar.findViewById(R.id.navLabel2);
        TextView label3 = navBar.findViewById(R.id.navLabel3);
        TextView label4 = navBar.findViewById(R.id.navLabel4);

        int normalColor =
                ContextCompat.getColor(
                        navBar.getContext(),
                        R.color.screen_text
                );

        int selectedColor =
                ContextCompat.getColor(
                        navBar.getContext(),
                        R.color.primary_pink
                );

        label1.setTextColor(normalColor);
        label2.setTextColor(normalColor);
        label3.setTextColor(normalColor);
        label4.setTextColor(normalColor);

        switch (selectedIndex) {

            case 1:
                item1.setAlpha(1.0f);
                label1.setTextColor(selectedColor);
                break;

            case 2:
                item2.setAlpha(1.0f);
                label2.setTextColor(selectedColor);
                break;

            case 3:
                item3.setAlpha(1.0f);
                label3.setTextColor(selectedColor);
                break;

            case 4:
                item4.setAlpha(1.0f);
                label4.setTextColor(selectedColor);
                break;
        }
    }


    private static void setupClickListeners(
            Context context,
            LinearLayout navBar,
            int selectedIndex,
            boolean sellerMode) {

        View item1 = navBar.findViewById(R.id.navItem1);
        View item2 = navBar.findViewById(R.id.navItem2);
        View item3 = navBar.findViewById(R.id.navItem3);
        View item4 = navBar.findViewById(R.id.navItem4);


        item1.setOnClickListener(v -> {

            if (selectedIndex == 1) return;

            Intent intent;

            if (sellerMode) {
                intent = new Intent(
                        context,
                        SellerDashboardActivity.class
                );
            } else {
                intent = new Intent(
                        context,
                        MainActivity.class
                );
            }

            openScreen(context, intent);
        });


        item2.setOnClickListener(v -> {

            if (selectedIndex == 2) return;

            Intent intent;

            if (sellerMode) {
                intent = new Intent(
                        context,
                        SellerOrdersActivity.class
                );
            } else {
                intent = new Intent(
                        context,
                        MyOrdersActivity.class
                );
            }

            openScreen(context, intent);
        });


        item3.setOnClickListener(v -> {

            if (selectedIndex == 3) return;

            Intent intent = new Intent(
                    context,
                    ProfileActivity.class
            );

            openScreen(context, intent);
        });


        item4.setOnClickListener(v -> {

            if (selectedIndex == 4) return;

            Intent intent = new Intent(
                    context,
                    SettingsActivity.class
            );

            openScreen(context, intent);
        });
    }


    private static void setupAdminClickListeners(
            Context context,
            LinearLayout navBar,
            int selectedIndex) {

        View item1 = navBar.findViewById(R.id.navItem1);
        View item2 = navBar.findViewById(R.id.navItem2);
        View item3 = navBar.findViewById(R.id.navItem3);
        View item4 = navBar.findViewById(R.id.navItem4);


        item1.setOnClickListener(v -> {

            if (selectedIndex == 1) return;

            openScreen(
                    context,
                    new Intent(
                            context,
                            AdminDashboardActivity.class
                    )
            );
        });


        item2.setOnClickListener(v -> {

            if (selectedIndex == 2) return;

            openScreen(
                    context,
                    new Intent(
                            context,
                            AdminOrdersActivity.class
                    )
            );
        });


        item3.setOnClickListener(v -> {

            if (selectedIndex == 3) return;

            openScreen(
                    context,
                    new Intent(
                            context,
                            ProfileActivity.class
                    )
            );
        });


        item4.setOnClickListener(v -> {

            if (selectedIndex == 4) return;

            openScreen(
                    context,
                    new Intent(
                            context,
                            SettingsActivity.class
                    )
            );
        });
    }


    private static void openScreen(
            Context context,
            Intent intent) {

        context.startActivity(intent);

        if (context instanceof Activity) {
            ((Activity) context)
                    .overridePendingTransition(0, 0);
        }
    }
}