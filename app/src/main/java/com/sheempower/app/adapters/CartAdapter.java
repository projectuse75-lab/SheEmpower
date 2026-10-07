package com.sheempower.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.sheempower.app.R;
import com.sheempower.app.models.CartItem;
import com.sheempower.app.utils.PreferencesManager;

import java.util.List;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.VH> {

    public interface OnCartAction {
        void onQuantityChange(CartItem item, int newQty);
        void onRemove(CartItem item);
        void onSelectToggle(CartItem item, boolean isSelected);
    }

    private final List<CartItem> cartList;
    private OnCartAction listener;

    public CartAdapter(List<CartItem> cartList, OnCartAction listener) {
        this.cartList = cartList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cart, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        CartItem item = cartList.get(position);

        if (holder.cbSelect != null) {
            holder.cbSelect.setOnCheckedChangeListener(null);
            holder.cbSelect.setChecked(item.isSelected());
            holder.cbSelect.setOnCheckedChangeListener((buttonView, isChecked) -> {
                item.setSelected(isChecked);
                if (isChecked) {
                    // Allow multiple items from the SAME seller, uncheck other sellers' items
                    String activeSellerId = item.getSellerId() != null ? item.getSellerId().trim() : "";
                    for (CartItem ci : cartList) {
                        if (ci != item && ci.isSelected()) {
                            String otherSellerId = ci.getSellerId() != null ? ci.getSellerId().trim() : "";
                            if (!activeSellerId.isEmpty() && !activeSellerId.equalsIgnoreCase(otherSellerId)) {
                                ci.setSelected(false);
                            }
                        }
                    }
                    notifyDataSetChanged();
                }
                if (listener != null) listener.onSelectToggle(item, isChecked);
            });
        }

        boolean isDark = new PreferencesManager(holder.itemView.getContext()).isDarkThemeEnabled();
        int textColor = isDark ? 0xFFFFFFFF : 0xFF192A56;
        int priceColor = isDark ? 0xFFF7D794 : 0xFF192A56;

        holder.tvProductName.setText(item.getProductName());
        holder.tvProductName.setTextColor(textColor);

        holder.tvSellerName.setText("🏪 Seller: " + (item.getSellerName() != null && !item.getSellerName().isEmpty() ? item.getSellerName() : "Seller"));
        holder.tvSellerName.setTextColor(textColor);

        holder.tvPrice.setText("PKR " + (long) (item.getPrice() * item.getQuantity()));
        holder.tvPrice.setTextColor(priceColor);

        holder.tvQty.setText(String.valueOf(item.getQuantity()));
        holder.tvQty.setTextColor(textColor);

        if (item.getProductImageUrl() != null && !item.getProductImageUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(item.getProductImageUrl())
                    .placeholder(R.drawable.placeholder_product)
                    .error(R.drawable.placeholder_product)
                    .into(holder.ivProduct);
        } else {
            holder.ivProduct.setImageResource(R.drawable.placeholder_product);
        }

        holder.btnMinus.setOnClickListener(v -> {
            if (item.getQuantity() > 1) {
                if (listener != null) listener.onQuantityChange(item, item.getQuantity() - 1);
            } else {
                if (listener != null) listener.onRemove(item);
            }
        });

        holder.btnPlus.setOnClickListener(v -> {
            if (listener != null) listener.onQuantityChange(item, item.getQuantity() + 1);
        });

        holder.btnRemove.setOnClickListener(v -> {
            if (listener != null) listener.onRemove(item);
        });
    }

    @Override
    public int getItemCount() {
        return cartList.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        CheckBox cbSelect;
        ImageView ivProduct;
        TextView tvProductName, tvSellerName, tvPrice, tvQty, btnRemove;
        Button btnMinus, btnPlus;

        VH(@NonNull View itemView) {
            super(itemView);
            cbSelect        = itemView.findViewById(R.id.cbCartSelect);
            ivProduct       = itemView.findViewById(R.id.ivCartProduct);
            tvProductName   = itemView.findViewById(R.id.tvCartProductName);
            tvSellerName    = itemView.findViewById(R.id.tvCartSellerName);
            tvPrice         = itemView.findViewById(R.id.tvCartPrice);
            tvQty           = itemView.findViewById(R.id.tvQty);
            btnRemove       = itemView.findViewById(R.id.btnRemoveCartItem);
            btnMinus        = itemView.findViewById(R.id.btnQtyMinus);
            btnPlus         = itemView.findViewById(R.id.btnQtyPlus);
        }
    }
}