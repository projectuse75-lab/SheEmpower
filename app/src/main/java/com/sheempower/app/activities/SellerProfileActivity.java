package com.sheempower.app.activities;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
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
    private ImageView ivSellerVerifiedBadge;
    private TextView tvName, tvCategory, tvCity, tvBio, tvRating, tvStatus, tvCapacity, tvDeliveryBadge;
    private Button btnPlaceOrder, btnWhatsAppChat;
    private RecyclerView rvProducts;
    private ProductAdapter productAdapter;
    private final List<Product> productList = new ArrayList<>();

    // ✅ Admin-only panel
    private View layoutBuyerActions;
    private View layoutAdminReview;
    private Button btnViewCnic, btnPlayVoice, btnAdminApprove, btnAdminReject;
    private MediaPlayer mediaPlayer;

    private String sellerId;
    private Seller currentSeller;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seller_profile);

        session  = new SessionManager(this);
        sellerId = getIntent().getStringExtra(EXTRA_SELLER_ID);

        if (sellerId == null || sellerId.isEmpty()) {
            Toast.makeText(this, "بیچنے والی کی شناخت نہیں ملی", Toast.LENGTH_LONG).show();
            finish(); return;
        }

        ivSellerAvatar        = findViewById(R.id.ivSellerAvatar);
        ivSellerVerifiedBadge = findViewById(R.id.ivSellerVerifiedBadge);
        tvName          = findViewById(R.id.tvSellerName);
        tvCategory      = findViewById(R.id.tvSellerCategory);
        tvCity          = findViewById(R.id.tvSellerCity);
        tvBio           = findViewById(R.id.tvSellerBio);
        tvRating        = findViewById(R.id.tvSellerRating);
        tvStatus        = findViewById(R.id.tvSellerStatus);
        tvCapacity      = findViewById(R.id.tvSellerCapacity);
        tvDeliveryBadge = findViewById(R.id.tvSellerDeliveryBadge);
        btnPlaceOrder   = findViewById(R.id.btnPlaceOrder);
        btnWhatsAppChat = findViewById(R.id.btnWhatsAppChat);
        rvProducts      = findViewById(R.id.rvSellerProducts);

        layoutBuyerActions = findViewById(R.id.layoutBuyerActions);
        layoutAdminReview  = findViewById(R.id.layoutAdminReview);
        btnViewCnic     = findViewById(R.id.btnViewCnic);
        btnPlayVoice    = findViewById(R.id.btnPlayVoice);
        btnAdminApprove = findViewById(R.id.btnAdminApprove);
        btnAdminReject  = findViewById(R.id.btnAdminReject);

        VoiceGuide.announceScreen(this,
                "یہ بیچنے والی کی پروفائل ہے۔ نیچے اس کی تمام اشیاء نظر آئیں گی۔");

        rvProducts.setLayoutManager(new GridLayoutManager(this, 2));
        productAdapter = new ProductAdapter(productList,
                product -> {
                    Intent i = new Intent(this, ProductDetailActivity.class);
                    i.putExtra(ProductDetailActivity.EXTRA_PRODUCT_ID, product.getId());
                    startActivity(i);
                },
                product -> requireBuyerLogin(() -> goToSingleProductOrder(product)),
                product -> WhatsAppHelper.openChat(this,
                        currentSeller != null ? currentSeller.getWhatsappNumber() : null,
                        "السلام علیکم! مجھے " + product.getName() + " کے بارے میں پوچھنا تھا۔")
        );
        rvProducts.setAdapter(productAdapter);

        loadSellerData();

        // ✅ Admin hai to sirf review panel dikhega, buyer wale buttons chhupenge
        if (session.isAdmin()) {
            layoutBuyerActions.setVisibility(android.view.View.GONE);
            layoutAdminReview.setVisibility(android.view.View.VISIBLE);
            setupAdminActions();
        } else {
            layoutBuyerActions.setVisibility(android.view.View.VISIBLE);
            layoutAdminReview.setVisibility(android.view.View.GONE);

            btnPlaceOrder.setOnClickListener(v -> requireBuyerLogin(() -> {
                Intent i = new Intent(this, PlaceOrderActivity.class);
                i.putExtra(PlaceOrderActivity.EXTRA_SELLER_ID, sellerId);
                startActivity(i);
            }));

            btnWhatsAppChat.setOnClickListener(v -> WhatsAppHelper.openChat(this,
                    currentSeller != null ? currentSeller.getWhatsappNumber() : null, null));
        }
    }

    /** ✅ NAYA — admin CNIC dekh sakta hai, voice sun sakta hai, aur seedha yahin se approve/reject kar sakta hai */
    private void setupAdminActions() {
        btnViewCnic.setOnClickListener(v -> {
            if (currentSeller == null || currentSeller.getCnicUrl() == null || currentSeller.getCnicUrl().isEmpty()) {
                Toast.makeText(this, "شناختی کارڈ اپلوڈ نہیں ہوا", Toast.LENGTH_SHORT).show();
                return;
            }
            ImageView fullImage = new ImageView(this);
            fullImage.setAdjustViewBounds(true);
            fullImage.setPadding(20, 20, 20, 20);
            Glide.with(this).load(currentSeller.getCnicUrl()).into(fullImage);
            new AlertDialog.Builder(this)
                    .setTitle("شناختی کارڈ / CNIC")
                    .setView(fullImage)
                    .setPositiveButton("بند کریں", null)
                    .show();
        });

        btnPlayVoice.setOnClickListener(v -> {
            if (currentSeller == null || currentSeller.getVoiceUrl() == null || currentSeller.getVoiceUrl().isEmpty()) {
                Toast.makeText(this, "آواز کا نمونہ اپلوڈ نہیں ہوا", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                if (mediaPlayer != null) { mediaPlayer.release(); mediaPlayer = null; }
                mediaPlayer = new MediaPlayer();
                mediaPlayer.setDataSource(currentSeller.getVoiceUrl());
                mediaPlayer.setOnPreparedListener(MediaPlayer::start);
                mediaPlayer.setOnCompletionListener(mp -> Toast.makeText(this, "آواز مکمل ہوئی", Toast.LENGTH_SHORT).show());
                mediaPlayer.prepareAsync();
                Toast.makeText(this, "آواز چل رہی ہے...", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "آواز چلانے میں خرابی: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

        btnAdminApprove.setOnClickListener(v -> updateVerification("verified"));
        btnAdminReject.setOnClickListener(v -> updateVerification("rejected"));
    }

    private void updateVerification(String status) {
        FirebaseDatabase.getInstance().getReference().child(Constants.NODE_SELLERS)
                .child(sellerId).child("verificationStatus").setValue(status)
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, "حیثیت اپڈیٹ ہو گئی: " + status, Toast.LENGTH_SHORT).show();
                    loadSellerData(); // badge turant refresh ho jaye
                })
                .addOnFailureListener(e -> Toast.makeText(this, "خرابی: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void loadSellerData() {
        FirebaseDatabase.getInstance().getReference().child(Constants.NODE_SELLERS).child(sellerId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(DataSnapshot snapshot) {
                        currentSeller = snapshot.getValue(Seller.class);
                        if (currentSeller == null) {
                            Toast.makeText(SellerProfileActivity.this, "یہ بیچنے والی اب دستیاب نہیں ہے", Toast.LENGTH_LONG).show();
                            return;
                        }
                        bindSellerData();
                        loadSellerProducts();
                    }
                    @Override public void onCancelled(DatabaseError error) {
                        Toast.makeText(SellerProfileActivity.this, "ڈیٹا لوڈ نہیں ہو سکا: " + error.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void bindSellerData() {
        tvName.setText(currentSeller.getBusinessName());
        tvCategory.setText(Constants.getUrduLabel(currentSeller.getCategory()) + " (" + currentSeller.getCategory() + ")");
        tvCity.setText(currentSeller.getCity());
        tvBio.setText(currentSeller.getBio());
        tvRating.setText(String.format("⭐ %.1f (%d)", currentSeller.getRating(), currentSeller.getRatingCount()));

        boolean approved = Constants.isApproved(currentSeller.getVerificationStatus());
        tvStatus.setText(approved ? "✅ تصدیق شدہ" : "⏳ " + currentSeller.getVerificationStatus());
        ivSellerVerifiedBadge.setVisibility(approved ? android.view.View.VISIBLE : android.view.View.GONE);

        boolean travelType = Constants.isTravelType(currentSeller.getCategory());
        if (currentSeller.isCanDeliver()) {
            tvDeliveryBadge.setText(travelType ? "✅ یہ خود آپ کے پاس آ سکتی ہیں" : "✅ گھر ڈیلیوری دستیاب ہے");
            tvDeliveryBadge.setBackgroundResource(R.drawable.bg_rounded_green);
            tvDeliveryBadge.setTextColor(0xFF2E7D32);
        } else {
            tvDeliveryBadge.setText("⚠️ صرف خود آ کر لینا ہوگا\n📍 " + (currentSeller.getShopAddress() != null ? currentSeller.getShopAddress() : currentSeller.getCity()));
            tvDeliveryBadge.setBackgroundResource(R.drawable.bg_rounded_pink);
            tvDeliveryBadge.setTextColor(0xFFB25E00);
        }

        if (currentSeller.getCapacityDescription() != null && !currentSeller.getCapacityDescription().isEmpty()) {
            tvCapacity.setText("🙋‍♀️ " + currentSeller.getCapacityDescription());
            tvCapacity.setVisibility(android.view.View.VISIBLE);
        } else tvCapacity.setVisibility(android.view.View.GONE);

        if (currentSeller.getImageUrl() != null && !currentSeller.getImageUrl().isEmpty())
            Glide.with(this).load(currentSeller.getImageUrl()).into(ivSellerAvatar);
    }

    private void loadSellerProducts() {
        FirebaseDatabase.getInstance().getReference().child(Constants.NODE_PRODUCTS)
                .orderByChild("sellerId").equalTo(sellerId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(DataSnapshot snapshot) {
                        productList.clear();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            Product p = child.getValue(Product.class);
                            if (p != null) productList.add(p);
                        }
                        productAdapter.notifyDataSetChanged();
                    }
                    @Override public void onCancelled(DatabaseError error) {}
                });
    }

    private void goToSingleProductOrder(Product product) {
        Intent i = new Intent(this, PlaceOrderActivity.class);
        i.putExtra(PlaceOrderActivity.EXTRA_SELLER_ID, sellerId);
        i.putExtra(PlaceOrderActivity.EXTRA_PRODUCT_ID, product.getId());
        i.putExtra(PlaceOrderActivity.EXTRA_PRODUCT_NAME, product.getName());
        i.putExtra(PlaceOrderActivity.EXTRA_PRODUCT_PRICE, product.getPrice());
        startActivity(i);
    }

    private void requireBuyerLogin(Runnable action) {
        if (session.isLoggedIn()) action.run();
        else { Toast.makeText(this, "پہلے لاگ ان کریں", Toast.LENGTH_SHORT).show(); startActivity(new Intent(this, LoginActivity.class)); }
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) { mediaPlayer.release(); mediaPlayer = null; }
    }
}