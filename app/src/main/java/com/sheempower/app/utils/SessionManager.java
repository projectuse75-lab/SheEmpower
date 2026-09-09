package com.sheempower.app.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private static final String PREF_NAME = "she_empower_session";
    private static final String KEY_UID   = "uid";
    private static final String KEY_ROLE  = "role";
    private static final String KEY_NAME  = "name";

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

    public String getUid()  { return prefs.getString(KEY_UID, null); }
    public String getRole() { return prefs.getString(KEY_ROLE, null); }
    public String getName() { return prefs.getString(KEY_NAME, ""); }

    public boolean isLoggedIn() { return getUid() != null; }
    public boolean isSeller()   { return "seller".equals(getRole()); }
    public boolean isBuyer()    { return "buyer".equals(getRole()); }
    public boolean isAdmin()    { return "admin".equals(getRole()); }

    public void logout() {
        prefs.edit().clear().apply();
    }
}