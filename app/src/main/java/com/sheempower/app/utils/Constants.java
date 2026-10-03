package com.sheempower.app.utils;

import android.content.Context;

import com.google.firebase.database.DataSnapshot;
import com.sheempower.app.R;
import com.sheempower.app.models.Seller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Constants {
    public static final String NODE_USERS    = "users";
    public static final String NODE_SELLERS  = "sellers";
    public static final String NODE_BUYERS   = "buyers";
    public static final String NODE_ORDERS   = "orders";
    public static final String NODE_PRODUCTS = "products";
    public static final String NODE_META     = "meta";

    // ✅ NAYA: Notifications Node
    public static final String NODE_NOTIFICATIONS = "notifications";

    // ✅ NAYA: OneSignal Configuration
    public static final String ONESIGNAL_APP_ID = "c2bd81e9-0481-4600-8f2d-2d59a75de5cd";
    public static final String ONESIGNAL_REST_API_KEY = "os_v2_app_yk6yd2ieqfdabdznfvm2oxpfzurxckkrirpegi4o3y655tnxoilnhzglqbo5gftkoajy25njavv3fvom2lfjq56hzc6ldrvsjldxidy";
    public static final String KEY_ONESIGNAL_PLAYER_ID = "oneSignalPlayerId";

    // ✅ NAYA: Notification Channel
    public static final String CHANNEL_ID = "she_empower_orders";
    public static final String CHANNEL_NAME = "Order Notifications";

    public static final String CLOUDINARY_CLOUD_NAME    = "lstovyxq";
    public static final String CLOUDINARY_UPLOAD_PRESET = "sheempower_preset";
    public static final String CLOUDINARY_URL =
            "https://api.cloudinary.com/v1_1/" + CLOUDINARY_CLOUD_NAME + "/auto/upload";

    public static final String ADMIN_EMAIL    = "admin@sheempower.app";
    public static final String ADMIN_PASSWORD = "Admin@123";

    public static final String ROLE_BUYER   = "buyer";
    public static final String ROLE_SELLER  = "seller";
    public static final String ROLE_ADMIN   = "admin";

    public static final String DELIVERY_METHOD_DELIVERY = "Delivery";
    public static final String DELIVERY_METHOD_PICKUP   = "Self Pickup";

    public static final String STATUS_PENDING     = "Pending";
    public static final String STATUS_IN_PROGRESS = "In Progress";
    public static final String STATUS_COMPLETED   = "Completed";
    public static final String[] STATUS_ORDER = { STATUS_PENDING, STATUS_IN_PROGRESS, STATUS_COMPLETED };

    public static String getStatusLabel(String status, String deliveryMethod) {
        boolean isPickup = DELIVERY_METHOD_PICKUP.equals(deliveryMethod);
        switch (status) {
            case STATUS_PENDING:     return "زیر التواء / Not Started";
            case STATUS_IN_PROGRESS: return "کام جاری ہے / In Progress";
            case STATUS_COMPLETED:   return isPickup ? "مکمل — خریدار خود لے جائے" : "مکمل — ڈیلیوری کریں";
            default: return status;
        }
    }

    public static boolean isApproved(String verificationStatus) {
        if (verificationStatus == null || verificationStatus.isEmpty()) return true;
        return "verified".equalsIgnoreCase(verificationStatus) ||
                "approved".equalsIgnoreCase(verificationStatus) ||
                "active".equalsIgnoreCase(verificationStatus);
    }

    public static final String[] CATEGORIES = {"Cooking", "Stitching", "Mehndi", "Makeup", "Embroidery"};
    public static final String[] CATEGORY_LABELS_UR = {"کھانا پکانا", "سلائی", "مہندی", "میک اپ", "کڑھائی"};

    public static final String[] CATEGORY_IMAGES = {
            "https://images.unsplash.com/photo-1563379091339-03246963d96a?w=300&q=60",
            "https://images.unsplash.com/photo-1594633312681-425c7b97ccd1?w=300&q=60",
            "https://images.unsplash.com/photo-1622398925373-3f91b1e275f5?w=300&q=60",
            "https://images.unsplash.com/photo-1522335789203-aabd1fc54bc9?w=300&q=60",
            "https://images.unsplash.com/photo-1610701596007-11502861dcfa?w=300&q=60"
    };

    public static final int[] CATEGORY_IMAGE_RES = {
            R.drawable.cat_cooking,
            R.drawable.cat_stitching,
            R.drawable.cat_mehndi,
            R.drawable.cat_makeup,
            R.drawable.cat_embroidery
    };

    private static final List<String> TRAVEL_TYPE_CATEGORIES = Arrays.asList("Mehndi", "Makeup");
    public static boolean isTravelType(String category) { return TRAVEL_TYPE_CATEGORIES.contains(category); }

    public static String getUrduLabel(String categoryKey) {
        for (int i = 0; i < CATEGORIES.length; i++) if (CATEGORIES[i].equalsIgnoreCase(categoryKey)) return CATEGORY_LABELS_UR[i];
        return categoryKey;
    }

    public static String getCategoryDisplayLabel(Context ctx, String categoryKey) {
        String urdu = getUrduLabel(categoryKey);
        return LanguageHelper.getFormattedText(ctx, urdu, categoryKey);
    }

    // ============================================================
    // ✅ CATEGORY MATCHING HELPER (SINGLE SOURCE OF TRUTH)
    // ============================================================
    public static boolean sellerProvidesCategory(Seller seller, String category) {
        if (seller == null) return false;
        if (category == null || category.trim().isEmpty()) {
            return true;
        }
        String pCat = category.trim();

        List<String> cats = seller.getCategories();
        if (cats != null && !cats.isEmpty()) {
            for (String c : cats) {
                if (c != null && pCat.equalsIgnoreCase(c.trim())) {
                    return true;
                }
            }
        }

        String primaryCat = seller.getCategory();
        if (primaryCat != null && !primaryCat.trim().isEmpty()) {
            return pCat.equalsIgnoreCase(primaryCat.trim());
        }

        return false;
    }

    public static List<String> getActiveSellerCategories(Seller seller) {
        List<String> activeCats = new ArrayList<>();
        if (seller == null) return activeCats;

        List<String> cats = seller.getCategories();
        if (cats != null && !cats.isEmpty()) {
            for (String c : cats) {
                if (c != null && !c.trim().isEmpty()) {
                    String clean = c.trim();
                    if (!containsIgnoreCase(activeCats, clean)) {
                        activeCats.add(clean);
                    }
                }
            }
        }

        String primaryCat = seller.getCategory();
        if (primaryCat != null && !primaryCat.trim().isEmpty()) {
            String cleanPrimary = primaryCat.trim();
            if (!containsIgnoreCase(activeCats, cleanPrimary)) {
                activeCats.add(0, cleanPrimary);
            }
        }

        return activeCats;
    }

    public static List<String> parseCategoriesFromSnapshot(DataSnapshot sellerSnapshot) {
        List<String> result = new ArrayList<>();
        if (sellerSnapshot == null) return result;

        if (sellerSnapshot.hasChild("categories")) {
            DataSnapshot catsSnap = sellerSnapshot.child("categories");
            for (DataSnapshot child : catsSnap.getChildren()) {
                Object val = child.getValue();
                if (val != null) {
                    String sVal = val.toString().trim();
                    if (!sVal.isEmpty() && !containsIgnoreCase(result, sVal)) {
                        result.add(sVal);
                    }
                }
            }
        }

        if (sellerSnapshot.hasChild("category")) {
            String primaryCat = sellerSnapshot.child("category").getValue(String.class);
            if (primaryCat != null && !primaryCat.trim().isEmpty()) {
                String cleanPrimary = primaryCat.trim();
                if (!containsIgnoreCase(result, cleanPrimary)) {
                    result.add(0, cleanPrimary);
                }
            }
        }

        return result;
    }

    private static boolean containsIgnoreCase(List<String> list, String val) {
        for (String item : list) {
            if (item != null && item.equalsIgnoreCase(val)) return true;
        }
        return false;
    }

    public static final int DEMO_SEED_VERSION = 5;
    public static final String PREF_DEMO = "demo_prefs";
    public static final String KEY_HAS_REAL_DATA = "has_real_data";
}