package com.sheempower.app.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.sheempower.app.models.Notification;
import com.sheempower.app.models.Order;

public class NotificationHelper {

    /** App khulte hi ek dafa call hoti hai — Android 8+ ke liye channel zaroori hai */
    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    Constants.CHANNEL_ID,
                    Constants.CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("نئے آرڈرز کی اطلاع");
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    /** Order save hone ke foran baad PlaceOrderActivity se call hoti hai —
     *  Firebase mein record banati hai AUR OneSignal se push bhi bhejti hai */
    public static void createAndSendOrderNotification(Context context, Order order) {
        if (order == null || order.getSellerId() == null) return;

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference(Constants.NODE_NOTIFICATIONS)
                .child(order.getSellerId());

        String notifId = ref.push().getKey();
        if (notifId == null) return;

        String title = "🛒 نیا آرڈر / New Order";
        String message = (order.getBuyerName() != null ? order.getBuyerName() : "ایک خریدار")
                + " نے " + order.getProductName() + " کا آرڈر دیا ہے";

        Notification notification = new Notification(
                notifId, title, message, "new_order",
                order.getId(), order.getSellerId(), order.getBuyerId()
        );

        ref.child(notifId).setValue(notification)
                .addOnSuccessListener(v ->
                        OneSignalHelper.sendPushToSeller(order.getSellerId(), title, message))
                .addOnFailureListener(e ->
                        android.util.Log.e("NotificationHelper",
                                "Firebase notification save failed: " + e.getMessage()));
    }

    public static void markNotificationAsRead(String sellerUid, String orderId) {
        if (sellerUid == null || orderId == null) return;
        FirebaseDatabase.getInstance().getReference(Constants.NODE_NOTIFICATIONS)
                .child(sellerUid)
                .orderByChild("orderId").equalTo(orderId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    for (com.google.firebase.database.DataSnapshot child : snapshot.getChildren()) {
                        child.getRef().child("read").setValue(true);
                    }
                });
    }

    public interface UnreadCountCallback { void onResult(int count); }

    public static void getUnreadCount(String sellerUid, UnreadCountCallback callback) {
        if (sellerUid == null) { callback.onResult(0); return; }
        FirebaseDatabase.getInstance().getReference(Constants.NODE_NOTIFICATIONS)
                .child(sellerUid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    int count = 0;
                    for (com.google.firebase.database.DataSnapshot child : snapshot.getChildren()) {
                        Boolean read = child.child("read").getValue(Boolean.class);
                        if (read == null || !read) count++;
                    }
                    callback.onResult(count);
                })
                .addOnFailureListener(e -> callback.onResult(0));
    }
}