package com.sheempower.app.utils;

import android.content.Context;
import android.util.Log;

import com.google.firebase.database.FirebaseDatabase;
import com.onesignal.Continue;
import com.onesignal.OneSignal;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class OneSignalHelper {

    private static final String TAG = "OneSignalHelper";
    private static boolean initialized = false;

    /** SheEmpowerApplication.onCreate() mein sirf ek dafa call hoti hai */
    public static void init(Context context) {
        if (initialized) return;
        initialized = true;

        try {
            OneSignal.initWithContext(context, Constants.ONESIGNAL_APP_ID);
            // Android 13+ runtime notification permission request
            OneSignal.getNotifications().requestPermission(true, Continue.with(result -> {
                if (result.isSuccess() && Boolean.TRUE.equals(result.getData())) {
                    Log.d(TAG, "OneSignal notification permission granted");
                    OneSignal.getUser().getPushSubscription().optIn();
                } else {
                    Log.w(TAG, "OneSignal notification permission denied");
                }
            }));
            Log.d(TAG, "OneSignal initialized with App ID: " + Constants.ONESIGNAL_APP_ID);
        } catch (Exception e) {
            Log.e(TAG, "OneSignal init failed: " + e.getMessage(), e);
        }
    }

    /** Seller login/dashboard open hone par call hoti hai — device subscription ID ko Firebase mein save karti hai */
    public static void updateSellerPlayerId(String sellerUid) {
        if (sellerUid == null || sellerUid.trim().isEmpty()) return;

        try {
            // Is uid ko OneSignal ke user ke sath login/link kar dete hain
            OneSignal.login(sellerUid);

            // Subscription ID turant nikalte hain
            String subscriptionId = OneSignal.getUser().getPushSubscription().getId();
            if (subscriptionId != null && !subscriptionId.isEmpty()) {
                saveIdToFirebase(sellerUid, subscriptionId);
            }

            // Push subscription state change hone par automatically Firebase update hoga
            OneSignal.getUser().getPushSubscription().addObserver(state -> {
                if (state != null && state.getCurrent() != null) {
                    String id = state.getCurrent().getId();
                    if (id != null && !id.isEmpty()) {
                        saveIdToFirebase(sellerUid, id);
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "updateSellerPlayerId failed: " + e.getMessage(), e);
        }
    }

    private static void saveIdToFirebase(String uid, String subscriptionId) {
        FirebaseDatabase db = FirebaseDatabase.getInstance();
        db.getReference(Constants.NODE_USERS).child(uid)
                .child(Constants.KEY_ONESIGNAL_PLAYER_ID).setValue(subscriptionId);
        db.getReference(Constants.NODE_SELLERS).child(uid)
                .child(Constants.KEY_ONESIGNAL_PLAYER_ID).setValue(subscriptionId)
                .addOnSuccessListener(v -> Log.d(TAG, "Player ID saved for " + uid + ": " + subscriptionId))
                .addOnFailureListener(e -> Log.e(TAG, "Player ID save failed: " + e.getMessage()));
    }

    /** Seller ki saved Player ID / External ID ko fetch karke push notification bhejti hai */
    public static void sendPushToSeller(String sellerUid, String title, String message) {
        if (sellerUid == null || sellerUid.trim().isEmpty()) return;

        FirebaseDatabase.getInstance().getReference(Constants.NODE_SELLERS).child(sellerUid)
                .child(Constants.KEY_ONESIGNAL_PLAYER_ID)
                .get()
                .addOnSuccessListener(snapshot -> {
                    String playerId = snapshot.getValue(String.class);
                    if (playerId == null || playerId.trim().isEmpty()) {
                        Log.w(TAG, "Seller Player ID not found in Firebase, sending push via external_id alias: " + sellerUid);
                    }
                    callOneSignalRestApi(sellerUid, playerId, title, message);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Player ID fetch failed: " + e.getMessage());
                    callOneSignalRestApi(sellerUid, null, title, message);
                });
    }

    private static void callOneSignalRestApi(String sellerUid, String playerId, String title, String message) {
        new Thread(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("app_id", Constants.ONESIGNAL_APP_ID);

                // OneSignal SDK v5 compatibility:
                // Pass subscription ID via "include_subscription_ids" and "include_player_ids"
                if (playerId != null && !playerId.trim().isEmpty()) {
                    JSONArray idsArray = new JSONArray().put(playerId.trim());
                    body.put("include_subscription_ids", idsArray);
                    body.put("include_player_ids", idsArray);
                } else {
                    // Fallback to targeting by OneSignal login external_id alias
                    JSONObject aliases = new JSONObject();
                    aliases.put("external_id", new JSONArray().put(sellerUid));
                    body.put("include_aliases", aliases);
                    body.put("target_channel", "push");
                }

                JSONObject headings = new JSONObject();
                headings.put("en", title);
                body.put("headings", headings);

                JSONObject contents = new JSONObject();
                contents.put("en", message);
                body.put("contents", contents);

                body.put("android_channel_id", Constants.CHANNEL_ID);

                OkHttpClient client = new OkHttpClient();
                RequestBody requestBody = RequestBody.create(
                        body.toString(), MediaType.parse("application/json"));

                Request request = new Request.Builder()
                        .url("https://onesignal.com/api/v1/notifications")
                        .addHeader("Authorization", "Key " + Constants.ONESIGNAL_REST_API_KEY)
                        .addHeader("Content-Type", "application/json")
                        .post(requestBody)
                        .build();

                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        Log.e(TAG, "OneSignal REST call failed: " + e.getMessage(), e);
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        String resBody = response.body() != null ? response.body().string() : "";
                        Log.d(TAG, "OneSignal response (" + response.code() + "): " + resBody);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "OneSignal push build failed: " + e.getMessage(), e);
            }
        }).start();
    }
}