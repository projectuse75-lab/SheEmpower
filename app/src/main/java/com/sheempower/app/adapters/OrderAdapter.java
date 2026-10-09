package com.sheempower.app.adapters;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
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
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.sheempower.app.R;
import com.sheempower.app.activities.AddReviewActivity;
import com.sheempower.app.models.Order;
import com.sheempower.app.utils.CloudinaryHelper;
import com.sheempower.app.utils.Constants;
import com.sheempower.app.utils.PreferencesManager;
import com.sheempower.app.utils.SessionManager;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.VH> {

    public interface OnStatusChange { void onChange(Order order, String newStatus); }

    private final List<Order> orders;
    private final boolean isSellerView;
    private final boolean isAdminView;
    private OnStatusChange statusChangeListener;

    public OrderAdapter(List<Order> orders, boolean isSellerView) {
        this(orders, isSellerView, false);
    }

    public OrderAdapter(List<Order> orders, boolean isSellerView, boolean isAdminView) {
        this.orders = orders;
        this.isSellerView = isSellerView;
        this.isAdminView = isAdminView;
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
        Context ctx = holder.itemView.getContext();

        holder.tvProduct.setText(o.getProductName());
        holder.tvAmount.setText("PKR " + (long) o.getAmount());
        holder.tvStatus.setText(Constants.getStatusLabel(o.getStatus(), o.getDeliveryMethod()));
        holder.tvStatus.setTextColor(0xFFFFFFFF);

        String cleanedDetails = cleanDetailsText(o.getDetails());
        holder.tvDetails.setText(cleanedDetails);
        holder.tvDetails.setMovementMethod(LinkMovementMethod.getInstance());
        Linkify.addLinks(holder.tvDetails, Linkify.WEB_URLS);

        String who = isSellerView
                ? "خریدار: " + (o.getBuyerName() != null ? o.getBuyerName() : "—")
                : "بیچنے والی: " + (o.getSellerName() != null ? o.getSellerName() : "—");
        holder.tvParty.setText(who);

        if (holder.tvOrderTime != null) {
            String formattedTime = o.getFormattedOrderDateTime();
            if (formattedTime != null && !formattedTime.isEmpty()) {
                holder.tvOrderTime.setVisibility(View.VISIBLE);
                holder.tvOrderTime.setText("📅 " + formattedTime);
            } else {
                holder.tvOrderTime.setVisibility(View.GONE);
            }
        }

        holder.tvAddress.setText("📍 " + (o.getAddress() != null ? o.getAddress() : ""));

        // PAYMENT INFO BINDING
        boolean isDark = new PreferencesManager(ctx).isDarkThemeEnabled();
        int paymentTextColor = isDark ? 0xFFFFFFFF : 0xFF192A56;
        int paymentStatusColor = isDark ? 0xFFFFFFFF : 0xFF6B7A99;

        String pMethod = o.getPaymentMethod() != null ? o.getPaymentMethod().toUpperCase() : "COD";
        String pStatus = o.getPaymentStatus() != null ? o.getPaymentStatus() : "pending";

        if (holder.tvPaymentMethod != null) {
            holder.tvPaymentMethod.setText("💳 Payment Method: " + pMethod);
            holder.tvPaymentMethod.setTextColor(paymentTextColor);
        }
        if (holder.tvPaymentStatus != null) {
            String pStatusLabel = "Status: " + pStatus;
            if ("verified".equalsIgnoreCase(pStatus)) pStatusLabel = "✅ Payment Received ✓ / ادائیگی موصول ہو گئی";
            else if ("rejected".equalsIgnoreCase(pStatus)) pStatusLabel = "❌ Payment Not Confirmed / ادائیگی کی تصدیق نہیں ہوئی";
            else if ("submitted".equalsIgnoreCase(pStatus)) pStatusLabel = "⏳ Proof Submitted / رسید بھیج دی گئی ہے";
            holder.tvPaymentStatus.setText(pStatusLabel);
            holder.tvPaymentStatus.setTextColor(paymentStatusColor);
        }

        if (holder.btnViewPaymentProof != null) {
            holder.btnViewPaymentProof.setTextColor(isDark ? 0xFFFFFFFF : 0xFF192A56);
        }

        SessionManager session = new SessionManager(ctx);
        String currentUid = session.getUid() != null ? session.getUid().trim() : "";
        boolean isOrderOwnerSeller = isSellerView && currentUid.equalsIgnoreCase(o.getSellerId() != null ? o.getSellerId().trim() : "");
        boolean hasProof = o.getPaymentProofUrl() != null && !o.getPaymentProofUrl().isEmpty();

        // 1. Seller View: Show Payment Proof Viewer + Verify/Reject Buttons for Seller who owns the order!
        if (isSellerView && isOrderOwnerSeller) {
            if (holder.layoutAdminPaymentActions != null) {
                holder.layoutAdminPaymentActions.setVisibility(hasProof ? View.VISIBLE : View.GONE);
            }
            if (holder.btnViewPaymentProof != null) {
                holder.btnViewPaymentProof.setVisibility(hasProof ? View.VISIBLE : View.GONE);
                holder.btnViewPaymentProof.setOnClickListener(v -> showProofImageDialog(ctx, o.getPaymentProofUrl(), "Payment Proof Screenshot"));
            }

            // Only show Verify/Reject buttons for online submitted payments
            boolean showVerifyReject = hasProof && "submitted".equalsIgnoreCase(pStatus);
            if (holder.btnVerifyPayment != null) {
                holder.btnVerifyPayment.setVisibility(showVerifyReject ? View.VISIBLE : View.GONE);
                holder.btnVerifyPayment.setOnClickListener(v -> updatePaymentStatusInFirebase(ctx, o.getId(), "verified"));
            }
            if (holder.btnRejectPayment != null) {
                holder.btnRejectPayment.setVisibility(showVerifyReject ? View.VISIBLE : View.GONE);
                holder.btnRejectPayment.setOnClickListener(v -> updatePaymentStatusInFirebase(ctx, o.getId(), "rejected"));
            }
        }
        // 2. Admin View: Read-only view for payment proof (NO verify/reject buttons for Admin)
        else if (isAdminView) {
            if (holder.layoutAdminPaymentActions != null) {
                holder.layoutAdminPaymentActions.setVisibility(hasProof ? View.VISIBLE : View.GONE);
            }
            if (holder.btnViewPaymentProof != null) {
                holder.btnViewPaymentProof.setVisibility(hasProof ? View.VISIBLE : View.GONE);
                holder.btnViewPaymentProof.setOnClickListener(v -> showProofImageDialog(ctx, o.getPaymentProofUrl(), "Payment Proof Screenshot"));
            }
            if (holder.btnVerifyPayment != null) holder.btnVerifyPayment.setVisibility(View.GONE);
            if (holder.btnRejectPayment != null) holder.btnRejectPayment.setVisibility(View.GONE);
        }
        // 3. Buyer View
        else {
            if (holder.layoutAdminPaymentActions != null) {
                if (hasProof) {
                    holder.layoutAdminPaymentActions.setVisibility(View.VISIBLE);
                    if (holder.btnViewPaymentProof != null) {
                        holder.btnViewPaymentProof.setVisibility(View.VISIBLE);
                        holder.btnViewPaymentProof.setOnClickListener(v -> showProofImageDialog(ctx, o.getPaymentProofUrl(), "My Payment Proof"));
                    }
                    if (holder.btnVerifyPayment != null) holder.btnVerifyPayment.setVisibility(View.GONE);
                    if (holder.btnRejectPayment != null) holder.btnRejectPayment.setVisibility(View.GONE);
                } else {
                    holder.layoutAdminPaymentActions.setVisibility(View.GONE);
                }
            }
        }

        // Buyer Re-upload Payment Proof button if rejected
        if (!isSellerView && !isAdminView && "rejected".equalsIgnoreCase(pStatus)) {
            if (holder.btnReuploadPaymentProof != null) {
                holder.btnReuploadPaymentProof.setVisibility(View.VISIBLE);
                holder.btnReuploadPaymentProof.setOnClickListener(v -> showReuploadProofDialog(ctx, o));
            }
        } else {
            if (holder.btnReuploadPaymentProof != null) holder.btnReuploadPaymentProof.setVisibility(View.GONE);
        }

        // Rate & Review button for Buyer on COMPLETED orders
        if (!isSellerView && !isAdminView && Constants.STATUS_COMPLETED.equalsIgnoreCase(o.getStatus())) {
            if (holder.btnRateReview != null) {
                holder.btnRateReview.setTextColor(isDark ? 0xFFFFFFFF : 0xFF192A56);
                holder.btnRateReview.setVisibility(View.VISIBLE);
                holder.btnRateReview.setOnClickListener(v -> {
                    Intent intent = new Intent(ctx, AddReviewActivity.class);
                    intent.putExtra(AddReviewActivity.EXTRA_ORDER_ID, o.getId());
                    intent.putExtra(AddReviewActivity.EXTRA_SELLER_ID, o.getSellerId());
                    intent.putExtra(AddReviewActivity.EXTRA_SELLER_NAME, o.getSellerName());
                    intent.putExtra(AddReviewActivity.EXTRA_PRODUCT_ID, o.getProductId());
                    intent.putExtra(AddReviewActivity.EXTRA_PROD_NAME, o.getProductName());
                    ctx.startActivity(intent);
                });
            }
        } else {
            if (holder.btnRateReview != null) holder.btnRateReview.setVisibility(View.GONE);
        }

        // Sample Images
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

                iv.setOnClickListener(v -> showProofImageDialog(ctx, imgUrl, "Sample Image"));
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
        } else if (!isAdminView) {
            // Buyer View: Show "I've Received / Picked Up" button for active pending/in_progress orders!
            boolean isPendingOrProgress = Constants.STATUS_PENDING.equalsIgnoreCase(o.getStatus()) || Constants.STATUS_IN_PROGRESS.equalsIgnoreCase(o.getStatus());
            if (isPendingOrProgress) {
                holder.btnAdvance.setVisibility(View.VISIBLE);
                holder.btnAdvance.setText("📦 I've Received / Picked Up (میں نے آرڈر وصول کر لیا)");
                holder.btnAdvance.setOnClickListener(v -> showConfirmReceiptDialog(ctx, o));
            } else {
                holder.btnAdvance.setVisibility(View.GONE);
            }
        } else {
            holder.btnAdvance.setVisibility(View.GONE);
        }
    }

    private void showConfirmReceiptDialog(Context ctx, Order order) {
        new AlertDialog.Builder(ctx)
                .setTitle("Have you received your order? / کیا آپ نے اپنا آرڈر وصول کر لیا ہے؟")
                .setMessage("آرڈر کی وصولی کی تصدیق کرنے پر آرڈر مکمل ہو جائے گا۔\nConfirming receipt will mark this order as completed.")
                .setPositiveButton("ہاں / Yes", (dialog, which) -> {
                    if (order == null || order.getId() == null) return;
                    String oid = order.getId().trim();

                    DatabaseReference orderRef = FirebaseDatabase.getInstance().getReference()
                            .child(Constants.NODE_ORDERS).child(oid);

                    orderRef.child("status").setValue(Constants.STATUS_COMPLETED);
                    orderRef.child("paymentStatus").setValue("verified");
                    orderRef.child("deliveryStatus").setValue("delivered")
                            .addOnSuccessListener(aVoid -> Toast.makeText(ctx, "آرڈر کی وصولی کی تصدیق ہو گئی! ✅", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(ctx, "خرابی: " + e.getMessage(), Toast.LENGTH_LONG).show());
                })
                .setNegativeButton("منسوخ / Cancel", null)
                .show();
    }

    private void updatePaymentStatusInFirebase(Context ctx, String orderId, String newStatus) {
        if (orderId == null || orderId.isEmpty()) return;
        FirebaseDatabase.getInstance().getReference().child(Constants.NODE_ORDERS)
                .child(orderId).child("paymentStatus").setValue(newStatus)
                .addOnSuccessListener(aVoid -> Toast.makeText(ctx, "ادائیگی کی حیثیت اپڈیٹ ہو گئی: " + newStatus, Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(ctx, "خرابی: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void showProofImageDialog(Context ctx, String imageUrl, String title) {
        if (imageUrl == null || imageUrl.isEmpty()) return;
        Dialog dialog = new Dialog(ctx, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_fullscreen_image);

        ImageView ivFull = dialog.findViewById(R.id.ivFullScreenProduct);
        TextView tvTitle = dialog.findViewById(R.id.tvImageTitle);
        View btnClose = dialog.findViewById(R.id.btnCloseImage);

        if (tvTitle != null) tvTitle.setText(title != null ? title : "");
        if (btnClose != null) btnClose.setOnClickListener(v -> dialog.dismiss());

        if (ivFull != null) {
            Glide.with(ctx)
                    .load(imageUrl)
                    .placeholder(R.drawable.placeholder_product)
                    .error(R.drawable.placeholder_product)
                    .into(ivFull);

            ivFull.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }

    private void showReuploadProofDialog(Context ctx, Order order) {
        new AlertDialog.Builder(ctx)
                .setTitle("نئی رسید اپلوڈ کریں / Re-upload Payment Proof")
                .setMessage("آپ کی پچھلی رسید منسوخ کر دی گئی تھی۔ براہ کرم دوبارہ صحیح اسکرین شاٹ واٹس ایپ پر یا ایڈمن سے رابطہ کر کے فراہم کریں۔")
                .setPositiveButton("ٹھیک ہے / OK", null)
                .show();
    }

    private String cleanDetailsText(String details) {
        if (details == null || details.isEmpty()) return "";

        String[] lines = details.split("\n");
        StringBuilder cleaned = new StringBuilder();

        for (String line : lines) {
            String trimmed = line.trim();
            String lower = trimmed.toLowerCase();

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
        TextView tvProduct, tvAmount, tvStatus, tvDetails, tvParty, tvAddress, tvOrderTime;
        TextView tvPaymentMethod, tvPaymentStatus;
        Button btnAdvance, btnViewPaymentProof, btnVerifyPayment, btnRejectPayment, btnReuploadPaymentProof, btnRateReview;
        LinearLayout layoutSampleImagesContainer, layoutSampleImages, layoutAdminPaymentActions;

        VH(@NonNull View itemView) {
            super(itemView);
            tvProduct = itemView.findViewById(R.id.tvOrderProduct);
            tvAmount  = itemView.findViewById(R.id.tvOrderAmount);
            tvStatus  = itemView.findViewById(R.id.tvOrderStatus);
            tvDetails = itemView.findViewById(R.id.tvOrderDetails);
            tvParty   = itemView.findViewById(R.id.tvOrderParty);
            tvAddress = itemView.findViewById(R.id.tvOrderAddress);
            tvOrderTime = itemView.findViewById(R.id.tvOrderTime);
            btnAdvance= itemView.findViewById(R.id.btnAdvanceStatus);
            btnRateReview = itemView.findViewById(R.id.btnRateReview);
            layoutSampleImagesContainer = itemView.findViewById(R.id.layoutSampleImagesContainer);
            layoutSampleImages          = itemView.findViewById(R.id.layoutSampleImages);

            tvPaymentMethod        = itemView.findViewById(R.id.tvOrderPaymentMethod);
            tvPaymentStatus        = itemView.findViewById(R.id.tvOrderPaymentStatus);
            layoutAdminPaymentActions = itemView.findViewById(R.id.layoutAdminPaymentActions);
            btnViewPaymentProof    = itemView.findViewById(R.id.btnViewPaymentProof);
            btnVerifyPayment       = itemView.findViewById(R.id.btnVerifyPayment);
            btnRejectPayment       = itemView.findViewById(R.id.btnRejectPayment);
            btnReuploadPaymentProof = itemView.findViewById(R.id.btnReuploadPaymentProof);
        }
    }
}