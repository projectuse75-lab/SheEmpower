package com.sheempower.app.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private static final String PREF_NAME = "she_empower_session";
    private static final String KEY_UID         = "uid";
    private static final String KEY_ROLE        = "role";
    private static final String KEY_NAME        = "name";
    private static final String KEY_EMAIL       = "email";
    private static final String KEY_PHONE       = "phone";
    private static final String KEY_CITY        = "city";
    private static final String KEY_FATHER_NAME = "father_name";
    private static final String KEY_CLASS       = "user_class";

    private final SharedPreferences prefs;

    public SessionManager(Context ctx) {
        prefs = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveUser(String uid, String role, String name) {
        prefs.edit()
                .putString(KEY_UID, uid)
                .putString(KEY_ROLE, role)
                .putString(KEY_NAME, name)
                .apply();
    }

    public String getUid() {
        return prefs.getString(KEY_UID, null);
    }

    public String getRole() {
        return prefs.getString(KEY_ROLE, null);
    }

    public String getName() {
        return prefs.getString(KEY_NAME, "");
    }

    public String getUserName() {
        return getName();
    }

    public void saveName(String name) {
        prefs.edit().putString(KEY_NAME, name).apply();
    }

    public String getFatherName() {
        return prefs.getString(KEY_FATHER_NAME, "");
    }

    public void saveFatherName(String fatherName) {
        prefs.edit().putString(KEY_FATHER_NAME, fatherName).apply();
    }

    public String getUserClass() {
        return prefs.getString(KEY_CLASS, "");
    }

    public void saveUserClass(String userClass) {
        prefs.edit().putString(KEY_CLASS, userClass).apply();
    }

    public String getEmail() {
        return prefs.getString(KEY_EMAIL, "");
    }

    public String getUserEmail() {
        return getEmail();
    }

    public void saveEmail(String email) {
        prefs.edit().putString(KEY_EMAIL, email).apply();
    }

    public String getPhone() {
        return prefs.getString(KEY_PHONE, "");
    }

    public String getUserPhone() {
        return getPhone();
    }

    public void savePhone(String phone) {
        prefs.edit().putString(KEY_PHONE, phone).apply();
    }

    public String getCity() {
        return prefs.getString(KEY_CITY, "");
    }

    public String getUserCity() {
        return getCity();
    }

    public void saveCity(String city) {
        prefs.edit().putString(KEY_CITY, city).apply();
    }

    public boolean isLoggedIn() {
        return getUid() != null;
    }

    public boolean isSeller() {
        return "seller".equals(getRole());
    }

    public boolean isBuyer() {
        return "buyer".equals(getRole());
    }

    public boolean isAdmin() {
        return "admin".equals(getRole());
    }

    public void saveRole(String role) {
        prefs.edit().putString(KEY_ROLE, role).apply();
    }

    // Account switch ke liye
    public void switchRole(String newRole) {
        prefs.edit().putString(KEY_ROLE, newRole).apply();
    }

    public void logout() {
        prefs.edit().clear().apply();
    }
}
