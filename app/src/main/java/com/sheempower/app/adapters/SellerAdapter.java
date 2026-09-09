package com.sheempower.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.sheempower.app.R;
import com.sheempower.app.models.Seller;
import de.hdodenhof.circleimageview.CircleImageView;
import java.util.ArrayList;
import java.util.List;

public class SellerAdapter extends RecyclerView.Adapter<SellerAdapter.VH> {

    public interface OnSellerClick { void onClick(Seller seller); }

    private List<Seller> sellers;
    private final OnSellerClick listener;

    public SellerAdapter(List<Seller> sellers, OnSellerClick listener) {
        this.sellers = sellers != null ? sellers : new ArrayList<>();
        this.listener = listener;
    }

    public void updateList(List<Seller> newList) {
        this.sellers = newList != null ? new ArrayList<>(newList) : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_seller, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        // ✅ SAFETY: Check position and list
        if (position >= sellers.size() || sellers.get(position) == null) {
            return;
        }

        Seller s = sellers.get(position);

        // ✅ Null safety for name
        holder.tvName.setText(s.getBusinessName() != null ? s.getBusinessName() : "Unknown");

        // ✅ Null safety for city
        String city = s.getCity() != null ? s.getCity() : "";
        String category = s.getCategory() != null ? s.getCategory() : "";
        holder.tvCity.setText(category + " · " + city);

        // ✅ Null safety for rating
        if (s.getRating() > 0) {
            holder.tvRating.setText("⭐ " + s.getRating());
        } else {
            holder.tvRating.setText("⭐ N/A");
        }

        // ✅ Null safety for image
        if (s.getImageUrl() != null && !s.getImageUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(s.getImageUrl())
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .error(R.drawable.ic_profile_placeholder)
                    .into(holder.ivAvatar);
        } else {
            holder.ivAvatar.setImageResource(R.drawable.ic_profile_placeholder);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onClick(s);
            }
        });
    }

    @Override public int getItemCount() { return sellers != null ? sellers.size() : 0; }

    static class VH extends RecyclerView.ViewHolder {
        CircleImageView ivAvatar;
        TextView tvName, tvCity, tvRating;

        VH(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivSellerAvatar);
            tvName   = itemView.findViewById(R.id.tvSellerBizName);
            tvCity   = itemView.findViewById(R.id.tvSellerCity);
            tvRating = itemView.findViewById(R.id.tvSellerRatingBadge);
        }
    }
}