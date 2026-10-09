package com.sheempower.app.utils;

import android.util.Log;

import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessaging;

public class FcmTokenHelper {

    private static final String TAG = "FCM";

    public static void saveCurrentToken(String uid) {
        if (uid == null || uid.trim().isEmpty()) {
            Log.w(TAG, "saveCurrentToken: uid is null or empty");
            return;
        }

        final String cleanUid = uid.trim();

        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (!task.isSuccessful() || task.getResult() == null) {
                Log.e(TAG, "Token fetch failed: " + (task.getException() != null ? task.getException().getMessage() : "null"), task.getException());
                return;
            }

            String token = task.getResult();
            Log.d(TAG, "Token fetched successfully for uid " + cleanUid + ": " + token);

            // Save token to users/{uid}/fcmToken in Realtime Database
            FirebaseDatabase.getInstance().getReference(Constants.NODE_USERS)
                    .child(cleanUid)
                    .child("fcmToken")
                    .setValue(token)
                    .addOnSuccessListener(v -> Log.d(TAG, "fcmToken saved successfully to users/" + cleanUid + "/fcmToken"))
                    .addOnFailureListener(e -> Log.e(TAG, "fcmToken save failed: " + e.getMessage()));
        });
    }
}