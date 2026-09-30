package com.sheempower.app.activities;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.LayoutInflater;
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
    public static final String EXTRA_SELECTED_CATEGORY = "selectedCategory";

    private CircleImageView ivSellerAvatar;
    private ImageView ivSellerVerifiedBadge;
    private TextView tvName, tvCategory, tvCity, tvBio, tvRating, tvStatus, tvCapacity, tvDeliveryBadge;
    private Button btnPlaceOrder, btnWhatsAppChat;
    private RecyclerView rvProducts;
    private ProductAdapter productAdapter;
    private final List<Product> productList = new ArrayList<>();

    private View layoutBuyerActions;
    private View layoutAdminReview;
    private Button btnViewCnic, btnPlayVoice, btnAdminApprove, btnAdminReject;
    private MediaPlayer mediaPlayer;

    private String sellerId;
    private String selectedCategoryFilter;
    private Seller currentSeller;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seller_profile);

        session  = new SessionManager(this);
        sellerId = getIntent().getStringExtra(EXTRA_SELLER_ID);
        selectedCategoryFilter = getIntent().getStringExtra(EXTRA_SELECTED_CATEGORY);
        if (selectedCategoryFilter != null) {
            selectedCategoryFilter = selectedCategoryFilter.trim();
        }

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

        BottomNavHelper.setupBackButton(this);

        ImageButton btnSpeaker = findViewById(R.id.btnSpeaker);
        VoiceGuide.setupSpeakerButton(btnSpeaker, this,
                "یہ بیچنے والی کی پروفائل ہے۔ نیچے اس کی تمام اشیاء نظر آئیں گی۔"
        );

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

        if (session.isAdmin()) {
            if (layoutBuyerActions != null) layoutBuyerActions.setVisibility(View.GONE);
            if (layoutAdminReview != null) layoutAdminReview.setVisibility(View.VISIBLE);
            setupAdminActions();
        } else {
            if (layoutBuyerActions != null) layoutBuyerActions.setVisibility(View.VISIBLE);
            if (layoutAdminReview != null) layoutAdminReview.setVisibility(View.GONE);

            if (btnPlaceOrder != null) {
                btnPlaceOrder.setOnClickListener(v -> requireBuyerLogin(() -> {
                    Intent i = new Intent(this, PlaceOrderActivity.class);
                    i.putExtra(PlaceOrderActivity.EXTRA_SELLER_ID, sellerId);
                    if (selectedCategoryFilter != null && !selectedCategoryFilter.trim().isEmpty()) {
                        i.putExtra(PlaceOrderActivity.EXTRA_PRODUCT_CATEGORY, selectedCategoryFilter.trim());
                    } else if (currentSeller != null && currentSeller.getCategory() != null && !currentSeller.getCategory().trim().isEmpty()) {
                        i.putExtra(PlaceOrderActivity.EXTRA_PRODUCT_CATEGORY, currentSeller.getCategory().trim());
                    }
                    startActivity(i);
                }));
            }

            if (btnWhatsAppChat != null) {
                btnWhatsAppChat.setOnClickListener(v -> WhatsAppHelper.openChat(this,
                        currentSeller != null ? currentSeller.getWhatsappNumber() : null, null));
            }
        }
    }

    private void setupAdminActions() {
        if (btnViewCnic != null) {
            btnViewCnic.setOnClickListener(v -> {
                if (currentSeller == null || currentSeller.getCnicUrl() == null || currentSeller.getCnicUrl().isEmpty()) {
                    Toast.makeText(this, "شناختی کارڈ اپلوڈ نہیں ہوا", Toast.LENGTH_SHORT).show();
                    return;
                }
                View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_cnic_view, null);
                ImageView ivCnicFull = dialogView.findViewById(R.id.ivCnicFull);

                Glide.with(this)
                        .load(currentSeller.getCnicUrl())
                        .placeholder(R.drawable.placeholder_product)
                        .error(R.drawable.placeholder_product)
                        .into(ivCnicFull);

                new AlertDialog.Builder(this)
                        .setTitle("شناختی کارڈ / CNIC")
                        .setView(dialogView)
                        .setPositiveButton("بند کریں", null)
                        .show();
            });
        }

        if (btnPlayVoice != null) {
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
        }

        if (btnAdminApprove != null) btnAdminApprove.setOnClickListener(v -> updateVerification("verified"));
        if (btnAdminReject != null) btnAdminReject.setOnClickListener(v -> updateVerification("rejected"));
    }

    private void updateVerification(String status) {
        FirebaseDatabase.getInstance().getReference().child(Constants.NODE_SELLERS)
                .child(sellerId).child("verificationStatus").setValue(status)
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, "حیثیت اپڈیٹ ہو گئی: " + status, Toast.LENGTH_SHORT).show();
                    loadSellerData();
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

                        if (currentSeller.getImageUrl() == null || currentSeller.getImageUrl().trim().isEmpty()) {
                            FirebaseDatabase.getInstance().getReference().child(Constants.NODE_USERS).child(sellerId)
                                    .child("imageUrl").get()
                                    .addOnSuccessListener(userImgSnap -> {
                                        String img = userImgSnap.getValue(String.class);
                                        if (img != null && !img.trim().isEmpty()) {
                                            currentSeller.setImageUrl(img.trim());
                                        }
                                        bindSellerData();
                                        loadSellerProducts();
                                    })
                                    .addOnFailureListener(e -> {
                                        bindSellerData();
                                        loadSellerProducts();
                                    });
                        } else {
                            bindSellerData();
                            loadSellerProducts();
                        }
                    }
                    @Override public void onCancelled(DatabaseError error) {
                        Toast.makeText(SellerProfileActivity.this, "ڈیٹا لوڈ نہیں ہو سکا: " + error.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void bindSellerData() {
        String business = currentSeller.getBusinessName() != null && !currentSeller.getBusinessName().isEmpty()
                ? currentSeller.getBusinessName()
                : currentSeller.getOwnerName();
        tvName.setText(business);

        tvCategory.setText(getCategoriesText(currentSeller));
        tvCity.setText(currentSeller.getCity());
        tvBio.setText(currentSeller.getBio());
        tvRating.setText(String.format("⭐ %.1f (%d)", currentSeller.getRating(), currentSeller.getRatingCount()));

        boolean approved = Constants.isApproved(currentSeller.getVerificationStatus());
        tvStatus.setText(approved ? "✅ تصدیق شدہ" : "⏳ " + currentSeller.getVerificationStatus());
        if (ivSellerVerifiedBadge != null) ivSellerVerifiedBadge.setVisibility(approved ? View.VISIBLE : View.GONE);

        boolean travelType = Constants.isTravelType(currentSeller.getCategory());
        if (currentSeller.isCanDeliver()) {
            tvDeliveryBadge.setText(travelType ? "✅ یہ خود آپ کے پاس آ سکتی ہیں" : "✅ گھر ڈیلیوری دستیاب ہے");
            tvDeliveryBadge.setBackgroundResource(R.drawable.bg_rounded_green);
            tvDeliveryBadge.setTextColor(0xFF2E7D32);
        } else {
            tvDeliveryBadge.setText("⚠️ صرف خود آ کر لینا ہوگا\n📍 " + (currentSeller.getShopAddress() != null ? currentSeller.getShopAddress() : currentSeller.getCity()));
            tvDeliveryBadge.setBackgroundResource(R.drawable.bg_profile_field);
            tvDeliveryBadge.setTextColor(0xFFB25E00);
        }

        if (currentSeller.getCapacityDescription() != null && !currentSeller.getCapacityDescription().isEmpty()) {
            tvCapacity.setText("🙋‍♀️ " + currentSeller.getCapacityDescription());
            tvCapacity.setVisibility(View.VISIBLE);
        } else tvCapacity.setVisibility(View.GONE);

        if (currentSeller.getImageUrl() != null && !currentSeller.getImageUrl().trim().isEmpty()) {
            Glide.with(this)
                    .load(currentSeller.getImageUrl().trim())
                    .placeholder(R.drawable.placeholder_avatar)
                    .error(R.drawable.placeholder_avatar)
                    .into(ivSellerAvatar);
        } else {
            ivSellerAvatar.setImageResource(R.drawable.placeholder_avatar);
        }
    }

    private String getCategoriesText(Seller seller) {
        List<String> cats = seller.getCategories();
        if (cats != null && !cats.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (String c : cats) {
                if (sb.length() > 0) sb.append(" • ");
                sb.append(Constants.getUrduLabel(c)).append(" (").append(c).append(")");
            }
            return sb.toString();
        }
        return Constants.getUrduLabel(seller.getCategory()) + " (" + seller.getCategory() + ")";
    }

    private void loadSellerProducts() {
        if (sellerId == null || sellerId.trim().isEmpty()) return;
        final String targetSellerId = sellerId.trim();
        final String activeCatFilter = selectedCategoryFilter;

        FirebaseDatabase.getInstance().getReference().child(Constants.NODE_PRODUCTS)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(DataSnapshot snapshot) {
                        productList.clear();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            Product p = child.getValue(Product.class);
                            if (p != null) {
                                String pid = child.getKey();
                                if (pid != null) p.setId(pid.trim());

                                if (p.getSellerId() != null) {
                                    String pSellerId = p.getSellerId().trim();
                                    boolean isMatch = targetSellerId.equalsIgnoreCase(pSellerId);

                                    if (!isMatch && currentSeller != null) {
                                        if (currentSeller.getUid() != null && currentSeller.getUid().trim().equalsIgnoreCase(pSellerId)) {
                                            isMatch = true;
                                        } else if (currentSeller.getEmail() != null && currentSeller.getEmail().trim().equalsIgnoreCase(pSellerId)) {
                                            isMatch = true;
                                        } else if (currentSeller.getPhone() != null && currentSeller.getPhone().trim().equalsIgnoreCase(pSellerId)) {
                                            isMatch = true;
                                        } else if (currentSeller.getWhatsappNumber() != null && currentSeller.getWhatsappNumber().trim().equalsIgnoreCase(pSellerId)) {
                                            isMatch = true;
                                        }
                                    }

                                    if (isMatch) {
                                        if (activeCatFilter != null && !activeCatFilter.isEmpty()) {
                                            if (p.getCategory() != null && activeCatFilter.equalsIgnoreCase(p.getCategory().trim())) {
                                                productList.add(p);
                                            }
                                        } else {
                                            productList.add(p);
                                        }
                                    }
                                }
                            }
                        }
                        if (productAdapter != null) productAdapter.notifyDataSetChanged();
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
        i.putExtra(PlaceOrderActivity.EXTRA_PRODUCT_CATEGORY, product.getCategory());
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