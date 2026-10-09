package com.sheempower.app.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.sheempower.app.R;
import com.sheempower.app.adapters.ReviewAdapter;
import com.sheempower.app.models.Review;
import com.sheempower.app.utils.Constants;
import com.sheempower.app.utils.PreferencesManager;
import com.sheempower.app.utils.SessionManager;
import com.sheempower.app.utils.VoiceGuide;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProductReviewsActivity extends AppCompatActivity {

    public static final String EXTRA_SELLER_ID  = "extra_seller_id";
    public static final String EXTRA_PRODUCT_ID = "extra_product_id";
    public static final String EXTRA_BUYER_ID   = "extra_buyer_id";

    private TextView tvAvgRating, tvCountLabel, tvNoReviews;
    private RecyclerView rvReviews;
    private HorizontalScrollView scrollCategoryFilter;
    private Button btnCatAll, btnCatCooking, btnCatStitching, btnCatMehndi, btnCatMakeup, btnCatEmbroidery;

    private ReviewAdapter adapter;
    private final List<Review> fullReviewList = new ArrayList<>();
    private final List<Review> displayedReviewList = new ArrayList<>();

    private String sellerId, productId, buyerId;
    private String selectedCategoryFilter = "ALL";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_reviews);

        if (getIntent() != null) {
            sellerId  = getIntent().getStringExtra(EXTRA_SELLER_ID);
            productId = getIntent().getStringExtra(EXTRA_PRODUCT_ID);
            buyerId   = getIntent().getStringExtra(EXTRA_BUYER_ID);
        }

        tvAvgRating   = findViewById(R.id.tvReviewsAvgRating);
        tvCountLabel  = findViewById(R.id.tvReviewsCountLabel);
        tvNoReviews   = findViewById(R.id.tvNoProductReviews);
        rvReviews     = findViewById(R.id.rvProductReviewsList);
        scrollCategoryFilter = findViewById(R.id.scrollCategoryFilter);

        btnCatAll        = findViewById(R.id.btnCatAll);
        btnCatCooking    = findViewById(R.id.btnCatCooking);
        btnCatStitching  = findViewById(R.id.btnCatStitching);
        btnCatMehndi     = findViewById(R.id.btnCatMehndi);
        btnCatMakeup     = findViewById(R.id.btnCatMakeup);
        btnCatEmbroidery = findViewById(R.id.btnCatEmbroidery);

        ImageButton btnBack = findViewById(R.id.btnBackProductReviews);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        ImageButton btnSpeaker = findViewById(R.id.btnSpeaker);
        VoiceGuide.setupSpeakerButton(btnSpeaker, this, "یہ خریداروں کے جائزوں اور ریٹنگز کی لسٹ ہے۔");

        setupCategoryFilterButtons();

        rvReviews.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ReviewAdapter(displayedReviewList);
        rvReviews.setAdapter(adapter);

        if (sellerId != null && !sellerId.trim().isEmpty() && productId == null && buyerId == null) {
            if (scrollCategoryFilter != null) scrollCategoryFilter.setVisibility(View.VISIBLE);
        }

        loadReviews();
    }

    private void setupCategoryFilterButtons() {
        if (btnCatAll != null) btnCatAll.setOnClickListener(v -> applyCategoryFilter("ALL"));
        if (btnCatCooking != null) btnCatCooking.setOnClickListener(v -> applyCategoryFilter("Cooking"));
        if (btnCatStitching != null) btnCatStitching.setOnClickListener(v -> applyCategoryFilter("Stitching"));
        if (btnCatMehndi != null) btnCatMehndi.setOnClickListener(v -> applyCategoryFilter("Mehndi"));
        if (btnCatMakeup != null) btnCatMakeup.setOnClickListener(v -> applyCategoryFilter("Makeup"));
        if (btnCatEmbroidery != null) btnCatEmbroidery.setOnClickListener(v -> applyCategoryFilter("Embroidery"));
    }

    private void applyCategoryFilter(String category) {
        selectedCategoryFilter = category;

        updateCatBtnStyle(btnCatAll, "ALL".equalsIgnoreCase(category));
        updateCatBtnStyle(btnCatCooking, "Cooking".equalsIgnoreCase(category));
        updateCatBtnStyle(btnCatStitching, "Stitching".equalsIgnoreCase(category));
        updateCatBtnStyle(btnCatMehndi, "Mehndi".equalsIgnoreCase(category));
        updateCatBtnStyle(btnCatMakeup, "Makeup".equalsIgnoreCase(category));
        updateCatBtnStyle(btnCatEmbroidery, "Embroidery".equalsIgnoreCase(category));

        displayedReviewList.clear();
        double totalRating = 0;

        for (Review r : fullReviewList) {
            if (r == null) continue;
            boolean matchesCategory = "ALL".equalsIgnoreCase(category) || (r.getCategory() != null && category.equalsIgnoreCase(r.getCategory().trim()));

            if (matchesCategory) {
                displayedReviewList.add(r);
                totalRating += r.getRating();
            }
        }

        if (adapter != null) adapter.notifyDataSetChanged();

        int count = displayedReviewList.size();
        if (count > 0) {
            double avg = totalRating / count;
            tvAvgRating.setText(String.format(Locale.ENGLISH, "⭐ %.1f / 5.0", avg));
            tvCountLabel.setText("Based on " + count + " review(s)");
            tvNoReviews.setVisibility(View.GONE);
            rvReviews.setVisibility(View.VISIBLE);
        } else {
            tvAvgRating.setText("⭐ 5.0 / 5.0");
            tvCountLabel.setText("No reviews in this category");
            tvNoReviews.setVisibility(View.VISIBLE);
            tvNoReviews.setText("اس کیٹیگری میں کوئی جائزہ موجود نہیں / No reviews in this category");
            rvReviews.setVisibility(View.GONE);
        }
    }

    private void updateCatBtnStyle(Button btn, boolean isSelected) {
        if (btn == null) return;
        if (isSelected) {
            btn.setBackgroundResource(R.drawable.bg_skeu_button_primary);
            btn.setTextColor(0xFFFFFFFF);
        } else {
            btn.setBackgroundResource(R.drawable.bg_skeu_button_cream);
            btn.setTextColor(0xFF192A56);
        }
    }

    private void loadReviews() {
        if (buyerId == null && sellerId == null && productId == null) {
            SessionManager sm = new SessionManager(this);
            buyerId = sm.getUid();
        }
        if (buyerId == null && sellerId == null && productId == null) return;

        fullReviewList.clear();

        FirebaseDatabase.getInstance().getReference().child("reviews")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        parseAndAddReviewsFromSnapshot(snapshot);
                        loadSellersSubReviews();
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        loadSellersSubReviews();
                    }
                });
    }

    private void loadSellersSubReviews() {
        FirebaseDatabase.getInstance().getReference().child(Constants.NODE_SELLERS)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        for (DataSnapshot sellerSnap : snapshot.getChildren()) {
                            DataSnapshot reviewsSnap = sellerSnap.child("reviews");
                            if (reviewsSnap.exists()) {
                                parseAndAddReviewsFromSnapshot(reviewsSnap);
                            }
                        }
                        applyCategoryFilter(selectedCategoryFilter);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        applyCategoryFilter(selectedCategoryFilter);
                    }
                });
    }

    private void parseAndAddReviewsFromSnapshot(DataSnapshot snapshot) {
        if (snapshot == null || !snapshot.exists()) return;

        for (DataSnapshot child : snapshot.getChildren()) {
            Review r = child.getValue(Review.class);
            if (r == null) continue;

            boolean match = false;

            // 1. My Reviews (Buyer Specific)
            if (buyerId != null && !buyerId.trim().isEmpty()) {
                if (r.getBuyerId() != null && buyerId.trim().equalsIgnoreCase(r.getBuyerId().trim())) {
                    match = true;
                }
            }
            // 2. Product Specific Public Reviews
            else if (productId != null && !productId.trim().isEmpty()) {
                if (r.getProductId() != null && productId.trim().equalsIgnoreCase(r.getProductId().trim())) {
                    match = true;
                }
            }
            // 3. Seller Specific Reviews
            else if (sellerId != null && !sellerId.trim().isEmpty()) {
                if (r.getSellerId() != null && sellerId.trim().equalsIgnoreCase(r.getSellerId().trim())) {
                    match = true;
                }
            }

            if (match) {
                if (r.getReviewId() == null) r.setReviewId(child.getKey());
                boolean exists = false;
                for (Review existing : fullReviewList) {
                    if (existing.getReviewId() != null && existing.getReviewId().equalsIgnoreCase(r.getReviewId())) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    fullReviewList.add(r);
                }
            }
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
}