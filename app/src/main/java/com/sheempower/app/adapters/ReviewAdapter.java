package com.sheempower.app.adapters;

import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.sheempower.app.R;
import com.sheempower.app.models.Review;
import java.util.List;

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
        holder.tvName.setText(r.getBuyerName() != null ? r.getBuyerName() : "Buyer");
        holder.tvRating.setText("⭐ " + String.format("%.1f", r.getRating()));

        if (r.getReviewText() != null && !r.getReviewText().trim().isEmpty()) {
            holder.tvText.setText(r.getReviewText().trim());
            holder.tvText.setVisibility(View.VISIBLE);
        } else {
            holder.tvText.setVisibility(View.GONE);
        }

        CharSequence timeAgo = DateUtils.getRelativeTimeSpanString(r.getTimestamp(), System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS);
        holder.tvDate.setText(timeAgo);
    }

    @Override
    public int getItemCount() {
        return reviewList.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvRating, tvText, tvDate;
        VH(@NonNull View itemView) {
            super(itemView);
            tvName   = itemView.findViewById(R.id.tvReviewerName);
            tvRating = itemView.findViewById(R.id.tvReviewRating);
            tvText   = itemView.findViewById(R.id.tvReviewText);
            tvDate   = itemView.findViewById(R.id.tvReviewDate);
        }
    }
}