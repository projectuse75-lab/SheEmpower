package com.sheempower.app.utils;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.util.Locale;

public class TTSHelper {

    private static final String TAG = "TTSHelper";
    private static TTSHelper instance;

    private final Context appContext;
    private TextToSpeech tts;
    private boolean ready = false;
    private String pendingText = null;

    private TTSHelper(Context ctx) {
        this.appContext = ctx != null ? ctx.getApplicationContext() : null;
        try {
            tts = new TextToSpeech(this.appContext, status -> {
                try {
                    if (status != TextToSpeech.SUCCESS || tts == null) {
                        ready = false;
                        return;
                    }

                    int langResult = tts.setLanguage(new Locale("ur", "PK"));

                    if (langResult == TextToSpeech.LANG_MISSING_DATA ||
                            langResult == TextToSpeech.LANG_NOT_SUPPORTED) {

                        langResult = tts.setLanguage(new Locale("hi", "IN"));
                    }

                    if (langResult == TextToSpeech.LANG_MISSING_DATA ||
                            langResult == TextToSpeech.LANG_NOT_SUPPORTED) {

                        tts.setLanguage(Locale.US);
                    }

                    if (tts != null) {
                        tts.setSpeechRate(0.9f);
                        ready = true;
                        if (pendingText != null && appContext != null && !VoiceGuide.isMuted(appContext)) {
                            speakInternal(pendingText);
                        }
                        pendingText = null;
                    }
                } catch (Throwable t) {
                    Log.w(TAG, "Error configuring TextToSpeech engine: " + t.getMessage());
                    ready = false;
                }
            });
        } catch (Throwable t) {
            Log.w(TAG, "Failed to initialize TextToSpeech engine: " + t.getMessage());
            ready = false;
            tts = null;
        }
    }

    public static synchronized TTSHelper getInstance(Context ctx) {
        if (instance == null || instance.tts == null) {
            instance = new TTSHelper(ctx);
        }
        return instance;
    }

    public void speak(String text) {
        if (appContext != null && VoiceGuide.isMuted(appContext)) {
            stop();
            return;
        }

        if (text == null || text.trim().isEmpty()) {
            return;
        }

        if (ready && tts != null) {
            speakInternal(text);
        } else {
            pendingText = text;
        }
    }

    private void speakInternal(String text) {
        if (appContext != null && VoiceGuide.isMuted(appContext)) {
            return;
        }
        try {
            if (tts != null) {
                tts.stop();
                tts.speak(
                        text,
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        "she_empower_tts"
                );
            }
        } catch (Throwable t) {
            Log.w(TAG, "Failed to speak TTS text: " + t.getMessage());
        }
    }

    public void stop() {
        pendingText = null;
        if (tts != null) {
            try {
                tts.stop();
            } catch (Throwable ignored) {}
        }
    }

    public void shutdown() {
        ready = false;
        pendingText = null;

        if (tts != null) {
            try {
                tts.stop();
                tts.shutdown();
            } catch (Throwable ignored) {
            }

            tts = null;
        }
    }
}