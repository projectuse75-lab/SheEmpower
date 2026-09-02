package com.sheempower.app.utils;

import android.content.Context;

public class VoiceGuide {

    public static void announceScreen(Context ctx, String urduMessage) {
        TTSHelper.getInstance(ctx).speak(urduMessage);
    }

    public static void explain(Context ctx, String urduExplanation) {
        TTSHelper.getInstance(ctx).speak(urduExplanation);
    }
}