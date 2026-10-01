package com.sheempower.app.models;

public class Notification {
    private String id;
    private String title;
    private String message;
    private String type;
    private String orderId;
    private String sellerId;
    private String buyerId;
    private long timestamp;
    private boolean read;

    public Notification() {
        // Required for Firebase
    }

    public Notification(String id, String title, String message, String type, String orderId, String sellerId, String buyerId, long timestamp, boolean read) {
        this.id = id;
        this.title = title;
        this.message = message;
        this.type = type;
        this.orderId = orderId;
        this.sellerId = sellerId;
        this.buyerId = buyerId;
        this.timestamp = timestamp;
        this.read = read;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }

    public String getBuyerId() { return buyerId; }
    public void setBuyerId(String buyerId) { this.buyerId = buyerId; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
}
