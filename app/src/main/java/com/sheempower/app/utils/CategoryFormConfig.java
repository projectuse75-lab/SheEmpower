package com.sheempower.app.utils;

import com.sheempower.app.models.OrderField;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CategoryFormConfig {

    private static final Map<String, List<OrderField>> FORMS = new HashMap<>();

    static {
        // ---------------- Cooking ----------------
        List<OrderField> cooking = new ArrayList<>();
        cooking.add(new OrderField("مصالحہ کتنا تیز چاہیے؟", "Taste Style", "spinner",
                new String[]{"تیز مصالحہ", "ہلکا مصالحہ", "تندوری انداز", "دیسی انداز"}));
        cooking.add(new OrderField("کس فنکشن کے لیے؟", "Occasion", "spinner",
                new String[]{"عام دن", "مہندی", "بارات", "ولیمہ", "دعوت", "دیگر"}));
        cooking.add(new OrderField("کس دن چاہیے؟", "Which Day", "مثال: 15 اگست", "text"));
        cooking.add(new OrderField("کس وقت تک چاہیے؟", "Time Needed By", "مثال: شام 6 بجے تک", "text"));
        FORMS.put("Cooking", cooking);

        // ---------------- Stitching ----------------
        List<OrderField> stitching = new ArrayList<>();
        stitching.add(new OrderField("کس کے لیے سلائی چاہیے؟", "For Whom", "spinner",
                new String[]{"بچے کے لیے", "خاتون کے لیے", "بزرگ کے لیے", "مرد کے لیے"}));
        stitching.add(new OrderField("کپڑے کی قسم", "Garment Type", "مثال: کرتی، شلوار قمیض، فراک، لہنگا", "text"));
        stitching.add(new OrderField("ناپ (سائز/لمبائی)", "Size / Measurements", "مثال: چھاتی 36, لمبائی 40, بازو 18", "text"));
        stitching.add(new OrderField("کپڑا آپ دیں گی یا سلائی والی؟", "Fabric Provided By", "spinner",
                new String[]{"میں کپڑا دوں گی", "سلائی والی خود لائے"}));
        stitching.add(new OrderField("کڑھائی چاہیے یا نہیں؟", "Embroidery Needed", "spinner",
                new String[]{"کڑھائی نہیں چاہیے", "ہاں، کڑھائی چاہیے"}));
        stitching.add(new OrderField("دھاگے / کڑھائی کا انداز", "Thread / Style", "spinner",
                new String[]{"ریشم کا دھاگہ", "دبکا ورک", "گوٹا ورک", "آری ورک", "سادہ سلائی"}));
        stitching.add(new OrderField("ڈیزائن کی تفصیل", "Design Details", "مثال: گلے کا ڈیزائن، آستین کی لمبائی", "text"));
        stitching.add(new OrderField("کب تک چاہیے؟", "Needed By", "مثال: 10 دن میں", "text"));
        FORMS.put("Stitching", stitching);

        // ---------------- Mehndi ----------------
        List<OrderField> mehndi = new ArrayList<>();
        mehndi.add(new OrderField("کس فنکشن کے لیے مہندی چاہیے؟", "Function Type", "spinner",
                new String[]{"مہندی کی رسم", "بارات", "ولیمہ", "نکاح", "ڈھولکی", "عام فنکشن"}));
        mehndi.add(new OrderField("ڈیزائن کا انداز", "Design Style", "spinner",
                new String[]{"عربی ڈیزائن", "روایتی پاکستانی", "راجستھانی", "ہلکا سادہ ڈیزائن"}));
        mehndi.add(new OrderField("کس دن چاہیے؟", "Which Day", "مثال: 20 اگست", "text"));
        mehndi.add(new OrderField("کس وقت چاہیے؟", "Time Needed", "مثال: دوپہر 2 بجے", "text"));
        FORMS.put("Mehndi", mehndi);

        // ---------------- Makeup ----------------
        List<OrderField> makeup = new ArrayList<>();
        makeup.add(new OrderField("کس فنکشن کے لیے میک اپ چاہیے؟", "Function Type", "spinner",
                new String[]{"بارات", "ولیمہ", "مہندی", "نکاح", "پارٹی", "منگنی"}));
        makeup.add(new OrderField("میک اپ کا انداز", "Makeup Style", "spinner",
                new String[]{"ہلکا / نیچرل میک اپ", "بھاری / HD میک اپ", "پارٹی میک اپ"}));
        makeup.add(new OrderField("اضافی خدمات (اختیاری)", "Additional Services", "multi",
                new String[]{"ویکسنگ / Waxing", "تھریڈنگ / Threading", "آئی برو / Eyebrow Plucking",
                        "فیشل / Facial", "نیل آرٹ / Nail Art", "ہیئر اسٹائل / Hair Styling"}));
        makeup.add(new OrderField("کس دن چاہیے؟", "Which Day", "مثال: 22 اگست", "text"));
        makeup.add(new OrderField("کس وقت چاہیے؟", "Time Needed", "مثال: صبح 9 بجے", "text"));
        FORMS.put("Makeup", makeup);

        // ---------------- Embroidery ----------------
        List<OrderField> embroidery = new ArrayList<>();
        embroidery.add(new OrderField("کس چیز پر کڑھائی کرنی ہے؟", "What Garment/Item to Embroider", "مثال: دوپٹہ، کرتا، وال آرٹ، چادر", "text"));
        embroidery.add(new OrderField("دھاگے کی قسم", "Thread Type", "spinner",
                new String[]{"ریشم کا دھاگہ", "دبکا ورک", "گوٹا ورک", "آری ورک"}));
        embroidery.add(new OrderField("ڈیزائن کی تفصیل", "Design Details", "مثال: پھولوں کا ڈیزائن", "text"));
        embroidery.add(new OrderField("کب تک چاہیے؟", "Needed By", "مثال: 15 دن میں", "text"));
        FORMS.put("Embroidery", embroidery);
    }

    public static List<OrderField> getFieldsForCategory(String category) {
        if (category == null) return new ArrayList<>();
        List<OrderField> fields = FORMS.get(category);
        return fields != null ? fields : new ArrayList<>();
    }
}