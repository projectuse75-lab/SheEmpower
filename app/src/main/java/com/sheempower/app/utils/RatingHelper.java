package com.sheempower.app.utils;

import com.google.firebase.database.DataSnapshot;
import com.sheempower.app.models.Review;

public class RatingHelper {

    public static float parseRating(DataSnapshot child) {
        if (child == null) return 0f;
        try {
            Review r = child.getValue(Review.class);
            if (r != null && r.getRating() > 0) {
                return r.getRating();
            }
        } catch (Exception ignored) {}

        try {
            Object val = child.child("rating").getValue();
            if (val instanceof Number) {
                return ((Number) val).floatValue();
            } else if (val instanceof String) {
                return Float.parseFloat(((String) val).trim());
            }
        } catch (Exception ignored) {}

        return 0f;
    }

    public static Review parseReview(DataSnapshot child) {
        if (child == null || !child.exists()) return null;
        try {
            Review r = child.getValue(Review.class);
            if (r != null) {
                if (r.getReviewId() == null || r.getReviewId().isEmpty()) {
                    r.setReviewId(child.getKey());
                }
                if (r.getRating() <= 0) {
                    r.setRating(parseRating(child));
                }
                return r;
            }
        } catch (Exception ignored) {}

        try {
            Review r = new Review();
            r.setReviewId(child.getKey());
            r.setOrderId(getString(child, "orderId"));
            r.setSellerId(getString(child, "sellerId"));
            r.setBuyerId(getString(child, "buyerId"));
            r.setBuyerName(getString(child, "buyerName"));
            r.setProductId(getString(child, "productId"));
            r.setProductName(getString(child, "productName"));
            r.setCategory(getString(child, "category"));
            r.setReviewText(getString(child, "reviewText"));
            r.setRating(parseRating(child));

            Object ts = child.child("timestamp").getValue();
            if (ts instanceof Number) {
                r.setTimestamp(((Number) ts).longValue());
            }
            return r;
        } catch (Exception ignored) {}

        return null;
    }

    private static String getString(DataSnapshot parent, String key) {
        if (parent == null || !parent.hasChild(key)) return "";
        Object val = parent.child(key).getValue();
        return val != null ? val.toString().trim() : "";
    }
}