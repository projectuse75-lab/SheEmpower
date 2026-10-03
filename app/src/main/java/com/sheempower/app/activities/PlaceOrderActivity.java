package com.sheempower.app.activities;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
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
    private final Map<String, String> categoryFilledFormsMap = new HashMap<>();
    private String currentActiveCategory = "";

    private TextView tvProductFormTabHeader;
    private LinearLayout layoutProductFormTabs;
    private final Map<String, String> productFormDetailsMap = new HashMap<>();
    private Product currentlyActiveFormProduct = null;
    private Product currentProduct = null;

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
        tvProductFormTabHeader = findViewById(R.id.tvProductFormTabHeader);
        layoutProductFormTabs  = findViewById(R.id.layoutProductFormTabs);
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

        boolean isDark = isDarkMode();
        int headerColor = isDark ? 0xFFFFFFFF : 0xFF192A56;

        if (tvCategoryHeader != null) tvCategoryHeader.setTextColor(headerColor);
        if (tvChecklistHeader != null) tvChecklistHeader.setTextColor(headerColor);
        if (tvSelectedItem != null) tvSelectedItem.setTextColor(headerColor);

        if (singleProductMode) {
            tvSelectedItem.setText("منتخب کردہ چیز / Selected Item:\n" + productName);
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
                        updateSellerMetaCard(productCategory);
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

    private void updateSellerMetaCard(String activeCategory) {
        if (currentSeller == null) return;

        String displayCategory = (activeCategory != null && !activeCategory.trim().isEmpty())
                ? activeCategory.trim()
                : ((productCategory != null && !productCategory.trim().isEmpty())
                        ? productCategory.trim()
                        : (currentSeller.getCategory() != null ? currentSeller.getCategory() : "Cooking"));

        boolean isDark = isDarkMode();
        int nameColor = isDark ? 0xFFFFFFFF : 0xFF192A56;
        int metaColor = isDark ? 0xFFFFFFFF : 0xFF6B7A99;

        String formattedCategoryLabel = Constants.getCategoryDisplayLabel(this, displayCategory);
        if (tvSellerMeta != null) {
            tvSellerMeta.setText(formattedCategoryLabel + " · ⭐ " + currentSeller.getRating());
            tvSellerMeta.setTextColor(metaColor);
        }
        if (tvSellerName != null) {
            tvSellerName.setText(currentSeller.getBusinessName());
            tvSellerName.setTextColor(nameColor);
        }
    }

    private void saveCurrentFormState() {
        if (currentActiveCategory == null || currentActiveCategory.trim().isEmpty()) return;

        StringBuilder sb = new StringBuilder();
        boolean hasContent = false;

        for (Map.Entry<String, EditText> entry : dynamicTextInputs.entrySet()) {
            String val = entry.getValue().getText().toString().trim();
            if (!val.isEmpty()) {
                hasContent = true;
                sb.append(entry.getKey()).append(": ").append(val).append("\n");
            }
        }
        for (Map.Entry<String, Spinner> entry : dynamicSpinners.entrySet()) {
            Object sel = entry.getValue().getSelectedItem();
            if (sel != null) {
                sb.append(entry.getKey()).append(": ").append(sel).append("\n");
            }
        }
        for (Map.Entry<String, List<CheckBox>> entry : dynamicCheckboxGroups.entrySet()) {
            List<String> chosen = new ArrayList<>();
            for (CheckBox cb : entry.getValue()) {
                if (cb.isChecked()) chosen.add(cb.getText().toString());
            }
            if (!chosen.isEmpty()) {
                sb.append(entry.getKey()).append(": ").append(String.join(", ", chosen)).append("\n");
            }
        }

        if (hasContent) {
            categoryFilledFormsMap.put(currentActiveCategory.trim(), sb.toString().trim());
        }
    }

    private void onCategoryChanged(String selectedCat) {
        if (selectedCat == null || selectedCat.isEmpty()) return;
        saveCurrentFormState();
        currentActiveCategory = selectedCat.trim();

        boolean isDark = isDarkMode();
        int headerColor = isDark ? 0xFFFFFFFF : 0xFF192A56;
        if (tvCategoryHeader != null) {
            tvCategoryHeader.setText("📋 " + selectedCat + " Details\n" + Constants.getUrduLabel(selectedCat) + " کی تفصیلات");
            tvCategoryHeader.setTextColor(headerColor);
        }
        updateSellerMetaCard(selectedCat);
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

    private boolean isDarkMode() {
        PreferencesManager prefs = new PreferencesManager(this);
        return prefs.isDarkThemeEnabled();
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

        boolean isDark = isDarkMode();
        int textColor = isDark ? 0xFFFFFFFF : 0xFF192A56;
        int priceColor = isDark ? 0xFFF7D794 : 0xFF192A56;

        for (Product p : products) {
            LinearLayout row = new LinearLayout(PlaceOrderActivity.this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(20, 16, 20, 16);
            row.setBackgroundResource(R.drawable.bg_skeu_card);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            rowParams.setMargins(0, 8, 0, 8);
            row.setLayoutParams(rowParams);

            CheckBox cb = new CheckBox(PlaceOrderActivity.this);
            cb.setText(p.getName());
            cb.setTextColor(textColor);
            cb.setTextSize(14);
            LinearLayout.LayoutParams cbParams = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            cb.setLayoutParams(cbParams);

            TextView tvPrice = new TextView(PlaceOrderActivity.this);
            tvPrice.setText("PKR " + (long) p.getPrice());
            tvPrice.setTextColor(priceColor);
            tvPrice.setTypeface(null, Typeface.BOLD);

            cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    currentlyActiveFormProduct = p;
                }
                updateProductFormTabs();
                recalcTotal();
            });

            row.addView(cb);
            row.addView(tvPrice);
            layoutProductChecklist.addView(row);

            checklistMap.put(cb, p);
        }

        recalcTotal();
        updateProductFormTabs();
    }

    private void saveCurrentProductFormDetails() {
        if (currentlyActiveFormProduct == null || currentlyActiveFormProduct.getId() == null) return;

        StringBuilder sb = new StringBuilder();
        boolean hasInputs = false;

        for (Map.Entry<String, EditText> entry : dynamicTextInputs.entrySet()) {
            String val = entry.getValue().getText().toString().trim();
            if (!val.isEmpty()) {
                hasInputs = true;
                sb.append(entry.getKey()).append(": ").append(val).append("\n");
            }
        }
        for (Map.Entry<String, Spinner> entry : dynamicSpinners.entrySet()) {
            Object sel = entry.getValue().getSelectedItem();
            if (sel != null) {
                sb.append(entry.getKey()).append(": ").append(sel).append("\n");
            }
        }
        for (Map.Entry<String, List<CheckBox>> entry : dynamicCheckboxGroups.entrySet()) {
            List<String> chosen = new ArrayList<>();
            for (CheckBox cb : entry.getValue()) {
                if (cb.isChecked()) chosen.add(cb.getText().toString());
            }
            if (!chosen.isEmpty()) {
                sb.append(entry.getKey()).append(": ").append(String.join(", ", chosen)).append("\n");
            }
        }

        if (hasInputs) {
            String pId = currentlyActiveFormProduct.getId().trim();
            productFormDetailsMap.put(pId, sb.toString().trim());
        }
    }

    private void updateProductFormTabs() {
        if (layoutProductFormTabs == null) return;
        layoutProductFormTabs.removeAllViews();

        List<Product> checkedProducts = new ArrayList<>();
        if (singleProductMode && currentProduct != null) {
            checkedProducts.add(currentProduct);
        } else {
            for (Map.Entry<CheckBox, Product> entry : checklistMap.entrySet()) {
                if (entry.getKey().isChecked() && entry.getValue() != null) {
                    checkedProducts.add(entry.getValue());
                }
            }
        }

        if (checkedProducts.isEmpty()) {
            if (tvProductFormTabHeader != null) tvProductFormTabHeader.setVisibility(View.GONE);
            layoutProductFormTabs.setVisibility(View.GONE);
            return;
        }

        if (tvProductFormTabHeader != null) tvProductFormTabHeader.setVisibility(View.VISIBLE);
        layoutProductFormTabs.setVisibility(View.VISIBLE);

        for (Product p : checkedProducts) {
            if (p == null || p.getId() == null) continue;
            final String pid = p.getId().trim();
            boolean isCompleted = productFormDetailsMap.containsKey(pid) && !productFormDetailsMap.get(pid).isEmpty();
            boolean isActive = currentlyActiveFormProduct != null && pid.equals(currentlyActiveFormProduct.getId().trim());

            LinearLayout tabRow = new LinearLayout(this);
            tabRow.setOrientation(LinearLayout.HORIZONTAL);
            tabRow.setPadding(20, 16, 20, 16);
            tabRow.setBackgroundResource(isActive ? R.drawable.bg_skeu_button_primary : R.drawable.bg_skeu_card);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            rowParams.setMargins(0, 8, 0, 8);
            tabRow.setLayoutParams(rowParams);

            TextView tvName = new TextView(this);
            tvName.setText("📌 " + p.getName() + " (" + Constants.getCategoryDisplayLabel(this, p.getCategory()) + ")");
            tvName.setTextColor(isActive ? 0xFFFFFFFF : (isDarkMode() ? 0xFFFFFFFF : 0xFF192A56));
            tvName.setTextSize(14);
            tvName.setTypeface(null, Typeface.BOLD);
            LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            tvName.setLayoutParams(nameParams);

            TextView tvStatus = new TextView(this);
            tvStatus.setText(isCompleted ? "✅ Form Completed" : "⏳ Form Pending");
            tvStatus.setTextColor(isCompleted ? 0xFF2ECC71 : (isActive ? 0xFFF7D794 : 0xFFFF6F00));
            tvStatus.setTextSize(12);
            tvStatus.setTypeface(null, Typeface.BOLD);

            tabRow.addView(tvName);
            tabRow.addView(tvStatus);

            tabRow.setOnClickListener(v -> {
                saveCurrentProductFormDetails();
                currentlyActiveFormProduct = p;
                updateProductFormTabs();
                buildDynamicForm(p.getCategory());
            });

            layoutProductFormTabs.addView(tabRow);
        }
    }

    private long getQuantityFromNumberInput() {
        for (Map.Entry<String, EditText> entry : dynamicTextInputs.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) continue;
            String label = entry.getKey().toLowerCase();
            if (label.contains("number of people") || label.contains("quantity") || label.contains("people") || label.contains("suits")) {
                String val = entry.getValue().getText().toString().trim();
                if (!val.isEmpty()) {
                    try {
                        String digits = val.replaceAll("[^0-9]", "");
                        if (!digits.isEmpty()) {
                            long q = Long.parseLong(digits);
                            if (q > 0) return q;
                        }
                    } catch (Exception ignored) {}
                }
            }
        }
        return 1;
    }

    private void recalcTotal() {
        double basePrice = 0;

        if (singleProductMode) {
            basePrice = productPrice;
        } else {
            for (Map.Entry<CheckBox, Product> entry : checklistMap.entrySet()) {
                if (entry.getKey().isChecked()) {
                    basePrice += entry.getValue().getPrice();
                }
            }
        }

        long quantity = getQuantityFromNumberInput();
        double total = basePrice * quantity;

        if (etAmount != null) {
            etAmount.setText(String.valueOf((long) total));
        }

        if (tvAmountLockedNote != null) {
            if (quantity > 1) {
                tvAmountLockedNote.setText("🔒 (PKR " + (long) basePrice + " × " + quantity + " = PKR " + (long) total + ")");
            } else if (singleProductMode) {
                tvAmountLockedNote.setText("🔒 یہ قیمت بیچنے والی نے مقرر کی ہے");
            } else {
                tvAmountLockedNote.setText("🔒 یہ رقم آپ کی منتخب کردہ چیزوں کے مطابق خود بخود بنتی ہے");
            }
        }
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

        if (category == null || category.trim().isEmpty()) return;
        String catName = category.trim();

        List<OrderField> fields = CategoryFormConfig.getFieldsForCategory(catName);
        if (fields.isEmpty()) return;

        boolean isDark = isDarkMode();
        int labelColor = isDark ? 0xFFFFFFFF : 0xFF192A56;
        int inputTextColor = isDark ? 0xFFFFFFFF : 0xFF192A56;
        int hintColor = isDark ? 0xFFB0B8CC : 0xFF888888;

        for (OrderField field : fields) {
            LinearLayout fieldCard = new LinearLayout(this);
            fieldCard.setOrientation(LinearLayout.VERTICAL);
            fieldCard.setPadding(24, 18, 24, 18);
            fieldCard.setBackgroundResource(R.drawable.bg_skeu_card);
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            cardParams.setMargins(0, 14, 0, 0);
            fieldCard.setLayoutParams(cardParams);

            TextView label = new TextView(this);
            label.setText(field.labelEnglish + " / " + field.labelUrdu);
            label.setTextColor(labelColor);
            label.setTextSize(15);
            label.setTypeface(null, Typeface.BOLD);
            label.setPadding(0, 0, 0, 8);
            fieldCard.addView(label);

            if ("spinner".equals(field.type)) {
                Spinner spinner = new Spinner(this);
                ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, field.options);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinner.setAdapter(adapter);
                fieldCard.addView(spinner);
                dynamicSpinners.put(field.labelEnglish, spinner);

            } else if ("multi".equals(field.type)) {
                List<CheckBox> group = new ArrayList<>();
                LinearLayout checkboxContainer = new LinearLayout(this);
                checkboxContainer.setOrientation(LinearLayout.VERTICAL);
                for (String option : field.options) {
                    CheckBox cb = new CheckBox(this);
                    cb.setText(option);
                    cb.setTextColor(labelColor);
                    cb.setTextSize(14);
                    checkboxContainer.addView(cb);
                    group.add(cb);
                }
                fieldCard.addView(checkboxContainer);
                dynamicCheckboxGroups.put(field.labelEnglish, group);

            } else {
                EditText input = new EditText(this);
                input.setHint(field.labelEnglish + " / " + field.hintUrdu);
                input.setHintTextColor(hintColor);
                input.setTextColor(inputTextColor);
                input.setTextSize(15);
                input.setPadding(28, 24, 28, 24);
                input.setBackgroundResource(R.drawable.bg_skeu_input);

                String engLower = field.labelEnglish.toLowerCase();
                boolean isTimeField = engLower.contains("time") || "time".equalsIgnoreCase(field.type);
                boolean isDateField = !isTimeField && (engLower.contains("day") || engLower.contains("date") || engLower.contains("needed by") || "date".equalsIgnoreCase(field.type));

                if (isTimeField) {
                    input.setFocusable(false);
                    input.setClickable(true);
                    input.setHint("⏰ Select Time / " + field.hintUrdu);
                    input.setOnClickListener(v -> showTimePickerDialog(input));
                } else if (isDateField) {
                    input.setFocusable(false);
                    input.setClickable(true);
                    input.setHint("📅 Select Date / " + field.hintUrdu);
                    input.setOnClickListener(v -> showDatePickerDialog(input));
                } else if ("number".equals(field.type)) {
                    input.setInputType(InputType.TYPE_CLASS_NUMBER);
                    input.addTextChangedListener(new TextWatcher() {
                        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                        @Override public void afterTextChanged(Editable s) {
                            recalcTotal();
                        }
                    });
                }
                fieldCard.addView(input);
                dynamicTextInputs.put(field.labelEnglish, input);
            }

            layoutDynamicFields.addView(fieldCard);
        }
    }

    private void submitOrder() {
        saveCurrentProductFormDetails();

        // 1. Identify all checked products in order
        List<Product> checkedProducts = new ArrayList<>();
        if (singleProductMode && currentProduct != null) {
            checkedProducts.add(currentProduct);
        } else {
            for (Map.Entry<CheckBox, Product> entry : checklistMap.entrySet()) {
                if (entry.getKey().isChecked() && entry.getValue() != null) {
                    checkedProducts.add(entry.getValue());
                }
            }
        }

        if (checkedProducts.isEmpty()) {
            Toast.makeText(this, "کم از کم ایک چیز منتخب کریں / Select at least one product", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. MANDATORY PRODUCT FORM CHECK: Enforce that form details exist for EVERY checked product!
        for (Product p : checkedProducts) {
            if (p == null || p.getId() == null) continue;
            String pid = p.getId().trim();

            if (!productFormDetailsMap.containsKey(pid) || productFormDetailsMap.get(pid) == null || productFormDetailsMap.get(pid).trim().isEmpty()) {
                currentlyActiveFormProduct = p;
                updateProductFormTabs();
                buildDynamicForm(p.getCategory());

                String prodName = p.getName() != null ? p.getName() : "Product";
                Toast.makeText(this, "براہ کرم پہلے پروڈکٹ '" + prodName + "' کا فارم بھی پر کریں!", Toast.LENGTH_LONG).show();
                VoiceGuide.explain(this, "براہ کرم پہلے پروڈکٹ " + prodName + " کا فارم پر کریں۔");
                return;
            }
        }

        // 3. Field-level validation for active on-screen input fields
        for (Map.Entry<String, EditText> entry : dynamicTextInputs.entrySet()) {
            EditText input = entry.getValue();
            if (input != null) {
                String val = input.getText().toString().trim();
                if (TextUtils.isEmpty(val)) {
                    input.setError("یہ فارم پر کرنا ضروری ہے / Required");
                    input.requestFocus();
                    Toast.makeText(this, "براہ کرم اس فارم کی تمام تفصیلات پر کریں / Please fill form details", Toast.LENGTH_LONG).show();
                    return;
                }
            }
        }

        String amountStr = etAmount.getText().toString().trim();
        String notes = etNotes.getText().toString().trim();
        boolean needsBuyerAddress = layoutBuyerAddress.getVisibility() == View.VISIBLE;
        String address = needsBuyerAddress ? etAddress.getText().toString().trim()
                : (currentSeller.getShopAddress() != null ? currentSeller.getShopAddress() : currentSeller.getCity());

        String finalProductName = productName;
        if (!singleProductMode) {
            List<String> chosenNames = new ArrayList<>();
            for (Product p : checkedProducts) {
                chosenNames.add(p.getName() + " (PKR " + (long) p.getPrice() + ")");
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

        // Combine all saved product form details for final order details
        StringBuilder detailsBuilder = new StringBuilder();
        for (Product p : checkedProducts) {
            if (p == null || p.getId() == null) continue;
            String pid = p.getId().trim();
            if (productFormDetailsMap.containsKey(pid)) {
                detailsBuilder.append("=== ").append(p.getName()).append(" (").append(p.getCategory()).append(") ===\n");
                detailsBuilder.append(productFormDetailsMap.get(pid)).append("\n\n");
            }
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
    private void showDatePickerDialog(EditText input) {
        if (input == null) return;
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dpd = new DatePickerDialog(this, (view, selectedYear, selectedMonth, selectedDay) -> {
            Calendar selectedCal = Calendar.getInstance();
            selectedCal.set(selectedYear, selectedMonth, selectedDay);

            SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy (EEEE)", Locale.ENGLISH);
            String formattedDate = sdf.format(selectedCal.getTime());

            input.setText(formattedDate);
            input.setError(null);
        }, year, month, day);

        dpd.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        dpd.show();
    }

    private void showTimePickerDialog(EditText input) {
        if (input == null) return;
        final Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);

        TimePickerDialog tpd = new TimePickerDialog(this, (view, selectedHour, selectedMinute) -> {
            String amPm = selectedHour >= 12 ? "PM" : "AM";
            int displayHour = selectedHour % 12;
            if (displayHour == 0) displayHour = 12;

            String formattedTime = String.format(Locale.ENGLISH, "%02d:%02d %s", displayHour, selectedMinute, amPm);

            input.setText(formattedTime);
            input.setError(null);
        }, hour, minute, false);

        tpd.show();
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