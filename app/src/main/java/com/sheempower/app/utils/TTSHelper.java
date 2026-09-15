package com.sheempower.app.utils;

import android.content.Context;
import android.speech.tts.TextToSpeech;

import java.util.Locale;

public class TTSHelper {

    private static TTSHelper instance;

    private TextToSpeech tts;
    private boolean ready = false;

    private TTSHelper(Context ctx) {

        Context appContext = ctx.getApplicationContext();

        tts = new TextToSpeech(appContext, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {

                if (status != TextToSpeech.SUCCESS) {
                    ready = false;
                    return;
                }

                if (tts == null) {
                    ready = false;
                    return;
                }

                int result = tts.setLanguage(new Locale("ur", "PK"));

                if (result == TextToSpeech.LANG_MISSING_DATA ||
                        result == TextToSpeech.LANG_NOT_SUPPORTED) {

                    result = tts.setLanguage(new Locale("hi", "IN"));
                }

                if (result == TextToSpeech.LANG_MISSING_DATA ||
                        result == TextToSpeech.LANG_NOT_SUPPORTED) {

                    result = tts.setLanguage(Locale.US);
                }

                if (tts != null) {
                    tts.setSpeechRate(0.9f);
                    ready = true;
                }
            }
        });
    }

    public static synchronized TTSHelper getInstance(Context ctx) {

        if (instance == null || instance.tts == null) {
            instance = new TTSHelper(ctx);
        }

        return instance;
    }

    public void speak(String text) {

        if (ready && tts != null && text != null && !text.isEmpty()) {
            tts.speak(
                    text,
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "she_empower_tts"
            );
        }
    }

    public void shutdown() {

        ready = false;

        if (tts != null) {
            try {
                tts.stop();
                tts.shutdown();
            } catch (Exception ignored) {
            }

            tts = null;
        }
    }
}