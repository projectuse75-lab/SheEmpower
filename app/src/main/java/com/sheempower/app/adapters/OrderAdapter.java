package com.sheempower.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.sheempower.app.R;
import com.sheempower.app.models.Order;
import com.sheempower.app.utils.Constants;
import java.util.List;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.VH> {

    public interface OnStatusChange {
        void onChange(Order order, String newStatus);
    }

    public interface OnReceiptClick {
        void onReceiptClick(Order order);
    }

    public interface OnChatClick {
        void onChatClick(Order order);
    }

    private final List<Order> orders;
    private final boolean isSellerView;
    private OnStatusChange statusChangeListener;
    private OnReceiptClick receiptClickListener;
    private OnChatClick chatClickListener;

    public OrderAdapter(List<Order> orders, boolean isSellerView) {
        this.orders = orders;
        this.isSellerView = isSellerView;
    }

    public void setOnStatusChangeListener(OnStatusChange listener) {
        this.statusChangeListener = listener;
    }

    public void setOnReceiptClickListener(OnReceiptClick listener) {
        this.receiptClickListener = listener;
    }

    public void setOnChatClickListener(OnChatClick listener) {
        this.chatClickListener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_order, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Order o = orders.get(position);

        // ✅ Basic Info
        holder.tvProduct.setText(o.getProductName());
        holder.tvAmount.setText("PKR " + (long) o.getAmount());
        holder.tvDetails.setText(o.getDetails());
        holder.tvAddress.setText("📍 " + o.getAddress());
        holder.tvStatus.setText("📌 " + o.getStatus());

        // ✅ Receipt Preview
        if (o.getReceiptUrl() != null && !o.getReceiptUrl().isEmpty()) {
            holder.ivReceipt.setVisibility(View.VISIBLE);
            Glide.with(holder.itemView.getContext())
                    .load(o.getReceiptUrl())
                    .placeholder(R.drawable.placeholder_product)
                    .into(holder.ivReceipt);
            holder.ivReceipt.setOnClickListener(v -> {
                if (receiptClickListener != null) receiptClickListener.onReceiptClick(o);
            });
        } else {
            holder.ivReceipt.setVisibility(View.GONE);
        }

        // ✅ Seller View: Status Advancement Button
        if (isSellerView) {
            holder.btnAdvance.setVisibility(View.VISIBLE);
            holder.btnChat.setVisibility(View.GONE);
            holder.btnReceipt.setVisibility(View.GONE);

            int idx = indexOf(o.getStatus());
            if (idx >= Constants.STATUS_ORDER.length - 1) {
                holder.btnAdvance.setVisibility(View.GONE);
            } else {
                String nextStatus = Constants.STATUS_ORDER[idx + 1];
                holder.btnAdvance.setText("→ " + nextStatus);
                holder.btnAdvance.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(0xFFE91E8C));
                holder.btnAdvance.setOnClickListener(v -> {
                    if (statusChangeListener != null) {
                        statusChangeListener.onChange(o, nextStatus);
                    }
                });
            }
        } else {
            // ✅ Buyer View: Receipt Upload + Chat Buttons
            holder.btnAdvance.setVisibility(View.GONE);

            // Show "View Receipt" if receipt exists
            if (o.getReceiptUrl() != null && !o.getReceiptUrl().isEmpty()) {
                holder.btnReceipt.setVisibility(View.VISIBLE);
                holder.btnReceipt.setText("📎 Receipt");
                holder.btnReceipt.setOnClickListener(v -> {
                    if (receiptClickListener != null) receiptClickListener.onReceiptClick(o);
                });
            } else if (Constants.STATUS_PENDING.equals(o.getStatus())) {
                // Pending order: Show "Upload Receipt" button
                holder.btnReceipt.setVisibility(View.VISIBLE);
                holder.btnReceipt.setText("📷 Upload Receipt");
                holder.btnReceipt.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(0xFFE91E8C));
                holder.btnReceipt.setOnClickListener(v -> {
                    if (receiptClickListener != null) receiptClickListener.onReceiptClick(o);
                });
            } else {
                holder.btnReceipt.setVisibility(View.GONE);
            }

            // Chat Button (Only if order is not completed)
            if (!Constants.STATUS_DONE.equals(o.getStatus())) {
                holder.btnChat.setVisibility(View.VISIBLE);
                holder.btnChat.setText("💬 Chat");
                holder.btnChat.setOnClickListener(v -> {
                    if (chatClickListener != null) chatClickListener.onChatClick(o);
                });
            } else {
                holder.btnChat.setVisibility(View.GONE);
            }
        }

        // ✅ Status color coding
        int statusColor = getStatusColor(o.getStatus());
        holder.tvStatus.setTextColor(statusColor);
    }

    private int getStatusColor(String status) {
        switch (status) {
            case Constants.STATUS_PENDING:
                return 0xFFFF9800; // Orange
            case Constants.STATUS_VERIFIED:
                return 0xFF2196F3; // Blue
            case Constants.STATUS_PROGRESS:
                return 0xFF9C27B0; // Purple
            case Constants.STATUS_DONE:
                return 0xFF4CAF50; // Green
            default:
                return 0xFF666666;
        }
    }

    private int indexOf(String status) {
        String[] arr = Constants.STATUS_ORDER;
        for (int i = 0; i < arr.length; i++) {
            if (arr[i].equals(status)) return i;
        }
        return 0;
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvProduct, tvAmount, tvStatus, tvDetails, tvAddress;
        Button btnAdvance, btnReceipt, btnChat;
        ImageView ivReceipt;

        VH(@NonNull View itemView) {
            super(itemView);
            tvProduct = itemView.findViewById(R.id.tvOrderProduct);
            tvAmount = itemView.findViewById(R.id.tvOrderAmount);
            tvStatus = itemView.findViewById(R.id.tvOrderStatus);
            tvDetails = itemView.findViewById(R.id.tvOrderDetails);
            tvAddress = itemView.findViewById(R.id.tvOrderAddress);
            btnAdvance = itemView.findViewById(R.id.btnAdvanceStatus);
            btnReceipt = itemView.findViewById(R.id.btnReceipt);
            btnChat = itemView.findViewById(R.id.btnChat);
            ivReceipt = itemView.findViewById(R.id.ivOrderReceipt);
        }
    }
}