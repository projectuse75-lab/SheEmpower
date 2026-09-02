package com.sheempower.app.utils;

public class Constants {

    // ============================================================
    // FIREBASE NODES
    // ============================================================
    public static final String NODE_USERS = "users";
    public static final String NODE_SELLERS = "sellers";
    public static final String NODE_ORDERS = "orders";
    public static final String NODE_PRODUCTS = "products";

    // ============================================================
    // CLOUDINARY (APNA REAL CLOUD NAME & PRESET)
    // ============================================================
    public static final String CLOUDINARY_CLOUD_NAME = "lstovyxq";   // ✅ APNA CLOUD NAME
    public static final String CLOUDINARY_UPLOAD_PRESET = "sheempower_preset"; // ✅ APNA UNSIGNED PRESET

    public static final String CLOUDINARY_URL =
            "https://api.cloudinary.com/v1_1/" + CLOUDINARY_CLOUD_NAME + "/auto/upload";

    // ============================================================
    // ADMIN CREDENTIALS
    // ============================================================
    public static final String ADMIN_EMAIL = "admin@sheempower.app";
    public static final String ADMIN_PASSWORD = "Admin@123";

    // ============================================================
    // ROLES
    // ============================================================
    public static final String ROLE_BUYER = "buyer";
    public static final String ROLE_SELLER = "seller";
    public static final String ROLE_ADMIN = "admin";

    // ============================================================
    // ORDER STATUSES
    // ============================================================
    public static final String STATUS_PENDING = "Pending";
    public static final String STATUS_VERIFIED = "Payment Verified";
    public static final String STATUS_PROGRESS = "In Progress";
    public static final String STATUS_DONE = "Completed";
    public static final String[] STATUS_ORDER = {
            STATUS_PENDING, STATUS_VERIFIED, STATUS_PROGRESS, STATUS_DONE
    };

    // ============================================================
    // ✅ 5 VILLAGE-BASED CATEGORIES (ONLY THESE)
    // ============================================================
    public static final String[] CATEGORIES = {
            "Cooking",
            "Stitching",
            "Mehndi",
            "Makeup",
            "Krhae"
    };

    public static final String[] CATEGORY_EMOJIS = {
            "🍳", "🧵", "🌿", "💄", "🪡"
    };

    public static final String[] CATEGORY_IMAGES = {
            "https://images.unsplash.com/photo-1563379091339-03246963d96a?w=200&q=60",  // Cooking
            "https://images.unsplash.com/photo-1594633312681-425c7b97ccd1?w=200&q=60",  // Stitching
            "https://images.unsplash.com/photo-1622398925373-3f91b1e275f5?w=200&q=60",  // Mehndi
            "https://images.unsplash.com/photo-1522335789203-aabd1fc54bc9?w=200&q=60",  // Makeup
            "https://images.unsplash.com/photo-1610701596007-11502861dcfa?w=200&q=60"   // Krhae
    };

    // ============================================================
    // SHAREDPREFERENCES KEYS
    // ============================================================
    public static final String PREF_DEMO = "demo_prefs";
    public static final String KEY_DEMO_SEEDED = "demo_seeded";
    public static final String KEY_HAS_REAL_DATA = "has_real_data";
}