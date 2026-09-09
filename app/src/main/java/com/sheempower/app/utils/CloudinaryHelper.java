package com.sheempower.app.utils;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import okhttp3.*;
import java.io.IOException;
import java.io.InputStream;
import org.json.JSONObject;

public class CloudinaryHelper {

    private static final String TAG = "CloudinaryHelper";

    public interface UploadCallback {
        void onResult(String url, String errorMessage); // errorMessage null = success
    }

    public static void upload(Context ctx, Uri fileUri, String folder, UploadCallback callback) {
        new Thread(() -> {
            try {
                InputStream is = ctx.getContentResolver().openInputStream(fileUri);
                byte[] bytes = readAllBytes(is);

                OkHttpClient client = new OkHttpClient();
                RequestBody fileBody = RequestBody.create(bytes, MediaType.parse("application/octet-stream"));

                RequestBody body = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("file", "upload", fileBody)
                        .addFormDataPart("upload_preset", Constants.CLOUDINARY_UPLOAD_PRESET)
                        .addFormDataPart("folder", folder)
                        .build();

                Request request = new Request.Builder().url(Constants.CLOUDINARY_URL).post(body).build();
                Response response = client.newCall(request).execute();
                String resStr = response.body() != null ? response.body().string() : null;

                if (response.isSuccessful() && resStr != null) {
                    JSONObject json = new JSONObject(resStr);
                    String secureUrl = json.optString("secure_url", null);
                    new Handler(Looper.getMainLooper()).post(() -> callback.onResult(secureUrl, null));
                } else {
                    String errMsg = "HTTP " + response.code();
                    if (resStr != null) {
                        try {
                            JSONObject errJson = new JSONObject(resStr);
                            errMsg = errJson.optJSONObject("error") != null
                                    ? errJson.optJSONObject("error").optString("message", errMsg) : errMsg;
                        } catch (Exception ignored) {}
                    }
                    final String finalErr = errMsg;
                    Log.e(TAG, "Upload failed: " + finalErr);
                    new Handler(Looper.getMainLooper()).post(() -> callback.onResult(null, finalErr));
                }
            } catch (Exception e) {
                Log.e(TAG, "Upload exception: " + e.getMessage(), e);
                new Handler(Looper.getMainLooper()).post(() -> callback.onResult(null, e.getMessage()));
            }
        }).start();
    }

    private static byte[] readAllBytes(InputStream is) throws IOException {
        java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[4096];
        while ((nRead = is.read(data, 0, data.length)) != -1) buffer.write(data, 0, nRead);
        return buffer.toByteArray();
    }
}