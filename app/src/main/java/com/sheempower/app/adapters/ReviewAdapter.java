package com.sheempower.app.adapters;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.sheempower.app.R;
import com.sheempower.app.models.Review;
import com.sheempower.app.utils.Constants;
import com.sheempower.app.utils.PreferencesManager;

import java.util.List;
import java.util.Locale;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.VH> {

    private final List<Review> reviewList;

    public ReviewAdapter(List<Review> reviewList) {
        this.reviewList = reviewList;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_review, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Review r = reviewList.get(position);
        Context ctx = holder.itemView.getContext();
        boolean isDark = new PreferencesManager(ctx).isDarkThemeEnabled();

        int textColor = isDark ? 0xFFFFFFFF : 0xFF192A56;
        int secondaryColor = isDark ? 0xFFFFFFFF : 0xFF6B7A99;

        if (holder.tvName != null) {
            holder.tvName.setText(r.getBuyerName() != null && !r.getBuyerName().trim().isEmpty() ? r.getBuyerName() : "Buyer");
            holder.tvName.setTextColor(textColor);
        }

        if (holder.tvRating != null) {
            holder.tvRating.setText("⭐ " + String.format(Locale.ENGLISH, "%.1f", r.getRating()));
        }

        if (holder.tvProduct != null) {
            String pName = r.getProductName() != null ? r.getProductName() : "";
            String cat = r.getCategory() != null ? r.getCategory() : "";
            if (!pName.isEmpty() || !cat.isEmpty()) {
                holder.tvProduct.setText("📌 " + pName + (!cat.isEmpty() ? " (" + Constants.getCategoryDisplayLabel(ctx, cat) + ")" : ""));
                holder.tvProduct.setVisibility(View.VISIBLE);
            } else {
                holder.tvProduct.setVisibility(View.GONE);
            }
        }

        if (holder.tvText != null) {
            if (r.getReviewText() != null && !r.getReviewText().trim().isEmpty()) {
                holder.tvText.setText(r.getReviewText().trim());
                holder.tvText.setTextColor(textColor);
                holder.tvText.setVisibility(View.VISIBLE);
            } else {
                holder.tvText.setVisibility(View.GONE);
            }
        }

        if (holder.tvDate != null) {
            CharSequence timeAgo = DateUtils.getRelativeTimeSpanString(r.getTimestamp(), System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS);
            holder.tvDate.setText(timeAgo);
            holder.tvDate.setTextColor(secondaryColor);
        }
    }

    @Override
    public int getItemCount() {
        return reviewList.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvRating, tvProduct, tvText, tvDate;
        VH(@NonNull View itemView) {
            super(itemView);
            tvName    = itemView.findViewById(R.id.tvReviewerName);
            tvRating  = itemView.findViewById(R.id.tvReviewRating);
            tvProduct = itemView.findViewById(R.id.tvReviewProduct);
            tvText    = itemView.findViewById(R.id.tvReviewText);
            tvDate    = itemView.findViewById(R.id.tvReviewDate);
        }
    }
}