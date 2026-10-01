package com.sheempower.app.utils;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.onesignal.Continue;
import com.onesignal.OneSignal;
import com.onesignal.user.subscriptions.IPushSubscriptionObserver;
import com.onesignal.user.subscriptions.PushSubscriptionChangedState;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class OneSignalHelper {

    private static final String TAG = "OneSignalHelper";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final OkHttpClient httpClient = new OkHttpClient();
    private static boolean isObserverRegistered = false;

    public static void init(Context context) {
        try {
            if (Constants.ONESIGNAL_APP_ID != null && !Constants.ONESIGNAL_APP_ID.isEmpty() &&
                    !"YOUR_ONESIGNAL_APP_ID".equals(Constants.ONESIGNAL_APP_ID)) {
                OneSignal.initWithContext(context, Constants.ONESIGNAL_APP_ID);
                OneSignal.getNotifications().requestPermission(true, Continue.none());
            } else {
                Log.w(TAG, "OneSignal App ID is not configured yet in Constants.java.");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error initializing OneSignal: " + e.getMessage(), e);
        }
    }

    public static void updateSellerPlayerId(String sellerUid) {
        if (sellerUid == null || sellerUid.trim().isEmpty()) return;

        final String uid = sellerUid.trim();

        try {
            OneSignal.login(uid);

            String subscriptionId = OneSignal.getUser().getPushSubscription().getId();
            if (subscriptionId != null && !subscriptionId.isEmpty()) {
                savePlayerIdToFirebase(uid, subscriptionId);
            }

            if (!isObserverRegistered) {
                isObserverRegistered = true;
                OneSignal.getUser().getPushSubscription().addObserver(new IPushSubscriptionObserver() {
                    @Override
                    public void onPushSubscriptionChange(@NonNull PushSubscriptionChangedState state) {
                        String newId = state.getCurrent().getId();
                        if (newId != null && !newId.isEmpty()) {
                            savePlayerIdToFirebase(uid, newId);
                        }
                    }
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating seller OneSignal Player ID: " + e.getMessage(), e);
        }
    }

    private static void savePlayerIdToFirebase(String sellerUid, String playerId) {
        if (sellerUid == null || playerId == null) return;

        DatabaseReference db = FirebaseDatabase.getInstance().getReference();
        db.child(Constants.NODE_USERS).child(sellerUid).child(Constants.KEY_ONESIGNAL_PLAYER_ID).setValue(playerId);
        db.child(Constants.NODE_SELLERS).child(sellerUid).child(Constants.KEY_ONESIGNAL_PLAYER_ID).setValue(playerId);
        Log.d(TAG, "Successfully updated OneSignal Player ID in Firebase for seller: " + sellerUid);
    }

    public static void sendNewOrderPushNotification(Context context, String sellerUid, String buyerName, String orderId) {
        if (sellerUid == null || sellerUid.trim().isEmpty()) return;

        final String targetUid = sellerUid.trim();
        final String name = (buyerName != null && !buyerName.isEmpty()) ? buyerName : "Buyer / خریدار";

        FirebaseDatabase.getInstance().getReference().child(Constants.NODE_USERS).child(targetUid)
                .child(Constants.KEY_ONESIGNAL_PLAYER_ID)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String playerId = snapshot.getValue(String.class);
                        postNotificationToApi(targetUid, playerId, name, orderId);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        postNotificationToApi(targetUid, null, name, orderId);
                    }
                });
    }

    private static void postNotificationToApi(String sellerUid, String playerId, String buyerName, String orderId) {
        if (Constants.ONESIGNAL_APP_ID == null || Constants.ONESIGNAL_APP_ID.isEmpty() ||
                "YOUR_ONESIGNAL_APP_ID".equals(Constants.ONESIGNAL_APP_ID) ||
                Constants.ONESIGNAL_REST_API_KEY == null || Constants.ONESIGNAL_REST_API_KEY.isEmpty() ||
                "YOUR_ONESIGNAL_REST_API_KEY".equals(Constants.ONESIGNAL_REST_API_KEY)) {
            Log.w(TAG, "OneSignal App ID or REST API Key not configured. Skipping push API call.");
            return;
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                JSONObject jsonBody = new JSONObject();
                jsonBody.put("app_id", Constants.ONESIGNAL_APP_ID);

                if (playerId != null && !playerId.isEmpty()) {
                    JSONArray playerIds = new JSONArray();
                    playerIds.put(playerId);
                    jsonBody.put("include_player_ids", playerIds);
                } else {
                    JSONObject includeAliases = new JSONObject();
                    JSONArray externalIds = new JSONArray();
                    externalIds.put(sellerUid);
                    includeAliases.put("external_id", externalIds);
                    jsonBody.put("include_aliases", includeAliases);
                    jsonBody.put("target_channel", "push");
                }

                JSONObject headings = new JSONObject();
                headings.put("en", "New Order Received");
                headings.put("ur", "نیا آرڈر موصول ہوا");
                jsonBody.put("headings", headings);

                JSONObject contents = new JSONObject();
                contents.put("en", "You have received a new order from " + buyerName + ".");
                contents.put("ur", "آپ کو " + buyerName + " کی طرف سے نیا آرڈر موصول ہوا ہے۔");
                jsonBody.put("contents", contents);

                JSONObject data = new JSONObject();
                data.put("orderId", orderId);
                data.put("sellerId", sellerUid);
                data.put("type", "new_order");
                jsonBody.put("data", data);

                RequestBody body = RequestBody.create(jsonBody.toString(), JSON);
                Request request = new Request.Builder()
                        .url("https://onesignal.com/api/v1/notifications")
                        .post(body)
                        .addHeader("Authorization", "Basic " + Constants.ONESIGNAL_REST_API_KEY)
                        .addHeader("Content-Type", "application/json; charset=utf-8")
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        Log.d(TAG, "OneSignal Push Notification sent successfully: " + (response.body() != null ? response.body().string() : ""));
                    } else {
                        Log.e(TAG, "OneSignal Push Notification failed with code: " + response.code() + " body: " + (response.body() != null ? response.body().string() : ""));
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "Network error sending OneSignal Push: " + e.getMessage(), e);
            } catch (Exception e) {
                Log.e(TAG, "Error forming OneSignal Push request: " + e.getMessage(), e);
            }
        });
    }
}
