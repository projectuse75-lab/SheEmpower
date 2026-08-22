package com.sheempower.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
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

        ivSellerAvatar = findViewById(R.id.ivSellerAvatar);
        tvName = findViewById(R.id.tvSellerName);
        tvCategory = findViewById(R.id.tvSellerCategory);
        tvCity = findViewById(R.id.tvSellerCity);
        tvBio = findViewById(R.id.tvSellerBio);
        tvRating = findViewById(R.id.tvSellerRating);
        tvStatus = findViewById(R.id.tvSellerStatus);
        btnPlaceOrder = findViewById(R.id.btnPlaceOrder);
        btnWhatsAppChat = findViewById(R.id.btnWhatsAppChat);
        rvProducts = findViewById(R.id.rvSellerProducts);

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
                        "Assalam o Alaikum! Mujhe " + product.getName() + " ke baare mein poochna tha.")
        );
        rvProducts.setAdapter(productAdapter);

        // ✅ Speaker icon (assumes you have a speaker button in the layout)
        findViewById(R.id.btnSpeaker).setOnClickListener(v -> {
            if (currentSeller != null) {
                String instruction = "Seller profile for " + currentSeller.getBusinessName() +
                        ". Category: " + currentSeller.getCategory() +
                        ". You can place an order or chat with the seller via WhatsApp.";
                tts.speak(instruction);
            }
        });

        loadSellerData();
        loadSellerProducts();

        btnPlaceOrder.setOnClickListener(v -> requireBuyerLogin(() -> {
            Intent i = new Intent(this, PlaceOrderActivity.class);
            i.putExtra(PlaceOrderActivity.EXTRA_SELLER_ID, sellerId);
            startActivity(i);
        }));

        btnWhatsAppChat.setOnClickListener(v -> WhatsAppHelper.openChat(this,
                currentSeller != null ? currentSeller.getWhatsappNumber() : null, null));

        // ✅ Speak screen instruction on create (if desired)
        tts.speak("Seller profile screen loaded.");
    }

    private void loadSellerData() {
        FirebaseDatabase.getInstance().getReference()
                .child(Constants.NODE_SELLERS).child(sellerId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        currentSeller = snapshot.getValue(Seller.class);
                        if (currentSeller == null) return;
                        tvName.setText(currentSeller.getBusinessName());
                        tvCategory.setText(currentSeller.getCategory());
                        tvCity.setText(currentSeller.getCity());
                        tvBio.setText(currentSeller.getBio());
                        tvRating.setText(String.format("⭐ %.1f (%d)",
                                currentSeller.getRating(), currentSeller.getRatingCount()));
                        tvStatus.setText("✓ " + currentSeller.getVerificationStatus());
                        if (currentSeller.getImageUrl() != null && !currentSeller.getImageUrl().isEmpty())
                            Glide.with(SellerProfileActivity.this).load(currentSeller.getImageUrl()).into(ivSellerAvatar);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {}
                });
    }

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
                            if (p != null) productList.add(p);
                        }
                        productAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {}
                });
    }

    private void goToOrder(Product product) {
        Intent i = new Intent(this, PlaceOrderActivity.class);
        i.putExtra(PlaceOrderActivity.EXTRA_SELLER_ID, sellerId);
        i.putExtra(PlaceOrderActivity.EXTRA_PRODUCT_ID, product.getId());
        i.putExtra(PlaceOrderActivity.EXTRA_PRODUCT_NAME, product.getName());
        i.putExtra(PlaceOrderActivity.EXTRA_PRODUCT_PRICE, product.getPrice());
        startActivity(i);
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