package com.sheempower.app.utils;

public class Constants {
    // Firebase nodes
    public static final String NODE_USERS    = "users";
    public static final String NODE_SELLERS  = "sellers";
    public static final String NODE_ORDERS   = "orders";
    public static final String NODE_PRODUCTS = "products";

    // Cloudinary
    public static final String CLOUDINARY_CLOUD_NAME    = "lstovyxq";
    public static final String CLOUDINARY_UPLOAD_PRESET = "sheempower_preset";
    public static final String CLOUDINARY_URL =
            "https://api.cloudinary.com/v1_1/lstovyxq/auto/upload";

    // Admin Credentials
    public static final String ADMIN_EMAIL    = "admin@sheempower.app";
    public static final String ADMIN_PASSWORD = "Admin@123";

    // Roles
    public static final String ROLE_BUYER   = "buyer";
    public static final String ROLE_SELLER  = "seller";
    public static final String ROLE_ADMIN   = "admin";

    // Order Statuses
    public static final String STATUS_PENDING   = "Pending";
    public static final String STATUS_VERIFIED  = "Payment Verified";
    public static final String STATUS_PROGRESS  = "In Progress";
    public static final String STATUS_DONE      = "Completed";
    public static final String[] STATUS_ORDER   =
            { STATUS_PENDING, STATUS_VERIFIED, STATUS_PROGRESS, STATUS_DONE };

    // Categories (8 categories with emojis)
    public static final String[] CATEGORIES = {
            "Handicrafts", "Clothing", "Jewelry", "Food & Bakery",
            "Beauty & Skincare", "Home Decor", "Education", "Digital Services"
    };

    // Category Emojis
    public static final String[] CATEGORY_EMOJIS = {
            "🎨", "👗", "💍", "🍳", "💄", "🏠", "📚", "💻"
    };

    // Category Images (Unsplash - no API key needed)
    public static final String[] CATEGORY_IMAGES = {
            "https://images.unsplash.com/photo-1610701596007-11502861dcfa?w=200&q=60",   // Handicrafts
            "https://images.unsplash.com/photo-1594633312681-425c7b97ccd1?w=200&q=60",   // Clothing
            "https://images.unsplash.com/photo-1515562141207-7a88fb7ce338?w=200&q=60",   // Jewelry
            "https://images.unsplash.com/photo-1509440159596-0249088772ff?w=200&q=60",   // Food & Bakery
            "https://images.unsplash.com/photo-1596462502278-27bfdc403348?w=200&q=60",   // Beauty & Skincare
            "https://images.unsplash.com/photo-1586023492125-27b2c045efd7?w=200&q=60",   // Home Decor
            "https://images.unsplash.com/photo-1503676260728-1c00da094a0b?w=200&q=60",   // Education
            "https://images.unsplash.com/photo-1518770660439-4636190af475?w=200&q=60"    // Digital Services
    };

    // SharedPreferences
    public static final String PREF_DEMO = "demo_prefs";
    public static final String KEY_DEMO_SEEDED   = "demo_seeded";
    public static final String KEY_HAS_REAL_DATA = "has_real_data";
}