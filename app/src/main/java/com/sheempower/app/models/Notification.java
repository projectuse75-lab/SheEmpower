package com.sheempower.app.models;

public class Notification {
    private String id, title, message, type, orderId, sellerId, buyerId;
    private long timestamp;
    private boolean read;

    public Notification() {}

    public Notification(String id, String title, String message, String type,
                        String orderId, String sellerId, String buyerId) {
        this.id = id;
        this.title = title;
        this.message = message;
        this.type = type;
        this.orderId = orderId;
        this.sellerId = sellerId;
        this.buyerId = buyerId;
        this.timestamp = System.currentTimeMillis();
        this.read = false;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public String getType() { return type; }
    public String getOrderId() { return orderId; }
    public String getSellerId() { return sellerId; }
    public String getBuyerId() { return buyerId; }
    public long getTimestamp() { return timestamp; }
    public boolean isRead() { return read; }

    public void setId(String v) { id = v; }
    public void setTitle(String v) { title = v; }
    public void setMessage(String v) { message = v; }
    public void setType(String v) { type = v; }
    public void setOrderId(String v) { orderId = v; }
    public void setSellerId(String v) { sellerId = v; }
    public void setBuyerId(String v) { buyerId = v; }
    public void setTimestamp(long v) { timestamp = v; }
    public void setRead(boolean v) { read = v; }
}