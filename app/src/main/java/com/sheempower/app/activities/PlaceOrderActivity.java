package com.sheempower.app.activities;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.*;
import com.sheempower.app.R;
import com.sheempower.app.models.Order;
import com.sheempower.app.models.OrderField;
import com.sheempower.app.models.Product;
import com.sheempower.app.models.Seller;
import com.sheempower.app.utils.PreferencesManager;
import com.sheempower.app.utils.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PlaceOrderActivity extends AppCompatActivity {

    public static final String EXTRA_SELLER_ID        = "sellerId";
    public static final String EXTRA_PRODUCT_ID       = "productId";
    public static final String EXTRA_PRODUCT_NAME     = "productName";
    public static final String EXTRA_PRODUCT_PRICE    = "productPrice";
    public static final String EXTRA_PRODUCT_CATEGORY = "productCategory";

    private TextView tvSellerName, tvSellerMeta, tvSellerVerified, tvSelectedItem, tvDeliveryInfo,
            tvCategoryHeader, tvAmountLockedNote, tvChecklistHeader, tvChecklistEmpty;
    private Spinner spinnerOrderCategory;
    private LinearLayout layoutDynamicFields, layoutBuyerAddress, layoutProductChecklist;
    private TextInputEditText etAmount, etAddress, etNotes;
    private Button btnSubmitOrder;

    private String sellerId, productId, productName, productCategory;
    private double productPrice;
    private Seller currentSeller;
    private SessionManager session;
    private String resolvedDeliveryMethod = Constants.DELIVERY_METHOD_PICKUP;
    private boolean isUpdatingCategorySpinner = false;

    /** Agar buyer seedha ek product select karke aaya ho (single, fixed price) */
    private boolean singleProductMode;

    private final Map<String, EditText> dynamicTextInputs = new HashMap<>();
    private final Map<String, Spinner> dynamicSpinners = new HashMap<>();
    private final Map<String, List<CheckBox>> dynamicCheckboxGroups = new HashMap<>();

    /** Seller Profile se order karte waqt — seller ki saari products checklist yahan track hoti hai */
    private final Map<CheckBox, Product> checklistMap = new LinkedHashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_place_order);

        session = new SessionManager(this);

        if (getIntent() != null) {
            sellerId         = getIntent().getStringExtra(EXTRA_SELLER_ID);
            if (sellerId != null) sellerId = sellerId.trim();
            productId        = getIntent().getStringExtra(EXTRA_PRODUCT_ID);
            if (productId != null) productId = productId.trim();
            productName      = getIntent().getStringExtra(EXTRA_PRODUCT_NAME);
            productPrice     = getIntent().getDoubleExtra(EXTRA_PRODUCT_PRICE, 0);
            productCategory  = getIntent().getStringExtra(EXTRA_PRODUCT_CATEGORY);
        }
        singleProductMode = (productId != null && !productId.isEmpty());

        tvSellerName        = findViewById(R.id.tvSellerName);
        tvSellerMeta        = findViewById(R.id.tvSellerMeta);
        tvSellerVerified    = findViewById(R.id.tvSellerVerified);
        tvSelectedItem      = findViewById(R.id.tvSelectedItem);
        tvDeliveryInfo      = findViewById(R.id.tvDeliveryInfo);
        tvCategoryHeader    = findViewById(R.id.tvCategoryHeader);
        spinnerOrderCategory = findViewById(R.id.spinnerOrderCategory);
        tvAmountLockedNote  = findViewById(R.id.tvAmountLockedNote);
        tvChecklistHeader   = findViewById(R.id.tvChecklistHeader);
        tvChecklistEmpty    = findViewById(R.id.tvChecklistEmpty);
        layoutDynamicFields = findViewById(R.id.layoutDynamicFields);
        layoutBuyerAddress  = findViewById(R.id.layoutBuyerAddress);
        layoutProductChecklist = findViewById(R.id.layoutProductChecklist);
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

        setupOrderCategorySpinner();

        // ہمیشہ قیمت کا خانہ بند رکھتے ہیں — خریدار کبھی خود قیمت تبدیل نہیں کر سکے گا
        etAmount.setEnabled(false);
        etAmount.setFocusable(false);
        tvAmountLockedNote.setVisibility(View.VISIBLE);

        if (singleProductMode) {
            VoiceGuide.announceScreen(this,
                    "یہ آرڈر کی سکرین ہے۔ آپ نے ایک مخصوص چیز منتخب کی ہے، اس کی قیمت بیچنے والی نے پہلے سے مقرر کی ہے اور اسے تبدیل نہیں کیا جا سکتا۔ نیچے تفصیلات بھر کر آرڈر جمع کریں۔");
            tvSelectedItem.setText("منتخب کردہ چیز:\n" + productName);
            etAmount.setText(String.valueOf((long) productPrice));
            tvAmountLockedNote.setText("🔒 یہ قیمت بیچنے والی نے مقرر کی ہے");
            tvSelectedItem.setVisibility(View.VISIBLE);
            layoutProductChecklist.setVisibility(View.GONE);
            tvChecklistHeader.setVisibility(View.GONE);
        } else {
            VoiceGuide.announceScreen(this,
                    "یہ آرڈر کی سکرین ہے۔ نیچے دی گئی فہرست میں سے وہ چیزیں منتخب کریں جو آپ منگوانا چاہتی ہیں، ہر چیز کے آگے اس کی قیمت لکھی ہے۔ منتخب کردہ چیزوں کی کل قیمت خود بخود جمع ہو جائے گی۔");
            tvSelectedItem.setVisibility(View.GONE);
            tvChecklistHeader.setVisibility(View.VISIBLE);
            etAmount.setText("0");
            tvAmountLockedNote.setText("🔒 یہ رقم آپ کی منتخب کردہ چیزوں کے مطابق خود بخود بنتی ہے");
        }

        loadSellerAndBuildForm();
        btnSubmitOrder.setOnClickListener(v -> submitOrder());
    }

    private void setupOrderCategorySpinner() {
        if (spinnerOrderCategory == null) return;
        String[] categoryDisplayNames = new String[Constants.CATEGORIES.length];
        for (int i = 0; i < Constants.CATEGORIES.length; i++) {
            categoryDisplayNames[i] = Constants.CATEGORY_LABELS_UR[i] + " / " + Constants.CATEGORIES[i];
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categoryDisplayNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerOrderCategory.setAdapter(adapter);

        spinnerOrderCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (isUpdatingCategorySpinner) return;
                if (position >= 0 && position < Constants.CATEGORIES.length) {
                    String selectedCat = Constants.CATEGORIES[position];
                    tvCategoryHeader.setText("📋 " + Constants.getUrduLabel(selectedCat) + " کی تفصیلات");
                    buildDynamicForm(selectedCat);
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void applyActiveCategory() {
        String category = (productCategory != null && !productCategory.trim().isEmpty())
                ? productCategory.trim()
                : (currentSeller != null ? currentSeller.getCategory() : "Cooking");

        if (category == null || category.isEmpty()) category = "Cooking";

        int selectionIndex = 0;
        for (int i = 0; i < Constants.CATEGORIES.length; i++) {
            if (Constants.CATEGORIES[i].equalsIgnoreCase(category)) {
                selectionIndex = i;
                break;
            }
        }

        isUpdatingCategorySpinner = true;
        if (spinnerOrderCategory != null) spinnerOrderCategory.setSelection(selectionIndex);
        isUpdatingCategorySpinner = false;

        String selectedCat = Constants.CATEGORIES[selectionIndex];
        tvCategoryHeader.setText("📋 " + Constants.getUrduLabel(selectedCat) + " کی تفصیلات");
        buildDynamicForm(selectedCat);
    }

    private void loadSellerAndBuildForm() {
        if (sellerId == null || sellerId.isEmpty()) {
            Toast.makeText(this, "بیچنے والی نہیں ملی", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
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

                        buildDeliveryBlock(currentSeller);
                        if (!singleProductMode) buildProductChecklist(sellerId);

                        if (singleProductMode && productId != null && (productCategory == null || productCategory.trim().isEmpty())) {
                            FirebaseDatabase.getInstance().getReference().child(Constants.NODE_PRODUCTS).child(productId)
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(DataSnapshot prodSnap) {
                                            Product prod = prodSnap.getValue(Product.class);
                                            if (prod != null && prod.getCategory() != null && !prod.getCategory().trim().isEmpty()) {
                                                productCategory = prod.getCategory();
                                            }
                                            applyActiveCategory();
                                        }
                                        @Override public void onCancelled(DatabaseError error) { applyActiveCategory(); }
                                    });
                        } else {
                            applyActiveCategory();
                        }
                    }
                    @Override public void onCancelled(DatabaseError error) {
                        Toast.makeText(PlaceOrderActivity.this, "خرابی: " + error.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    /** YEH ASAL FEATURE HAI — seller ki saari products, har ek ki apni fixed price ke sath.
     *  Buyer jo bhi tick karta hai, uski total qeemat khud-ba-khud jud kar etAmount mein aa jati hai */
    private void buildProductChecklist(String sellerId) {
        FirebaseDatabase.getInstance().getReference().child(Constants.NODE_PRODUCTS)
                .orderByChild("sellerId").equalTo(sellerId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(DataSnapshot snapshot) {
                        layoutProductChecklist.removeAllViews();
                        checklistMap.clear();

                        List<Product> products = new ArrayList<>();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            Product p = child.getValue(Product.class);
                            if (p != null) products.add(p);
                        }

                        if (products.isEmpty()) {
                            tvChecklistEmpty.setVisibility(View.VISIBLE);
                            return;
                        }
                        tvChecklistEmpty.setVisibility(View.GONE);

                        for (Product p : products) {
                            LinearLayout row = new LinearLayout(PlaceOrderActivity.this);
                            row.setOrientation(LinearLayout.HORIZONTAL);
                            row.setGravity(Gravity.CENTER_VERTICAL);
                            row.setPadding(0, 12, 0, 12);

                            CheckBox cb = new CheckBox(PlaceOrderActivity.this);
                            cb.setText(p.getName());
                            cb.setTextSize(14);
                            LinearLayout.LayoutParams cbParams = new LinearLayout.LayoutParams(
                                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                            cb.setLayoutParams(cbParams);

                            TextView tvPrice = new TextView(PlaceOrderActivity.this);
                            tvPrice.setText("PKR " + (long) p.getPrice());
                            tvPrice.setTextColor(0xFF192A56);
                            tvPrice.setTypeface(null, Typeface.BOLD);

                            cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                                recalcTotal();
                                if (isChecked && p.getCategory() != null && !p.getCategory().trim().isEmpty()) {
                                    productCategory = p.getCategory();
                                    applyActiveCategory();
                                }
                            });

                            row.addView(cb);
                            row.addView(tvPrice);
                            layoutProductChecklist.addView(row);

                            checklistMap.put(cb, p);
                        }
                    }
                    @Override public void onCancelled(DatabaseError error) {}
                });
    }

    private void recalcTotal() {
        double total = 0;
        for (Map.Entry<CheckBox, Product> entry : checklistMap.entrySet()) {
            if (entry.getKey().isChecked()) total += entry.getValue().getPrice();
        }
        etAmount.setText(String.valueOf((long) total));
    }

    private void buildDeliveryBlock(Seller seller) {
        boolean travelType = Constants.isTravelType(seller.getCategory());
        if (seller.isCanDeliver()) {
            resolvedDeliveryMethod = Constants.DELIVERY_METHOD_DELIVERY;
            tvDeliveryInfo.setText(travelType ? "✅ یہ بیچنے والی خود آپ کے پاس آ سکتی ہیں" : "✅ یہ بیچنے والی گھر ڈیلیوری کر سکتی ہیں");
            tvDeliveryInfo.setTextColor(0xFF4CAF50);
            layoutBuyerAddress.setVisibility(View.VISIBLE);
        } else {
            resolvedDeliveryMethod = Constants.DELIVERY_METHOD_PICKUP;
            String note = travelType
                    ? "⚠️ یہ بیچنے والی خود آپ کے پاس نہیں آ سکتیں — آپ کو خود ان کے پاس جانا ہوگا۔"
                    : "⚠️ یہ بیچنے والی ڈیلیوری نہیں کرتیں — آپ کو خود لینے جانا ہوگا۔";
            tvDeliveryInfo.setText(note + "\n📍 پتہ: " + (seller.getShopAddress() != null ? seller.getShopAddress() : seller.getCity()));
            tvDeliveryInfo.setTextColor(0xFFFF6F00);
            layoutBuyerAddress.setVisibility(View.GONE);
        }
    }

    private void buildDynamicForm(String category) {
        layoutDynamicFields.removeAllViews();
        dynamicTextInputs.clear();
        dynamicSpinners.clear();
        dynamicCheckboxGroups.clear();
        List<OrderField> fields = CategoryFormConfig.getFieldsForCategory(category);

        for (OrderField field : fields) {
            TextView label = new TextView(this);
            label.setText(field.labelUrdu + " / " + field.labelEnglish);
            label.setTextColor(0xFF192A56);
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

            } else if ("multi".equals(field.type)) {
                List<CheckBox> group = new ArrayList<>();
                LinearLayout checkboxContainer = new LinearLayout(this);
                checkboxContainer.setOrientation(LinearLayout.VERTICAL);
                for (String option : field.options) {
                    CheckBox cb = new CheckBox(this);
                    cb.setText(option);
                    cb.setTextSize(14);
                    checkboxContainer.addView(cb);
                    group.add(cb);
                }
                layoutDynamicFields.addView(checkboxContainer);
                dynamicCheckboxGroups.put(field.labelEnglish, group);

            } else {
                EditText input = new EditText(this);
                input.setHint(field.hintUrdu);
                input.setTextSize(16);
                input.setPadding(28, 28, 28, 28);
                input.setBackgroundResource(R.drawable.bg_skeu_input);
                if ("number".equals(field.type)) input.setInputType(InputType.TYPE_CLASS_NUMBER);
                layoutDynamicFields.addView(input);
                dynamicTextInputs.put(field.labelEnglish, input);
            }
        }
    }

    private void submitOrder() {
        String amountStr = etAmount.getText().toString().trim();
        String notes = etNotes.getText().toString().trim();
        boolean needsBuyerAddress = layoutBuyerAddress.getVisibility() == View.VISIBLE;
        String address = needsBuyerAddress ? etAddress.getText().toString().trim()
                : (currentSeller.getShopAddress() != null ? currentSeller.getShopAddress() : currentSeller.getCity());

        // Checklist mode mein kam az kam ek cheez tick honi chahiye
        String finalProductName = productName;
        if (!singleProductMode) {
            List<String> chosenNames = new ArrayList<>();
            for (Map.Entry<CheckBox, Product> entry : checklistMap.entrySet()) {
                if (entry.getKey().isChecked()) chosenNames.add(entry.getValue().getName() + " (PKR " + (long) entry.getValue().getPrice() + ")");
            }
            if (chosenNames.isEmpty()) {
                Toast.makeText(this, "کم از کم ایک چیز منتخب کریں", Toast.LENGTH_LONG).show();
                return;
            }
            finalProductName = String.join(" + ", chosenNames);
        }

        if (needsBuyerAddress && TextUtils.isEmpty(address)) { etAddress.setError("ضروری ہے"); return; }

        double amount;
        try { amount = Double.parseDouble(amountStr); } catch (Exception e) { amount = 0; }
        if (amount <= 0) {
            Toast.makeText(this, "رقم صفر نہیں ہو سکتی", Toast.LENGTH_LONG).show();
            return;
        }
        final double finalAmount = amount;
        final String finalAddress = address;
        final String finalDeliveryMethod = resolvedDeliveryMethod;
        final String finalProductNameCopy = finalProductName;

        StringBuilder detailsBuilder = new StringBuilder();
        for (Map.Entry<String, EditText> entry : dynamicTextInputs.entrySet()) {
            String value = entry.getValue().getText().toString().trim();
            if (!value.isEmpty()) detailsBuilder.append(entry.getKey()).append(": ").append(value).append("\n");
        }
        for (Map.Entry<String, Spinner> entry : dynamicSpinners.entrySet()) {
            Object selected = entry.getValue().getSelectedItem();
            if (selected != null) detailsBuilder.append(entry.getKey()).append(": ").append(selected).append("\n");
        }
        for (Map.Entry<String, List<CheckBox>> entry : dynamicCheckboxGroups.entrySet()) {
            List<String> chosen = new ArrayList<>();
            for (CheckBox cb : entry.getValue()) if (cb.isChecked()) chosen.add(cb.getText().toString());
            if (!chosen.isEmpty()) detailsBuilder.append(entry.getKey()).append(": ").append(String.join(", ", chosen)).append("\n");
        }
        if (!notes.isEmpty()) detailsBuilder.append("Notes: ").append(notes);
        String detailsTemp = detailsBuilder.toString().trim();
        if (detailsTemp.isEmpty()) detailsTemp = finalProductNameCopy;
        final String details = detailsTemp;

        btnSubmitOrder.setEnabled(false);

        String uid = session.getUid();
        if (uid == null || uid.trim().isEmpty()) {
            Toast.makeText(this, "پہلے لاگ ان کریں", Toast.LENGTH_SHORT).show();
            btnSubmitOrder.setEnabled(true);
            return;
        }

        FirebaseDatabase.getInstance().getReference().child(Constants.NODE_USERS).child(uid.trim())
                .get().addOnSuccessListener(userSnap -> {
                    String buyerName = userSnap.child("name").getValue(String.class);
                    saveOrder(finalAmount, finalAddress, finalDeliveryMethod, details, notes, buyerName, finalProductNameCopy);
                }).addOnFailureListener(e -> saveOrder(finalAmount, finalAddress, finalDeliveryMethod, details, notes, session.getName(), finalProductNameCopy));
    }

    private void saveOrder(double amount, String address, String deliveryMethod, String details, String notes, String buyerName, String finalProductName) {
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();
        String orderId = dbRef.child(Constants.NODE_ORDERS).push().getKey();
        if (orderId == null) return;

        Order order = new Order();
        order.setId(orderId);
        order.setBuyerId(session.getUid());
        order.setBuyerName(buyerName != null ? buyerName : "خریدار");
        order.setSellerId(sellerId);
        order.setSellerName(currentSeller.getBusinessName());
        order.setProductId(productId != null ? productId : "multiple");
        order.setProductName(finalProductName);
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
                    VoiceGuide.explain(this, "آپ کا آرڈر کامیابی سے جمع ہو گیا ہے۔ بیچنے والی سے واٹس ایپ پر رابطہ ہو رہا ہے۔");
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

    // ✅ DARK THEME - ADD AT END OF CLASS
    private void applyTheme() {
        PreferencesManager prefs = new PreferencesManager(this);
        if (prefs.isDarkThemeEnabled()) {
            setTheme(R.style.Theme_SheEmpower_Dark);
        } else {
            setTheme(R.style.Theme_SheEmpower);
        }
    }
}