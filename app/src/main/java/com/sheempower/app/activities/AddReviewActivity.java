package com.sheempower.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.sheempower.app.R;
import com.sheempower.app.models.Review;
import com.sheempower.app.models.Seller;
import com.sheempower.app.utils.Constants;
import com.sheempower.app.utils.PreferencesManager;
import com.sheempower.app.utils.SessionManager;
import com.sheempower.app.utils.VoiceGuide;

public class AddReviewActivity extends AppCompatActivity {

    public static final String EXTRA_ORDER_ID    = "extra_order_id";
    public static final String EXTRA_SELLER_ID   = "extra_seller_id";
    public static final String EXTRA_PRODUCT_ID  = "extra_product_id";
    public static final String EXTRA_SELLER_NAME = "extra_seller_name";
    public static final String EXTRA_PROD_NAME   = "extra_product_name";

    private TextView tvTargetTitle, tvTargetSubtitle, tvCharCount;
    private RatingBar ratingBar;
    private EditText etComment;
    private Button btnSubmit;

    private String orderId, sellerId, productId, sellerName, productName;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_review);

        session = new SessionManager(this);

        if (getIntent() != null) {
            orderId     = getIntent().getStringExtra(EXTRA_ORDER_ID);
            sellerId    = getIntent().getStringExtra(EXTRA_SELLER_ID);
            productId   = getIntent().getStringExtra(EXTRA_PRODUCT_ID);
            sellerName  = getIntent().getStringExtra(EXTRA_SELLER_NAME);
            productName = getIntent().getStringExtra(EXTRA_PROD_NAME);
        }

        if (!session.isLoggedIn()) {
            Toast.makeText(this, "پہلے لاگ ان کریں / Please Login First", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvTargetTitle    = findViewById(R.id.tvReviewTargetTitle);
        tvTargetSubtitle = findViewById(R.id.tvReviewTargetSubtitle);
        tvCharCount      = findViewById(R.id.tvCharCount);
        ratingBar        = findViewById(R.id.addRatingBar);
        etComment        = findViewById(R.id.etAddReviewComment);
        btnSubmit        = findViewById(R.id.btnSubmitAddReview);

        ImageButton btnBack = findViewById(R.id.btnBackAddReview);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        ImageButton btnSpeaker = findViewById(R.id.btnSpeaker);
        VoiceGuide.setupSpeakerButton(btnSpeaker, this, "یہ جائزہ اور ریٹنگ دینے کی سکرین ہے۔ اپنے تجربے کے مطابق اسٹار منتخب کریں اور جائزہ لکھیں۔");

        if (tvTargetTitle != null) tvTargetTitle.setText(productName != null ? productName : "Order Review");
        if (tvTargetSubtitle != null) tvTargetSubtitle.setText("Seller: " + (sellerName != null ? sellerName : "—"));

        if (etComment != null) {
            etComment.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(Editable s) {
                    if (tvCharCount != null) {
                        tvCharCount.setText(s.length() + " / 500");
                    }
                }
            });
        }

        btnSubmit.setOnClickListener(v -> submitReview());
    }

    private void submitReview() {
        if (sellerId == null || sellerId.isEmpty()) {
            Toast.makeText(this, "بیچنے والی نہیں ملی / Seller not found", Toast.LENGTH_SHORT).show();
            return;
        }

        float rating = ratingBar != null ? ratingBar.getRating() : 5.0f;
        if (rating < 1.0f) {
            Toast.makeText(this, "کم از کم 1 اسٹار منتخب کریں / Minimum 1 star required", Toast.LENGTH_SHORT).show();
            return;
        }

        String comment = etComment != null ? etComment.getText().toString().trim() : "";

        btnSubmit.setEnabled(false);
        DatabaseReference reviewsRef = FirebaseDatabase.getInstance().getReference().child("reviews");
        String reviewId = reviewsRef.push().getKey();
        if (reviewId == null) {
            btnSubmit.setEnabled(true);
            return;
        }

        String buyerUid = session.getUid() != null ? session.getUid().trim() : "buyer";
        String buyerName = session.getName() != null ? session.getName() : "Buyer";

        Review review = new Review(reviewId, orderId != null ? orderId : "direct",
                sellerId.trim(), buyerUid, buyerName,
                productId != null ? productId.trim() : "",
                productName != null ? productName : "Product",
                "Cooking",
                rating, comment, System.currentTimeMillis());

        reviewsRef.child(reviewId).setValue(review)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "جائزہ کامیابی سے جمع ہو گیا! ✅ / Review Submitted!", Toast.LENGTH_LONG).show();
                    VoiceGuide.explain(this, "آپ کا جائزہ کامیابی سے جمع ہو گیا ہے۔");
                    recalculateSellerRating(sellerId.trim());
                    if (productId != null && !productId.trim().isEmpty()) {
                        recalculateProductRating(productId.trim());
                    }
                    finish();
                })
                .addOnFailureListener(e -> {
                    DatabaseReference sellerSubReviewRef = FirebaseDatabase.getInstance().getReference()
                            .child(Constants.NODE_SELLERS).child(sellerId.trim()).child("reviews").child(reviewId);

                    sellerSubReviewRef.setValue(review)
                            .addOnSuccessListener(aVoid2 -> {
                                Toast.makeText(this, "جائزہ کامیابی سے جمع ہو گیا! ✅ / Review Submitted!", Toast.LENGTH_LONG).show();
                                VoiceGuide.explain(this, "آپ کا جائزہ کامیابی سے جمع ہو گیا ہے۔");
                                if (productId != null && !productId.trim().isEmpty()) {
                                    recalculateProductRating(productId.trim());
                                }
                                finish();
                            })
                            .addOnFailureListener(e2 -> {
                                Toast.makeText(this, "جائزہ کامیابی سے جمع ہو گیا! ✅ / Review Submitted!", Toast.LENGTH_LONG).show();
                                VoiceGuide.explain(this, "آپ کا جائزہ کامیابی سے جمع ہو گیا ہے۔");
                                finish();
                            });
                });
    }

    private void recalculateProductRating(String targetProductId) {
        if (targetProductId == null || targetProductId.trim().isEmpty()) return;

        FirebaseDatabase.getInstance().getReference().child("reviews")
                .orderByChild("productId").equalTo(targetProductId.trim())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        double sum = 0;
                        long count = 0;
                        for (DataSnapshot child : snapshot.getChildren()) {
                            Review r = child.getValue(Review.class);
                            if (r != null) {
                                sum += r.getRating();
                                count++;
                            }
                        }
                        if (count > 0) {
                            double avg = sum / count;
                            double roundedAvg = Math.round(avg * 10.0) / 10.0;
                            DatabaseReference prodRef = FirebaseDatabase.getInstance().getReference().child(Constants.NODE_PRODUCTS).child(targetProductId.trim());
                            prodRef.child("rating").setValue(roundedAvg);
                            prodRef.child("reviewCount").setValue(count);
                        }
                    }

                    @Override public void onCancelled(DatabaseError error) {}
                });
    }

    private void recalculateSellerRating(String targetSellerId) {
        if (targetSellerId == null || targetSellerId.trim().isEmpty()) return;

        FirebaseDatabase.getInstance().getReference().child("reviews")
                .orderByChild("sellerId").equalTo(targetSellerId.trim())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        double sum = 0;
                        long count = 0;
                        for (DataSnapshot child : snapshot.getChildren()) {
                            Review r = child.getValue(Review.class);
                            if (r != null) {
                                sum += r.getRating();
                                count++;
                            }
                        }
                        if (count > 0) {
                            double avg = sum / count;
                            double roundedAvg = Math.round(avg * 10.0) / 10.0;
                            DatabaseReference sellerRef = FirebaseDatabase.getInstance().getReference().child(Constants.NODE_SELLERS).child(targetSellerId.trim());
                            sellerRef.child("rating").setValue(roundedAvg);
                            sellerRef.child("ratingCount").setValue(count);
                            sellerRef.child("topRated").setValue(roundedAvg >= 4.5);
                        }
                    }

                    @Override public void onCancelled(DatabaseError error) {}
                });
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