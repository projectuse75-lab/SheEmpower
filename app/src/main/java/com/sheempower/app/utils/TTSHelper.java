package com.sheempower.app.utils;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.Locale;

public class TTSHelper {

    private static final String TAG = "TTSHelper";
    private static TTSHelper instance;

    private final Context appContext;
    private TextToSpeech tts;
    private boolean ready = false;

    private String pendingText = null;
    private int pendingActivityHashCode = 0;
    private int activeActivityHashCode = 0;
    private WeakReference<Activity> activeActivityRef = new WeakReference<>(null);

    private boolean lifecycleInitialized = false;

    private TTSHelper(Context ctx) {
        this.appContext = ctx != null ? ctx.getApplicationContext() : null;

        if (this.appContext instanceof Application) {
            initLifecycle((Application) this.appContext);
        }

        initTtsEngine();
    }

    public static synchronized TTSHelper getInstance(Context ctx) {
        if (instance == null || instance.tts == null) {
            instance = new TTSHelper(ctx);
        } else if (ctx != null && !instance.lifecycleInitialized) {
            Context app = ctx.getApplicationContext();
            if (app instanceof Application) {
                instance.initLifecycle((Application) app);
            }
        }
        return instance;
    }

    public synchronized void initLifecycle(Application application) {
        if (lifecycleInitialized || application == null) return;
        lifecycleInitialized = true;

        application.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityPaused(Activity activity) {
                if (activity != null && activity.hashCode() == activeActivityHashCode) {
                    stop();
                }
            }

            @Override
            public void onActivityStopped(Activity activity) {
                if (activity != null && activity.hashCode() == activeActivityHashCode) {
                    stop();
                }
            }

            @Override
            public void onActivityDestroyed(Activity activity) {
                if (activity != null && activity.hashCode() == activeActivityHashCode) {
                    stop();
                    if (activeActivityHashCode == activity.hashCode()) {
                        activeActivityHashCode = 0;
                        activeActivityRef = new WeakReference<>(null);
                    }
                }
            }

            @Override public void onActivityCreated(Activity activity, Bundle savedInstanceState) {}
            @Override public void onActivityStarted(Activity activity) {}
            @Override public void onActivityResumed(Activity activity) {}
            @Override public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}
        });
    }

    private void initTtsEngine() {
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

                        // Check pending text execution safely
                        processPendingSpeechOnReady();
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

    private synchronized void processPendingSpeechOnReady() {
        if (pendingText == null || pendingText.trim().isEmpty()) {
            return;
        }

        Activity activeAct = activeActivityRef.get();
        if (activeAct != null) {
            if (activeAct.isFinishing() || activeAct.isDestroyed()) {
                pendingText = null;
                pendingActivityHashCode = 0;
                return;
            }
            if (pendingActivityHashCode != 0 && pendingActivityHashCode != activeAct.hashCode()) {
                pendingText = null;
                pendingActivityHashCode = 0;
                return;
            }
        }

        if (appContext != null && VoiceGuide.isMuted(appContext)) {
            pendingText = null;
            pendingActivityHashCode = 0;
            return;
        }

        String textToSpeak = pendingText;
        pendingText = null;
        pendingActivityHashCode = 0;

        speakInternal(textToSpeak);
    }

    public synchronized void speak(Context context, String text) {
        if (text == null || text.trim().isEmpty()) {
            return;
        }

        Activity activity = getActivity(context);
        if (activity != null) {
            if (activity.isFinishing() || activity.isDestroyed()) {
                return;
            }
            activeActivityHashCode = activity.hashCode();
            activeActivityRef = new WeakReference<>(activity);
        }

        // Stop any currently playing audio from previous screen
        if (ready && tts != null) {
            try {
                tts.stop();
            } catch (Throwable ignored) {}
        }

        if (appContext != null && VoiceGuide.isMuted(appContext)) {
            return;
        }

        if (ready && tts != null) {
            if (activity != null && (activity.isFinishing() || activity.isDestroyed())) {
                return;
            }
            speakInternal(text);
        } else {
            pendingText = text;
            pendingActivityHashCode = activity != null ? activity.hashCode() : activeActivityHashCode;
        }
    }

    public void speak(String text) {
        Activity activeAct = activeActivityRef.get();
        speak(activeAct != null ? activeAct : appContext, text);
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

    public synchronized void stop() {
        pendingText = null;
        pendingActivityHashCode = 0;
        if (tts != null) {
            try {
                tts.stop();
            } catch (Throwable ignored) {}
        }
    }

    public synchronized void stop(Context context) {
        Activity act = getActivity(context);
        if (act != null) {
            if (act.hashCode() == activeActivityHashCode) {
                stop();
            }
        } else {
            stop();
        }
    }

    public synchronized void shutdown() {
        ready = false;
        pendingText = null;
        pendingActivityHashCode = 0;
        activeActivityHashCode = 0;
        activeActivityRef = new WeakReference<>(null);

        if (tts != null) {
            try {
                tts.stop();
                tts.shutdown();
            } catch (Throwable ignored) {
            }

            tts = null;
        }
    }

    public static Activity getActivity(Context context) {
        if (context == null) return null;
        if (context instanceof Activity) return (Activity) context;
        if (context instanceof ContextWrapper) {
            return getActivity(((ContextWrapper) context).getBaseContext());
        }
        return null;
    }
}