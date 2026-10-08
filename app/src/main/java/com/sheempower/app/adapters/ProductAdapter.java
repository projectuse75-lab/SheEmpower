package com.sheempower.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.sheempower.app.R;
import com.sheempower.app.models.Product;
import com.sheempower.app.utils.PreferencesManager;

import java.util.ArrayList;
import java.util.List;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.VH> {

    public interface OnProductClick { void onClick(Product product); }

    private List<Product> products;
    private final OnProductClick onOpen, onOrder, onChat;

    public ProductAdapter(List<Product> products, OnProductClick onOpen,
                          OnProductClick onOrder, OnProductClick onChat) {
        this.products = products;
        this.onOpen = onOpen;
        this.onOrder = onOrder;
        this.onChat = onChat;
    }

    public ProductAdapter(List<Product> products, OnProductClick onOpen) {
        this.products = products;
        this.onOpen = onOpen;
        this.onOrder = null;
        this.onChat = null;
    }

    public void updateList(List<Product> newList) {
        this.products = new ArrayList<>(newList);
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Product p = products.get(position);

        boolean isDark = new PreferencesManager(holder.itemView.getContext()).isDarkThemeEnabled();
        int textColor = isDark ? 0xFFFFFFFF : 0xFF192A56;
        int priceColor = isDark ? 0xFFF7D794 : 0xFF192A56;

        holder.tvName.setText(p.getName());
        holder.tvName.setTextColor(textColor);

        holder.tvPrice.setText("PKR " + (long) p.getPrice());
        holder.tvPrice.setTextColor(priceColor);
        if (p.getImageUrl() != null && !p.getImageUrl().isEmpty()) {
            Glide.with(holder.itemView).load(p.getImageUrl()).into(holder.ivImage);
        } else {
            holder.ivImage.setImageResource(R.drawable.placeholder_product);
        }

        if (onOpen != null) {
            holder.itemView.setOnClickListener(v -> onOpen.onClick(p));
        }

        if (onOrder == null && onChat == null) {
            if (holder.layoutActionButtons != null) {
                holder.layoutActionButtons.setVisibility(View.GONE);
            } else {
                if (holder.btnOrder != null) holder.btnOrder.setVisibility(View.GONE);
                if (holder.btnChat != null) holder.btnChat.setVisibility(View.GONE);
            }
        } else {
            if (holder.layoutActionButtons != null) {
                holder.layoutActionButtons.setVisibility(View.VISIBLE);
            }
            if (holder.btnOrder != null) {
                if (onOrder != null) {
                    holder.btnOrder.setVisibility(View.VISIBLE);
                    holder.btnOrder.setOnClickListener(v -> onOrder.onClick(p));
                } else {
                    holder.btnOrder.setVisibility(View.GONE);
                }
            }
            if (holder.btnChat != null) {
                if (onChat != null) {
                    holder.btnChat.setVisibility(View.VISIBLE);
                    holder.btnChat.setOnClickListener(v -> onChat.onClick(p));
                } else {
                    holder.btnChat.setVisibility(View.GONE);
                }
            }
        }
    }

    @Override public int getItemCount() { return products.size(); }

    static class VH extends RecyclerView.ViewHolder {
        ImageView ivImage;
        TextView tvName, tvPrice;
        Button btnOrder, btnChat;
        View layoutActionButtons;

        VH(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.ivProductImage);
            tvName = itemView.findViewById(R.id.tvProductName);
            tvPrice = itemView.findViewById(R.id.tvProductPrice);
            btnOrder = itemView.findViewById(R.id.btnOrder);
            btnChat = itemView.findViewById(R.id.btnChat);
            layoutActionButtons = itemView.findViewById(R.id.layoutActionButtons);
        }
    }
}