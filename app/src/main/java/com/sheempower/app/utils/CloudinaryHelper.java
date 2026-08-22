package com.sheempower.app.utils;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import okhttp3.*;
import java.io.IOException;
import java.io.InputStream;
import org.json.JSONObject;

public class CloudinaryHelper {

    public interface UploadCallback { void onResult(String url); }

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

                Request request = new Request.Builder()
                        .url(Constants.CLOUDINARY_URL)
                        .post(body)
                        .build();

                Response response = client.newCall(request).execute();
                String resStr = response.body() != null ? response.body().string() : null;

                String secureUrl = null;
                if (response.isSuccessful() && resStr != null) {
                    JSONObject json = new JSONObject(resStr);
                    secureUrl = json.optString("secure_url", null);
                }

                final String finalUrl = secureUrl;
                new Handler(Looper.getMainLooper()).post(() -> callback.onResult(finalUrl));

            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onResult(null));
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