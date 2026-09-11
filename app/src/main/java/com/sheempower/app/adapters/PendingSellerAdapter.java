package com.sheempower.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.sheempower.app.R;
import com.sheempower.app.models.Seller;
import java.util.List;

public class PendingSellerAdapter extends RecyclerView.Adapter<PendingSellerAdapter.VH> {

    public interface OnAction { void onAction(Seller seller); }

    private final List<Seller> sellers;
    private final OnAction onApprove, onReject, onRowClick;

    public PendingSellerAdapter(List<Seller> sellers, OnAction onApprove, OnAction onReject, OnAction onRowClick) {
        this.sellers = sellers;
        this.onApprove = onApprove;
        this.onReject = onReject;
        this.onRowClick = onRowClick;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pending_seller, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Seller s = sellers.get(position);
        holder.tvName.setText(s.getBusinessName());
        holder.tvMeta.setText(s.getOwnerName() + " · " + s.getCategory() + " · " + s.getCity());
        holder.tvWhatsApp.setText("📱 " + s.getWhatsappNumber());
        holder.btnApprove.setOnClickListener(v -> onApprove.onAction(s));
        holder.btnReject.setOnClickListener(v -> onReject.onAction(s));
        holder.itemView.setOnClickListener(v -> onRowClick.onAction(s));
    }

    @Override public int getItemCount() { return sellers.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvMeta, tvWhatsApp;
        Button btnApprove, btnReject;
        VH(@NonNull View itemView) {
            super(itemView);
            tvName     = itemView.findViewById(R.id.tvPendingName);
            tvMeta     = itemView.findViewById(R.id.tvPendingMeta);
            tvWhatsApp = itemView.findViewById(R.id.tvPendingWhatsApp);
            btnApprove = itemView.findViewById(R.id.btnApprove);
            btnReject  = itemView.findViewById(R.id.btnReject);
        }
    }
}