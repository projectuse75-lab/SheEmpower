package com.sheempower.app.utils;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.sheempower.app.models.Product;
import com.sheempower.app.models.Seller;

public class DemoDataSeeder {

    private static final String[] DEMO_SELLER_IDS = {
            "demo_seller_1", "demo_seller_2", "demo_seller_3", "demo_seller_4"
    };
    private static final String[] DEMO_PRODUCT_IDS = {
            "demo_p1", "demo_p2", "demo_p3", "demo_p4", "demo_p5", "demo_p6"
    };
    private static final String[] DEMO_PENDING_IDS = {
            "pending_seller_1", "pending_seller_2"
    };

    public static void seedIfNeeded(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(Constants.PREF_DEMO, Context.MODE_PRIVATE);
        if (prefs.getBoolean(Constants.KEY_DEMO_SEEDED, false)) return;
        if (prefs.getBoolean(Constants.KEY_HAS_REAL_DATA, false)) return;

        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();

        // ============================================================
        // SELLER 1: Fatima's Embroidery Works (Handicrafts)
        // ============================================================
        Seller s1 = new Seller();
        s1.setUid("demo_seller_1");
        s1.setOwnerName("Fatima Zahra");
        s1.setBusinessName("Fatima's Embroidery Works");
        s1.setBio("Handmade embroidery pieces with traditional Pakistani designs.");
        s1.setCity("Lahore");
        s1.setCategory("Handicrafts");
        s1.setWhatsappNumber("923001234567");
        s1.setVerificationStatus("approved");
        s1.setTopRated(true);
        s1.setRating(4.9);
        s1.setRatingCount(342);
        s1.setImageUrl("https://i.pravatar.cc/150?img=47");
        s1.setCnicUrl("https://res.cloudinary.com/demo/image/upload/v1/samples/cnic_fatima");
        s1.setVoiceUrl("https://res.cloudinary.com/demo/video/upload/v1/samples/voice_fatima");
        dbRef.child(Constants.NODE_SELLERS).child("demo_seller_1").setValue(s1);

        // ============================================================
        // SELLER 2: Sara's Fine Jewelry (Jewelry)
        // ============================================================
        Seller s2 = new Seller();
        s2.setUid("demo_seller_2");
        s2.setOwnerName("Sara Ahmed");
        s2.setBusinessName("Sara's Fine Jewelry");
        s2.setBio("Handmade pearl and custom jewelry pieces.");
        s2.setCity("Lahore");
        s2.setCategory("Jewelry");
        s2.setWhatsappNumber("923007654321");
        s2.setVerificationStatus("approved");
        s2.setTopRated(true);
        s2.setRating(4.8);
        s2.setRatingCount(210);
        s2.setImageUrl("https://i.pravatar.cc/150?img=32");
        s2.setCnicUrl("https://res.cloudinary.com/demo/image/upload/v1/samples/cnic_sara");
        s2.setVoiceUrl("https://res.cloudinary.com/demo/video/upload/v1/samples/voice_sara");
        dbRef.child(Constants.NODE_SELLERS).child("demo_seller_2").setValue(s2);

        // ============================================================
        // SELLER 3: Ayesha's Kitchen (Food & Bakery)
        // ============================================================
        Seller s3 = new Seller();
        s3.setUid("demo_seller_3");
        s3.setOwnerName("Ayesha Khan");
        s3.setBusinessName("Ayesha's Kitchen");
        s3.setBio("Home-cooked biryani, catering and fresh bakery items.");
        s3.setCity("Karachi");
        s3.setCategory("Food & Bakery");
        s3.setWhatsappNumber("923009876543");
        s3.setVerificationStatus("approved");
        s3.setTopRated(true);
        s3.setRating(4.7);
        s3.setRatingCount(158);
        s3.setImageUrl("https://i.pravatar.cc/150?img=25");
        s3.setCnicUrl("https://res.cloudinary.com/demo/image/upload/v1/samples/cnic_ayesha");
        s3.setVoiceUrl("https://res.cloudinary.com/demo/video/upload/v1/samples/voice_ayesha");
        dbRef.child(Constants.NODE_SELLERS).child("demo_seller_3").setValue(s3);

        // ============================================================
        // SELLER 4: Zara Stitching Studio (Clothing)
        // ============================================================
        Seller s4 = new Seller();
        s4.setUid("demo_seller_4");
        s4.setOwnerName("Zara Malik");
        s4.setBusinessName("Zara Stitching Studio");
        s4.setBio("Custom stitching, bridal wear and everyday clothing.");
        s4.setCity("Islamabad");
        s4.setCategory("Clothing");
        s4.setWhatsappNumber("923004567890");
        s4.setVerificationStatus("approved");
        s4.setTopRated(false);
        s4.setRating(4.5);
        s4.setRatingCount(76);
        s4.setImageUrl("https://i.pravatar.cc/150?img=44");
        s4.setCnicUrl("https://res.cloudinary.com/demo/image/upload/v1/samples/cnic_zara");
        s4.setVoiceUrl("https://res.cloudinary.com/demo/video/upload/v1/samples/voice_zara");
        dbRef.child(Constants.NODE_SELLERS).child("demo_seller_4").setValue(s4);

        // ============================================================
        // PENDING SELLERS (for Admin testing)
        // ============================================================
        seedPendingSellers();

        // ============================================================
        // PRODUCTS
        // ============================================================
        dbRef.child(Constants.NODE_PRODUCTS).child("demo_p1").setValue(withCategory(
                new Product("demo_p1", "demo_seller_1", "Hand Embroidered Wall Art",
                        "Handmade embroidered wall hanging with traditional patterns.", 3500,
                        "https://images.unsplash.com/photo-1610701596007-11502861dcfa?w=400&q=70"),
                "Handicrafts"));

        dbRef.child(Constants.NODE_PRODUCTS).child("demo_p2").setValue(withCategory(
                new Product("demo_p2", "demo_seller_1", "Pottery Tea Set (6 cups)",
                        "Handmade ceramic tea set, 6 cups with tray.", 4200,
                        "https://images.unsplash.com/photo-1593618998160-e34014e67546?w=400&q=70"),
                "Handicrafts"));

        dbRef.child(Constants.NODE_PRODUCTS).child("demo_p3").setValue(withCategory(
                new Product("demo_p3", "demo_seller_2", "Pearl Necklace Set",
                        "Elegant handmade pearl necklace with matching earrings.", 5500,
                        "https://images.unsplash.com/photo-1515562141207-7a88fb7ce338?w=400&q=70"),
                "Jewelry"));

        dbRef.child(Constants.NODE_PRODUCTS).child("demo_p4").setValue(withCategory(
                new Product("demo_p4", "demo_seller_3", "Chicken Biryani (2kg)",
                        "Aromatic basmati rice, tender chicken, whole spices. Serves 4-5.", 1200,
                        "https://images.unsplash.com/photo-1563379091339-03246963d96a?w=400&q=70"),
                "Food & Bakery"));

        dbRef.child(Constants.NODE_PRODUCTS).child("demo_p5").setValue(withCategory(
                new Product("demo_p5", "demo_seller_3", "Assorted Bakery Box",
                        "Fresh cookies, cupcakes and pastries — mixed box of 12.", 1800,
                        "https://images.unsplash.com/photo-1509440159596-0249088772ff?w=400&q=70"),
                "Food & Bakery"));

        dbRef.child(Constants.NODE_PRODUCTS).child("demo_p6").setValue(withCategory(
                new Product("demo_p6", "demo_seller_4", "Custom Kurti Stitching",
                        "Apni pasand ke design mein kurti silwayein.", 1500,
                        "https://images.unsplash.com/photo-1594633312681-425c7b97ccd1?w=400&q=70"),
                "Clothing"));

        prefs.edit().putBoolean(Constants.KEY_DEMO_SEEDED, true).apply();
    }

    private static Product withCategory(Product p, String category) {
        p.setCategory(category);
        return p;
    }

    // ============================================================
    // PENDING SELLERS (for Admin testing)
    // ============================================================
    private static void seedPendingSellers() {
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();

        Seller pending1 = new Seller();
        pending1.setUid("pending_seller_1");
        pending1.setOwnerName("Zara Ahmed");
        pending1.setBusinessName("Zara's Beauty Salon");
        pending1.setBio("Professional makeup artist and bridal services. 5 years of experience.");
        pending1.setCity("Lahore");
        pending1.setCategory("Art & Beauty");
        pending1.setWhatsappNumber("923001112233");
        pending1.setVerificationStatus("pending");
        pending1.setTopRated(false);
        pending1.setRating(0);
        pending1.setRatingCount(0);
        pending1.setCnicUrl("https://res.cloudinary.com/demo/image/upload/v1/samples/cnic_zara_pending");
        pending1.setVoiceUrl("https://res.cloudinary.com/demo/video/upload/v1/samples/voice_zara_pending");
        dbRef.child(Constants.NODE_SELLERS).child("pending_seller_1").setValue(pending1);

        Seller pending2 = new Seller();
        pending2.setUid("pending_seller_2");
        pending2.setOwnerName("Ali Hassan");
        pending2.setBusinessName("Ali's Handmade Carpets");
        pending2.setBio("Handwoven carpets and rugs with traditional Pakistani designs.");
        pending2.setCity("Karachi");
        pending2.setCategory("Handicrafts");
        pending2.setWhatsappNumber("923004445566");
        pending2.setVerificationStatus("pending");
        pending2.setTopRated(false);
        pending2.setRating(0);
        pending2.setRatingCount(0);
        pending2.setCnicUrl("https://res.cloudinary.com/demo/image/upload/v1/samples/cnic_ali_pending");
        pending2.setVoiceUrl("https://res.cloudinary.com/demo/video/upload/v1/samples/voice_ali_pending");
        dbRef.child(Constants.NODE_SELLERS).child("pending_seller_2").setValue(pending2);
    }

    // ============================================================
    // CLEAR DEMO DATA ON FIRST REAL USE
    // ============================================================
    public static void clearDemoDataOnFirstRealUse(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(Constants.PREF_DEMO, Context.MODE_PRIVATE);
        if (prefs.getBoolean(Constants.KEY_HAS_REAL_DATA, false)) return;

        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();

        // Remove approved demo sellers
        for (String id : DEMO_SELLER_IDS) {
            dbRef.child(Constants.NODE_SELLERS).child(id).removeValue();
        }

        // Remove pending demo sellers
        for (String id : DEMO_PENDING_IDS) {
            dbRef.child(Constants.NODE_SELLERS).child(id).removeValue();
        }

        // Remove demo products
        for (String id : DEMO_PRODUCT_IDS) {
            dbRef.child(Constants.NODE_PRODUCTS).child(id).removeValue();
        }

        prefs.edit().putBoolean(Constants.KEY_HAS_REAL_DATA, true).apply();
    }
}