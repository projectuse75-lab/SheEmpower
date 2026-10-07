package com.sheempower.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.sheempower.app.R;
import com.sheempower.app.adapters.CartAdapter;
import com.sheempower.app.models.CartItem;
import com.sheempower.app.utils.BottomNavHelper;
import com.sheempower.app.utils.CartManager;
import com.sheempower.app.utils.Constants;
import com.sheempower.app.utils.PreferencesManager;
import com.sheempower.app.utils.SessionManager;
import com.sheempower.app.utils.VoiceGuide;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CartActivity extends AppCompatActivity {

    private RecyclerView rvCartItems;
    private TextView tvCartEmpty, tvCartTotalAmount;
    private LinearLayout layoutCartSummary;
    private Button btnProceedToOrder;
    private CartAdapter cartAdapter;
    private final List<CartItem> cartItemList = new ArrayList<>();
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        session = new SessionManager(this);

        if (!session.isLoggedIn()) {
            Toast.makeText(this, "پہلے لاگ ان کریں / Please Login First", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        rvCartItems = findViewById(R.id.rvCartItems);
        tvCartEmpty = findViewById(R.id.tvCartEmpty);
        tvCartTotalAmount = findViewById(R.id.tvCartTotalAmount);
        layoutCartSummary = findViewById(R.id.layoutCartSummary);
        btnProceedToOrder = findViewById(R.id.btnProceedToOrder);

        ImageButton btnBack = findViewById(R.id.btnBackCart);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        ImageButton btnSpeaker = findViewById(R.id.btnSpeaker);
        VoiceGuide.setupSpeakerButton(btnSpeaker, this, "یہ آپ کی ٹوکری ہے۔ یہاں شامل کردہ چیزوں کی مقدار بڑھائیں، یا آرڈر کا عمل شروع کریں۔");

        rvCartItems.setLayoutManager(new LinearLayoutManager(this));
        cartAdapter = new CartAdapter(cartItemList, new CartAdapter.OnCartAction() {
            @Override
            public void onQuantityChange(CartItem item, int newQty) {
                updateCartItemQuantity(item, newQty);
            }

            @Override
            public void onRemove(CartItem item) {
                removeCartItem(item);
            }

            @Override
            public void onSelectToggle(CartItem item, boolean isSelected) {
                recalculateCartSummary();
            }
        });
        rvCartItems.setAdapter(cartAdapter);

        BottomNavHelper.setupForBuyerOrVisitor(this, findViewById(R.id.bottomNav), 0);

        btnProceedToOrder.setOnClickListener(v -> proceedToCheckout());

        loadCartItems();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCartItems();
    }

    private void loadCartItems() {
        // 1. Load local cart items instantly
        cartItemList.clear();
        cartItemList.addAll(CartManager.getInstance(this).getCartItems());

        // Ensure selected items belong to the same seller (allow multiple items from same seller)
        String activeSellerId = "";
        for (CartItem ci : cartItemList) {
            if (ci.isSelected()) {
                String sid = ci.getSellerId() != null ? ci.getSellerId().trim() : "";
                if (activeSellerId.isEmpty()) {
                    activeSellerId = sid;
                } else if (!activeSellerId.equalsIgnoreCase(sid)) {
                    ci.setSelected(false);
                }
            }
        }
        if (activeSellerId.isEmpty() && !cartItemList.isEmpty()) {
            cartItemList.get(0).setSelected(true);
        }

        recalculateCartSummary();

        // 2. Sync from Firebase
        String uid = session.getUid();
        if (uid == null) return;

        DatabaseReference cartRef = FirebaseDatabase.getInstance().getReference().child("carts").child(uid.trim());
        cartRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (!snapshot.exists() && !cartItemList.isEmpty()) {
                    for (CartItem item : cartItemList) {
                        cartRef.child(item.getProductId()).setValue(item);
                    }
                    return;
                }

                List<CartItem> fbItems = new ArrayList<>();
                double fbTotal = 0;

                for (DataSnapshot child : snapshot.getChildren()) {
                    CartItem item = child.getValue(CartItem.class);
                    if (item != null) {
                        if (item.getCartItemId() == null) item.setCartItemId(child.getKey());
                        fbItems.add(item);
                        fbTotal += (item.getPrice() * item.getQuantity());
                    }
                }

                if (!fbItems.isEmpty()) {
                    cartItemList.clear();
                    cartItemList.addAll(fbItems);
                    CartManager.getInstance(CartActivity.this).saveCartItems(fbItems);

                    if (cartAdapter != null) cartAdapter.notifyDataSetChanged();
                    tvCartEmpty.setVisibility(View.GONE);
                    layoutCartSummary.setVisibility(View.VISIBLE);
                    tvCartTotalAmount.setText("PKR " + (long) fbTotal);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {}
        });
    }

    private void updateCartItemQuantity(CartItem item, int newQty) {
        if (item == null || item.getProductId() == null) return;

        if (newQty <= 0) {
            removeCartItem(item);
            return;
        }

        item.setQuantity(newQty);
        CartManager.getInstance(this).updateQuantity(item.getProductId(), newQty);

        if (cartAdapter != null) cartAdapter.notifyDataSetChanged();
        recalculateCartSummary();

        String uid = session.getUid();
        if (uid != null) {
            FirebaseDatabase.getInstance().getReference().child("carts").child(uid.trim()).child(item.getProductId()).child("quantity").setValue(newQty);
        }
    }

    private void removeCartItem(CartItem item) {
        if (item == null || item.getProductId() == null) return;

        for (int i = 0; i < cartItemList.size(); i++) {
            if (item.getProductId().equals(cartItemList.get(i).getProductId())) {
                cartItemList.remove(i);
                break;
            }
        }

        CartManager.getInstance(this).removeFromCart(item.getProductId());

        if (cartAdapter != null) cartAdapter.notifyDataSetChanged();
        recalculateCartSummary();

        String uid = session.getUid();
        if (uid != null) {
            FirebaseDatabase.getInstance().getReference().child("carts").child(uid.trim()).child(item.getProductId()).removeValue();
        }

        Toast.makeText(this, "آئٹم حذف کر دیا گیا / Item removed", Toast.LENGTH_SHORT).show();
    }

    private void recalculateCartSummary() {
        double selectedTotal = 0;
        int selectedCount = 0;

        for (CartItem item : cartItemList) {
            if (item.isSelected()) {
                selectedTotal += (item.getPrice() * item.getQuantity());
                selectedCount++;
            }
        }

        if (cartItemList.isEmpty()) {
            tvCartEmpty.setVisibility(View.VISIBLE);
            layoutCartSummary.setVisibility(View.GONE);
        } else {
            tvCartEmpty.setVisibility(View.GONE);
            layoutCartSummary.setVisibility(View.VISIBLE);
            tvCartTotalAmount.setText("PKR " + (long) selectedTotal + " (" + selectedCount + " items)");
        }
    }

    private void launchPlaceOrderForSellerItems(List<CartItem> items) {
        if (items == null || items.isEmpty()) return;

        CartItem firstItem = items.get(0);
        Intent i = new Intent(this, PlaceOrderActivity.class);
        i.putExtra(PlaceOrderActivity.EXTRA_SELLER_ID, firstItem.getSellerId());

        ArrayList<String> checkedProductIds = new ArrayList<>();
        for (CartItem ci : items) {
            if (ci.getProductId() != null && !ci.getProductId().trim().isEmpty()) {
                checkedProductIds.add(ci.getProductId().trim());
            }
        }
        i.putStringArrayListExtra(PlaceOrderActivity.EXTRA_CART_CHECKED_IDS, checkedProductIds);

        if (items.size() == 1) {
            i.putExtra(PlaceOrderActivity.EXTRA_PRODUCT_ID, firstItem.getProductId());
            i.putExtra(PlaceOrderActivity.EXTRA_PRODUCT_NAME, firstItem.getProductName());
            i.putExtra(PlaceOrderActivity.EXTRA_PRODUCT_PRICE, firstItem.getPrice());
            i.putExtra(PlaceOrderActivity.EXTRA_PRODUCT_CATEGORY, firstItem.getCategory());
        } else {
            // Multiple items from same seller! Disable singleProductMode so all checklist categories are loaded & validated
            i.putExtra(PlaceOrderActivity.EXTRA_PRODUCT_ID, (String) null);
            i.putExtra(PlaceOrderActivity.EXTRA_PRODUCT_CATEGORY, firstItem.getCategory());
        }
        startActivity(i);
    }

    private void proceedToCheckout() {
        List<CartItem> selectedItems = new ArrayList<>();
        for (CartItem item : cartItemList) {
            if (item.isSelected()) {
                selectedItems.add(item);
            }
        }

        if (selectedItems.isEmpty()) {
            Toast.makeText(this, "براہ کرم کم از کم ایک چیز منتخب کریں / Please select at least one item", Toast.LENGTH_LONG).show();
            return;
        }

        Map<String, List<CartItem>> sellerMap = new HashMap<>();
        for (CartItem item : selectedItems) {
            String sid = (item.getSellerId() != null && !item.getSellerId().trim().isEmpty()) ? item.getSellerId().trim() : "default";
            List<CartItem> list = sellerMap.get(sid);
            if (list == null) {
                list = new ArrayList<>();
                sellerMap.put(sid, list);
            }
            list.add(item);
        }

        if (sellerMap.size() <= 1) {
            launchPlaceOrderForSellerItems(selectedItems);
        } else {
            showMultipleSellersCheckoutDialog(sellerMap);
        }
    }

    private void showMultipleSellersCheckoutDialog(Map<String, List<CartItem>> sellerMap) {
        if (sellerMap.isEmpty() || isFinishing() || isDestroyed()) return;

        List<String> sellerIds = new ArrayList<>(sellerMap.keySet());
        String[] sellerDisplayNames = new String[sellerIds.size()];

        for (int i = 0; i < sellerIds.size(); i++) {
            List<CartItem> sItems = sellerMap.get(sellerIds.get(i));
            String name = (sItems != null && !sItems.isEmpty() && sItems.get(0).getSellerName() != null && !sItems.get(0).getSellerName().isEmpty())
                    ? sItems.get(0).getSellerName()
                    : "Seller " + (i + 1);
            int count = sItems != null ? sItems.size() : 0;
            sellerDisplayNames[i] = "🏪 " + name + " (" + count + " items)";
        }

        new AlertDialog.Builder(this)
                .setTitle("ایک وقت میں ایک سیلر کا آرڈر دیں / Single Seller Checkout")
                .setMessage("آپ کی ٹوکری میں ایک سے زیادہ سیلرز کا سامان موجود ہے۔ ایک وقت میں صرف ایک سیلر کا آرڈر دیا جا سکتا ہے۔ براہ کرم وہ سیلر منتخب کریں جس کا آرڈر آپ حتمی کرنا چاہتی ہیں:")
                .setItems(sellerDisplayNames, (dialog, which) -> {
                    if (which >= 0 && which < sellerIds.size()) {
                        String chosenSellerId = sellerIds.get(which);
                        List<CartItem> chosenItems = sellerMap.get(chosenSellerId);
                        if (chosenItems != null && !chosenItems.isEmpty()) {
                            launchPlaceOrderForSellerItems(chosenItems);
                        }
                    }
                })
                .setNegativeButton("منسوخ / Cancel", null)
                .show();
    }

    private void applyTheme() {
        PreferencesManager prefs = new PreferencesManager(this);
        if (prefs.isDarkThemeEnabled()) {
            setTheme(R.style.Theme_SheEmpower_Dark);
        } else {
            setTheme(R.style.Theme_SheEmpower);
        }
    }
}