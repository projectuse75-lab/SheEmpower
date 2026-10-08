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
    private TextView tvName, tvCategory, tvCity, tvBio, tvRating, tvStatus, tvCapacity, tvDeliveryBadge, tvProductsHeader;
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



        applyTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seller_profile);

        LanguageHelper.processViewHierarchy(findViewById(android.R.id.content), this);

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
        tvProductsHeader = findViewById(R.id.tvProductsHeader);

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

        boolean isBuyer = !session.isSeller() && !session.isAdmin();

        rvProducts.setLayoutManager(new GridLayoutManager(this, 2));
        if (isBuyer) {
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
        } else {
            // Admin or Seller view: NO Order or Chat buttons!
            productAdapter = new ProductAdapter(productList, product -> {
                Intent i = new Intent(this, ProductDetailActivity.class);
                i.putExtra(ProductDetailActivity.EXTRA_PRODUCT_ID, product.getId());
                i.putExtra(ProductDetailActivity.EXTRA_IS_SELLER_VIEW, true);
                startActivity(i);
            });
        }
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

        Button btnAdminDeleteSeller = findViewById(R.id.btnAdminDeleteSeller);
        if (btnAdminDeleteSeller != null) {
            btnAdminDeleteSeller.setOnClickListener(v -> confirmAndDeleteSeller());
        }
    }

    private void confirmAndDeleteSeller() {
        if (sellerId == null || sellerId.trim().isEmpty()) {
            Toast.makeText(this, "سیلر نہیں ملا / Seller ID not found", Toast.LENGTH_SHORT).show();
            return;
        }

        String sellerName = (currentSeller != null && currentSeller.getBusinessName() != null && !currentSeller.getBusinessName().isEmpty())
                ? currentSeller.getBusinessName()
                : "Seller";

        new AlertDialog.Builder(this)
                .setTitle("سیلر ختم کریں / Delete Seller")
                .setMessage("کیا آپ واقعی اس سیلر \"" + sellerName + "\" کو ڈیٹا بیس سے مکمل طور پر ختم کرنا چاہتے ہیں؟\n\nAre you sure you want to delete seller \"" + sellerName + "\"?")
                .setPositiveButton("ہاں، ختم کریں / Yes, Delete", (dialog, which) -> deleteSellerFromFirebase())
                .setNegativeButton("منسوخ / Cancel", null)
                .show();
    }

    private void deleteSellerFromFirebase() {
        if (sellerId == null || sellerId.trim().isEmpty()) return;
        final String targetUid = sellerId.trim();

        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();

        // 1. Remove from sellers node
        dbRef.child(Constants.NODE_SELLERS).child(targetUid).removeValue();

        // 2. Remove from users node
        dbRef.child(Constants.NODE_USERS).child(targetUid).removeValue();

        Toast.makeText(this, "سیلر کامیابی سے ختم کر دیا گیا! ✅ / Seller deleted!", Toast.LENGTH_LONG).show();
        VoiceGuide.explain(this, "سیلر کامیابی سے ختم کر دیا گیا ہے۔");

        finish();
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

                        if (currentSeller.getUid() == null || currentSeller.getUid().isEmpty()) {
                            currentSeller.setUid(snapshot.getKey());
                        }
                        currentSeller.setCategories(Constants.parseCategoriesFromSnapshot(snapshot));

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

    @Override
    protected void onResume() {
        super.onResume();
        LanguageHelper.processViewHierarchy(findViewById(android.R.id.content), this);
        ImageButton btnSpeaker = findViewById(R.id.btnSpeaker);
        if (btnSpeaker != null) {
            VoiceGuide.updateSpeakerIcon(btnSpeaker, VoiceGuide.isMuted(this));
        }
    }

    private boolean isDarkMode() {
        PreferencesManager prefs = new PreferencesManager(this);
        return prefs.isDarkThemeEnabled();
    }

    private void bindSellerData() {
        boolean isDark = isDarkMode();
        int textColor = isDark ? 0xFFFFFFFF : 0xFF192A56;

        String business = currentSeller.getBusinessName() != null && !currentSeller.getBusinessName().isEmpty()
                ? currentSeller.getBusinessName()
                : currentSeller.getOwnerName();
        tvName.setText(business);
        tvName.setTextColor(textColor);

        tvCategory.setText(getCategoriesText(currentSeller));
        tvCategory.setTextColor(textColor);

        if (tvProductsHeader != null) {
            tvProductsHeader.setTextColor(textColor);
        }

        tvCity.setText(currentSeller.getCity());
        tvCity.setTextColor(isDark ? 0xFFFFFFFF : 0xFF6B7A99);
        tvBio.setText(currentSeller.getBio());
        tvBio.setTextColor(textColor);
        tvRating.setText(String.format("⭐ %.1f (%d)", currentSeller.getRating(), currentSeller.getRatingCount()));

        boolean approved = Constants.isApproved(currentSeller.getVerificationStatus());
        tvStatus.setText(approved
                ? LanguageHelper.processDualString(this, "✅ تصدیق شدہ / Verified")
                : LanguageHelper.processDualString(this, "⏳ " + currentSeller.getVerificationStatus()));
        if (ivSellerVerifiedBadge != null) ivSellerVerifiedBadge.setVisibility(approved ? View.VISIBLE : View.GONE);

        boolean travelType = Constants.isTravelType(currentSeller.getCategory());
        if (currentSeller.isCanDeliver()) {
            tvDeliveryBadge.setText(travelType
                    ? LanguageHelper.processDualString(this, "✅ یہ خود آپ کے پاس آ سکتی ہیں / Available to visit your location")
                    : LanguageHelper.processDualString(this, "✅ گھر ڈیلیوری دستیاب ہے / Home Delivery Available"));
            tvDeliveryBadge.setBackgroundResource(R.drawable.bg_rounded_green);
            tvDeliveryBadge.setTextColor(0xFF2E7D32);
        } else {
            String shopAddr = currentSeller.getShopAddress() != null ? currentSeller.getShopAddress() : currentSeller.getCity();
            tvDeliveryBadge.setText(LanguageHelper.processDualString(this, "⚠️ صرف خود آ کر لینا ہوگا / Self-pickup only") + "\n📍 " + shopAddr);
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
        List<String> cats = Constants.getActiveSellerCategories(seller);
        if (!cats.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (String c : cats) {
                if (sb.length() > 0) sb.append(" • ");
                sb.append(Constants.getCategoryDisplayLabel(this, c));
            }
            return sb.toString();
        }
        return Constants.getCategoryDisplayLabel(this, seller.getCategory());
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

                                    if (isMatch && Constants.sellerProvidesCategory(currentSeller, p.getCategory())) {
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
        else {
            Toast.makeText(this, LanguageHelper.processDualString(this, "پہلے لاگ ان کریں / Please Login First"), Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
        }
    }

    private void applyTheme() {
        PreferencesManager prefs = new PreferencesManager(this);
        if (prefs.isDarkThemeEnabled()) {
            setTheme(R.style.Theme_SheEmpower_Dark);
        } else {
            setTheme(R.style.Theme_SheEmpower);
        }
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) { mediaPlayer.release(); mediaPlayer = null; }
    }
}