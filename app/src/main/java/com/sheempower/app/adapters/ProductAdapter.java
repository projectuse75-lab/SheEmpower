package com.sheempower.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.sheempower.app.R;
import com.sheempower.app.models.Product;
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
        holder.tvName.setText(p.getName());
        holder.tvPrice.setText("PKR " + (long) p.getPrice());
        if (p.getImageUrl() != null && !p.getImageUrl().isEmpty()) {
            Glide.with(holder.itemView).load(p.getImageUrl()).into(holder.ivImage);
        } else {
            holder.ivImage.setImageResource(R.drawable.placeholder_product);
        }
        holder.itemView.setOnClickListener(v -> onOpen.onClick(p));
        holder.btnOrder.setOnClickListener(v -> onOrder.onClick(p));
        holder.btnChat.setOnClickListener(v -> onChat.onClick(p));
    }

    @Override public int getItemCount() { return products.size(); }

    static class VH extends RecyclerView.ViewHolder {
        android.widget.ImageView ivImage;
        TextView tvName, tvPrice;
        Button btnOrder, btnChat;

        VH(@NonNull View itemView) {
            super(itemView);
            ivImage  = itemView.findViewById(R.id.ivProductImage);
            tvName   = itemView.findViewById(R.id.tvProductName);
            tvPrice  = itemView.findViewById(R.id.tvProductPrice);
            btnOrder = itemView.findViewById(R.id.btnOrder);
            btnChat  = itemView.findViewById(R.id.btnChat);
        }
    }
}