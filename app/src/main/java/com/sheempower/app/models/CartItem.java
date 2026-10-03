package com.sheempower.app.models;

public class CartItem {
    private String cartItemId, productId, productName, productImageUrl, category;
    private String sellerId, sellerName;
    private double price;
    private int quantity;
    private long timestamp;

    public CartItem() {
        // Required for Firebase
    }

    public CartItem(String cartItemId, String productId, String productName,
                    String productImageUrl, String category, String sellerId,
                    String sellerName, double price, int quantity) {
        this.cartItemId = cartItemId;
        this.productId = productId;
        this.productName = productName;
        this.productImageUrl = productImageUrl;
        this.category = category;
        this.sellerId = sellerId;
        this.sellerName = sellerName;
        this.price = price;
        this.quantity = quantity;
        this.timestamp = System.currentTimeMillis();
    }

    private boolean selected = true;

    public String getCartItemId() { return cartItemId; }
    public String getProductId() { return productId; }
    public String getProductName() { return productName; }
    public String getProductImageUrl() { return productImageUrl; }
    public String getCategory() { return category; }
    public String getSellerId() { return sellerId; }
    public String getSellerName() { return sellerName; }
    public double getPrice() { return price; }
    public int getQuantity() { return quantity; }
    public long getTimestamp() { return timestamp; }
    public boolean isSelected() { return selected; }

    public void setCartItemId(String v) { cartItemId = v; }
    public void setProductId(String v) { productId = v; }
    public void setProductName(String v) { productName = v; }
    public void setProductImageUrl(String v) { productImageUrl = v; }
    public void setCategory(String v) { category = v; }
    public void setSellerId(String v) { sellerId = v; }
    public void setSellerName(String v) { sellerName = v; }
    public void setPrice(double v) { price = v; }
    public void setQuantity(int v) { quantity = v; }
    public void setTimestamp(long v) { timestamp = v; }
    public void setSelected(boolean v) { selected = v; }
}