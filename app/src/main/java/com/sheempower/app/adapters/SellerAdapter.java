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
import com.sheempower.app.utils.PreferencesManager;

import de.hdodenhof.circleimageview.CircleImageView;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class SellerAdapter extends RecyclerView.Adapter<SellerAdapter.VH> {

    public interface OnSellerClick {
        void onClick(Seller seller);
    }

    private List<Seller> sellers;
    private final OnSellerClick listener;

    public SellerAdapter(List<Seller> sellers, OnSellerClick listener) {
        this.sellers = sellers != null
                ? sellers
                : new ArrayList<>();

        this.listener = listener;
    }

    public void updateList(List<Seller> newList) {
        this.sellers = newList != null
                ? new ArrayList<>(newList)
                : new ArrayList<>();

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {

        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_seller, parent, false);

        return new VH(v);
    }

    @Override
    public void onBindViewHolder(
            @NonNull VH holder,
            int position
    ) {

        if (position < 0 || position >= sellers.size()) {
            return;
        }

        Seller seller = sellers.get(position);

        if (seller == null) {
            return;
        }

        boolean isDark = new PreferencesManager(holder.itemView.getContext()).isDarkThemeEnabled();
        int textColor = isDark ? 0xFFFFFFFF : 0xFF192A56;
        int ratingColor = isDark ? 0xFFF7D794 : 0xFF192A56;

        // =========================================================
        // BUSINESS NAME
        // =========================================================

        String businessName = seller.getBusinessName();

        if (businessName == null || businessName.trim().isEmpty()) {
            businessName = "Seller";
        }

        holder.tvName.setText(businessName);
        holder.tvName.setTextColor(textColor);


        // =========================================================
        // CATEGORIES + CITY
        // =========================================================

        String categories = getCategoriesText(seller);
        String city = safe(seller.getCity());

        String locationText;

        if (!categories.isEmpty() && !city.isEmpty()) {
            locationText = categories + " · " + city;

        } else if (!categories.isEmpty()) {
            locationText = categories;

        } else if (!city.isEmpty()) {
            locationText = city;

        } else {
            locationText = "";
        }

        holder.tvCity.setText(locationText);
        holder.tvCity.setTextColor(textColor);


        // =========================================================
        // RATING
        // =========================================================

        double rVal = seller.getRating();
        long rCount = seller.getRatingCount();

        if (rCount > 0 && rVal > 0) {
            holder.tvRating.setText(String.format(Locale.ENGLISH, "⭐ %.1f (%d)", rVal, rCount));
        } else {
            holder.tvRating.setText("⭐ N/A");
        }
        holder.tvRating.setTextColor(ratingColor);


        // =========================================================
        // PROFILE IMAGE
        // =========================================================

        String imageUrl = safe(seller.getImageUrl());

        if (!imageUrl.isEmpty()) {

            Glide.with(holder.itemView.getContext())
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .error(R.drawable.ic_profile_placeholder)
                    .into(holder.ivAvatar);

        } else {

            holder.ivAvatar.setImageResource(
                    R.drawable.ic_profile_placeholder
            );
        }


        // =========================================================
        // CLICK
        // =========================================================

        holder.itemView.setOnClickListener(v -> {

            if (listener != null) {
                listener.onClick(seller);
            }
        });
    }

    /**
     * Displays ALL selected categories.
     * Example:
     * Cooking • Stitching • Mehndi
     */
    private String getCategoriesText(Seller seller) {

        Set<String> uniqueCategories =
                new LinkedHashSet<>();

        if (seller.getCategories() != null) {

            for (String category : seller.getCategories()) {

                if (category != null &&
                        !category.trim().isEmpty()) {

                    uniqueCategories.add(category.trim());
                }
            }
        }

        // Backward compatibility for old Firebase records
        if (uniqueCategories.isEmpty()) {

            String primary = seller.getCategory();

            if (primary != null &&
                    !primary.trim().isEmpty()) {

                uniqueCategories.add(primary.trim());
            }
        }

        StringBuilder result = new StringBuilder();

        for (String category : uniqueCategories) {

            if (result.length() > 0) {
                result.append(" • ");
            }

            result.append(category);
        }

        return result.toString();
    }

    private String safe(String value) {

        return value == null
                ? ""
                : value.trim();
    }

    @Override
    public int getItemCount() {

        return sellers != null
                ? sellers.size()
                : 0;
    }

    static class VH extends RecyclerView.ViewHolder {

        CircleImageView ivAvatar;

        TextView tvName;
        TextView tvCity;
        TextView tvRating;

        VH(@NonNull View itemView) {

            super(itemView);

            ivAvatar =
                    itemView.findViewById(
                            R.id.ivSellerAvatar
                    );

            tvName =
                    itemView.findViewById(
                            R.id.tvSellerBizName
                    );

            tvCity =
                    itemView.findViewById(
                            R.id.tvSellerCity
                    );

            tvRating =
                    itemView.findViewById(
                            R.id.tvSellerRatingBadge
                    );
        }
    }
}