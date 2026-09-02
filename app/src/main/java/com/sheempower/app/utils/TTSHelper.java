package com.sheempower.app.utils;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import java.util.Locale;

public class TTSHelper {
    private static TTSHelper instance;
    private TextToSpeech tts;
    private boolean ready = false;

    private TTSHelper(Context ctx) {
        tts = new TextToSpeech(ctx.getApplicationContext(), status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(new Locale("ur", "PK"));
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    result = tts.setLanguage(new Locale("hi", "IN"));
                }
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts.setLanguage(Locale.US);
                }
                tts.setSpeechRate(0.9f);
                ready = true;
            }
        });
    }

    public static synchronized TTSHelper getInstance(Context ctx) {
        if (instance == null) instance = new TTSHelper(ctx);
        return instance;
    }

    public void speak(String text) {
        if (ready && tts != null) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    // ✅ FIXED: Proper shutdown
    public void shutdown() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();  // ✅ Release resources
            tts = null;      // ✅ Prevent memory leak
        }
    }
}