package com.sheempower.app.utils;

import android.util.Log;

import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessaging;

public class FcmHelper {

    private static final String TAG = "FcmHelper";
    public static final String KEY_FCM_TOKEN = "fcmToken";

    /** Fetches the FCM registration token for current device and saves it in Firebase Database */
    public static void initAndSyncToken(String userUid) {
        if (userUid == null || userUid.trim().isEmpty()) return;

        final String uid = userUid.trim();

        try {
            FirebaseMessaging.getInstance().getToken()
                    .addOnCompleteListener(task -> {
                        if (!task.isSuccessful() || task.getResult() == null) {
                            Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                            return;
                        }

                        String token = task.getResult();
                        Log.d(TAG, "FCM Token for " + uid + ": " + token);
                        saveTokenToFirebase(uid, token);
                    });
        } catch (Exception e) {
            Log.e(TAG, "initAndSyncToken error: " + e.getMessage(), e);
        }
    }

    public static void saveTokenToFirebase(String uid, String token) {
        if (uid == null || token == null) return;
        FirebaseDatabase db = FirebaseDatabase.getInstance();

        db.getReference(Constants.NODE_USERS).child(uid)
                .child(KEY_FCM_TOKEN).setValue(token);

        db.getReference(Constants.NODE_SELLERS).child(uid)
                .child(KEY_FCM_TOKEN).setValue(token)
                .addOnSuccessListener(v -> Log.d(TAG, "FCM token saved successfully for seller: " + uid))
                .addOnFailureListener(e -> Log.e(TAG, "FCM token save failed: " + e.getMessage()));
    }

    /** Verifies FCM token for seller in Firebase Database */
    public static void sendPushToSeller(String sellerUid, String title, String message) {
        if (sellerUid == null || sellerUid.trim().isEmpty()) return;

        final String cleanSellerUid = sellerUid.trim();

        FirebaseDatabase.getInstance().getReference(Constants.NODE_SELLERS).child(cleanSellerUid)
                .child(KEY_FCM_TOKEN)
                .get()
                .addOnSuccessListener(snapshot -> {
                    String fcmToken = snapshot.getValue(String.class);
                    if (fcmToken != null && !fcmToken.trim().isEmpty()) {
                        Log.d(TAG, "Seller FCM token verified: " + fcmToken);
                    } else {
                        FirebaseDatabase.getInstance().getReference(Constants.NODE_USERS).child(cleanSellerUid)
                                .child(KEY_FCM_TOKEN)
                                .get()
                                .addOnSuccessListener(userSnap -> {
                                    String userToken = userSnap.getValue(String.class);
                                    if (userToken != null) {
                                        Log.d(TAG, "User FCM token verified: " + userToken);
                                    }
                                });
                    }
                });
    }
}