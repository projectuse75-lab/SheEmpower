package com.sheempower.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
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

        // ✅ Speaker Icon for TTS Voice Guidance
        findViewById(R.id.btnSpeaker).setOnClickListener(v -> {
            String instruction = "Product detail for " +
                    (currentProduct != null ? currentProduct.getName() : "this product") +
                    ". You can place an order, view the seller profile, or chat via WhatsApp.";
            tts.speak(instruction);
        });

        loadProduct(productId);

        // WhatsApp Chat Button — opens WhatsApp directly
        btnWhatsApp.setOnClickListener(v -> WhatsAppHelper.openChat(this,
                currentSeller != null ? currentSeller.getWhatsappNumber() : null,
                currentProduct != null ? "Assalam o Alaikum! Mujhe " + currentProduct.getName() + " ke baare mein poochna tha." : null));

        btnPlaceOrder.setOnClickListener(v -> requireBuyerLogin(() -> {
            if (currentProduct == null) return;
            Intent i = new Intent(this, PlaceOrderActivity.class);
            i.putExtra(PlaceOrderActivity.EXTRA_SELLER_ID, currentProduct.getSellerId());
            i.putExtra(PlaceOrderActivity.EXTRA_PRODUCT_ID, currentProduct.getId());
            i.putExtra(PlaceOrderActivity.EXTRA_PRODUCT_NAME, currentProduct.getName());
            i.putExtra(PlaceOrderActivity.EXTRA_PRODUCT_PRICE, currentProduct.getPrice());
            startActivity(i);
        }));

        // ✅ Auto-speak screen instruction when loaded
        tts.speak("Product detail screen loaded.");
    }

    private void loadProduct(String productId) {
        FirebaseDatabase.getInstance().getReference()
                .child(Constants.NODE_PRODUCTS).child(productId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        currentProduct = snapshot.getValue(Product.class);
                        if (currentProduct == null) {
                            Toast.makeText(ProductDetailActivity.this, "Product not found", Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }
                        tvName.setText(currentProduct.getName());
                        tvPrice.setText("PKR " + (long) currentProduct.getPrice());
                        tvDescription.setText(currentProduct.getDescription());
                        if (currentProduct.getImageUrl() != null && !currentProduct.getImageUrl().isEmpty()) {
                            Glide.with(ProductDetailActivity.this)
                                    .load(currentProduct.getImageUrl())
                                    .placeholder(R.drawable.placeholder_product)
                                    .into(ivProduct);
                        }
                        loadSeller(currentProduct.getSellerId());
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Toast.makeText(ProductDetailActivity.this, "Error loading product", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadSeller(String sellerId) {
        FirebaseDatabase.getInstance().getReference()
                .child(Constants.NODE_SELLERS).child(sellerId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        currentSeller = snapshot.getValue(Seller.class);
                        if (currentSeller == null) return;

                        tvSellerName.setText(currentSeller.getBusinessName());
                        tvSellerMeta.setText(currentSeller.getCategory() + " · " + currentSeller.getCity());
                        tvSellerRating.setText("⭐ " + currentSeller.getRating() +
                                (currentSeller.isTopRated() ? "  🏅 Top Rated" : ""));

                        btnViewSeller.setOnClickListener(v -> {
                            Intent i = new Intent(ProductDetailActivity.this, SellerProfileActivity.class);
                            i.putExtra(SellerProfileActivity.EXTRA_SELLER_ID, sellerId);
                            startActivity(i);
                        });
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {}
                });
    }

    // ✅ FIXED: Also check if user is actually a buyer
    private void requireBuyerLogin(Runnable action) {
        if (!session.isLoggedIn()) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            return;
        }
        if (!session.isBuyer()) {
            Toast.makeText(this, "Only buyers can place orders. Please register as a buyer.", Toast.LENGTH_LONG).show();
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