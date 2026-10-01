package com.sheempower.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.sheempower.app.R;
import com.sheempower.app.models.Order;
import com.sheempower.app.utils.Constants;
import java.util.List;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.VH> {

    public interface OnStatusChange { void onChange(Order order, String newStatus); }

    private final List<Order> orders;
    private final boolean isSellerView;
    private OnStatusChange statusChangeListener;

    public OrderAdapter(List<Order> orders, boolean isSellerView) {
        this.orders = orders;
        this.isSellerView = isSellerView;
    }

    public void setOnStatusChangeListener(OnStatusChange listener) { this.statusChangeListener = listener; }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Order o = orders.get(position);
        holder.tvProduct.setText(o.getProductName());
        holder.tvAmount.setText("PKR " + (long) o.getAmount());
        holder.tvStatus.setText(Constants.getStatusLabel(o.getStatus(), o.getDeliveryMethod()));
        holder.tvStatus.setTextColor(0xFFFFFFFF);
        holder.tvDetails.setText(o.getDetails());

        String who = isSellerView
                ? "خریدار: " + (o.getBuyerName() != null ? o.getBuyerName() : "—")
                : "بیچنے والی: " + (o.getSellerName() != null ? o.getSellerName() : "—");
        holder.tvParty.setText(who);
        holder.tvAddress.setText("📍 " + (o.getAddress() != null ? o.getAddress() : ""));

        if (isSellerView) {
            if (Constants.STATUS_PENDING.equals(o.getStatus())) {
                holder.btnAdvance.setVisibility(View.VISIBLE);
                holder.btnAdvance.setText("▶️ کام شروع کریں / Start Work");
                holder.btnAdvance.setOnClickListener(v -> {
                    if (statusChangeListener != null) statusChangeListener.onChange(o, Constants.STATUS_IN_PROGRESS);
                });
            } else if (Constants.STATUS_IN_PROGRESS.equals(o.getStatus())) {
                holder.btnAdvance.setVisibility(View.VISIBLE);
                boolean isPickup = Constants.DELIVERY_METHOD_PICKUP.equals(o.getDeliveryMethod());
                holder.btnAdvance.setText(isPickup ? "✅ مکمل — خریدار خود لے جائے" : "✅ مکمل — ڈیلیوری کریں");
                holder.btnAdvance.setOnClickListener(v -> {
                    if (statusChangeListener != null) statusChangeListener.onChange(o, Constants.STATUS_COMPLETED);
                });
            } else {
                holder.btnAdvance.setVisibility(View.GONE);
            }
        } else {
            holder.btnAdvance.setVisibility(View.GONE);
        }
    }

    @Override public int getItemCount() { return orders.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvProduct, tvAmount, tvStatus, tvDetails, tvParty, tvAddress;
        Button btnAdvance;
        VH(@NonNull View itemView) {
            super(itemView);
            tvProduct = itemView.findViewById(R.id.tvOrderProduct);
            tvAmount  = itemView.findViewById(R.id.tvOrderAmount);
            tvStatus  = itemView.findViewById(R.id.tvOrderStatus);
            tvDetails = itemView.findViewById(R.id.tvOrderDetails);
            tvParty   = itemView.findViewById(R.id.tvOrderParty);
            tvAddress = itemView.findViewById(R.id.tvOrderAddress);
            btnAdvance= itemView.findViewById(R.id.btnAdvanceStatus);
        }
    }
}