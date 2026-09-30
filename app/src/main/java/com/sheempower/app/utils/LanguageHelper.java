package com.sheempower.app.utils;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import java.util.HashMap;
import java.util.Map;

public class LanguageHelper {

    public interface OnLanguageSelectedListener {
        void onLanguageSelected(String langMode);
    }

    private static final Map<String, String> URDU_TO_ENGLISH_MAP = new HashMap<>();
    static {
        URDU_TO_ENGLISH_MAP.put("کھانا پکانا", "Cooking");
        URDU_TO_ENGLISH_MAP.put("سلائی", "Stitching");
        URDU_TO_ENGLISH_MAP.put("مہندی", "Mehndi");
        URDU_TO_ENGLISH_MAP.put("میک اپ", "Makeup");
        URDU_TO_ENGLISH_MAP.put("کڑھائی", "Embroidery");

        URDU_TO_ENGLISH_MAP.put("زیر التواء", "Not Started / Pending");
        URDU_TO_ENGLISH_MAP.put("کام جاری ہے", "In Progress");
        URDU_TO_ENGLISH_MAP.put("مکمل", "Completed");

        URDU_TO_ENGLISH_MAP.put("تصدیق شدہ", "Verified");
        URDU_TO_ENGLISH_MAP.put("غیر تصدیق شدہ", "Unverified");
        URDU_TO_ENGLISH_MAP.put("ایڈمن کی منظوری کا انتظار", "Pending Admin Approval");

        URDU_TO_ENGLISH_MAP.put("گھر ڈیلیوری دستیاب ہے", "Home delivery available");
        URDU_TO_ENGLISH_MAP.put("یہ خود آپ کے پاس آ سکتی ہیں", "Available to visit your location");
        URDU_TO_ENGLISH_MAP.put("صرف خود آ کر لینا ہوگا", "Self-pickup only");

        URDU_TO_ENGLISH_MAP.put("پہلے لاگ ان کریں", "Please login first");
        URDU_TO_ENGLISH_MAP.put("ڈیٹا لوڈ نہیں ہو سکا", "Failed to load data");
        URDU_TO_ENGLISH_MAP.put("یہ بیچنے والی اب دستیاب نہیں ہے", "This seller is no longer available");
        URDU_TO_ENGLISH_MAP.put("بیچنے والی کی شناخت نہیں ملی", "Seller ID not found");

        URDU_TO_ENGLISH_MAP.put("آرڈر دیں", "Place Order");
        URDU_TO_ENGLISH_MAP.put("واٹس ایپ پر بات کریں", "Chat on WhatsApp");
        URDU_TO_ENGLISH_MAP.put("اشیاء", "Products");
        URDU_TO_ENGLISH_MAP.put("بیچنے والیاں", "Sellers");
        URDU_TO_ENGLISH_MAP.put("زمرہ جات", "Categories");
        URDU_TO_ENGLISH_MAP.put("بہترین بیچنے والیاں", "Top Sellers");

        URDU_TO_ENGLISH_MAP.put("لاگ ان", "Login");
        URDU_TO_ENGLISH_MAP.put("رجسٹر", "Register");
        URDU_TO_ENGLISH_MAP.put("تلاش", "Search");
        URDU_TO_ENGLISH_MAP.put("تفصیل", "Description");
        URDU_TO_ENGLISH_MAP.put("قیمت", "Price");
    }

    public static String getFormattedText(Context ctx, String urdu, String english) {
        if (ctx == null) return (urdu != null ? urdu : "") + " / " + (english != null ? english : "");
        PreferencesManager prefs = new PreferencesManager(ctx);
        String mode = prefs.getAppLanguage();

        if (PreferencesManager.LANG_URDU.equals(mode)) {
            if (!TextUtils.isEmpty(urdu)) return urdu;
            return english != null ? english : "";
        } else if (PreferencesManager.LANG_ENGLISH.equals(mode)) {
            if (!TextUtils.isEmpty(english)) return english;
            if (!TextUtils.isEmpty(urdu) && URDU_TO_ENGLISH_MAP.containsKey(urdu.trim())) {
                return URDU_TO_ENGLISH_MAP.get(urdu.trim());
            }
            return urdu != null ? urdu : "";
        } else {
            // LANG_MIX
            if (!TextUtils.isEmpty(urdu) && !TextUtils.isEmpty(english)) {
                if (urdu.trim().equalsIgnoreCase(english.trim())) return urdu;
                return urdu + " / " + english;
            } else if (!TextUtils.isEmpty(urdu)) {
                String translated = URDU_TO_ENGLISH_MAP.get(urdu.trim());
                if (translated != null && !translated.equalsIgnoreCase(urdu)) {
                    return urdu + " / " + translated;
                }
                return urdu;
            } else {
                return english != null ? english : "";
            }
        }
    }

    public static String processDualString(Context ctx, String combinedText) {
        if (combinedText == null || combinedText.isEmpty()) return "";

        String delimiter = null;
        if (combinedText.contains(" / ")) {
            delimiter = " / ";
        } else if (combinedText.contains("\n") && isDualLanguageLine(combinedText)) {
            delimiter = "\n";
        }

        if (delimiter == null) {
            PreferencesManager prefs = ctx != null ? new PreferencesManager(ctx) : null;
            String mode = prefs != null ? prefs.getAppLanguage() : PreferencesManager.LANG_MIX;

            if (PreferencesManager.LANG_ENGLISH.equals(mode) && URDU_TO_ENGLISH_MAP.containsKey(combinedText.trim())) {
                return URDU_TO_ENGLISH_MAP.get(combinedText.trim());
            }
            return combinedText;
        }

        String[] parts = combinedText.split(delimiter, 2);
        String urdu = parts[0].trim();
        String english = parts.length > 1 ? parts[1].trim() : "";

        String emojiPrefix = extractLeadingEmoji(urdu);
        if (!emojiPrefix.isEmpty()) {
            urdu = urdu.substring(emojiPrefix.length()).trim();
            if (!english.isEmpty() && !hasLeadingEmoji(english)) {
                english = emojiPrefix + " " + english;
            }
            if (!urdu.isEmpty()) {
                urdu = emojiPrefix + " " + urdu;
            } else {
                urdu = emojiPrefix;
            }
        }

        return getFormattedText(ctx, urdu, english);
    }

    private static boolean isDualLanguageLine(String text) {
        String[] lines = text.split("\n", 2);
        if (lines.length < 2) return false;
        boolean firstHasUrdu = containsUrdu(lines[0]);
        boolean secondHasEnglish = containsEnglish(lines[1]);
        return firstHasUrdu || secondHasEnglish;
    }

    public static boolean containsUrdu(String str) {
        if (str == null) return false;
        for (char c : str.toCharArray()) {
            if ((c >= 0x0600 && c <= 0x06FF) || (c >= 0x0750 && c <= 0x077F) || (c >= 0xFB50 && c <= 0xFDFF) || (c >= 0xFE70 && c <= 0xFEFF)) {
                return true;
            }
        }
        return false;
    }

    public static boolean containsEnglish(String str) {
        if (str == null) return false;
        for (char c : str.toCharArray()) {
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) return true;
        }
        return false;
    }

    private static String extractLeadingEmoji(String str) {
        if (str == null) return "";
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < str.length()) {
            int codePoint = str.codePointAt(i);
            int charCount = Character.charCount(codePoint);
            if (!Character.isLetterOrDigit(codePoint) && !Character.isWhitespace(codePoint) && codePoint > 127) {
                sb.append(str, i, i + charCount);
                i += charCount;
            } else {
                break;
            }
        }
        return sb.toString().trim();
    }

    private static boolean hasLeadingEmoji(String str) {
        return !extractLeadingEmoji(str).isEmpty();
    }

    public static void processViewHierarchy(View view, Context ctx) {
        if (view == null || ctx == null) return;

        if (view instanceof TextView) {
            TextView tv = (TextView) view;
            CharSequence cs = tv.getText();
            if (cs != null && cs.length() > 0) {
                String original = cs.toString();
                String processed = processDualString(ctx, original);
                if (!original.equals(processed)) {
                    tv.setText(processed);
                }
            }
            if (view instanceof EditText) {
                EditText et = (EditText) view;
                CharSequence hintCs = et.getHint();
                if (hintCs != null && hintCs.length() > 0) {
                    String originalHint = hintCs.toString();
                    String processedHint = processDualString(ctx, originalHint);
                    if (!originalHint.equals(processedHint)) {
                        et.setHint(processedHint);
                    }
                }
            }
        }

        if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++) {
                processViewHierarchy(vg.getChildAt(i), ctx);
            }
        }
    }

    public static String getLanguageDisplayName(String mode) {
        if (PreferencesManager.LANG_URDU.equals(mode)) {
            return "🇵🇰 اردو (Urdu Only)";
        } else if (PreferencesManager.LANG_ENGLISH.equals(mode)) {
            return "🇬🇧 English (English Only)";
        } else {
            return "🇵🇰🇬🇧 اردو + English (Mix)";
        }
    }

    public static void showLanguageSelectionDialog(Context ctx, OnLanguageSelectedListener listener) {
        if (ctx == null) return;
        PreferencesManager prefs = new PreferencesManager(ctx);
        String currentMode = prefs.getAppLanguage();

        String[] options = {
                "🇵🇰 اردو (Urdu Only)",
                "🇬🇧 English (English Only)",
                "🇵🇰🇬🇧 اردو + English (Mix)"
        };

        int selectedIndex = 2;
        if (PreferencesManager.LANG_URDU.equals(currentMode)) selectedIndex = 0;
        else if (PreferencesManager.LANG_ENGLISH.equals(currentMode)) selectedIndex = 1;

        new AlertDialog.Builder(ctx)
                .setTitle(processDualString(ctx, "🌐 Select App Language / زبان منتخب کریں"))
                .setSingleChoiceItems(options, selectedIndex, (dialog, which) -> {
                    String chosenMode = PreferencesManager.LANG_MIX;
                    if (which == 0) chosenMode = PreferencesManager.LANG_URDU;
                    else if (which == 1) chosenMode = PreferencesManager.LANG_ENGLISH;

                    prefs.setAppLanguage(chosenMode);
                    prefs.setHasSelectedLanguage(true);
                    dialog.dismiss();

                    if (listener != null) {
                        listener.onLanguageSelected(chosenMode);
                    }
                })
                .setNegativeButton(processDualString(ctx, "منسوخ کریں / Cancel"), null)
                .show();
    }
}