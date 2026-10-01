package com.sheempower.app.models;

import java.util.ArrayList;
import java.util.List;

public class Seller {

    private String uid, businessName, ownerName, email, phone;
    private String category, bio, city, cnicUrl, voiceUrl;
    private String verificationStatus, whatsappNumber, imageUrl;
    private String capacityDescription, shopAddress, oneSignalPlayerId;

    // Multiple categories support
    private List<String> categories;

    private boolean topRated;
    private boolean canDeliver;
    private double rating;
    private long ratingCount;

    public Seller() {
        // Required for Firebase
    }

    public String getUid() {
        return uid;
    }

    public String getBusinessName() {
        return businessName;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getCategory() {
        return category;
    }

    public String getBio() {
        return bio;
    }

    public String getCity() {
        return city;
    }

    public String getCnicUrl() {
        return cnicUrl;
    }

    public String getVoiceUrl() {
        return voiceUrl;
    }

    public String getVerificationStatus() {
        return verificationStatus;
    }

    public String getWhatsappNumber() {
        return whatsappNumber;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getCapacityDescription() {
        return capacityDescription;
    }

    public String getShopAddress() {
        return shopAddress;
    }

    public String getOneSignalPlayerId() {
        return oneSignalPlayerId;
    }

    public List<String> getCategories() {
        if (categories == null) {
            categories = new ArrayList<>();
        }
        return categories;
    }

    public boolean isTopRated() {
        return topRated;
    }

    public boolean isCanDeliver() {
        return canDeliver;
    }

    public double getRating() {
        return rating;
    }

    public long getRatingCount() {
        return ratingCount;
    }

    public void setUid(String v) {
        uid = v;
    }

    public void setBusinessName(String v) {
        businessName = v;
    }

    public void setOwnerName(String v) {
        ownerName = v;
    }

    public void setEmail(String v) {
        email = v;
    }

    public void setPhone(String v) {
        phone = v;
    }

    public void setCategory(String v) {
        category = v;
    }

    public void setBio(String v) {
        bio = v;
    }

    public void setCity(String v) {
        city = v;
    }

    public void setCnicUrl(String v) {
        cnicUrl = v;
    }

    public void setVoiceUrl(String v) {
        voiceUrl = v;
    }

    public void setVerificationStatus(String v) {
        verificationStatus = v;
    }

    public void setWhatsappNumber(String v) {
        whatsappNumber = v;
    }

    public void setImageUrl(String v) {
        imageUrl = v;
    }

    public void setCapacityDescription(String v) {
        capacityDescription = v;
    }

    public void setShopAddress(String v) {
        shopAddress = v;
    }

    public void setOneSignalPlayerId(String v) {
        this.oneSignalPlayerId = v;
    }

    public void setCategories(List<String> v) {
        categories = v;
    }

    public void setTopRated(boolean v) {
        topRated = v;
    }

    public void setCanDeliver(boolean v) {
        canDeliver = v;
    }

    public void setRating(double v) {
        rating = v;
    }

    public void setRatingCount(long v) {
        ratingCount = v;
    }
}
