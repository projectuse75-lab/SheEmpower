package com.sheempower.app.utils;

import com.sheempower.app.R;
import java.util.Arrays;
import java.util.List;

public class Constants {
    public static final String NODE_USERS    = "users";
    public static final String NODE_SELLERS  = "sellers";
    public static final String NODE_ORDERS   = "orders";
    public static final String NODE_PRODUCTS = "products";
    public static final String NODE_META     = "meta";

    // ⚠️ Cloudinary Dashboard → Settings → Upload → apna Cloud Name aur Unsigned Preset yahan dalein
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

    /** YEH ZAROORI FIX HAI — aapki JSON mein "approved" likha hai, code "verified"
     *  check karta tha. Ab dono qabool hote hain, isliye Firebase data waisa hi rahega jaisa aapne daala */
    public static boolean isApproved(String verificationStatus) {
        return "verified".equalsIgnoreCase(verificationStatus) || "approved".equalsIgnoreCase(verificationStatus);
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

    // ✅ NAYA ARRAY — Local drawable images ke liye
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
        for (int i = 0; i < CATEGORIES.length; i++) if (CATEGORIES[i].equals(categoryKey)) return CATEGORY_LABELS_UR[i];
        return categoryKey;
    }

    public static final int DEMO_SEED_VERSION = 5;
    public static final String PREF_DEMO = "demo_prefs";
    public static final String KEY_HAS_REAL_DATA = "has_real_data";
}