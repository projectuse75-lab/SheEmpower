package com.sheempower.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.firebase.database.*;
import com.sheempower.app.R;
import com.sheempower.app.adapters.ProductAdapter;
import com.sheempower.app.models.Product;
import com.sheempower.app.models.Seller;
import com.sheempower.app.utils.*;
import de.hdodenhof.circleimageview.CircleImageView;
import java.util.ArrayList;
import java.util.List;

public class SellerProfileActivity extends AppCompatActivity {

    public static final String EXTRA_SELLER_ID = "sellerId";

    private CircleImageView ivSellerAvatar;
    private TextView tvName, tvCategory, tvCity, tvBio, tvRating, tvStatus;
    private TextView tvCapacity;  // ✅ NEW: Capacity Description
    private Button btnPlaceOrder, btnWhatsAppChat;
    private RecyclerView rvProducts;
    private ProductAdapter productAdapter;
    private final List<Product> productList = new ArrayList<>();

    private String sellerId;
    private Seller currentSeller;
    private SessionManager session;
    private TTSHelper tts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seller_profile);

        session = new SessionManager(this);
        tts = TTSHelper.getInstance(this);
        sellerId = getIntent().getStringExtra(EXTRA_SELLER_ID);

        // Find Views
        ivSellerAvatar = findViewById(R.id.ivSellerAvatar);
        tvName = findViewById(R.id.tvSellerName);
        tvCategory = findViewById(R.id.tvSellerCategory);
        tvCity = findViewById(R.id.tvSellerCity);
        tvBio = findViewById(R.id.tvSellerBio);
        tvRating = findViewById(R.id.tvSellerRating);
        tvStatus = findViewById(R.id.tvSellerStatus);
        tvCapacity = findViewById(R.id.tvSellerCapacity);  // ✅ NEW
        btnPlaceOrder = findViewById(R.id.btnPlaceOrder);
        btnWhatsAppChat = findViewById(R.id.btnWhatsAppChat);
        rvProducts = findViewById(R.id.rvSellerProducts);

        // ✅ Speaker Icon
        findViewById(R.id.btnSpeaker).setOnClickListener(v -> {
            VoiceGuide.announceScreen(this,
                    "یہ بیچنے والی کی پروفائل ہے۔ آپ یہاں سے آرڈر دے سکتی ہیں یا واٹس ایپ پر براہ راست بات کر سکتی ہیں۔");
        });

        VoiceGuide.announceScreen(this,
                "یہ بیچنے والی کی پروفائل ہے۔ آپ یہاں سے آرڈر دے سکتی ہیں یا واٹس ایپ پر براہ راست بات کر سکتی ہیں۔");

        // Setup RecyclerView
        rvProducts.setLayoutManager(new GridLayoutManager(this, 2));
        productAdapter = new ProductAdapter(productList,
                product -> {
                    Intent i = new Intent(this, ProductDetailActivity.class);
                    i.putExtra(ProductDetailActivity.EXTRA_PRODUCT_ID, product.getId());
                    startActivity(i);
                },
                product -> requireBuyerLogin(() -> goToOrder(product)),
                product -> WhatsAppHelper.openChat(this,
                        currentSeller != null ? currentSeller.getWhatsappNumber() : null,
                        "السلام علیکم! مجھے " + product.getName() + " کے بارے میں پوچھنا تھا۔")
        );
        rvProducts.setAdapter(productAdapter);

        loadSellerData();
        loadSellerProducts();

        // Place Order Button
        btnPlaceOrder.setOnClickListener(v -> requireBuyerLogin(() -> {
            Intent i = new Intent(this, PlaceOrderActivity.class);
            i.putExtra(PlaceOrderActivity.EXTRA_SELLER_ID, sellerId);
            startActivity(i);
        }));

        // WhatsApp Chat Button
        btnWhatsAppChat.setOnClickListener(v -> WhatsAppHelper.openChat(this,
                currentSeller != null ? currentSeller.getWhatsappNumber() : null, null));
    }

    // ============================================================
    // LOAD SELLER DATA
    // ============================================================
    private void loadSellerData() {
        FirebaseDatabase.getInstance().getReference()
                .child(Constants.NODE_SELLERS).child(sellerId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        currentSeller = snapshot.getValue(Seller.class);
                        if (currentSeller == null) {
                            Toast.makeText(SellerProfileActivity.this, "بیچنے والی نہیں ملی / Seller not found", Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }

                        tvName.setText(currentSeller.getBusinessName());
                        tvCategory.setText(currentSeller.getCategory());
                        tvCity.setText(currentSeller.getCity());
                        tvBio.setText(currentSeller.getBio());
                        tvRating.setText(String.format("⭐ %.1f (%d خریداریاں)",
                                currentSeller.getRating(), currentSeller.getRatingCount()));

                        // ✅ Capacity Description
                        if (currentSeller.getCapacityDescription() != null && !currentSeller.getCapacityDescription().isEmpty()) {
                            tvCapacity.setText("🙋‍♀️ " + currentSeller.getCapacityDescription());
                            tvCapacity.setVisibility(android.view.View.VISIBLE);
                        } else {
                            tvCapacity.setVisibility(android.view.View.GONE);
                        }

                        // Bilingual Status Display
                        String statusText;
                        if ("approved".equals(currentSeller.getVerificationStatus())) {
                            statusText = "✅ منظور شدہ / Approved";
                        } else if ("pending".equals(currentSeller.getVerificationStatus())) {
                            statusText = "⏳ زیرِ جائزہ / Pending";
                        } else if ("rejected".equals(currentSeller.getVerificationStatus())) {
                            statusText = "❌ مسترد / Rejected";
                        } else {
                            statusText = "✓ " + currentSeller.getVerificationStatus();
                        }
                        tvStatus.setText(statusText);

                        // Load Avatar
                        if (currentSeller.getImageUrl() != null && !currentSeller.getImageUrl().isEmpty()) {
                            Glide.with(SellerProfileActivity.this)
                                    .load(currentSeller.getImageUrl())
                                    .placeholder(R.drawable.placeholder_avatar)
                                    .into(ivSellerAvatar);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Toast.makeText(SellerProfileActivity.this, "Error loading seller", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ============================================================
    // LOAD SELLER PRODUCTS
    // ============================================================
    private void loadSellerProducts() {
        FirebaseDatabase.getInstance().getReference()
                .child(Constants.NODE_PRODUCTS)
                .orderByChild("sellerId").equalTo(sellerId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        productList.clear();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            Product p = child.getValue(Product.class);
                            if (p != null) {
                                productList.add(p);
                            }
                        }
                        productAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Toast.makeText(SellerProfileActivity.this, "Error loading products", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ============================================================
    // GO TO ORDER
    // ============================================================
    private void goToOrder(Product product) {
        Intent i = new Intent(this, PlaceOrderActivity.class);
        i.putExtra(PlaceOrderActivity.EXTRA_SELLER_ID, sellerId);
        i.putExtra(PlaceOrderActivity.EXTRA_PRODUCT_ID, product.getId());
        i.putExtra(PlaceOrderActivity.EXTRA_PRODUCT_NAME, product.getName());
        i.putExtra(PlaceOrderActivity.EXTRA_PRODUCT_PRICE, product.getPrice());
        i.putExtra(PlaceOrderActivity.EXTRA_CATEGORY, product.getCategory());
        startActivity(i);
    }

    // ============================================================
    // BUYER LOGIN CHECK
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
    // ON DESTROY
    // ============================================================
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // TTSHelper is a singleton — don't shut it down here
    }
}