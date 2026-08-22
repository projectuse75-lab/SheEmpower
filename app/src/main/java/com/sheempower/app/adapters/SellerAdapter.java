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
        this.sellers = sellers;
        this.listener = listener;
    }

    public void updateList(List<Seller> newList) {
        this.sellers = new ArrayList<>(newList);
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_seller, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Seller s = sellers.get(position);
        holder.tvName.setText(s.getBusinessName());
        holder.tvCity.setText(s.getCategory() + " · " + s.getCity());
        holder.tvRating.setText("⭐ " + s.getRating());
        if (s.getImageUrl() != null && !s.getImageUrl().isEmpty()) {
            Glide.with(holder.itemView).load(s.getImageUrl()).into(holder.ivAvatar);
        }
        holder.itemView.setOnClickListener(v -> listener.onClick(s));
    }

    @Override public int getItemCount() { return sellers.size(); }

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