package com.sheempower.app.models;

public class Seller {
    private String uid, businessName, ownerName, email, phone;
    private String category, bio, city, cnicUrl, voiceUrl;
    private String verificationStatus, whatsappNumber, imageUrl;
    private boolean isTopRated;
    private double rating;
    private long ratingCount;

    public Seller() {}

    public String getUid()                { return uid; }
    public String getBusinessName()       { return businessName; }
    public String getOwnerName()          { return ownerName; }
    public String getEmail()              { return email; }
    public String getPhone()              { return phone; }
    public String getCategory()           { return category; }
    public String getBio()                { return bio; }
    public String getCity()               { return city; }
    public String getCnicUrl()            { return cnicUrl; }
    public String getVoiceUrl()           { return voiceUrl; }
    public String getVerificationStatus() { return verificationStatus; }
    public String getWhatsappNumber()     { return whatsappNumber; }
    public String getImageUrl()           { return imageUrl; }
    public boolean isTopRated()           { return isTopRated; }
    public double getRating()             { return rating; }
    public long getRatingCount()          { return ratingCount; }

    public void setUid(String v)                { uid = v; }
    public void setBusinessName(String v)       { businessName = v; }
    public void setOwnerName(String v)          { ownerName = v; }
    public void setEmail(String v)              { email = v; }
    public void setPhone(String v)              { phone = v; }
    public void setCategory(String v)           { category = v; }
    public void setBio(String v)                { bio = v; }
    public void setCity(String v)               { city = v; }
    public void setCnicUrl(String v)            { cnicUrl = v; }
    public void setVoiceUrl(String v)           { voiceUrl = v; }
    public void setVerificationStatus(String v) { verificationStatus = v; }
    public void setWhatsappNumber(String v)     { whatsappNumber = v; }
    public void setImageUrl(String v)           { imageUrl = v; }
    public void setTopRated(boolean v)          { isTopRated = v; }
    public void setRating(double v)             { rating = v; }
    public void setRatingCount(long v)          { ratingCount = v; }
}