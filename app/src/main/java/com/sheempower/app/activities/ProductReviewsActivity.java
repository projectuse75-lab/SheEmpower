package com.sheempower.app.activities;

import android.os.Bundle;
import android.view.View;
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
import com.sheempower.app.utils.PreferencesManager;
import com.sheempower.app.utils.VoiceGuide;
import java.util.ArrayList;
import java.util.List;

public class ProductReviewsActivity extends AppCompatActivity {

    public static final String EXTRA_SELLER_ID  = "extra_seller_id";
    public static final String EXTRA_PRODUCT_ID = "extra_product_id";

    private TextView tvAvgRating, tvCountLabel, tvNoReviews;
    private RecyclerView rvReviews;
    private ReviewAdapter adapter;
    private final List<Review> reviewList = new ArrayList<>();

    private String sellerId, productId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_reviews);

        if (getIntent() != null) {
            sellerId  = getIntent().getStringExtra(EXTRA_SELLER_ID);
            productId = getIntent().getStringExtra(EXTRA_PRODUCT_ID);
        }

        tvAvgRating   = findViewById(R.id.tvReviewsAvgRating);
        tvCountLabel  = findViewById(R.id.tvReviewsCountLabel);
        tvNoReviews   = findViewById(R.id.tvNoProductReviews);
        rvReviews     = findViewById(R.id.rvProductReviewsList);

        ImageButton btnBack = findViewById(R.id.btnBackProductReviews);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        ImageButton btnSpeaker = findViewById(R.id.btnSpeaker);
        VoiceGuide.setupSpeakerButton(btnSpeaker, this, "یہ خریداروں کے جائزوں اور ریٹنگز کی لسٹ ہے۔");

        rvReviews.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ReviewAdapter(reviewList);
        rvReviews.setAdapter(adapter);

        loadReviews();
    }

    private void loadReviews() {
        if (sellerId == null && productId == null) return;

        FirebaseDatabase.getInstance().getReference().child("reviews")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        reviewList.clear();
                        double totalRating = 0;

                        for (DataSnapshot child : snapshot.getChildren()) {
                            Review r = child.getValue(Review.class);
                            if (r == null) continue;

                            boolean match = false;
                            if (sellerId != null && !sellerId.trim().isEmpty() && r.getSellerId() != null && sellerId.trim().equalsIgnoreCase(r.getSellerId().trim())) {
                                match = true;
                            }
                            if (productId != null && !productId.trim().isEmpty() && r.getOrderId() != null && productId.trim().equalsIgnoreCase(r.getOrderId().trim())) {
                                match = true;
                            }

                            if (match) {
                                if (r.getReviewId() == null) r.setReviewId(child.getKey());
                                reviewList.add(r);
                                totalRating += r.getRating();
                            }
                        }

                        if (adapter != null) adapter.notifyDataSetChanged();

                        int count = reviewList.size();
                        if (count > 0) {
                            double avg = totalRating / count;
                            tvAvgRating.setText(String.format("⭐ %.1f / 5.0", avg));
                            tvCountLabel.setText("Based on " + count + " review(s)");
                            tvNoReviews.setVisibility(View.GONE);
                            rvReviews.setVisibility(View.VISIBLE);
                        } else {
                            tvAvgRating.setText("⭐ 5.0 / 5.0");
                            tvCountLabel.setText("No reviews yet");
                            tvNoReviews.setVisibility(View.VISIBLE);
                            rvReviews.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {}
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