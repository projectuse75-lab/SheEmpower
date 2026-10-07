package com.sheempower.app.adapters;

import android.content.Intent;
import android.net.Uri;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.sheempower.app.R;
import com.sheempower.app.models.Order;
import com.sheempower.app.utils.Constants;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

        // Strip raw image URL lines from details text so only actual images show below
        String cleanedDetails = cleanDetailsText(o.getDetails());
        holder.tvDetails.setText(cleanedDetails);
        holder.tvDetails.setMovementMethod(LinkMovementMethod.getInstance());
        Linkify.addLinks(holder.tvDetails, Linkify.WEB_URLS);

        String who = isSellerView
                ? "خریدار: " + (o.getBuyerName() != null ? o.getBuyerName() : "—")
                : "بیچنے والی: " + (o.getSellerName() != null ? o.getSellerName() : "—");
        holder.tvParty.setText(who);
        holder.tvAddress.setText("📍 " + (o.getAddress() != null ? o.getAddress() : ""));

        // Extract and display sample image previews as actual image views
        List<String> imageUrls = extractImageUrls(o.getDetails());
        if (imageUrls != null && !imageUrls.isEmpty()) {
            holder.layoutSampleImagesContainer.setVisibility(View.VISIBLE);
            holder.layoutSampleImages.removeAllViews();

            float density = holder.itemView.getResources().getDisplayMetrics().density;
            int imgSize = Math.round(120 * density);
            int margin = Math.round(6 * density);

            for (String imgUrl : imageUrls) {
                ImageView iv = new ImageView(holder.itemView.getContext());
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(imgSize, imgSize);
                params.setMargins(0, 0, margin, 0);
                iv.setLayoutParams(params);
                iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                iv.setBackgroundResource(R.drawable.bg_skeu_card);

                Glide.with(holder.itemView.getContext())
                        .load(imgUrl)
                        .placeholder(R.drawable.placeholder_avatar)
                        .into(iv);

                iv.setOnClickListener(v -> {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(imgUrl));
                        holder.itemView.getContext().startActivity(intent);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

                holder.layoutSampleImages.addView(iv);
            }
        } else {
            holder.layoutSampleImagesContainer.setVisibility(View.GONE);
        }

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

    private String cleanDetailsText(String details) {
        if (details == null || details.isEmpty()) return "";

        String[] lines = details.split("\n");
        StringBuilder cleaned = new StringBuilder();

        for (String line : lines) {
            String trimmed = line.trim();
            String lower = trimmed.toLowerCase();

            // Skip lines that contain sample image links or standalone image URLs
            if (lower.contains("sample image:") || lower.contains("sample_image")
                    || (lower.startsWith("http") && (lower.contains("cloudinary") || lower.endsWith(".jpg")
                    || lower.contains(".jpeg") || lower.endsWith(".png") || lower.endsWith(".webp")))) {
                continue;
            }

            cleaned.append(line).append("\n");
        }

        return cleaned.toString().trim();
    }

    private List<String> extractImageUrls(String text) {
        if (text == null || text.isEmpty()) return null;
        List<String> list = new ArrayList<>();
        Pattern pattern = Pattern.compile("https?://[^\\s]+");
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            String url = matcher.group().replaceAll("[\\)\\]\\}\"']$", "");
            String lower = url.toLowerCase();
            if (lower.contains("cloudinary") || lower.endsWith(".jpg") || lower.contains(".jpeg") 
                    || lower.endsWith(".png") || lower.endsWith(".webp")) {
                if (!list.contains(url)) {
                    list.add(url);
                }
            }
        }
        return list;
    }

    @Override public int getItemCount() { return orders.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvProduct, tvAmount, tvStatus, tvDetails, tvParty, tvAddress;
        Button btnAdvance;
        LinearLayout layoutSampleImagesContainer, layoutSampleImages;

        VH(@NonNull View itemView) {
            super(itemView);
            tvProduct = itemView.findViewById(R.id.tvOrderProduct);
            tvAmount  = itemView.findViewById(R.id.tvOrderAmount);
            tvStatus  = itemView.findViewById(R.id.tvOrderStatus);
            tvDetails = itemView.findViewById(R.id.tvOrderDetails);
            tvParty   = itemView.findViewById(R.id.tvOrderParty);
            tvAddress = itemView.findViewById(R.id.tvOrderAddress);
            btnAdvance= itemView.findViewById(R.id.btnAdvanceStatus);
            layoutSampleImagesContainer = itemView.findViewById(R.id.layoutSampleImagesContainer);
            layoutSampleImages          = itemView.findViewById(R.id.layoutSampleImages);
        }
    }
}