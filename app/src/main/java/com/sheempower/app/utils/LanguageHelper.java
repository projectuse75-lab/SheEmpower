package com.sheempower.app.utils;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
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
        String cleanUrdu = urdu != null ? urdu.trim() : "";
        String cleanEnglish = english != null ? english.trim() : "";

        if (!cleanEnglish.isEmpty() && !cleanUrdu.isEmpty()) {
            if (cleanEnglish.equalsIgnoreCase(cleanUrdu)) return cleanEnglish;
            return cleanEnglish + " / " + cleanUrdu;
        } else if (!cleanEnglish.isEmpty()) {
            return cleanEnglish;
        } else if (!cleanUrdu.isEmpty()) {
            String translated = URDU_TO_ENGLISH_MAP.get(cleanUrdu);
            if (translated != null && !translated.equalsIgnoreCase(cleanUrdu)) {
                return translated + " / " + cleanUrdu;
            }
            return cleanUrdu;
        }
        return "";
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
            return combinedText;
        }

        String[] parts = combinedText.split(delimiter, 2);
        String part1 = parts[0].trim();
        String part2 = parts.length > 1 ? parts[1].trim() : "";

        String english = "";
        String urdu = "";

        if (containsEnglish(part1) && containsUrdu(part2)) {
            english = part1;
            urdu = part2;
        } else if (containsUrdu(part1) && containsEnglish(part2)) {
            urdu = part1;
            english = part2;
        } else {
            return combinedText;
        }

        if (delimiter.contains("\n")) {
            return english + "\n" + urdu;
        } else {
            return english + " / " + urdu;
        }
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
}