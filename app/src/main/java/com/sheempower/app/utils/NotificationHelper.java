package com.sheempower.app.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.sheempower.app.models.Notification;
import com.sheempower.app.models.Order;

public class NotificationHelper {

    private static final String TAG = "NotificationHelper";
    public static final String CHANNEL_ID = "sheempower_orders";
    public static final String CHANNEL_NAME = "New Orders / نئے آرڈرز";

    public interface UnreadCountCallback {
        void onCount(int count);
    }

    public static void createNotificationChannel(Context context) {
        if (context == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                if (manager.getNotificationChannel(CHANNEL_ID) != null) {
                    return; // Channel already exists
                }
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID,
                        CHANNEL_NAME,
                        NotificationManager.IMPORTANCE_HIGH
                );
                channel.setDescription("Notifications for newly received orders / نئے موصول ہونے والے آرڈرز کے نوٹیفکیشنز");
                channel.enableVibration(true);
                manager.createNotificationChannel(channel);
            }
        }
    }

    public static void createAndSendOrderNotification(Context context, Order order) {
        if (order == null || order.getId() == null || order.getSellerId() == null) {
            Log.e(TAG, "Cannot create notification: Order or Seller ID is null");
            return;
        }

        String buyerName = (order.getBuyerName() != null && !order.getBuyerName().isEmpty())
                ? order.getBuyerName()
                : "Buyer / خریدار";

        String title = "New Order Received / نیا آرڈر موصول ہوا";
        String message = "You have received a new order from " + buyerName + " / آپ کو " + buyerName + " کی طرف سے نیا آرڈر موصول ہوا";

        Notification notification = new Notification(
                order.getId(),
                title,
                message,
                "new_order",
                order.getId(),
                order.getSellerId(),
                order.getBuyerId(),
                System.currentTimeMillis(),
                false
        );

        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();
        dbRef.child(Constants.NODE_NOTIFICATIONS)
                .child(order.getSellerId())
                .child(order.getId())
                .setValue(notification)
                .addOnSuccessListener(v -> Log.d(TAG, "Notification saved to Firebase successfully for order: " + order.getId()))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to save notification to Firebase: " + e.getMessage()));

        OneSignalHelper.sendNewOrderPushNotification(context, order.getSellerId(), buyerName, order.getId());
    }

    public static void markNotificationAsRead(String sellerUid, String orderId) {
        if (sellerUid == null || orderId == null) return;

        FirebaseDatabase.getInstance().getReference()
                .child(Constants.NODE_NOTIFICATIONS)
                .child(sellerUid)
                .child(orderId)
                .child("read")
                .setValue(true);
    }

    public static void getUnreadCount(String sellerUid, UnreadCountCallback callback) {
        if (sellerUid == null || callback == null) return;

        FirebaseDatabase.getInstance().getReference()
                .child(Constants.NODE_NOTIFICATIONS)
                .child(sellerUid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        int unreadCount = 0;
                        for (DataSnapshot child : snapshot.getChildren()) {
                            Boolean isRead = child.child("read").getValue(Boolean.class);
                            if (isRead != null && !isRead) {
                                unreadCount++;
                            }
                        }
                        callback.onCount(unreadCount);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onCount(0);
                    }
                });
    }
}
