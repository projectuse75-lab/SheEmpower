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
                // 1. Try Urdu first (as per prompt)
                int urduResult = tts.setLanguage(new Locale("ur"));

                // 2. Fallback to English if Urdu is not available
                if (urduResult == TextToSpeech.LANG_MISSING_DATA ||
                        urduResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts.setLanguage(Locale.ENGLISH);
                }

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

    public void shutdown() {
        if (tts != null) {
            tts.stop();      // Stop any ongoing speech
            tts.shutdown();  // ✅ Release the TTS engine resources
            tts = null;      // ✅ Prevent memory leaks
        }
    }
}