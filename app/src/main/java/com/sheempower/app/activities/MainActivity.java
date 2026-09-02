package com.sheempower.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.sheempower.app.R;
import com.sheempower.app.adapters.ProductAdapter;
import com.sheempower.app.adapters.SellerAdapter;
import com.sheempower.app.models.Product;
import com.sheempower.app.models.Seller;
import com.sheempower.app.utils.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private EditText etSearch;
    private Button btnLogin, btnJoin;
    private LinearLayout layoutCategories;
    private RecyclerView rvTopSellers, rvLatestProducts;
    private TextView tvProductsTab, tvSellersTab;

    private SellerAdapter topSellerAdapter;
    private ProductAdapter productAdapter;
    private SellerAdapter allSellerAdapter;

    private final List<Seller> topSellers = new ArrayList<>();
    private final List<Product> allProducts = new ArrayList<>();
    private final List<Seller> allSellers = new ArrayList<>();

    private SessionManager session;
    private TTSHelper tts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        session = new SessionManager(this);
        tts = TTSHelper.getInstance(this);

        // Find Views
        etSearch = findViewById(R.id.etSearch);
        btnLogin = findViewById(R.id.btnLogin);
        btnJoin = findViewById(R.id.btnJoin);
        layoutCategories = findViewById(R.id.layoutCategories);
        rvTopSellers = findViewById(R.id.rvTopSellers);
        rvLatestProducts = findViewById(R.id.rvLatestProducts);
        tvProductsTab = findViewById(R.id.tvProductsTab);
        tvSellersTab = findViewById(R.id.tvSellersTab);

        // Speaker Icon
        findViewById(R.id.btnSpeaker).setOnClickListener(v -> {
            VoiceGuide.announceScreen(this,
                    "خواتین کی بااختیاری ایپ میں خوش آمدید۔ اپنے ہنر کو بیچیں اور روزگار شروع کریں۔");
        });

        // Auto Voice on Load
        VoiceGuide.announceScreen(this,
                "خوش آمدید! براہ کرم لاگ ان کریں یا رجسٹر کریں۔");

        // Setup Login/Join
        setupTopButtons();

        // Setup Categories
        setupCategoryButtonsText();

        // Setup RecyclerViews
        setupRecyclerViews();

        // Search
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {
                filterSearch(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        loadSellers();
        loadProducts();

        // ✅ Bottom Navigation (3 Tabs)
        BottomNavHelper.setupBottomNav(this, session);
        BottomNavHelper.setSelectedTab(this, 1); // Highlight Home Tab
    }

    // ============================================================
    // LOGIN / JOIN BUTTONS
    // ============================================================
    private void setupTopButtons() {
        if (btnLogin == null || btnJoin == null) {
            android.util.Log.e("MainActivity", "Login/Join null — cannot setup");
            return;
        }

        if (session.isLoggedIn()) {
            btnLogin.setVisibility(View.GONE);
            btnJoin.setText("👤 " + session.getName());
            btnJoin.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
        } else {
            btnLogin.setVisibility(View.VISIBLE);
            btnJoin.setText("📝 رجسٹر کریں");
            btnLogin.setOnClickListener(v -> startActivity(new Intent(this, LoginActivity.class)));
            btnJoin.setOnClickListener(v -> startActivity(new Intent(this, SellerRegistrationActivity.class)));
        }
    }

    // ============================================================
    // CATEGORIES (Village-Based)
    // ============================================================
    private void setupCategoryButtonsText() {
        if (layoutCategories == null) {
            android.util.Log.e("MainActivity", "layoutCategories is null!");
            return;
        }

        layoutCategories.removeAllViews();

        for (int i = 0; i < Constants.CATEGORIES.length; i++) {
            String cat = Constants.CATEGORIES[i];
            String emoji = Constants.CATEGORY_EMOJIS[i];

            Button b = new Button(this);
            b.setText(emoji + " " + cat);
            b.setAllCaps(false);
            b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFFFFF));
            b.setTextColor(0xFFFF4081);
            b.setTextSize(14);
            b.setPadding(24, 16, 24, 16);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(16);
            b.setLayoutParams(lp);

            b.setOnClickListener(v -> {
                Intent intent = new Intent(this, SellerListActivity.class);
                intent.putExtra(SellerListActivity.EXTRA_CATEGORY, cat);
                startActivity(intent);
            });

            layoutCategories.addView(b);
        }
    }

    // ============================================================
    // RECYCLERVIEWS
    // ============================================================
    private void setupRecyclerViews() {
        if (rvTopSellers == null || rvLatestProducts == null) {
            android.util.Log.e("MainActivity", "RecyclerView is null!");
            return;
        }

        rvTopSellers.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        topSellerAdapter = new SellerAdapter(topSellers, this::openSellerProfile);
        rvTopSellers.setAdapter(topSellerAdapter);

        rvLatestProducts.setLayoutManager(new GridLayoutManager(this, 2));
        productAdapter = new ProductAdapter(allProducts,
                this::openProductDetail,
                product -> requireBuyerLogin(() -> goToOrder(product)),
                product -> chatOnWhatsApp(product.getSellerId()));
        rvLatestProducts.setAdapter(productAdapter);

        allSellerAdapter = new SellerAdapter(allSellers, this::openSellerProfile);

        tvProductsTab.setOnClickListener(v -> switchTab(true));
        tvSellersTab.setOnClickListener(v -> switchTab(false));
    }

    private void switchTab(boolean products) {
        tvProductsTab.setAlpha(products ? 1f : 0.5f);
        tvSellersTab.setAlpha(products ? 0.5f : 1f);
        if (products) {
            rvLatestProducts.setLayoutManager(new GridLayoutManager(this, 2));
            rvLatestProducts.setAdapter(productAdapter);
        } else {
            rvLatestProducts.setLayoutManager(new LinearLayoutManager(this));
            rvLatestProducts.setAdapter(allSellerAdapter);
        }
    }

    // ============================================================
    // LOAD DATA
    // ============================================================
    private void loadSellers() {
        FirebaseDatabase.getInstance().getReference()
                .child(Constants.NODE_SELLERS)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        allSellers.clear();
                        topSellers.clear();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            Seller s = child.getValue(Seller.class);
                            if (s == null) continue;
                            if (!"approved".equals(s.getVerificationStatus())) continue;
                            allSellers.add(s);
                            if (s.isTopRated()) topSellers.add(s);
                        }
                        Collections.sort(topSellers, (a, b) -> Double.compare(b.getRating(), a.getRating()));
                        topSellerAdapter.notifyDataSetChanged();
                        allSellerAdapter.notifyDataSetChanged();
                    }
                    @Override
                    public void onCancelled(DatabaseError error) {
                        Toast.makeText(MainActivity.this, "Error loading sellers", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadProducts() {
        FirebaseDatabase.getInstance().getReference()
                .child(Constants.NODE_PRODUCTS)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        allProducts.clear();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            Product p = child.getValue(Product.class);
                            if (p != null) allProducts.add(p);
                        }
                        productAdapter.notifyDataSetChanged();
                    }
                    @Override
                    public void onCancelled(DatabaseError error) {
                        Toast.makeText(MainActivity.this, "Error loading products", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ============================================================
    // SEARCH
    // ============================================================
    private void filterSearch(String query) {
        String q = query.toLowerCase().trim();
        if (q.isEmpty()) {
            productAdapter.updateList(allProducts);
            allSellerAdapter.updateList(allSellers);
            return;
        }
        List<Product> filteredP = new ArrayList<>();
        for (Product p : allProducts) {
            if (p.getName().toLowerCase().contains(q)) filteredP.add(p);
        }
        productAdapter.updateList(filteredP);

        List<Seller> filteredS = new ArrayList<>();
        for (Seller s : allSellers) {
            if (s.getBusinessName().toLowerCase().contains(q) ||
                    s.getCategory().toLowerCase().contains(q)) {
                filteredS.add(s);
            }
        }
        allSellerAdapter.updateList(filteredS);
    }

    // ============================================================
    // NAVIGATION HELPERS
    // ============================================================
    private void openSellerProfile(Seller seller) {
        Intent intent = new Intent(this, SellerProfileActivity.class);
        intent.putExtra(SellerProfileActivity.EXTRA_SELLER_ID, seller.getUid());
        startActivity(intent);
    }

    private void openProductDetail(Product product) {
        Intent intent = new Intent(this, ProductDetailActivity.class);
        intent.putExtra(ProductDetailActivity.EXTRA_PRODUCT_ID, product.getId());
        startActivity(intent);
    }

    private void goToOrder(Product product) {
        Intent intent = new Intent(this, PlaceOrderActivity.class);
        intent.putExtra(PlaceOrderActivity.EXTRA_SELLER_ID, product.getSellerId());
        intent.putExtra(PlaceOrderActivity.EXTRA_PRODUCT_ID, product.getId());
        intent.putExtra(PlaceOrderActivity.EXTRA_PRODUCT_NAME, product.getName());
        intent.putExtra(PlaceOrderActivity.EXTRA_PRODUCT_PRICE, product.getPrice());
        startActivity(intent);
    }

    private void chatOnWhatsApp(String sellerId) {
        FirebaseDatabase.getInstance().getReference()
                .child(Constants.NODE_SELLERS).child(sellerId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        Seller s = snapshot.getValue(Seller.class);
                        if (s != null) {
                            WhatsAppHelper.openChat(MainActivity.this, s.getWhatsappNumber(), null);
                        }
                    }
                    @Override
                    public void onCancelled(DatabaseError error) {}
                });
    }

    private void requireBuyerLogin(Runnable action) {
        if (!session.isLoggedIn()) {
            Toast.makeText(this, "براہ کرم پہلے لاگ ان کریں", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            return;
        }
        if (!session.isBuyer()) {
            Toast.makeText(this, "صرف خریدار آرڈر دے سکتے ہیں۔", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, BuyerRegisterActivity.class));
            return;
        }
        action.run();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tts != null) tts.shutdown();
    }
}