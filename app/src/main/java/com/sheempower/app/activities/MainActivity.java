package com.sheempower.app.activities;

import android.content.Intent;
import android.text.Editable;
import android.text.TextWatcher;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.firebase.database.*;
import com.sheempower.app.R;
import com.sheempower.app.adapters.ProductAdapter;
import com.sheempower.app.adapters.SellerAdapter;
import com.sheempower.app.models.Product;
import com.sheempower.app.models.Seller;
import com.sheempower.app.utils.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private EditText etSearch;
    private Button btnLogin, btnJoin;
    private LinearLayout layoutCategories;
    private RecyclerView rvTopSellers, rvLatestProducts;
    private TextView tvProductsTab, tvSellersTab;

    private SellerAdapter topSellerAdapter, allSellerAdapter;
    private ProductAdapter productAdapter;

    private final List<Seller> topSellers = new ArrayList<>();
    private final List<Product> allProducts = new ArrayList<>();
    private final List<Seller> allSellers = new ArrayList<>();
    private final Set<String> verifiedSellerIds = new HashSet<>();

    private SessionManager session;
    private boolean voiceAnnounced = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        session = new SessionManager(this);

        etSearch         = findViewById(R.id.etSearch);
        btnLogin         = findViewById(R.id.btnLogin);
        btnJoin          = findViewById(R.id.btnJoin);
        layoutCategories = findViewById(R.id.layoutCategories);
        rvTopSellers     = findViewById(R.id.rvTopSellers);
        rvLatestProducts = findViewById(R.id.rvLatestProducts);
        tvProductsTab    = findViewById(R.id.tvProductsTab);
        tvSellersTab     = findViewById(R.id.tvSellersTab);

        setupTopButtons();
        setupCategoryButtons();
        setupRecyclerViews();

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { filterSearch(s.toString()); }
            @Override public void afterTextChanged(Editable s) {}
        });

        loadSellersThenProducts();
        BottomNavHelper.setupForBuyerOrVisitor(this, findViewById(R.id.bottomNav), 1);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupTopButtons();
        loadSellersThenProducts();
        if (!voiceAnnounced) {
            voiceAnnounced = true;
            VoiceGuide.announceScreen(this,
                    session.isLoggedIn()
                            ? "شی ایمپاور میں خوش آمدید۔ یہاں سے کسی زمرے کو منتخب کر کے بیچنے والیاں اور اشیاء دیکھیں، اور آرڈر دیں۔"
                            : "شی ایمپاور میں خوش آمدید۔ خریداری کے لیے لاگ ان کریں، یا اپنا کام شروع کرنے کے لیے رجسٹر کریں۔");
        }
    }

    private void setupTopButtons() {
        if (session.isLoggedIn()) {
            btnLogin.setVisibility(View.GONE);
            btnJoin.setText("👤 " + session.getName());
            btnJoin.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
        } else {
            btnLogin.setVisibility(View.VISIBLE);
            btnLogin.setText("🔑 لاگ ان");
            btnJoin.setText("📝 رجسٹر");
            btnLogin.setOnClickListener(v -> startActivity(new Intent(this, LoginActivity.class)));
            btnJoin.setOnClickListener(v -> startActivity(new Intent(this, SellerRegistrationActivity.class)));
        }
    }

    private void setupCategoryButtons() {
        layoutCategories.removeAllViews();
        for (int i = 0; i < Constants.CATEGORIES.length; i++) {
            String cat = Constants.CATEGORIES[i];
            String urduLabel = Constants.CATEGORY_LABELS_UR[i];

            View card = LayoutInflater.from(this).inflate(R.layout.item_category, layoutCategories, false);
            de.hdodenhof.circleimageview.CircleImageView iv = card.findViewById(R.id.ivCategoryImage);
            TextView tvName   = card.findViewById(R.id.tvCategoryName);
            TextView tvNameEn = card.findViewById(R.id.tvCategoryNameEn);

            tvName.setText(urduLabel);
            tvNameEn.setText(cat);

            iv.setImageResource(Constants.CATEGORY_IMAGE_RES[i]);

            card.setOnClickListener(v -> {
                Intent intent = new Intent(this, SellerListActivity.class);
                intent.putExtra(SellerListActivity.EXTRA_CATEGORY, cat);
                startActivity(intent);
            });
            layoutCategories.addView(card);
        }
    }

    private void setupRecyclerViews() {
        rvTopSellers.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        topSellerAdapter = new SellerAdapter(topSellers, this::openSellerProfile);
        rvTopSellers.setAdapter(topSellerAdapter);

        rvLatestProducts.setLayoutManager(new GridLayoutManager(this, 2));
        productAdapter = new ProductAdapter(allProducts, this::openProductDetail,
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

    private void loadSellersThenProducts() {
        FirebaseDatabase.getInstance().getReference().child(Constants.NODE_SELLERS)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(DataSnapshot snapshot) {
                        allSellers.clear(); topSellers.clear(); verifiedSellerIds.clear();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            Seller s = child.getValue(Seller.class);
                            if (s == null || !Constants.isApproved(s.getVerificationStatus())) continue;

                            String uid = child.getKey();
                            if (uid == null) continue;
                            s.setUid(uid.trim());

                            allSellers.add(s);
                            verifiedSellerIds.add(uid.trim());
                            if (s.isTopRated()) topSellers.add(s);
                        }
                        Collections.sort(topSellers, (a, b) -> Double.compare(b.getRating(), a.getRating()));
                        topSellerAdapter.notifyDataSetChanged();
                        allSellerAdapter.notifyDataSetChanged();
                        loadProducts();
                    }
                    @Override public void onCancelled(DatabaseError error) {
                        Toast.makeText(MainActivity.this, "ڈیٹا لوڈ نہیں ہوا: " + error.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void loadProducts() {
        FirebaseDatabase.getInstance().getReference().child(Constants.NODE_PRODUCTS)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(DataSnapshot snapshot) {
                        allProducts.clear();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            Product p = child.getValue(Product.class);
                            if (p != null) {
                                String pid = child.getKey();
                                if (pid != null) p.setId(pid.trim());

                                String sId = p.getSellerId() != null ? p.getSellerId().trim() : "";
                                if (verifiedSellerIds.contains(sId)) {
                                    p.setSellerId(sId);
                                    allProducts.add(p);
                                }
                            }
                        }
                        productAdapter.notifyDataSetChanged();
                    }
                    @Override public void onCancelled(DatabaseError error) {}
                });
    }

    private void filterSearch(String query) {
        String q = query.toLowerCase().trim();
        if (q.isEmpty()) { productAdapter.updateList(allProducts); allSellerAdapter.updateList(allSellers); return; }
        List<Product> fp = new ArrayList<>();
        for (Product p : allProducts) if (p.getName().toLowerCase().contains(q)) fp.add(p);
        productAdapter.updateList(fp);
        List<Seller> fs = new ArrayList<>();
        for (Seller s : allSellers) if (s.getBusinessName().toLowerCase().contains(q) || s.getCategory().toLowerCase().contains(q)) fs.add(s);
        allSellerAdapter.updateList(fs);
    }

    private void openSellerProfile(Seller seller) {
        Intent i = new Intent(this, SellerProfileActivity.class);
        i.putExtra(SellerProfileActivity.EXTRA_SELLER_ID, seller.getUid());
        startActivity(i);
    }

    private void openProductDetail(Product product) {
        Intent i = new Intent(this, ProductDetailActivity.class);
        i.putExtra(ProductDetailActivity.EXTRA_PRODUCT_ID, product.getId());
        startActivity(i);
    }

    private void goToOrder(Product product) {
        Intent i = new Intent(this, PlaceOrderActivity.class);
        i.putExtra(PlaceOrderActivity.EXTRA_SELLER_ID, product.getSellerId());
        i.putExtra(PlaceOrderActivity.EXTRA_PRODUCT_ID, product.getId());
        i.putExtra(PlaceOrderActivity.EXTRA_PRODUCT_NAME, product.getName());
        i.putExtra(PlaceOrderActivity.EXTRA_PRODUCT_PRICE, product.getPrice());
        startActivity(i);
    }

    private void chatOnWhatsApp(String sellerId) {
        FirebaseDatabase.getInstance().getReference().child(Constants.NODE_SELLERS).child(sellerId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(DataSnapshot snapshot) {
                        Seller s = snapshot.getValue(Seller.class);
                        if (s != null) WhatsAppHelper.openChat(MainActivity.this, s.getWhatsappNumber(), null);
                    }
                    @Override public void onCancelled(DatabaseError error) {}
                });
    }

    private void requireBuyerLogin(Runnable action) {
        if (session.isLoggedIn()) action.run();
        else { Toast.makeText(this, "پہلے لاگ ان کریں", Toast.LENGTH_SHORT).show(); startActivity(new Intent(this, LoginActivity.class)); }
    }
}