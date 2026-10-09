package com.sheempower.app.utils;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.widget.Button;
import android.widget.ImageButton;
import com.sheempower.app.R;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class ImageCropHelper {

    public interface CropCallback {
        void onCropCompleted(Uri croppedUri);
    }

    public static void cropImage(Activity activity, Uri originalUri, CropCallback callback) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed() || originalUri == null) {
            if (callback != null) callback.onCropCompleted(originalUri);
            return;
        }

        try {
            InputStream is = activity.getContentResolver().openInputStream(originalUri);
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            if (is != null) is.close();

            if (bitmap == null) {
                if (callback != null) callback.onCropCompleted(originalUri);
                return;
            }

            Dialog dialog = new Dialog(activity, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
            dialog.setContentView(R.layout.dialog_touch_crop);

            TouchCropView touchCropView = dialog.findViewById(R.id.touchCropView);
            ImageButton btnRotate = dialog.findViewById(R.id.btnRotateTouch);
            Button btnCancel = dialog.findViewById(R.id.btnTouchCancel);
            Button btnCrop = dialog.findViewById(R.id.btnTouchCrop);

            if (touchCropView != null) {
                touchCropView.setImageBitmap(bitmap);
            }

            if (btnRotate != null) {
                btnRotate.setOnClickListener(v -> {
                    if (touchCropView != null) touchCropView.rotate90(true);
                });
            }

            if (btnCancel != null) {
                btnCancel.setOnClickListener(v -> {
                    dialog.dismiss();
                    if (callback != null) callback.onCropCompleted(originalUri);
                });
            }

            if (btnCrop != null) {
                btnCrop.setOnClickListener(v -> {
                    dialog.dismiss();
                    Bitmap croppedBmp = touchCropView != null ? touchCropView.getCroppedBitmap() : null;
                    Uri processedUri = saveBitmapToTempUri(activity, croppedBmp);
                    if (callback != null) {
                        callback.onCropCompleted(processedUri != null ? processedUri : originalUri);
                    }
                });
            }

            dialog.show();

        } catch (Exception e) {
            if (callback != null) callback.onCropCompleted(originalUri);
        }
    }

    private static Uri saveBitmapToTempUri(Activity activity, Bitmap bitmap) {
        if (bitmap == null) return null;
        try {
            File tempFile = new File(activity.getCacheDir(), "cropped_img_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream fos = new FileOutputStream(tempFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.flush();
            fos.close();
            return Uri.fromFile(tempFile);
        } catch (Exception e) {
            return null;
        }
    }
}