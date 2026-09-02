package com.sheempower.app.utils;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.sheempower.app.models.Order;
import com.sheempower.app.models.Product;
import com.sheempower.app.models.Seller;
import com.sheempower.app.models.User;

public class DemoDataSeeder {

    private static final String[] DEMO_SELLER_IDS = {
            "demo_seller_1", "demo_seller_2", "demo_seller_3", "demo_seller_4",
            "demo_seller_5", "demo_seller_6", "demo_seller_7", "demo_seller_8"
    };
    private static final String[] DEMO_PRODUCT_IDS = {
            "demo_p1", "demo_p2", "demo_p3", "demo_p4", "demo_p5", "demo_p6",
            "demo_p7", "demo_p8", "demo_p9", "demo_p10", "demo_p11", "demo_p12"
    };
    private static final String[] DEMO_ORDER_IDS = {
            "demo_order_1", "demo_order_2", "demo_order_3"
    };

    public static void seedIfNeeded(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(Constants.PREF_DEMO, Context.MODE_PRIVATE);
        if (prefs.getBoolean(Constants.KEY_DEMO_SEEDED, false)) return;
        if (prefs.getBoolean(Constants.KEY_HAS_REAL_DATA, false)) return;

        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();

        // ============================================================
        // ✅ DEMO BUYER ACCOUNT (for testing orders)
        // ============================================================
        User demoBuyer = new User("demo_buyer", "ثمینہ احمد", "demo@sheempower.app", "923001111222", Constants.ROLE_BUYER);
        dbRef.child(Constants.NODE_USERS).child("demo_buyer").setValue(demoBuyer);

        // ============================================================
        // ✅ SELLER 1: Cooking
        // ============================================================
        Seller s1 = new Seller();
        s1.setUid("demo_seller_1");
        s1.setOwnerName("Ayesha Khan");
        s1.setBusinessName("عائشہ کا کچن / Ayesha's Kitchen");
        s1.setBio("گھر کا پکا کھانا، بریانی اور کیٹرنگ");
        s1.setCity("Karachi");
        s1.setCategory("Cooking");
        s1.setWhatsappNumber("923009876543");
        s1.setVerificationStatus("approved"); // ✅ FIXED
        s1.setTopRated(true);
        s1.setRating(4.7);
        s1.setRatingCount(158);
        s1.setImageUrl("https://i.pravatar.cc/150?img=25");
        s1.setCapacityDescription("میں 5 سے 50 لوگوں تک کے لیے کھانا بنا سکتی ہوں");
        dbRef.child(Constants.NODE_SELLERS).child("demo_seller_1").setValue(s1);

        // ============================================================
        // ✅ SELLER 2: Stitching
        // ============================================================
        Seller s2 = new Seller();
        s2.setUid("demo_seller_2");
        s2.setOwnerName("Zara Malik");
        s2.setBusinessName("زینب کی سلائی / Zara Stitching Studio");
        s2.setBio("سلائی، کڑھائی اور برائیڈل ملبوسات");
        s2.setCity("Islamabad");
        s2.setCategory("Stitching");
        s2.setWhatsappNumber("923004567890");
        s2.setVerificationStatus("approved"); // ✅ FIXED
        s2.setTopRated(false);
        s2.setRating(4.5);
        s2.setRatingCount(76);
        s2.setImageUrl("https://i.pravatar.cc/150?img=44");
        s2.setCapacityDescription("میں بچوں، خواتین اور بزرگ افراد کے لیے سلائی کر سکتی ہوں");
        dbRef.child(Constants.NODE_SELLERS).child("demo_seller_2").setValue(s2);

        // ============================================================
        // ✅ SELLER 3: Mehndi
        // ============================================================
        Seller s3 = new Seller();
        s3.setUid("demo_seller_3");
        s3.setOwnerName("Sana Bibi");
        s3.setBusinessName("ثنا کی مہندی / Sana's Mehndi Art");
        s3.setBio("دلہن اور مہمانوں کی مہندی، عربی اور روایتی ڈیزائن");
        s3.setCity("Lahore");
        s3.setCategory("Mehndi");
        s3.setWhatsappNumber("923001112223");
        s3.setVerificationStatus("approved"); // ✅ FIXED
        s3.setTopRated(true);
        s3.setRating(4.9);
        s3.setRatingCount(212);
        s3.setImageUrl("https://i.pravatar.cc/150?img=48");
        s3.setCapacityDescription("میں دلہن سمیت 30 مہمانوں تک مہندی لگا سکتی ہوں");
        dbRef.child(Constants.NODE_SELLERS).child("demo_seller_3").setValue(s3);

        // ============================================================
        // ✅ SELLER 4: Makeup
        // ============================================================
        Seller s4 = new Seller();
        s4.setUid("demo_seller_4");
        s4.setOwnerName("Mahnoor Ali");
        s4.setBusinessName("منہور میک اوور / Mahnoor Makeover");
        s4.setBio("برائیڈل اور پارٹی میک اپ، ہلکا اور بھاری دونوں");
        s4.setCity("Lahore");
        s4.setCategory("Makeup");
        s4.setWhatsappNumber("923003334445");
        s4.setVerificationStatus("approved"); // ✅ FIXED
        s4.setTopRated(true);
        s4.setRating(4.8);
        s4.setRatingCount(134);
        s4.setImageUrl("https://i.pravatar.cc/150?img=29");
        s4.setCapacityDescription("میں دلہن اور 10 مہمانوں تک کا میک اپ کر سکتی ہوں");
        dbRef.child(Constants.NODE_SELLERS).child("demo_seller_4").setValue(s4);

        // ============================================================
        // ✅ SELLER 5: Handicrafts
        // ============================================================
        Seller s5 = new Seller();
        s5.setUid("demo_seller_5");
        s5.setOwnerName("Fatima Zahra");
        s5.setBusinessName("فاطمہ کی کڑھائی / Fatima's Embroidery Works");
        s5.setBio("ہاتھ کی کڑھائی اور دستکاری کی اشیاء");
        s5.setCity("Multan");
        s5.setCategory("Handicrafts");
        s5.setWhatsappNumber("923005556667");
        s5.setVerificationStatus("approved"); // ✅ FIXED
        s5.setTopRated(true);
        s5.setRating(4.9);
        s5.setRatingCount(342);
        s5.setImageUrl("https://i.pravatar.cc/150?img=47");
        s5.setCapacityDescription("میں ایک ہفتے میں 5 سے 10 دستکاری کی اشیاء بنا سکتی ہوں");
        dbRef.child(Constants.NODE_SELLERS).child("demo_seller_5").setValue(s5);

        // ============================================================
        // ✅ SELLER 6: Jewelry
        // ============================================================
        Seller s6 = new Seller();
        s6.setUid("demo_seller_6");
        s6.setOwnerName("Sara Ahmed");
        s6.setBusinessName("سارہ کے زیورات / Sara's Fine Jewelry");
        s6.setBio("ہاتھ سے بنے موتیوں کے زیورات");
        s6.setCity("Lahore");
        s6.setCategory("Jewelry");
        s6.setWhatsappNumber("923007654321");
        s6.setVerificationStatus("approved"); // ✅ FIXED
        s6.setTopRated(false);
        s6.setRating(4.6);
        s6.setRatingCount(89);
        s6.setImageUrl("https://i.pravatar.cc/150?img=32");
        s6.setCapacityDescription("میں کسٹم آرڈر پر ہر قسم کے زیورات بنا سکتی ہوں");
        dbRef.child(Constants.NODE_SELLERS).child("demo_seller_6").setValue(s6);

        // ============================================================
        // ✅ SELLER 7: Home Decor
        // ============================================================
        Seller s7 = new Seller();
        s7.setUid("demo_seller_7");
        s7.setOwnerName("Nadia Khan");
        s7.setBusinessName("نادیہ کی سجاوٹ / Nadia Home Decor");
        s7.setBio("گھر کی سجاوٹ کی اشیاء، دیواروں کی آرٹ");
        s7.setCity("Islamabad");
        s7.setCategory("Home Decor");
        s7.setWhatsappNumber("923008889990");
        s7.setVerificationStatus("approved"); // ✅ FIXED
        s7.setTopRated(false);
        s7.setRating(4.4);
        s7.setRatingCount(67);
        s7.setImageUrl("https://i.pravatar.cc/150?img=38");
        s7.setCapacityDescription("میں کسٹم سائز کی سجاوٹ بنا سکتی ہوں");
        dbRef.child(Constants.NODE_SELLERS).child("demo_seller_7").setValue(s7);

        // ============================================================
        // ✅ SELLER 8: Education
        // ============================================================
        Seller s8 = new Seller();
        s8.setUid("demo_seller_8");
        s8.setOwnerName("Saima Malik");
        s8.setBusinessName("ثیماء اکیڈمی / Saima Education");
        s8.setBio("بچوں اور خواتین کو سلائی، کمپیوٹر اور انگلش سکھاتی ہوں");
        s8.setCity("Karachi");
        s8.setCategory("Education");
        s8.setWhatsappNumber("923009990001");
        s8.setVerificationStatus("approved"); // ✅ FIXED
        s8.setTopRated(false);
        s8.setRating(4.3);
        s8.setRatingCount(45);
        s8.setImageUrl("https://i.pravatar.cc/150?img=52");
        s8.setCapacityDescription("میں 5 سے 20 طلباء کو سکھا سکتی ہوں");
        dbRef.child(Constants.NODE_SELLERS).child("demo_seller_8").setValue(s8);

        // ============================================================
        // ✅ PRODUCTS (12 Products)
        // ============================================================
        dbRef.child(Constants.NODE_PRODUCTS).child("demo_p1").setValue(withCat(
                new Product("demo_p1", "demo_seller_1", "چکن بریانی (5 لوگوں کے لیے)",
                        "خوشبودار بریانی، مصالحہ درمیانہ", 1500,
                        "https://images.unsplash.com/photo-1563379091339-03246963d96a?w=400&q=70"), "Cooking"));

        dbRef.child(Constants.NODE_PRODUCTS).child("demo_p2").setValue(withCat(
                new Product("demo_p2", "demo_seller_2", "کسٹم کڑھائی والی کرتی",
                        "آپ کی پسند کے ڈیزائن میں کرتی سلائی", 1800,
                        "https://images.unsplash.com/photo-1594633312681-425c7b97ccd1?w=400&q=70"), "Stitching"));

        dbRef.child(Constants.NODE_PRODUCTS).child("demo_p3").setValue(withCat(
                new Product("demo_p3", "demo_seller_3", "دلہن کی مہندی (فل ہاتھ)",
                        "عربی اور روایتی ڈیزائن، فل ہاتھ اور پاؤں", 5000,
                        "https://images.unsplash.com/photo-1622398925373-3f91b1e275f5?w=400&q=70"), "Mehndi"));

        dbRef.child(Constants.NODE_PRODUCTS).child("demo_p4").setValue(withCat(
                new Product("demo_p4", "demo_seller_3", "مہمانوں کی مہندی (فی کس)",
                        "سادہ اور خوبصورت ڈیزائن", 300,
                        "https://images.unsplash.com/photo-1622398925373-3f91b1e275f5?w=400&q=70"), "Mehndi"));

        dbRef.child(Constants.NODE_PRODUCTS).child("demo_p5").setValue(withCat(
                new Product("demo_p5", "demo_seller_4", "برائیڈل میک اپ",
                        "مکمل برائیڈل لک، HD میک اپ", 8000,
                        "https://images.unsplash.com/photo-1522335789203-aabd1fc54bc9?w=400&q=70"), "Makeup"));

        dbRef.child(Constants.NODE_PRODUCTS).child("demo_p6").setValue(withCat(
                new Product("demo_p6", "demo_seller_4", "پارٹی میک اپ",
                        "ہلکا اور خوبصورت میک اپ", 2500,
                        "https://images.unsplash.com/photo-1522335789203-aabd1fc54bc9?w=400&q=70"), "Makeup"));

        dbRef.child(Constants.NODE_PRODUCTS).child("demo_p7").setValue(withCat(
                new Product("demo_p7", "demo_seller_5", "کڑھائی والا وال آرٹ",
                        "روایتی پیٹرن کے ساتھ", 3500,
                        "https://images.unsplash.com/photo-1610701596007-11502861dcfa?w=400&q=70"), "Handicrafts"));

        dbRef.child(Constants.NODE_PRODUCTS).child("demo_p8").setValue(withCat(
                new Product("demo_p8", "demo_seller_6", "موتیوں کا نیکلس سیٹ",
                        "میچنگ کانوں کی بالیوں کے ساتھ", 5500,
                        "https://images.unsplash.com/photo-1515562141207-7a88fb7ce338?w=400&q=70"), "Jewelry"));

        dbRef.child(Constants.NODE_PRODUCTS).child("demo_p9").setValue(withCat(
                new Product("demo_p9", "demo_seller_6", "چاندی کی انگوٹھی",
                        "ہاتھ سے بنی چاندی کی انگوٹھی", 2800,
                        "https://images.unsplash.com/photo-1589674781759-c21c37956a44?w=400&q=70"), "Jewelry"));

        dbRef.child(Constants.NODE_PRODUCTS).child("demo_p10").setValue(withCat(
                new Product("demo_p10", "demo_seller_7", "دیوار کی سجاوٹ (3D)",
                        "خوبصورت 3D وال آرٹ", 4500,
                        "https://images.unsplash.com/photo-1586023492125-27b2c045efd7?w=400&q=70"), "Home Decor"));

        dbRef.child(Constants.NODE_PRODUCTS).child("demo_p11").setValue(withCat(
                new Product("demo_p11", "demo_seller_8", "سلائی کلاسز (1 ماہ)",
                        "بنیادی سلائی سے شروع", 3000,
                        "https://images.unsplash.com/photo-1503676260728-1c00da094a0b?w=400&q=70"), "Education"));

        dbRef.child(Constants.NODE_PRODUCTS).child("demo_p12").setValue(withCat(
                new Product("demo_p12", "demo_seller_8", "انگلش کلاسز (1 ماہ)",
                        "بنیادی انگلش بول چال", 2500,
                        "https://images.unsplash.com/photo-1503676260728-1c00da094a0b?w=400&q=70"), "Education"));

        // ============================================================
        // ✅ DEMO ORDERS (for testing Buyer Dashboard)
        // ============================================================
        long now = System.currentTimeMillis();

        Order o1 = new Order();
        o1.setId("demo_order_1");
        o1.setBuyerId("demo_buyer");
        o1.setSellerId("demo_seller_1");
        o1.setProductId("demo_p1");
        o1.setProductName("چکن بریانی");
        o1.setDetails("خاندانی تقریب کے لیے 2 کلو بریانی");
        o1.setAddress("کراچی، پاکستان");
        o1.setAmount(3000);
        o1.setStatus(Constants.STATUS_PENDING);
        o1.setTimestamp(now - 3600000);
        dbRef.child(Constants.NODE_ORDERS).child("demo_order_1").setValue(o1);

        Order o2 = new Order();
        o2.setId("demo_order_2");
        o2.setBuyerId("demo_buyer");
        o2.setSellerId("demo_seller_3");
        o2.setProductId("demo_p3");
        o2.setProductName("دلہن کی مہندی");
        o2.setDetails("شادی کے لیے مکمل مہندی");
        o2.setAddress("لاہور، پاکستان");
        o2.setAmount(5000);
        o2.setStatus(Constants.STATUS_VERIFIED);
        o2.setTimestamp(now - 7200000);
        dbRef.child(Constants.NODE_ORDERS).child("demo_order_2").setValue(o2);

        Order o3 = new Order();
        o3.setId("demo_order_3");
        o3.setBuyerId("demo_buyer");
        o3.setSellerId("demo_seller_5");
        o3.setProductId("demo_p7");
        o3.setProductName("کڑھائی والا وال آرٹ");
        o3.setDetails("دیوار کی سجاوٹ کے لیے 2 عدد");
        o3.setAddress("ملتان، پاکستان");
        o3.setAmount(7000);
        o3.setStatus(Constants.STATUS_PROGRESS);
        o3.setTimestamp(now - 10800000);
        dbRef.child(Constants.NODE_ORDERS).child("demo_order_3").setValue(o3);

        prefs.edit().putBoolean(Constants.KEY_DEMO_SEEDED, true).apply();
    }

    private static Product withCat(Product p, String category) {
        p.setCategory(category);
        return p;
    }

    public static void clearDemoDataOnFirstRealUse(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(Constants.PREF_DEMO, Context.MODE_PRIVATE);
        if (prefs.getBoolean(Constants.KEY_HAS_REAL_DATA, false)) return;

        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();
        for (String id : DEMO_SELLER_IDS) dbRef.child(Constants.NODE_SELLERS).child(id).removeValue();
        for (String id : DEMO_PRODUCT_IDS) dbRef.child(Constants.NODE_PRODUCTS).child(id).removeValue();
        for (String id : DEMO_ORDER_IDS) dbRef.child(Constants.NODE_ORDERS).child(id).removeValue();
        dbRef.child(Constants.NODE_USERS).child("demo_buyer").removeValue();

        prefs.edit().putBoolean(Constants.KEY_HAS_REAL_DATA, true).apply();
    }
}