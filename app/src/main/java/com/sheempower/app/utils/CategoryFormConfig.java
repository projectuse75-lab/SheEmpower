package com.sheempower.app.utils;

import com.sheempower.app.models.OrderField;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CategoryFormConfig {

    private static final Map<String, List<OrderField>> FORMS = new HashMap<>();

    static {
        // ============================================================
        // 1. Cooking / کھانا پکانا
        // ============================================================
        List<OrderField> cooking = new ArrayList<>();
        cooking.add(new OrderField(
                "کتنے لوگوں کے لیے کھانا چاہیے؟",
                "Number of People",
                "مثال: 20 لوگ",
                "number"
        ));
        cooking.add(new OrderField(
                "کس فنکشن کے لیے؟",
                "Occasion",
                "spinner",
                new String[]{"عام دن", "مہندی", "بارات", "ولیمہ", "دعوت", "دیگر"}
        ));
        cooking.add(new OrderField(
                "مصالحہ کتنا تیز چاہیے؟",
                "Spice Level",
                "spinner",
                new String[]{"کم تیز", "درمیانہ", "زیادہ تیز"}
        ));
        cooking.add(new OrderField(
                "کون سا کھانا چاہیے؟",
                "Dish Details",
                "مثال: بریانی، قورمہ، حلوہ",
                "text"
        ));
        cooking.add(new OrderField(
                "کھانا کب چاہیے؟",
                "Time Needed",
                "مثال: کل دوپہر 1 بجے",
                "text"
        ));
        FORMS.put("Cooking", cooking);

        // ============================================================
        // 2. Stitching / سلائی
        // ============================================================
        List<OrderField> stitching = new ArrayList<>();
        stitching.add(new OrderField(
                "کس کے لیے سلائی چاہیے؟",
                "For Whom",
                "spinner",
                new String[]{"بچے کے لیے", "خاتون کے لیے", "بزرگ کے لیے", "مرد کے لیے"}
        ));
        stitching.add(new OrderField(
                "کپڑے کی قسم",
                "Garment Type",
                "مثال: کرتی، شلوار قمیض، فراک، لہنگا",
                "text"
        ));
        stitching.add(new OrderField(
                "ناپ (سائز)",
                "Measurements",
                "مثال: چھاتی 36, لمبائی 40, بازو 18",
                "text"
        ));
        stitching.add(new OrderField(
                "کپڑا کون دے گا؟",
                "Fabric Provided By",
                "spinner",
                new String[]{"میں کپڑا دوں گی", "سلائی والی خود لائے"}
        ));
        stitching.add(new OrderField(
                "کڑھائی چاہیے یا نہیں؟",
                "Embroidery Needed",
                "spinner",
                new String[]{"کڑھائی نہیں چاہیے", "ہاں، کڑھائی چاہیے"}
        ));
        stitching.add(new OrderField(
                "کڑھائی کا دھاگہ / انداز",
                "Embroidery Style",
                "spinner",
                new String[]{"ریشم کا دھاگہ", "دبکا ورک", "گوٹا ورک", "آری ورک", "سادہ سلائی"}
        ));
        stitching.add(new OrderField(
                "ڈیزائن کی تفصیل",
                "Design Details",
                "مثال: گلے کا ڈیزائن، آستین کی لمبائی",
                "text"
        ));
        FORMS.put("Stitching", stitching);

        // ============================================================
        // 3. Mehndi / مہندی
        // ============================================================
        List<OrderField> mehndi = new ArrayList<>();
        mehndi.add(new OrderField(
                "کس فنکشن کے لیے مہندی چاہیے؟",
                "Function Type",
                "spinner",
                new String[]{"مہندی کی رسم", "بارات", "ولیمہ", "نکاح", "ڈھولکی", "عام فنکشن"}
        ));
        mehndi.add(new OrderField(
                "کتنے لوگوں کی مہندی لگے گی؟",
                "Number of People",
                "مثال: دلہن اور 15 مہمان",
                "number"
        ));
        mehndi.add(new OrderField(
                "مہندی کہاں لگے گی؟",
                "Location",
                "spinner",
                new String[]{"میرے گھر پر", "مہندی لگانے والی کے پاس"}
        ));
        mehndi.add(new OrderField(
                "ڈیزائن کا انداز",
                "Design Style",
                "spinner",
                new String[]{"عربی ڈیزائن", "روایتی پاکستانی", "راجستھانی", "ہلکا سادہ ڈیزائن"}
        ));
        mehndi.add(new OrderField(
                "بجٹ کا اندازہ (روپے)",
                "Budget Estimate",
                "مثال: 3000 سے 5000",
                "text"
        ));
        FORMS.put("Mehndi", mehndi);

        // ============================================================
        // 4. Makeup / میک اپ
        // ============================================================
        List<OrderField> makeup = new ArrayList<>();
        makeup.add(new OrderField(
                "کس فنکشن کے لیے میک اپ چاہیے؟",
                "Function Type",
                "spinner",
                new String[]{"بارات", "ولیمہ", "مہندی", "نکاح", "پارٹی", "منگنی"}
        ));
        makeup.add(new OrderField(
                "کتنے لوگوں کا میک اپ چاہیے؟",
                "Number of People",
                "مثال: دلہن اور 3 مہمان",
                "number"
        ));
        makeup.add(new OrderField(
                "میک اپ کا انداز",
                "Makeup Style",
                "spinner",
                new String[]{"ہلکا / نیچرل میک اپ", "بھاری / HD میک اپ", "پارٹی میک اپ"}
        ));
        makeup.add(new OrderField(
                "میک اپ کہاں ہوگا؟",
                "Location",
                "spinner",
                new String[]{"میرے گھر پر", "میک اپ آرٹسٹ کے پاس"}
        ));
        makeup.add(new OrderField(
                "اضافی ہدایات",
                "Special Instructions",
                "مثال: برائیڈل میک اپ ہو، ہلکا میک اپ",
                "text"
        ));
        FORMS.put("Makeup", makeup);

        // ============================================================
        // 5. Krhae / کڑھائی (Embroidery)
        // ============================================================
        List<OrderField> krhae = new ArrayList<>();
        krhae.add(new OrderField(
                "کڑھائی کی قسم",
                "Embroidery Type",
                "spinner",
                new String[]{"سادہ کڑھائی", "پھول کاری", "موتی کا کام", "سندھی کڑھائی", "دیگر"}
        ));
        krhae.add(new OrderField(
                "کپڑے کی قسم",
                "Fabric Type",
                "spinner",
                new String[]{"سوتی", "ریشم", "شِفون", "کھادی", "دیگر"}
        ));
        krhae.add(new OrderField(
                "ڈیزائن کی تفصیل",
                "Design Description",
                "مثال: کناروں پر کڑھائی، درمیان میں پھول",
                "text"
        ));
        krhae.add(new OrderField(
                "دھاگے کا رنگ",
                "Thread Color",
                "مثال: سنہری، چاندی، گلابی، نیلا",
                "text"
        ));
        krhae.add(new OrderField(
                "کپڑا کون دے گا؟",
                "Fabric Provided By",
                "spinner",
                new String[]{"میں کپڑا دوں گی", "کڑھائی والی خود لائے"}
        ));
        FORMS.put("Krhae", krhae);
    }

    public static List<OrderField> getFieldsForCategory(String category) {
        if (category == null) return new ArrayList<>();
        List<OrderField> fields = FORMS.get(category);
        return fields != null ? fields : new ArrayList<>();
    }
}