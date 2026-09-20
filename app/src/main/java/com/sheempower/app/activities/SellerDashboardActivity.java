package com.sheempower.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.*;
import com.sheempower.app.R;
import com.sheempower.app.models.Order;
import com.sheempower.app.models.Seller;
import com.sheempower.app.utils.*;
import com.sheempower.app.utils.PreferencesManager;

public class SellerDashboardActivity extends AppCompatActivity {

    private TextView tvCategory, tvWelcome, tvStatus, tvTotalOrders, tvPendingOrders, tvCompletedOrders, tvEarnings;
    private Button btnAddProduct;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyTheme();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seller_dashboard);

        session = new SessionManager(this);

        VoiceGuide.announceScreen(this,
                "یہ آپ کی دکان کا ڈیش بورڈ ہے۔ یہاں آپ اپنی کل کمائی اور آرڈرز کی تعداد دیکھ سکتی ہیں۔ نئی چیز شامل کرنے کے لیے نیچے بٹن دبائیں۔ آرڈرز دیکھنے کے لیے نیچے مینو سے آرڈرز پر جائیں۔");

        // ============ Initialize Views ============
        tvCategory        = findViewById(R.id.tvCategory);
        tvWelcome         = findViewById(R.id.tvWelcome);
        tvStatus          = findViewById(R.id.tvStatus);
        tvTotalOrders     = findViewById(R.id.tvTotalOrders);
        tvPendingOrders   = findViewById(R.id.tvPendingOrders);
        tvCompletedOrders = findViewById(R.id.tvCompletedOrders);
        tvEarnings        = findViewById(R.id.tvEarnings);
        btnAddProduct     = findViewById(R.id.btnAddProduct);

        tvWelcome.setText("👋 " + session.getName());

        btnAddProduct.setOnClickListener(v -> startActivity(new Intent(this, AddProductActivity.class)));

        loadSellerStatus();
        loadStats();

        BottomNavHelper.setupForSeller(this, findViewById(R.id.bottomNav), 1);
    }


    @Override
    protected void onResume() {
        super.onResume();
        loadSellerStatus();
        loadStats();
    }

    // ============ LOAD SELLER STATUS + CATEGORY ============
    private void loadSellerStatus() {
        FirebaseDatabase.getInstance().getReference()
                .child(Constants.NODE_SELLERS)
                .child(session.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        Seller s = snapshot.getValue(Seller.class);
                        if (s == null) return;

                        // ============ LOAD CATEGORY ============
                        String categoryCode = s.getCategory();
                        if (categoryCode != null && !categoryCode.isEmpty()) {
                            String categoryDisplayName = getCategoryDisplayName(categoryCode);
                            tvCategory.setText(categoryDisplayName);
                        }

                        // ============ LOAD VERIFICATION STATUS ============
                        if (Constants.isApproved(s.getVerificationStatus())) {
                            tvStatus.setText("✅ تصدیق شدہ — آپ کی دکان خریداروں کو نظر آ رہی ہے / Verified");
                            tvStatus.setTextColor(0xFF4CAF50);
                            btnAddProduct.setEnabled(true);
                        } else {
                            tvStatus.setText("⏳ ایڈمن کی منظوری کا انتظار / Pending Approval");
                            tvStatus.setTextColor(0xFFFFA000);
                            btnAddProduct.setEnabled(false);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {}
                });
    }

    // ============ GET CATEGORY DISPLAY NAME (WITH EMOJI + URDU + ENGLISH) ============
    private String getCategoryDisplayName(String categoryCode) {
        // Map category code to index
        int index = -1;
        for (int i = 0; i < Constants.CATEGORIES.length; i++) {
            if (Constants.CATEGORIES[i].equals(categoryCode)) {
                index = i;
                break;
            }
        }

        if (index >= 0 && index < Constants.CATEGORIES.length) {
            String urduName = Constants.CATEGORY_LABELS_UR[index];
            String englishName = Constants.CATEGORIES[index];
            String emoji = getCategoryEmoji(englishName);
            return emoji + " " + urduName + " / " + englishName;
        }

        return "📦 " + categoryCode;
    }

    // ============ GET EMOJI FOR CATEGORY ============
    private String getCategoryEmoji(String category) {
        switch (category.toLowerCase()) {
            case "stitching": return "🧵";
            case "cooking": return "👨‍🍳";
            case "embroidery": return "🎨";
            case "tailoring": return "✂️";
            case "jewelry": return "💎";
            case "beauty": return "💄";
            case "crafts": return "🎭";
            case "handmade": return "🖐️";
            case "henna": return "🎨";
            case "home_decor": return "🏠";
            case "cosmetics": return "💅";
            case "clothing": return "👗";
            case "accessories": return "👜";
            case "baking": return "🍰";
            case "handicrafts": return "🛠️";
            default: return "📦";
        }
    }

    // ============ LOAD ORDER STATISTICS ============
    private void loadStats() {
        FirebaseDatabase.getInstance().getReference()
                .child(Constants.NODE_ORDERS)
                .orderByChild("sellerId")
                .equalTo(session.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        int total = 0, pending = 0, completed = 0;
                        double earnings = 0;

                        for (DataSnapshot child : snapshot.getChildren()) {
                            Order o = child.getValue(Order.class);
                            if (o == null) continue;

                            total++;

                            if (Constants.STATUS_PENDING.equals(o.getStatus())) {
                                pending++;
                            }

                            if (Constants.STATUS_COMPLETED.equals(o.getStatus())) {
                                completed++;
                                earnings += o.getAmount();
                            }
                        }

                        // ============ UPDATE UI ============
                        tvTotalOrders.setText(String.valueOf(total));
                        tvPendingOrders.setText(String.valueOf(pending));
                        tvCompletedOrders.setText(String.valueOf(completed));
                        tvEarnings.setText("PKR " + (long) earnings);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {}
                });
    }
    // ✅ Apply Dark Theme
    private void applyTheme() {
        PreferencesManager prefs = new PreferencesManager(this);
        if (prefs.isDarkThemeEnabled()) {
            setTheme(R.style.Theme_SheEmpower_Dark);
        } else {
            setTheme(R.style.Theme_SheEmpower);
        }
    }


}