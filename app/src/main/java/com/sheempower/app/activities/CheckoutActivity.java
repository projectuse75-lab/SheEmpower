package com.sheempower.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.sheempower.app.R;
import com.sheempower.app.models.CartItem;
import com.sheempower.app.models.Order;
import com.sheempower.app.utils.CartManager;
import com.sheempower.app.utils.Constants;
import com.sheempower.app.utils.PreferencesManager;
import com.sheempower.app.utils.SessionManager;
import com.sheempower.app.utils.VoiceGuide;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CheckoutActivity extends AppCompatActivity {

    private LinearLayout layoutCheckoutSummary;
    private EditText etAddress;
    private TextView tvGrandTotal;
    private CheckBox cbTerms;
    private Button btnPlaceOrder;
    private SessionManager session;
    private final List<CartItem> cartItems = new ArrayList<>();
    private final Map<String, List<CartItem>> sellerGroupMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

        session = new SessionManager(this);

        if (!session.isLoggedIn()) {
            Toast.makeText(this, "پہلے لاگ ان کریں / Please Login First", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        layoutCheckoutSummary = findViewById(R.id.layoutCheckoutSummary);
        etAddress = findViewById(R.id.etCheckoutAddress);
        tvGrandTotal = findViewById(R.id.tvCheckoutGrandTotal);
        cbTerms = findViewById(R.id.cbTerms);
        btnPlaceOrder = findViewById(R.id.btnPlaceCheckoutOrder);

        ImageButton btnBack = findViewById(R.id.btnBackCheckout);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        ImageButton btnSpeaker = findViewById(R.id.btnSpeaker);
        VoiceGuide.setupSpeakerButton(btnSpeaker, this, "یہ آرڈر حتمی کرنے کی سکرین ہے۔ اپنا مکمل ڈیلیوری پتہ درج کریں اور آرڈر جمع کروائیں۔");

        btnPlaceOrder.setOnClickListener(v -> processOrders());

        buildSummary();
    }

    private void buildSummary() {
        cartItems.clear();
        cartItems.addAll(CartManager.getInstance(this).getCartItems());

        if (cartItems.isEmpty()) {
            Toast.makeText(this, "ٹوکری خالی ہے / Cart is empty", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        sellerGroupMap.clear();
        double grandTotal = 0;

        for (CartItem item : cartItems) {
            String sid = item.getSellerId() != null ? item.getSellerId() : "default";
            List<CartItem> list = sellerGroupMap.get(sid);
            if (list == null) {
                list = new ArrayList<>();
                sellerGroupMap.put(sid, list);
            }
            list.add(item);
            grandTotal += (item.getPrice() * item.getQuantity());
        }

        tvGrandTotal.setText("PKR " + (long) grandTotal);

        layoutCheckoutSummary.removeAllViews();
        for (Map.Entry<String, List<CartItem>> entry : sellerGroupMap.entrySet()) {
            List<CartItem> sItems = entry.getValue();
            if (sItems.isEmpty()) continue;

            TextView sellerHeader = new TextView(this);
            sellerHeader.setText("🏪 Seller: " + (sItems.get(0).getSellerName() != null ? sItems.get(0).getSellerName() : "Seller"));
            sellerHeader.setTextColor(0xFF192A56);
            sellerHeader.setTextSize(14);
            sellerHeader.setPadding(0, 12, 0, 6);
            layoutCheckoutSummary.addView(sellerHeader);

            double subtotal = 0;
            for (CartItem item : sItems) {
                TextView itemRow = new TextView(this);
                double lineTotal = item.getPrice() * item.getQuantity();
                subtotal += lineTotal;
                itemRow.setText("  • " + item.getProductName() + " x" + item.getQuantity() + " = PKR " + (long) lineTotal);
                itemRow.setTextColor(0xFF6B7A99);
                itemRow.setTextSize(13);
                layoutCheckoutSummary.addView(itemRow);
            }

            TextView subtotalTv = new TextView(this);
            subtotalTv.setText("  Subtotal: PKR " + (long) subtotal);
            subtotalTv.setTextColor(0xFF2ECC71);
            subtotalTv.setTextSize(13);
            subtotalTv.setPadding(0, 4, 0, 16);
            layoutCheckoutSummary.addView(subtotalTv);
        }
    }

    private void processOrders() {
        String address = etAddress.getText().toString().trim();
        if (TextUtils.isEmpty(address)) {
            etAddress.setError("ڈیلیوری پتہ درج کرنا ضروری ہے / Address required");
            etAddress.requestFocus();
            return;
        }

        if (cbTerms != null && !cbTerms.isChecked()) {
            Toast.makeText(this, "شرائط و ضوابط تسلیم کرنا ضروری ہے / Agree to terms", Toast.LENGTH_SHORT).show();
            return;
        }

        btnPlaceOrder.setEnabled(false);
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();
        String buyerUid = session.getUid().trim();
        String buyerName = session.getName();

        List<Order> createdOrders = new ArrayList<>();

        for (Map.Entry<String, List<CartItem>> entry : sellerGroupMap.entrySet()) {
            String sellerId = entry.getKey();
            List<CartItem> sItems = entry.getValue();
            if (sItems.isEmpty()) continue;

            String orderId = dbRef.child(Constants.NODE_ORDERS).push().getKey();
            if (orderId == null) continue;

            StringBuilder namesBuilder = new StringBuilder();
            double sellerTotal = 0;
            String category = sItems.get(0).getCategory();

            for (CartItem item : sItems) {
                if (namesBuilder.length() > 0) namesBuilder.append(" + ");
                namesBuilder.append(item.getProductName()).append(" x").append(item.getQuantity());
                sellerTotal += (item.getPrice() * item.getQuantity());
            }

            Order order = new Order();
            order.setId(orderId);
            order.setBuyerId(buyerUid);
            order.setBuyerName(buyerName != null ? buyerName : "Buyer");
            order.setSellerId(sellerId);
            order.setSellerName(sItems.get(0).getSellerName());
            order.setProductId("cart_multiple");
            order.setProductName(namesBuilder.toString());
            order.setDetails("Cart Order:\n" + namesBuilder.toString());
            order.setAddress(address);
            order.setDeliveryAddress(address);
            order.setPaymentMethod("COD");
            order.setDeliveryMethod(Constants.DELIVERY_METHOD_DELIVERY);
            order.setAmount(sellerTotal);
            order.setStatus(Constants.STATUS_PENDING);
            order.setTimestamp(System.currentTimeMillis());
            if (category != null) order.setOrderCategory(category);

            dbRef.child(Constants.NODE_ORDERS).child(orderId).setValue(order);
            createdOrders.add(order);
        }

        CartManager.getInstance(this).clearCart();
        Toast.makeText(this, "آرڈرز کامیابی سے جمع ہو گئے! ✅ / Orders Placed!", Toast.LENGTH_LONG).show();
        VoiceGuide.explain(this, "آپ کا آرڈر کامیابی سے جمع ہو گیا ہے۔");

        Intent i = new Intent(this, MyOrdersActivity.class);
        startActivity(i);
        finish();
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