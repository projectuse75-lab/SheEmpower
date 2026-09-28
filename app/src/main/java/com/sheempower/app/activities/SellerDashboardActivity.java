package com.sheempower.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.database.*;
import com.sheempower.app.R;
import com.sheempower.app.adapters.ProductAdapter;
import com.sheempower.app.models.Order;
import com.sheempower.app.models.Product;
import com.sheempower.app.models.Seller;
import com.sheempower.app.utils.*;
import com.sheempower.app.utils.PreferencesManager;
import java.util.ArrayList;
import java.util.List;

public class SellerDashboardActivity extends AppCompatActivity {

    private TextView tvCategory, tvWelcome, tvStatus, tvTotalOrders, tvPendingOrders, tvCompletedOrders, tvEarnings;
    private Button btnAddProduct;
    private SessionManager session;

    // ✅ NAYA — "My Products" section
    private RecyclerView rvMyProducts;
    private TextView tvNoProducts;
    private ProductAdapter myProductAdapter;
    private final List<Product> myProductList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyTheme();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seller_dashboard);

        session = new SessionManager(this);

        VoiceGuide.announceScreen(this,
                "یہ آپ کی دکان کا ڈیش بورڈ ہے۔ یہاں آپ اپنی کل کمائی اور آرڈرز کی تعداد دیکھ سکتی ہیں۔ نیچے آپ کی شامل کردہ تمام اشیاء بھی نظر آئیں گی۔ نئی چیز شامل کرنے کے لیے بٹن دبائیں۔");

        // ============ Initialize Views ============
        tvCategory        = findViewById(R.id.tvCategory);
        tvWelcome         = findViewById(R.id.tvWelcome);
        tvStatus          = findViewById(R.id.tvStatus);
        tvTotalOrders     = findViewById(R.id.tvTotalOrders);
        tvPendingOrders   = findViewById(R.id.tvPendingOrders);
        tvCompletedOrders = findViewById(R.id.tvCompletedOrders);
        tvEarnings        = findViewById(R.id.tvEarnings);
        btnAddProduct     = findViewById(R.id.btnAddProduct);
        rvMyProducts      = findViewById(R.id.rvMyProducts);
        tvNoProducts      = findViewById(R.id.tvNoProducts);

        tvWelcome.setText("👋 " + session.getName());

        btnAddProduct.setOnClickListener(v -> startActivity(new Intent(this, AddProductActivity.class)));

        setupMyProductsList();

        loadSellerStatus();
        loadStats();
        loadMyProducts();

        BottomNavHelper.setupForSeller(this, findViewById(R.id.bottomNav), 1);
    }


    @Override
    protected void onResume() {
        super.onResume();
        loadSellerStatus();
        loadStats();
        loadMyProducts();
    }

    // ============ SETUP MY PRODUCTS RECYCLERVIEW ============
    private void setupMyProductsList() {
        rvMyProducts.setLayoutManager(new GridLayoutManager(this, 2));
        myProductAdapter = new ProductAdapter(
                myProductList,
                product -> {
                    Intent i = new Intent(this, ProductDetailActivity.class);
                    i.putExtra(ProductDetailActivity.EXTRA_PRODUCT_ID, product.getId());
                    startActivity(i);
                },
                // Apni khud ki cheez par order/chat dabana kuch nahi karta — sirf batata hai
                product -> Toast.makeText(this, "یہ آپ کی اپنی چیز ہے", Toast.LENGTH_SHORT).show(),
                product -> Toast.makeText(this, "یہ آپ کی اپنی چیز ہے", Toast.LENGTH_SHORT).show()
        );
        rvMyProducts.setAdapter(myProductAdapter);
    }

    // ============ LOAD SELLER'S OWN PRODUCTS ============
    private void loadMyProducts() {
        FirebaseDatabase.getInstance().getReference()
                .child(Constants.NODE_PRODUCTS)
                .orderByChild("sellerId")
                .equalTo(session.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        myProductList.clear();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            Product p = child.getValue(Product.class);
                            if (p != null) myProductList.add(p);
                        }
                        myProductAdapter.notifyDataSetChanged();
                        tvNoProducts.setVisibility(myProductList.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
                        rvMyProducts.setVisibility(myProductList.isEmpty() ? android.view.View.GONE : android.view.View.VISIBLE);
                    }
                    @Override public void onCancelled(DatabaseError error) {}
                });
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

                        String categoryCode = s.getCategory();
                        if (categoryCode != null && !categoryCode.isEmpty()) {
                            String categoryDisplayName = getCategoryDisplayName(categoryCode);
                            tvCategory.setText(categoryDisplayName);
                        }

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

    private String getCategoryDisplayName(String categoryCode) {
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

                        tvTotalOrders.setText(String.valueOf(total));
                        tvPendingOrders.setText(String.valueOf(pending));
                        tvCompletedOrders.setText(String.valueOf(completed));
                        tvEarnings.setText("PKR " + (long) earnings);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {}
                });
    }

    private void applyTheme() {
        PreferencesManager prefs = new PreferencesManager(this);
        if (prefs.isDarkThemeEnabled()) {
            setTheme(R.style.Theme_SheEmpower_Dark);
        } else {
            setTheme(R.style.Theme_SheEmpower);
        }
    }
}