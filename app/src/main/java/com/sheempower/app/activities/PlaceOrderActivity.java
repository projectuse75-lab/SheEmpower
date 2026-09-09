package com.sheempower.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.*;
import com.sheempower.app.R;
import com.sheempower.app.models.Order;
import com.sheempower.app.models.OrderField;
import com.sheempower.app.models.Seller;
import com.sheempower.app.utils.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlaceOrderActivity extends AppCompatActivity {

    public static final String EXTRA_SELLER_ID     = "sellerId";
    public static final String EXTRA_PRODUCT_ID    = "productId";
    public static final String EXTRA_PRODUCT_NAME  = "productName";
    public static final String EXTRA_PRODUCT_PRICE = "productPrice";

    private TextView tvSellerName, tvSellerMeta, tvSellerVerified, tvSelectedItem, tvDeliveryInfo, tvCategoryHeader;
    private LinearLayout layoutDynamicFields, layoutBuyerAddress;
    private TextInputEditText etAmount, etAddress, etNotes;
    private Button btnSubmitOrder;

    private String sellerId, productId, productName;
    private double productPrice;
    private Seller currentSeller;
    private SessionManager session;
    private String resolvedDeliveryMethod = Constants.DELIVERY_METHOD_PICKUP;

    private final Map<String, EditText> dynamicTextInputs = new HashMap<>();
    private final Map<String, Spinner> dynamicSpinners = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_place_order);

        session = new SessionManager(this);

        sellerId     = getIntent().getStringExtra(EXTRA_SELLER_ID);
        productId    = getIntent().getStringExtra(EXTRA_PRODUCT_ID);
        productName  = getIntent().getStringExtra(EXTRA_PRODUCT_NAME);
        productPrice = getIntent().getDoubleExtra(EXTRA_PRODUCT_PRICE, 0);

        tvSellerName        = findViewById(R.id.tvSellerName);
        tvSellerMeta        = findViewById(R.id.tvSellerMeta);
        tvSellerVerified    = findViewById(R.id.tvSellerVerified);
        tvSelectedItem      = findViewById(R.id.tvSelectedItem);
        tvDeliveryInfo      = findViewById(R.id.tvDeliveryInfo);
        tvCategoryHeader    = findViewById(R.id.tvCategoryHeader);
        layoutDynamicFields = findViewById(R.id.layoutDynamicFields);
        layoutBuyerAddress  = findViewById(R.id.layoutBuyerAddress);
        etAmount            = findViewById(R.id.etAmount);
        etAddress           = findViewById(R.id.etAddress);
        etNotes             = findViewById(R.id.etNotes);
        btnSubmitOrder      = findViewById(R.id.btnSubmitOrder);

        if (!session.isLoggedIn()) {
            Toast.makeText(this, "پہلے لاگ ان کریں", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        if (sellerId == null) { Toast.makeText(this, "بیچنے والی نہیں ملی", Toast.LENGTH_LONG).show(); finish(); return; }

        if (productName != null) {
            tvSelectedItem.setText("منتخب کردہ چیز:\n" + productName);
            etAmount.setText(String.valueOf((long) productPrice));
            tvSelectedItem.setVisibility(android.view.View.VISIBLE);
        } else tvSelectedItem.setVisibility(android.view.View.GONE);

        loadSellerAndBuildForm();
        btnSubmitOrder.setOnClickListener(v -> submitOrder());
    }

    private void loadSellerAndBuildForm() {
        FirebaseDatabase.getInstance().getReference().child(Constants.NODE_SELLERS).child(sellerId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(DataSnapshot snapshot) {
                        currentSeller = snapshot.getValue(Seller.class);
                        if (currentSeller == null) {
                            Toast.makeText(PlaceOrderActivity.this, "یہ بیچنے والی نہیں ملی", Toast.LENGTH_LONG).show();
                            finish();
                            return;
                        }
                        tvSellerName.setText(currentSeller.getBusinessName());
                        tvSellerMeta.setText(currentSeller.getCategory() + " · ⭐ " + currentSeller.getRating());
                        tvSellerVerified.setText("✓ تصدیق شدہ");

                        // ISI LINE SE category-specific form guaranteed banta hai —
                        // yeh hamesha seller ki apni asal category se aata hai, kabhi generic nahi
                        String category = currentSeller.getCategory();
                        tvCategoryHeader.setText("📋 " + Constants.getUrduLabel(category) + " کی تفصیلات / " + category + " Order Details");
                        buildDynamicForm(category);
                        buildDeliveryBlock(currentSeller);
                    }
                    @Override public void onCancelled(DatabaseError error) {
                        Toast.makeText(PlaceOrderActivity.this, "خرابی: " + error.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void buildDeliveryBlock(Seller seller) {
        boolean travelType = Constants.isTravelType(seller.getCategory());
        if (seller.isCanDeliver()) {
            resolvedDeliveryMethod = Constants.DELIVERY_METHOD_DELIVERY;
            tvDeliveryInfo.setText(travelType
                    ? "✅ یہ بیچنے والی خود آپ کے پاس آ سکتی ہیں"
                    : "✅ یہ بیچنے والی گھر ڈیلیوری کر سکتی ہیں");
            tvDeliveryInfo.setTextColor(0xFF4CAF50);
            layoutBuyerAddress.setVisibility(android.view.View.VISIBLE);
        } else {
            resolvedDeliveryMethod = Constants.DELIVERY_METHOD_PICKUP;
            String note = travelType
                    ? "⚠️ یہ بیچنے والی خود آپ کے پاس نہیں آ سکتیں — آپ کو خود ان کے پاس جانا ہوگا۔"
                    : "⚠️ یہ بیچنے والی ڈیلیوری نہیں کرتیں — آپ کو خود لینے جانا ہوگا۔";
            tvDeliveryInfo.setText(note + "\n📍 پتہ: " + (seller.getShopAddress() != null ? seller.getShopAddress() : seller.getCity()));
            tvDeliveryInfo.setTextColor(0xFFFF6F00);
            layoutBuyerAddress.setVisibility(android.view.View.GONE);
        }
    }

    private void buildDynamicForm(String category) {
        layoutDynamicFields.removeAllViews();
        dynamicTextInputs.clear();
        dynamicSpinners.clear();
        List<OrderField> fields = CategoryFormConfig.getFieldsForCategory(category);

        for (OrderField field : fields) {
            TextView label = new TextView(this);
            label.setText(field.labelUrdu + " / " + field.labelEnglish);
            label.setTextColor(0xFFAA3366);
            label.setTextSize(15);
            label.setPadding(0, 24, 0, 6);
            layoutDynamicFields.addView(label);

            if ("spinner".equals(field.type)) {
                Spinner spinner = new Spinner(this);
                ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, field.options);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinner.setAdapter(adapter);
                layoutDynamicFields.addView(spinner);
                dynamicSpinners.put(field.labelEnglish, spinner);
            } else {
                EditText input = new EditText(this);
                input.setHint(field.hintUrdu);
                input.setTextSize(16);
                input.setPadding(28, 28, 28, 28);
                input.setBackgroundResource(R.drawable.bg_rounded_pink);
                if ("number".equals(field.type)) input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
                layoutDynamicFields.addView(input);
                dynamicTextInputs.put(field.labelEnglish, input);
            }
        }
    }

    private void submitOrder() {
        String amountStr = etAmount.getText().toString().trim();
        String notes = etNotes.getText().toString().trim();
        boolean needsBuyerAddress = layoutBuyerAddress.getVisibility() == android.view.View.VISIBLE;
        String address = needsBuyerAddress ? etAddress.getText().toString().trim()
                : (currentSeller.getShopAddress() != null ? currentSeller.getShopAddress() : currentSeller.getCity());

        if (TextUtils.isEmpty(amountStr)) { etAmount.setError("ضروری ہے"); return; }
        if (needsBuyerAddress && TextUtils.isEmpty(address)) { etAddress.setError("ضروری ہے"); return; }

        double amount;
        try { amount = Double.parseDouble(amountStr); } catch (Exception e) { etAmount.setError("غلط رقم"); return; }
        final double finalAmount = amount;
        final String finalAddress = address;
        final String finalDeliveryMethod = resolvedDeliveryMethod;

        StringBuilder detailsBuilder = new StringBuilder();
        for (Map.Entry<String, EditText> entry : dynamicTextInputs.entrySet()) {
            String value = entry.getValue().getText().toString().trim();
            if (!value.isEmpty()) detailsBuilder.append(entry.getKey()).append(": ").append(value).append("\n");
        }
        for (Map.Entry<String, Spinner> entry : dynamicSpinners.entrySet()) {
            Object selected = entry.getValue().getSelectedItem();
            if (selected != null) detailsBuilder.append(entry.getKey()).append(": ").append(selected).append("\n");
        }
        if (!notes.isEmpty()) detailsBuilder.append("Notes: ").append(notes);
        String detailsTemp = detailsBuilder.toString().trim();
        if (detailsTemp.isEmpty()) detailsTemp = productName != null ? productName : "Order";
        final String details = detailsTemp;

        btnSubmitOrder.setEnabled(false);

        // Buyer ka naam nikal ke order ke saath save karte hain (denormalized — extra reads bachte hain)
        FirebaseDatabase.getInstance().getReference().child(Constants.NODE_USERS).child(session.getUid())
                .get().addOnSuccessListener(userSnap -> {
                    String buyerName = userSnap.child("name").getValue(String.class);
                    saveOrder(finalAmount, finalAddress, finalDeliveryMethod, details, notes, buyerName);
                }).addOnFailureListener(e -> saveOrder(finalAmount, finalAddress, finalDeliveryMethod, details, notes, session.getName()));
    }

    private void saveOrder(double amount, String address, String deliveryMethod, String details, String notes, String buyerName) {
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();
        String orderId = dbRef.child(Constants.NODE_ORDERS).push().getKey();
        if (orderId == null) return;

        Order order = new Order();
        order.setId(orderId);
        order.setBuyerId(session.getUid());
        order.setBuyerName(buyerName != null ? buyerName : "خریدار");
        order.setSellerId(sellerId);
        order.setSellerName(currentSeller.getBusinessName());
        order.setProductId(productId);
        order.setProductName(productName != null ? productName : details);
        order.setDetails(details);
        order.setAddress(address);
        order.setNotes(notes);
        order.setDeliveryMethod(deliveryMethod);
        order.setAmount(amount);
        order.setStatus(Constants.STATUS_PENDING);
        order.setTimestamp(System.currentTimeMillis());

        dbRef.child(Constants.NODE_ORDERS).child(orderId).setValue(order)
                .addOnSuccessListener(v -> {
                    DemoDataSeeder.clearDemoDataOnFirstRealUse(this);
                    Toast.makeText(this, "آرڈر جمع ہو گیا! ✅", Toast.LENGTH_LONG).show();
                    String msg = "السلام علیکم! میں نے شی ایمپاور ایپ سے آرڈر دیا ہے:\n"
                            + order.getProductName() + " — PKR " + (long) amount + "\n" + details;
                    WhatsAppHelper.openChat(this, currentSeller.getWhatsappNumber(), msg);
                    startActivity(new Intent(this, MyOrdersActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnSubmitOrder.setEnabled(true);
                    Toast.makeText(this, "خرابی: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}