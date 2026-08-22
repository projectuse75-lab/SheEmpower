package com.sheempower.app.adapters;

import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.sheempower.app.R;
import com.sheempower.app.models.Seller;
import java.util.List;

public class PendingSellerAdapter extends RecyclerView.Adapter<PendingSellerAdapter.VH> {

    // ✅ NEW: Interface with 3 actions
    public interface OnSellerAction {
        void onApprove(Seller seller);
        void onReject(Seller seller);
        void onToggleTopRated(Seller seller);
    }

    private final List<Seller> sellers;
    private final OnSellerAction actionListener;

    // ✅ UPDATED: Single listener object instead of two separate callbacks
    public PendingSellerAdapter(List<Seller> sellers, OnSellerAction actionListener) {
        this.sellers = sellers;
        this.actionListener = actionListener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pending_seller, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Seller s = sellers.get(position);

        // Basic info
        holder.tvName.setText(s.getBusinessName());
        holder.tvMeta.setText(s.getOwnerName() + " · " + s.getCategory() + " · " + s.getCity());
        holder.tvWhatsApp.setText("📱 " + s.getWhatsappNumber());

        // ✅ CNIC View Button
        holder.btnViewCnic.setOnClickListener(v -> {
            if (s.getCnicUrl() != null && !s.getCnicUrl().isEmpty()) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(s.getCnicUrl()));
                v.getContext().startActivity(intent);
            } else {
                Toast.makeText(v.getContext(), "No CNIC image uploaded", Toast.LENGTH_SHORT).show();
            }
        });

        // ✅ Voice Play Button
        holder.btnPlayVoice.setOnClickListener(v -> {
            if (s.getVoiceUrl() != null && !s.getVoiceUrl().isEmpty()) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(s.getVoiceUrl()));
                v.getContext().startActivity(intent);
            } else {
                Toast.makeText(v.getContext(), "No voice sample uploaded", Toast.LENGTH_SHORT).show();
            }
        });

        // ✅ Approve
        holder.btnApprove.setOnClickListener(v -> actionListener.onApprove(s));

        // ✅ Reject
        holder.btnReject.setOnClickListener(v -> actionListener.onReject(s));

        // ✅ Toggle Top Rated (only visible for approved sellers)
        if ("approved".equals(s.getVerificationStatus())) {
            holder.btnToggleTopRated.setVisibility(View.VISIBLE);
            holder.btnToggleTopRated.setText(s.isTopRated() ? "⭐ Remove Top" : "⭐ Mark Top");
        } else {
            holder.btnToggleTopRated.setVisibility(View.GONE);
        }
        holder.btnToggleTopRated.setOnClickListener(v -> actionListener.onToggleTopRated(s));
    }

    @Override
    public int getItemCount() {
        return sellers.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvMeta, tvWhatsApp;
        Button btnViewCnic, btnPlayVoice, btnApprove, btnReject, btnToggleTopRated;

        VH(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvPendingName);
            tvMeta = itemView.findViewById(R.id.tvPendingMeta);
            tvWhatsApp = itemView.findViewById(R.id.tvPendingWhatsApp);
            btnViewCnic = itemView.findViewById(R.id.btnViewCnic);
            btnPlayVoice = itemView.findViewById(R.id.btnPlayVoice);
            btnApprove = itemView.findViewById(R.id.btnApprove);
            btnReject = itemView.findViewById(R.id.btnReject);
            btnToggleTopRated = itemView.findViewById(R.id.btnToggleTopRated);
        }
    }
}