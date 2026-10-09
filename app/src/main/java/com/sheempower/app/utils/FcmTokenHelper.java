package com.sheempower.app.utils;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessaging;

public class FcmTokenHelper {

    private static final String TAG = "FCM";

    public static void saveCurrentToken(String fallbackUid) {
        saveCurrentTokenWithContext(null, fallbackUid);
    }

    public static void saveCurrentTokenWithContext(final Context context, String fallbackUid) {
        String targetUid = null;

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null && currentUser.getUid() != null && !currentUser.getUid().trim().isEmpty()) {
            targetUid = currentUser.getUid().trim();
        } else if (fallbackUid != null && !fallbackUid.trim().isEmpty()) {
            targetUid = fallbackUid.trim();
        }

        if (targetUid == null) {
            Log.w(TAG, "saveCurrentToken: No valid user UID found");
            return;
        }

        final String uid = targetUid;

        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (!task.isSuccessful() || task.getResult() == null) {
                String errMsg = task.getException() != null ? task.getException().getMessage() : "null";
                Log.e(TAG, "Token fetch failed: " + errMsg, task.getException());
                if (context != null) {
                    try {
                        Toast.makeText(context, "TOKEN ERROR: " + errMsg, Toast.LENGTH_LONG).show();
                    } catch (Exception ignored) {}
                }
                return;
            }

            String token = task.getResult();
            Log.d(TAG, "Token fetched successfully for uid " + uid + ": " + token);

            // Save token to users/{uid}/fcmToken in Realtime Database
            FirebaseDatabase.getInstance().getReference(Constants.NODE_USERS)
                    .child(uid)
                    .child("fcmToken")
                    .setValue(token)
                    .addOnSuccessListener(v -> {
                        Log.d(TAG, "fcmToken saved successfully to users/" + uid + "/fcmToken");
                        if (context != null) {
                            try {
                                Toast.makeText(context, "✅ FCM Token Saved to Firebase!", Toast.LENGTH_SHORT).show();
                            } catch (Exception ignored) {}
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "fcmToken save failed in users: " + e.getMessage());
                        if (context != null) {
                            try {
                                Toast.makeText(context, "SAVE ERROR: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            } catch (Exception ignored) {}
                        }
                    });

            // Also save token to sellers/{uid}/fcmToken in Realtime Database
            FirebaseDatabase.getInstance().getReference(Constants.NODE_SELLERS)
                    .child(uid)
                    .child("fcmToken")
                    .setValue(token)
                    .addOnSuccessListener(v -> Log.d(TAG, "fcmToken saved successfully to sellers/" + uid + "/fcmToken"))
                    .addOnFailureListener(e -> Log.e(TAG, "fcmToken save failed in sellers: " + e.getMessage()));
        });
    }
}