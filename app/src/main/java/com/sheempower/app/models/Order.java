package com.sheempower.app.models;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Order {
    private String id, buyerId, buyerName, sellerId, sellerName, productId, productName;
    private String details, address, notes, status, deliveryMethod, paymentMethod, deliveryAddress, orderCategory;
    private String paymentStatus, paymentProofUrl, paymentProofName;
    private long paymentProofUploadedAt;
    private double amount;
    private long timestamp;

    public Order() {
        this.paymentMethod = "cod";
        this.paymentStatus = "pending";
    }

    public String getId()             { return id; }
    public String getBuyerId()        { return buyerId; }
    public String getBuyerName()      { return buyerName; }
    public String getSellerId()       { return sellerId; }
    public String getSellerName()     { return sellerName; }
    public String getProductId()      { return productId; }
    public String getProductName()    { return productName; }
    public String getDetails()        { return details; }
    public String getAddress()        { return address; }
    public String getNotes()          { return notes; }
    public String getStatus()         { return status; }
    public String getDeliveryMethod() { return deliveryMethod; }
    public String getPaymentMethod()  { return paymentMethod != null ? paymentMethod : "cod"; }
    public String getDeliveryAddress(){ return deliveryAddress != null ? deliveryAddress : address; }
    public String getOrderCategory()  { return orderCategory; }
    public String getPaymentStatus()  { return paymentStatus != null ? paymentStatus : "pending"; }
    public String getPaymentProofUrl(){ return paymentProofUrl; }
    public String getPaymentProofName(){ return paymentProofName; }
    public long getPaymentProofUploadedAt(){ return paymentProofUploadedAt; }
    public double getAmount()         { return amount; }
    public long getTimestamp()        { return timestamp; }

    public String getFormattedOrderDateTime() {
        if (timestamp <= 0) return "";
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy (EEEE) 'at' hh:mm a", Locale.ENGLISH);
            return sdf.format(new Date(timestamp));
        } catch (Exception e) {
            return "";
        }
    }

    public void setId(String v)             { id = v; }
    public void setBuyerId(String v)        { buyerId = v; }
    public void setBuyerName(String v)      { buyerName = v; }
    public void setSellerId(String v)       { sellerId = v; }
    public void setSellerName(String v)     { sellerName = v; }
    public void setProductId(String v)      { productId = v; }
    public void setProductName(String v)    { productName = v; }
    public void setDetails(String v)        { details = v; }
    public void setAddress(String v)        { address = v; }
    public void setNotes(String v)          { notes = v; }
    public void setStatus(String v)         { status = v; }
    public void setDeliveryMethod(String v) { deliveryMethod = v; }
    public void setPaymentMethod(String v)  { paymentMethod = v; }
    public void setDeliveryAddress(String v){ deliveryAddress = v; }
    public void setOrderCategory(String v)  { orderCategory = v; }
    public void setPaymentStatus(String v)  { paymentStatus = v; }
    public void setPaymentProofUrl(String v){ paymentProofUrl = v; }
    public void setPaymentProofName(String v){ paymentProofName = v; }
    public void setPaymentProofUploadedAt(long v){ paymentProofUploadedAt = v; }
    public void setAmount(double v)         { amount = v; }
    public void setTimestamp(long v)        { timestamp = v; }
}