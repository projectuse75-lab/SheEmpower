package com.sheempower.app.models;

public class User {
    private String uid, name, email, phone, role, imageUrl, oneSignalPlayerId;

    public User() {}

    public User(String uid, String name, String email, String phone, String role) {
        this.uid = uid; this.name = name; this.email = email;
        this.phone = phone; this.role = role;
    }

    public String getUid()               { return uid; }
    public String getName()              { return name; }
    public String getEmail()             { return email; }
    public String getPhone()             { return phone; }
    public String getRole()              { return role; }
    public String getImageUrl()          { return imageUrl; }
    public String getOneSignalPlayerId() { return oneSignalPlayerId; }

    public void setUid(String v)               { uid = v; }
    public void setName(String v)              { name = v; }
    public void setEmail(String v)             { email = v; }
    public void setPhone(String v)             { phone = v; }
    public void setRole(String v)              { role = v; }
    public void setImageUrl(String v)          { imageUrl = v; }
    public void setOneSignalPlayerId(String v) { this.oneSignalPlayerId = v; }
}
