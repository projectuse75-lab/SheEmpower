package com.sheempower.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.google.firebase.database.*;
import com.sheempower.app.R;
import com.sheempower.app.models.Product;
import com.sheempower.app.models.Seller;
import com.sheempower.app.utils.*;

public class ProductDetailActivity extends AppCompatActivity {

    public static final String EXTRA_PRODUCT_ID = "productId";

    private ImageView ivProduct;
    private TextView tvName, tvPrice, tvSellerName, tvSellerMeta, tvSellerRating, tvDescription;
    private Button btnViewSeller, btnPlaceOrder, btnWhatsApp;

    private Product currentProduct;
    private Seller currentSeller;
    private SessionManager session;
    private TTSHelper tts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);

        session = new SessionManager(this);
        tts = TTSHelper.getInstance(this);
        String productId = getIntent().getStringExtra(EXTRA_PRODUCT_ID);

        // Find Views
        ivProduct = findViewById(R.id.ivProduct);
        tvName = findViewById(R.id.tvProductName);
        tvPrice = findViewById(R.id.tvProductPrice);
        tvSellerName = findViewById(R.id.tvSellerName);
        tvSellerMeta = findViewById(R.id.tvSellerMeta);
        tvSellerRating = findViewById(R.id.tvSellerRating);
        tvDescription = findViewById(R.id.tvDescription);
        btnViewSeller = findViewById(R.id.btnViewSeller);
        btnPlaceOrder = findViewById(R.id.btnPlaceOrder);
        btnWhatsApp = findViewById(R.id.btnWhatsApp);

        // ✅ Speaker Icon for Voice Guidance
        findViewById(R.id.btnSpeaker).setOnClickListener(v -> {
            VoiceGuide.announceScreen(this,
                    "یہ چیز کی تفصیل ہے۔ آرڈر دینے کے لیے نیچے بٹن دبائیں۔");
        });

        // ✅ Auto Voice on Load
        VoiceGuide.announceScreen(this,
                "یہ چیز کی تفصیل ہے۔ آرڈر دینے کے لیے نیچے بٹن دبائیں۔");

        loadProduct(productId);

        // ✅ WhatsApp Chat Button
        btnWhatsApp.setOnClickListener(v -> WhatsAppHelper.openChat(this,
                currentSeller != null ? currentSeller.getWhatsappNumber() : null,
                currentProduct != null ? "السلام علیکم! مجھے " + currentProduct.getName() + " کے بارے میں پوچھنا تھا۔" : null));

        // ✅ Place Order Button
        btnPlaceOrder.setOnClickListener(v -> requireBuyerLogin(() -> {
            if (currentProduct == null) return;
            Intent i = new Intent(this, PlaceOrderActivity.class);
            i.putExtra(PlaceOrderActivity.EXTRA_SELLER_ID, currentProduct.getSellerId());
            i.putExtra(PlaceOrderActivity.EXTRA_PRODUCT_ID, currentProduct.getId());
            i.putExtra(PlaceOrderActivity.EXTRA_PRODUCT_NAME, currentProduct.getName());
            i.putExtra(PlaceOrderActivity.EXTRA_PRODUCT_PRICE, currentProduct.getPrice());
            i.putExtra(PlaceOrderActivity.EXTRA_CATEGORY, currentProduct.getCategory()); // ✅ ADDED
            startActivity(i);
        }));

        // ✅ View Seller Button
        btnViewSeller.setOnClickListener(v -> {
            if (currentSeller != null) {
                Intent i = new Intent(this, SellerProfileActivity.class);
                i.putExtra(SellerProfileActivity.EXTRA_SELLER_ID, currentSeller.getUid());
                startActivity(i);
            } else {
                Toast.makeText(this, "بیچنے والی کی معلومات لوڈ نہیں ہوئی", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ============================================================
    // LOAD PRODUCT
    // ============================================================
    private void loadProduct(String productId) {
        FirebaseDatabase.getInstance().getReference()
                .child(Constants.NODE_PRODUCTS).child(productId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        currentProduct = snapshot.getValue(Product.class);
                        if (currentProduct == null) {
                            Toast.makeText(ProductDetailActivity.this, "چیز نہیں ملی / Product not found", Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }

                        tvName.setText(currentProduct.getName());
                        tvPrice.setText("PKR " + (long) currentProduct.getPrice());
                        tvDescription.setText(currentProduct.getDescription());

                        // ✅ Load Image with Placeholder
                        if (currentProduct.getImageUrl() != null && !currentProduct.getImageUrl().isEmpty()) {
                            Glide.with(ProductDetailActivity.this)
                                    .load(currentProduct.getImageUrl())
                                    .placeholder(R.drawable.placeholder_product)
                                    .into(ivProduct);
                        } else {
                            ivProduct.setImageResource(R.drawable.placeholder_product);
                        }

                        loadSeller(currentProduct.getSellerId());
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Toast.makeText(ProductDetailActivity.this, "Error loading product", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
    }

    // ============================================================
    // LOAD SELLER
    // ============================================================
    private void loadSeller(String sellerId) {
        FirebaseDatabase.getInstance().getReference()
                .child(Constants.NODE_SELLERS).child(sellerId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        currentSeller = snapshot.getValue(Seller.class);
                        if (currentSeller == null) {
                            Toast.makeText(ProductDetailActivity.this, "بیچنے والی نہیں ملی", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        tvSellerName.setText(currentSeller.getBusinessName());
                        tvSellerMeta.setText(currentSeller.getCategory() + " · " + currentSeller.getCity());

                        String ratingText = "⭐ " + currentSeller.getRating();
                        if (currentSeller.isTopRated()) {
                            ratingText += "  🏅 بہترین";
                        }
                        tvSellerRating.setText(ratingText);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Toast.makeText(ProductDetailActivity.this, "Error loading seller", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ============================================================
    // ✅ BUYER LOGIN CHECK (FIXED)
    // ============================================================
    private void requireBuyerLogin(Runnable action) {
        if (!session.isLoggedIn()) {
            Toast.makeText(this, "پہلے لاگ ان کریں / Please login first", Toast.LENGTH_SHORT).show();
            VoiceGuide.explain(this, "براہ کرم پہلے لاگ ان کریں");
            startActivity(new Intent(this, LoginActivity.class));
            return;
        }
        if (!session.isBuyer()) {
            Toast.makeText(this, "صرف خریدار آرڈر دے سکتے ہیں / Only buyers can order", Toast.LENGTH_LONG).show();
            VoiceGuide.explain(this, "صرف خریدار آرڈر دے سکتے ہیں");
            startActivity(new Intent(this, BuyerRegisterActivity.class));
            return;
        }
        action.run();
    }

    // ============================================================
    // ✅ ON DESTROY (FIXED)
    // ============================================================
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // TTSHelper is a singleton — don't shut it down here
    }
}