package com.sheempower.app.utils;

import android.content.Context;

public class VoiceGuide {

    public static void announceScreen(Context ctx, String urduMessage) {
        try {
            if (ctx != null) {
                TTSHelper.getInstance(ctx).speak(urduMessage);
            }
        } catch (Throwable ignored) {
        }
    }

    public static void explain(Context ctx, String urduExplanation) {
        try {
            if (ctx != null) {
                TTSHelper.getInstance(ctx).speak(urduExplanation);
            }
        } catch (Throwable ignored) {
        }
    }
}