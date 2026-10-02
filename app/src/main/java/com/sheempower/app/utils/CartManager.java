package com.sheempower.app.utils;

import android.content.Context;
import android.content.SharedPreferences;
import com.sheempower.app.models.CartItem;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class CartManager {
    private static final String PREF_NAME = "she_empower_cart_prefs";
    private static final String KEY_CART_ITEMS = "cart_items_json";
    private static CartManager instance;
    private final SharedPreferences prefs;

    private CartManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized CartManager getInstance(Context context) {
        if (instance == null) {
            instance = new CartManager(context);
        }
        return instance;
    }

    public synchronized List<CartItem> getCartItems() {
        List<CartItem> list = new ArrayList<>();
        String json = prefs.getString(KEY_CART_ITEMS, "[]");
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                CartItem item = new CartItem();
                item.setCartItemId(obj.optString("cartItemId"));
                item.setProductId(obj.optString("productId"));
                item.setProductName(obj.optString("productName"));
                item.setProductImageUrl(obj.optString("productImageUrl"));
                item.setCategory(obj.optString("category"));
                item.setSellerId(obj.optString("sellerId"));
                item.setSellerName(obj.optString("sellerName"));
                item.setPrice(obj.optDouble("price", 0));
                item.setQuantity(obj.optInt("quantity", 1));
                item.setTimestamp(obj.optLong("timestamp", System.currentTimeMillis()));
                list.add(item);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public synchronized void saveCartItems(List<CartItem> items) {
        try {
            JSONArray array = new JSONArray();
            for (CartItem item : items) {
                JSONObject obj = new JSONObject();
                obj.put("cartItemId", item.getCartItemId());
                obj.put("productId", item.getProductId());
                obj.put("productName", item.getProductName());
                obj.put("productImageUrl", item.getProductImageUrl());
                obj.put("category", item.getCategory());
                obj.put("sellerId", item.getSellerId());
                obj.put("sellerName", item.getSellerName());
                obj.put("price", item.getPrice());
                obj.put("quantity", item.getQuantity());
                obj.put("timestamp", item.getTimestamp());
                array.put(obj);
            }
            prefs.edit().putString(KEY_CART_ITEMS, array.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized void addToCart(CartItem newItem) {
        List<CartItem> items = getCartItems();
        boolean existingFound = false;
        for (CartItem item : items) {
            if (item.getProductId() != null && item.getProductId().equals(newItem.getProductId())) {
                item.setQuantity(item.getQuantity() + newItem.getQuantity());
                existingFound = true;
                break;
            }
        }
        if (!existingFound) {
            items.add(newItem);
        }
        saveCartItems(items);
    }

    public synchronized void updateQuantity(String productId, int newQty) {
        List<CartItem> items = getCartItems();
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getProductId() != null && items.get(i).getProductId().equals(productId)) {
                if (newQty <= 0) {
                    items.remove(i);
                } else {
                    items.get(i).setQuantity(newQty);
                }
                break;
            }
        }
        saveCartItems(items);
    }

    public synchronized void removeFromCart(String productId) {
        List<CartItem> items = getCartItems();
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getProductId() != null && items.get(i).getProductId().equals(productId)) {
                items.remove(i);
                break;
            }
        }
        saveCartItems(items);
    }

    public synchronized void clearCart() {
        prefs.edit().remove(KEY_CART_ITEMS).apply();
    }

    public synchronized int getCartCount() {
        List<CartItem> items = getCartItems();
        int count = 0;
        for (CartItem item : items) {
            count += item.getQuantity();
        }
        return count;
    }

    public synchronized double getTotalPrice() {
        List<CartItem> items = getCartItems();
        double total = 0;
        for (CartItem item : items) {
            total += (item.getPrice() * item.getQuantity());
        }
        return total;
    }
}