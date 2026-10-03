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
import java.util.Arrays;
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

    /** Seller Profile se order karte waqt — seller ki saari products categories ke mutabiq group hoti hain */
    private final Map<String, List<Product>> sellerCategoryProductsMap = new LinkedHashMap<>();
    private final List<String> availableOrderCategories = new ArrayList<>();
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

        BottomNavHelper.setupBackButton(this);

        // ہمیشہ قیمت کا خانہ بند رکھتے ہیں — خریدار کبھی خود قیمت تبدیل نہیں کر سکے گا
        etAmount.setEnabled(false);
        etAmount.setFocusable(false);
        tvAmountLockedNote.setVisibility(View.VISIBLE);

        ImageButton btnSpeaker = findViewById(R.id.btnSpeaker);
        VoiceGuide.setupSpeakerButton(btnSpeaker, this,
                singleProductMode
                        ? "یہ آرڈر کی سکرین ہے۔ آپ نے ایک مخصوص چیز منتخب کی ہے، اس کی قیمت بیچنے والی نے پہلے سے مقرر کی ہے اور اسے تبدیل نہیں کیا جا سکتا۔ نیچے تفصیلات بھر کر آرڈر جمع کریں۔"
                        : "یہ آرڈر کی سکرین ہے۔ نیچے دی گئی فہرست میں سے وہ چیزیں منتخب کریں جو آپ منگوانا چاہتی ہیں، ہر چیز کے آگے اس کی قیمت لکھی ہے۔"
        );

        if (singleProductMode) {
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

    private void loadSellerAndBuildForm() {
        if (sellerId == null || sellerId.isEmpty()) {
            Toast.makeText(this, "بیچنے والی نہیں ملی", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        final String targetSellerId = sellerId.trim();

        // 1. Pehle seller ki tamam products ko category-wise fetch karke group karte hain
        FirebaseDatabase.getInstance().getReference().child(Constants.NODE_PRODUCTS)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot prodSnapshot) {
                        sellerCategoryProductsMap.clear();

                        for (DataSnapshot child : prodSnapshot.getChildren()) {
                            Product p = child.getValue(Product.class);
                            if (p != null) {
                                String pKey = child.getKey();
                                if (pKey != null) p.setId(pKey.trim());

                                if (p.getSellerId() != null && targetSellerId.equalsIgnoreCase(p.getSellerId().trim())) {
                                    String cat = (p.getCategory() != null && !p.getCategory().trim().isEmpty())
                                            ? p.getCategory().trim()
                                            : "Cooking";

                                    List<Product> catList = sellerCategoryProductsMap.get(cat);
                                    if (catList == null) {
                                        catList = new ArrayList<>();
                                        sellerCategoryProductsMap.put(cat, catList);
                                    }
                                    catList.add(p);
                                }
                            }
                        }

                        loadSellerInfoAndCategorySetup();
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        loadSellerInfoAndCategorySetup();
                    }
                });
    }

    private void loadSellerInfoAndCategorySetup() {
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

                        // Filter products to keep ONLY active categories selected by seller
                        Map<String, List<Product>> filteredMap = new LinkedHashMap<>();
                        for (Map.Entry<String, List<Product>> entry : sellerCategoryProductsMap.entrySet()) {
                            if (isCategoryActiveForSeller(entry.getKey())) {
                                filteredMap.put(entry.getKey(), entry.getValue());
                            }
                        }
                        sellerCategoryProductsMap.clear();
                        sellerCategoryProductsMap.putAll(filteredMap);

                        // Agar seller ki registered categories mein koi products na bhi hon tab bhi option dikھayein
                        if (currentSeller.getCategories() != null) {
                            for (String cat : currentSeller.getCategories()) {
                                if (cat != null && !cat.trim().isEmpty() && !sellerCategoryProductsMap.containsKey(cat.trim())) {
                                    sellerCategoryProductsMap.put(cat.trim(), new ArrayList<>());
                                }
                            }
                        } else if (currentSeller.getCategory() != null && !currentSeller.getCategory().trim().isEmpty()) {
                            String primaryCat = currentSeller.getCategory().trim();
                            if (!sellerCategoryProductsMap.containsKey(primaryCat)) {
                                sellerCategoryProductsMap.put(primaryCat, new ArrayList<>());
                            }
                        }

                        if (sellerCategoryProductsMap.isEmpty()) {
                            for (String cat : Constants.CATEGORIES) {
                                sellerCategoryProductsMap.put(cat, new ArrayList<>());
                            }
                        }

                        setupOrderCategorySpinner();

                        if (singleProductMode && productId != null && (productCategory == null || productCategory.trim().isEmpty())) {
                            FirebaseDatabase.getInstance().getReference().child(Constants.NODE_PRODUCTS).child(productId)
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(DataSnapshot prodSnap) {
                                            Product prod = prodSnap.getValue(Product.class);
                                            if (prod != null && prod.getCategory() != null && !prod.getCategory().trim().isEmpty()) {
                                                productCategory = prod.getCategory().trim();
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

    private boolean isCategoryActiveForSeller(String productCategory) {
        if (productCategory == null || productCategory.trim().isEmpty()) return true;
        if (currentSeller == null) return true;

        String pCat = productCategory.trim();
        List<String> categories = currentSeller.getCategories();
        if (categories != null && !categories.isEmpty()) {
            for (String c : categories) {
                if (c != null && pCat.equalsIgnoreCase(c.trim())) return true;
            }
            return false;
        }

        String primaryCat = currentSeller.getCategory();
        if (primaryCat != null && !primaryCat.trim().isEmpty()) {
            return pCat.equalsIgnoreCase(primaryCat.trim());
        }

        return true;
    }

    private void setupOrderCategorySpinner() {
        if (spinnerOrderCategory == null) return;

        availableOrderCategories.clear();
        availableOrderCategories.addAll(sellerCategoryProductsMap.keySet());

        if (availableOrderCategories.isEmpty()) {
            availableOrderCategories.addAll(Arrays.asList(Constants.CATEGORIES));
        }

        String[] categoryDisplayNames = new String[availableOrderCategories.size()];
        for (int i = 0; i < availableOrderCategories.size(); i++) {
            String cat = availableOrderCategories.get(i);
            List<Product> prods = sellerCategoryProductsMap.get(cat);
            int count = prods != null ? prods.size() : 0;
            categoryDisplayNames[i] = Constants.getUrduLabel(cat) + " / " + cat + " (" + count + " " + (count == 1 ? "product" : "products") + ")";
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categoryDisplayNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerOrderCategory.setAdapter(adapter);

        spinnerOrderCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (isUpdatingCategorySpinner) return;
                if (position >= 0 && position < availableOrderCategories.size()) {
                    String selectedCat = availableOrderCategories.get(position);
                    productCategory = selectedCat;
                    onCategoryChanged(selectedCat);
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void onCategoryChanged(String selectedCat) {
        if (selectedCat == null || selectedCat.isEmpty()) return;
        tvCategoryHeader.setText("📋 " + Constants.getUrduLabel(selectedCat) + " کی تفصیلات");
        buildDynamicForm(selectedCat);
        if (!singleProductMode) {
            buildProductChecklistForCategory(selectedCat);
        }
    }

    private void applyActiveCategory() {
        String category = (productCategory != null && !productCategory.trim().isEmpty())
                ? productCategory.trim()
                : (currentSeller != null ? currentSeller.getCategory() : "Cooking");

        if (category == null || category.isEmpty()) category = "Cooking";

        int selectionIndex = 0;
        for (int i = 0; i < availableOrderCategories.size(); i++) {
            if (availableOrderCategories.get(i).equalsIgnoreCase(category)) {
                selectionIndex = i;
                break;
            }
        }

        isUpdatingCategorySpinner = true;
        if (spinnerOrderCategory != null && selectionIndex < availableOrderCategories.size()) {
            spinnerOrderCategory.setSelection(selectionIndex);
        }
        isUpdatingCategorySpinner = false;

        String selectedCat = (selectionIndex < availableOrderCategories.size())
                ? availableOrderCategories.get(selectionIndex)
                : category;

        onCategoryChanged(selectedCat);
    }

    /** Har Selected Category ke mutabiq uski Apni Products ki list dikhati hai (e.g. Cooking (3), Stitching (2)) */
    private void buildProductChecklistForCategory(String selectedCat) {
        layoutProductChecklist.removeAllViews();
        checklistMap.clear();

        List<Product> products = sellerCategoryProductsMap.get(selectedCat);

        if (products == null || products.isEmpty()) {
            tvChecklistEmpty.setVisibility(View.VISIBLE);
            tvChecklistEmpty.setText("اس زمرے (" + Constants.getUrduLabel(selectedCat) + ") میں فی الحال کوئی چیز موجود نہیں");
            recalcTotal();
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
                if (isChecked) {
                    for (CheckBox otherCb : checklistMap.keySet()) {
                        if (otherCb != cb && otherCb.isChecked()) {
                            otherCb.setChecked(false);
                        }
                    }
                }
                recalcTotal();
            });

            row.addView(cb);
            row.addView(tvPrice);
            layoutProductChecklist.addView(row);

            checklistMap.put(cb, p);
        }

        recalcTotal();
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
                    com.sheempower.app.utils.NotificationHelper.createAndSendOrderNotification(this, order);
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