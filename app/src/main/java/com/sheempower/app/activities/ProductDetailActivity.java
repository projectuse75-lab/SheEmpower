package com.sheempower.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.google.firebase.database.*;
import com.sheempower.app.R;
import com.sheempower.app.models.Product;
import com.sheempower.app.models.Seller;
import com.sheempower.app.utils.*;
import com.sheempower.app.utils.PreferencesManager;

public class ProductDetailActivity extends AppCompatActivity {

    public static final String EXTRA_PRODUCT_ID = "productId";

    private ImageView ivProduct;
    private TextView tvName, tvPrice, tvSellerName, tvSellerMeta, tvSellerRating, tvDescription;
    private Button btnViewSeller, btnPlaceOrder, btnWhatsApp;

    private Product currentProduct;
    private Seller currentSeller;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);

        session = new SessionManager(this);
        String productId = getIntent().getStringExtra(EXTRA_PRODUCT_ID);

        if (productId == null) { Toast.makeText(this, "چیز نہیں ملی", Toast.LENGTH_LONG).show(); finish(); return; }

        VoiceGuide.announceScreen(this,
                "یہ چیز کی مکمل تفصیل ہے۔ اس کی قیمت پہلے سے مقرر ہے۔ آرڈر دینے کے لیے نیچے بٹن دبائیں، یا واٹس ایپ پر بات کرنے کے لیے سبز بٹن دبائیں۔");

        ivProduct      = findViewById(R.id.ivProduct);
        tvName         = findViewById(R.id.tvProductName);
        tvPrice        = findViewById(R.id.tvProductPrice);
        tvSellerName   = findViewById(R.id.tvSellerName);
        tvSellerMeta   = findViewById(R.id.tvSellerMeta);
        tvSellerRating = findViewById(R.id.tvSellerRating);
        tvDescription  = findViewById(R.id.tvDescription);
        btnViewSeller  = findViewById(R.id.btnViewSeller);
        btnPlaceOrder  = findViewById(R.id.btnPlaceOrder);
        btnWhatsApp    = findViewById(R.id.btnWhatsApp);

        loadProduct(productId);

        btnWhatsApp.setOnClickListener(v -> WhatsAppHelper.openChat(this,
                currentSeller != null ? currentSeller.getWhatsappNumber() : null,
                currentProduct != null ? "السلام علیکم! مجھے " + currentProduct.getName() + " کے بارے میں پوچھنا تھا۔" : null));

        btnPlaceOrder.setOnClickListener(v -> requireBuyerLogin(() -> {
            if (currentProduct == null) return;
            Intent i = new Intent(this, PlaceOrderActivity.class);
            i.putExtra(PlaceOrderActivity.EXTRA_SELLER_ID, currentProduct.getSellerId());
            i.putExtra(PlaceOrderActivity.EXTRA_PRODUCT_ID, currentProduct.getId());
            i.putExtra(PlaceOrderActivity.EXTRA_PRODUCT_NAME, currentProduct.getName());
            i.putExtra(PlaceOrderActivity.EXTRA_PRODUCT_PRICE, currentProduct.getPrice());
            i.putExtra(PlaceOrderActivity.EXTRA_PRODUCT_CATEGORY, currentProduct.getCategory());
            startActivity(i);
        }));
    }

    private void loadProduct(String productId) {
        DatabaseReference prodRef = FirebaseDatabase.getInstance().getReference().child(Constants.NODE_PRODUCTS);
        
        // 1. Pehle direct key se check karein
        prodRef.child(productId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(DataSnapshot snapshot) {
                currentProduct = snapshot.getValue(Product.class);
                if (currentProduct != null) {
                    setupProductUI(snapshot.getKey());
                } else {
                    // 2. Agar direct nahi mila (ID mismatch), toh poori list scan karein
                    scanAllProductsForId(productId);
                }
            }
            @Override public void onCancelled(DatabaseError error) {}
        });
    }

    private void scanAllProductsForId(String targetId) {
        FirebaseDatabase.getInstance().getReference().child(Constants.NODE_PRODUCTS)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(DataSnapshot snapshot) {
                        for (DataSnapshot child : snapshot.getChildren()) {
                            String key = child.getKey();
                            if (key != null && key.trim().equals(targetId.trim())) {
                                currentProduct = child.getValue(Product.class);
                                if (currentProduct != null) {
                                    setupProductUI(key);
                                    return;
                                }
                            }
                        }
                        Toast.makeText(ProductDetailActivity.this, "چیز اب دستیاب نہیں ہے", Toast.LENGTH_LONG).show();
                    }
                    @Override public void onCancelled(DatabaseError error) {}
                });
    }

    private void setupProductUI(String actualId) {
        if (currentProduct.getId() == null) currentProduct.setId(actualId);
        tvName.setText(currentProduct.getName());
        tvPrice.setText("PKR " + (long) currentProduct.getPrice());
        tvDescription.setText(currentProduct.getDescription());
        if (currentProduct.getImageUrl() != null && !currentProduct.getImageUrl().isEmpty())
            Glide.with(ProductDetailActivity.this).load(currentProduct.getImageUrl()).into(ivProduct);
        if (currentProduct.getSellerId() != null && !currentProduct.getSellerId().trim().isEmpty()) {
            loadSeller(currentProduct.getSellerId().trim());
        }
    }

    private void loadSeller(String sellerId) {
        if (sellerId == null || sellerId.trim().isEmpty()) return;
        FirebaseDatabase.getInstance().getReference().child(Constants.NODE_SELLERS).child(sellerId.trim())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(DataSnapshot snapshot) {
                        currentSeller = snapshot.getValue(Seller.class);
                        if (currentSeller == null) return;

                        // ✅ FIX: UID fallback
                        if (currentSeller.getUid() == null || currentSeller.getUid().isEmpty())
                            currentSeller.setUid(snapshot.getKey());

                        tvSellerName.setText(currentSeller.getBusinessName());
                        tvSellerMeta.setText(Constants.getUrduLabel(currentSeller.getCategory()) + " · " + currentSeller.getCity());
                        tvSellerRating.setText("⭐ " + currentSeller.getRating() + (currentSeller.isTopRated() ? "  🏅 بہترین" : ""));
                        btnViewSeller.setOnClickListener(v -> {
                            if (sellerId.isEmpty()) {
                                Toast.makeText(ProductDetailActivity.this, "بیچنے والی کی معلومات نہیں ملی", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            Intent i = new Intent(ProductDetailActivity.this, SellerProfileActivity.class);
                            i.putExtra(SellerProfileActivity.EXTRA_SELLER_ID, sellerId);
                            startActivity(i);
                        });
                    }
                    @Override public void onCancelled(DatabaseError error) {}
                });
    }

    private void requireBuyerLogin(Runnable action) {
        if (session.isLoggedIn()) action.run();
        else { Toast.makeText(this, "پہلے لاگ ان کریں", Toast.LENGTH_SHORT).show(); startActivity(new Intent(this, LoginActivity.class)); }
    }

    // ✅ DARK THEME - ADD AT END OF CLASS
    private void applyTheme() {
        PreferencesManager prefs = new PreferencesManager(this);
        if (prefs.isDarkThemeEnabled()) {
            setTheme(R.style.Theme_SheEmpower_Dark);
        } else {
            setTheme(R.style.Theme_SheEmpower);
        }
    }
}