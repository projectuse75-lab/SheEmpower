package com.sheempower.app.services;

import android.app.PendingIntent;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.sheempower.app.R;
import com.sheempower.app.activities.SellerOrdersActivity;
import com.sheempower.app.utils.Constants;
import com.sheempower.app.utils.FcmTokenHelper;
import com.sheempower.app.utils.SessionManager;

public class SheEmpowerMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FCM";

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "onNewToken called: " + token);
        String uid = new SessionManager(this).getUid();
        if (uid != null && !uid.trim().isEmpty()) {
            FcmTokenHelper.saveCurrentToken(uid);
        }
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage msg) {
        super.onMessageReceived(msg);
        Log.d(TAG, "onMessageReceived from: " + msg.getFrom());

        String title = "🛒 نیا آرڈر / New Order";
        String body = "آپ کو ایک نیا آرڈر موصول ہوا ہے";

        if (msg.getNotification() != null) {
            if (msg.getNotification().getTitle() != null) {
                title = msg.getNotification().getTitle();
            }
            if (msg.getNotification().getBody() != null) {
                body = msg.getNotification().getBody();
            }
        }

        if (msg.getData() != null && !msg.getData().isEmpty()) {
            Log.d(TAG, "Message data payload: " + msg.getData());
            if (msg.getData().containsKey("title") && msg.getData().get("title") != null) {
                title = msg.getData().get("title");
            }
            if (msg.getData().containsKey("message") && msg.getData().get("message") != null) {
                body = msg.getData().get("message");
            } else if (msg.getData().containsKey("body") && msg.getData().get("body") != null) {
                body = msg.getData().get("body");
            }
        }

        Log.d(TAG, "Displaying notification - Title: " + title + ", Body: " + body);

        Intent intent = new Intent(this, SellerOrdersActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pi = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, Constants.CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pi);

        try {
            NotificationManagerCompat.from(this).notify((int) System.currentTimeMillis(), builder.build());
        } catch (SecurityException e) {
            Log.e(TAG, "Notification permission error: " + e.getMessage());
        }
    }
}