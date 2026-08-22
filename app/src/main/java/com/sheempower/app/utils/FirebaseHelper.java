package com.sheempower.app.utils;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;

/**
 * Centralized Realtime Database reference getters.
 * In-app chat node (chats) is intentionally removed — WhatsApp is used instead.
 */
public class FirebaseHelper {

    private static final FirebaseDatabase database = FirebaseDatabase.getInstance();

    // -------- ROOT REFERENCES --------
    public static DatabaseReference getUsersRef() {
        return database.getReference(Constants.NODE_USERS);
    }

    public static DatabaseReference getSellersRef() {
        return database.getReference(Constants.NODE_SELLERS);
    }

    public static DatabaseReference getOrdersRef() {
        return database.getReference(Constants.NODE_ORDERS);
    }

    public static DatabaseReference getProductsRef() {
        return database.getReference(Constants.NODE_PRODUCTS);
    }

    // -------- SPECIFIC CHILD REFERENCES --------
    public static DatabaseReference getUserRef(String uid) {
        return getUsersRef().child(uid);
    }

    public static DatabaseReference getSellerRef(String sellerUid) {
        return getSellersRef().child(sellerUid);
    }

    public static DatabaseReference getOrderRef(String orderId) {
        return getOrdersRef().child(orderId);
    }

    public static DatabaseReference getProductRef(String productId) {
        return getProductsRef().child(productId);
    }

    // -------- CONVENIENCE QUERY HELPERS (Optional but Recommended) --------

    /** Returns query to get all orders placed by a specific buyer */
    public static Query getOrdersByBuyer(String buyerId) {
        return getOrdersRef().orderByChild("buyerId").equalTo(buyerId);
    }

    /** Returns query to get all orders received by a specific seller */
    public static Query getOrdersBySeller(String sellerId) {
        return getOrdersRef().orderByChild("sellerId").equalTo(sellerId);
    }

    /** Returns query to get all products listed by a specific seller */
    public static Query getProductsBySeller(String sellerId) {
        return getProductsRef().orderByChild("sellerId").equalTo(sellerId);
    }

    /** Returns query to get all verified/approved sellers */
    public static Query getApprovedSellers() {
        return getSellersRef().orderByChild("verificationStatus").equalTo("approved");
    }

    /** Returns query to get all top-rated sellers */
    public static Query getTopRatedSellers() {
        return getSellersRef().orderByChild("isTopRated").equalTo(true);
    }
}