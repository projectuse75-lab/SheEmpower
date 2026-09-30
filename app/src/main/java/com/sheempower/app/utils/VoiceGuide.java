package com.sheempower.app.utils;

import android.content.Context;
import android.widget.ImageButton;
import com.sheempower.app.R;

public class VoiceGuide {

    public static boolean isMuted(Context ctx) {
        if (ctx == null) return false;
        PreferencesManager prefs = new PreferencesManager(ctx);
        return prefs.isVoiceMuted();
    }

    public static boolean toggleMute(Context ctx) {
        if (ctx == null) return false;
        PreferencesManager prefs = new PreferencesManager(ctx);
        boolean currentMuted = prefs.isVoiceMuted();
        boolean newMuted = !currentMuted;
        prefs.setVoiceMuted(newMuted);

        if (newMuted) {
            stop(ctx);
        }
        return newMuted;
    }

    public static void announceScreen(Context ctx, String urduMessage) {
        if (isMuted(ctx)) return;
        try {
            if (ctx != null) {
                stop(ctx);
                TTSHelper.getInstance(ctx).speak(urduMessage);
            }
        } catch (Throwable ignored) {
        }
    }

    public static void explain(Context ctx, String urduExplanation) {
        if (isMuted(ctx)) return;
        try {
            if (ctx != null) {
                stop(ctx);
                TTSHelper.getInstance(ctx).speak(urduExplanation);
            }
        } catch (Throwable ignored) {
        }
    }

    public static void stop(Context ctx) {
        try {
            if (ctx != null) {
                TTSHelper.getInstance(ctx).stop();
            }
        } catch (Throwable ignored) {
        }
    }

    public static void setupSpeakerButton(ImageButton btnSpeaker, Context ctx, String urduMessage) {
        if (btnSpeaker == null || ctx == null) return;

        updateSpeakerIcon(btnSpeaker, isMuted(ctx));

        btnSpeaker.setOnClickListener(v -> {
            boolean nowMuted = toggleMute(ctx);
            updateSpeakerIcon(btnSpeaker, nowMuted);

            if (!nowMuted) {
                announceScreen(ctx, urduMessage);
            }
        });
    }

    public static void updateSpeakerIcon(ImageButton btnSpeaker, boolean muted) {
        if (btnSpeaker == null) return;
        if (muted) {
            btnSpeaker.setImageResource(R.drawable.ic_speaker_off);
        } else {
            btnSpeaker.setImageResource(R.drawable.ic_speaker);
        }
    }
}