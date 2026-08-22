package com.sheempower.app.models;

public class Order {
    private String id, buyerId, sellerId, productId, productName;
    private String details, address, deliveryDate, notes, status;
    private String receiptUrl;  // <-- ADD THIS (required for payment verification)
    private double amount;
    private long timestamp;

    public Order() {}

    public String getId()           { return id; }
    public String getBuyerId()      { return buyerId; }
    public String getSellerId()     { return sellerId; }
    public String getProductId()    { return productId; }
    public String getProductName()  { return productName; }
    public String getDetails()      { return details; }
    public String getAddress()      { return address; }
    public String getDeliveryDate() { return deliveryDate; }
    public String getNotes()        { return notes; }
    public String getStatus()       { return status; }
    public String getReceiptUrl()   { return receiptUrl; } // <-- ADD THIS
    public double getAmount()       { return amount; }
    public long getTimestamp()      { return timestamp; }

    public void setId(String v)           { id = v; }
    public void setBuyerId(String v)      { buyerId = v; }
    public void setSellerId(String v)     { sellerId = v; }
    public void setProductId(String v)    { productId = v; }
    public void setProductName(String v)  { productName = v; }
    public void setDetails(String v)      { details = v; }
    public void setAddress(String v)      { address = v; }
    public void setDeliveryDate(String v) { deliveryDate = v; }
    public void setNotes(String v)        { notes = v; }
    public void setStatus(String v)       { status = v; }
    public void setReceiptUrl(String v)   { receiptUrl = v; } // <-- ADD THIS
    public void setAmount(double v)       { amount = v; }
    public void setTimestamp(long v)      { timestamp = v; }
}