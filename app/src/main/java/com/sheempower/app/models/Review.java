package com.sheempower.app.models;

public class Review {
    private String reviewId, orderId, sellerId, buyerId, buyerName, reviewText;
    private String productId, productName, category;
    private float rating;
    private long timestamp;

    public Review() {
        // Required for Firebase
    }

    public Review(String reviewId, String orderId, String sellerId,
                  String buyerId, String buyerName, float rating,
                  String reviewText, long timestamp) {
        this.reviewId = reviewId;
        this.orderId = orderId;
        this.sellerId = sellerId;
        this.buyerId = buyerId;
        this.buyerName = buyerName;
        this.rating = rating;
        this.reviewText = reviewText;
        this.timestamp = timestamp;
    }

    public Review(String reviewId, String orderId, String sellerId,
                  String buyerId, String buyerName, String productId,
                  String productName, String category, float rating,
                  String reviewText, long timestamp) {
        this.reviewId = reviewId;
        this.orderId = orderId;
        this.sellerId = sellerId;
        this.buyerId = buyerId;
        this.buyerName = buyerName;
        this.productId = productId;
        this.productName = productName;
        this.category = category;
        this.rating = rating;
        this.reviewText = reviewText;
        this.timestamp = timestamp;
    }

    public String getReviewId() { return reviewId; }
    public String getOrderId() { return orderId; }
    public String getSellerId() { return sellerId; }
    public String getBuyerId() { return buyerId; }
    public String getBuyerName() { return buyerName; }
    public String getProductId() { return productId; }
    public String getProductName() { return productName; }
    public String getCategory() { return category; }
    public String getReviewText() { return reviewText; }
    public float getRating() { return rating; }
    public long getTimestamp() { return timestamp; }

    public void setReviewId(String v) { reviewId = v; }
    public void setOrderId(String v) { orderId = v; }
    public void setSellerId(String v) { sellerId = v; }
    public void setBuyerId(String v) { buyerId = v; }
    public void setBuyerName(String v) { buyerName = v; }
    public void setProductId(String v) { productId = v; }
    public void setProductName(String v) { productName = v; }
    public void setCategory(String v) { category = v; }
    public void setReviewText(String v) { reviewText = v; }
    public void setRating(float v) { rating = v; }
    public void setTimestamp(long v) { timestamp = v; }
}