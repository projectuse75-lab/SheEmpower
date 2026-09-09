package com.sheempower.app.models;

public class Product {
    private String id, sellerId, name, description, imageUrl, category;
    private double price;

    public Product() {}

    public Product(String id, String sellerId, String name,
                   String description, double price, String imageUrl) {
        this.id = id; this.sellerId = sellerId; this.name = name;
        this.description = description; this.price = price;
        this.imageUrl = imageUrl;
    }

    public String getId()          { return id; }
    public String getSellerId()    { return sellerId; }
    public String getName()        { return name; }
    public String getDescription() { return description; }
    public double getPrice()       { return price; }
    public String getImageUrl()    { return imageUrl; }
    public String getCategory()    { return category; }

    public void setId(String id)          { this.id = id; }
    public void setSellerId(String s)     { this.sellerId = s; }
    public void setName(String n)         { this.name = n; }
    public void setDescription(String d)  { this.description = d; }
    public void setPrice(double p)        { this.price = p; }
    public void setImageUrl(String u)     { this.imageUrl = u; }
    public void setCategory(String c)     { this.category = c; }
}