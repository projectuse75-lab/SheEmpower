package com.sheempower.app.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

public class WhatsAppHelper {

    /** Chat button click par yeh call hota hai — seedha WhatsApp khulta hai, in-app chat nahi */
    public static void openChat(Context ctx, String whatsappNumber, String prefilledMessage) {
        if (whatsappNumber == null || whatsappNumber.trim().isEmpty()) {
            Toast.makeText(ctx, "Seller ka WhatsApp number available nahi hai", Toast.LENGTH_SHORT).show();
            return;
        }
        String number = whatsappNumber.replaceAll("[^0-9]", "");
        String msg = prefilledMessage == null
                ? "Assalam o Alaikum! Mujhe She Empower app se aapki profile mili."
                : prefilledMessage;

        String url = "https://wa.me/" + number + "?text=" + Uri.encode(msg);
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            ctx.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(ctx, "WhatsApp install nahi hai is device par", Toast.LENGTH_SHORT).show();
        }
    }
}