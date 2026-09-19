package com.sheempower.app.models;

public class Buyer {

    private String uid;
    private String name;
    private String email;
    private String phone;
    private String imageUrl;
    private boolean profileCreated;

    public Buyer() {
        // Firebase ke liye empty constructor
    }

    public Buyer(String uid,
                 String name,
                 String email,
                 String phone,
                 String imageUrl,
                 boolean profileCreated) {

        this.uid = uid;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.imageUrl = imageUrl;
        this.profileCreated = profileCreated;
    }

    public String getUid() {
        return uid;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public boolean isProfileCreated() {
        return profileCreated;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setProfileCreated(boolean profileCreated) {
        this.profileCreated = profileCreated;
    }
}
