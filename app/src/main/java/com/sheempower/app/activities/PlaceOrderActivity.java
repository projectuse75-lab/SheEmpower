package com.sheempower.app.activities;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.*;
import com.sheempower.app.R;
import com.sheempower.app.models.Order;
import com.sheempower.app.models.OrderField;
import com.sheempower.app.models.Product;
import com.sheempower.app.models.Seller;
import com.sheempower.app.utils.ImageCropHelper;
import com.sheempower.app.utils.PreferencesManager;
import com.sheempower.app.utils.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class PlaceOrderActivity extends AppCompatActivity {

    public static final String EXTRA_SELLER_ID        = "sellerId";
    public static final String EXTRA_PRODUCT_ID       = "productId";
    public static final String EXTRA_PRODUCT_NAME     = "productName";
    public static final String EXTRA_PRODUCT_PRICE    = "productPrice";
    public static final String EXTRA_PRODUCT_CATEGORY = "productCategory";
    public static final String EXTRA_CART_CHECKED_IDS = "cartCheckedProductIds";

    private ArrayList<String> cartCheckedProductIds = null;

    private static final int REQUEST_PICK_SAMPLE_IMAGE = 1005;

    private TextView tvSellerName, tvSellerMeta, tvSellerVerified, tvSelectedItem, tvDeliveryInfo,
            tvCategoryHeader, tvAmountLockedNote, tvChecklistHeader, tvChecklistEmpty, tvPageIndicator,
            tvProductFormTabHeader;
    private Spinner spinnerOrderCategory;
    private LinearLayout layoutDynamicFields, layoutBuyerAddress, layoutProductChecklist, layoutWizardNav, layoutProductFormTabs, cardMultiCategoryGuide;
    private TextInputEditText etAmount, etAddress, etNotes;
    private Button btnSubmitOrder, btnPrevCategory, btnNextCategory, btnBulkWhatsApp, btnOnlyThisOrder, btnBrowseMore;

    private String sellerId, productId, productName, productCategory;
    private double productPrice;
    private Seller currentSeller;
    private SessionManager session;
    private String resolvedDeliveryMethod = Constants.DELIVERY_METHOD_PICKUP;
    private boolean isUpdatingCategorySpinner = false;
    private String currentActiveCategory = "";
    private boolean isOnlyThisOrderSelected = false;

    /** Single product mode when opened directly from a product card */
    private boolean singleProductMode;
    private String initialLockedProductId = null;
    private Product initialProduct = null;

    private final Map<String, EditText> dynamicTextInputs = new HashMap<>();
    private final Map<String, Spinner> dynamicSpinners = new HashMap<>();
    private final Map<String, List<CheckBox>> dynamicCheckboxGroups = new HashMap<>();

    /** Product-by-Product Form Details: Product ID -> Form Text Breakdown */
    private final Map<String, String> productFormDetailsMap = new HashMap<>();

    /** Product-by-Product Form Widget State: Product ID -> Map<FieldLabel, Value> */
    private final Map<String, Map<String, Object>> productWidgetInputsMap = new HashMap<>();

    /** Product-by-Product Sample Images: Product ID -> Cloudinary URL */
    private final Map<String, String> productFormImagesMap = new HashMap<>();

    private RadioGroup rgPaymentMethod;
    private RadioButton rbPaymentCOD, rbPaymentEasyPaisa, rbPaymentJazzCash;
    private LinearLayout layoutPaymentProofSection;
    private TextView tvSellerAccountInfo, tvPaymentProofStatus;
    private Button btnPickPaymentProof;
    private ImageView ivPaymentProofPreview;

    private String selectedPaymentMethod = "cod";
    private String selectedPaymentStatus = "pending";
    private String uploadedPaymentProofUrl = null;
    private Uri pendingPaymentProofUri = null;

    private final ActivityResultLauncher<String> paymentProofPicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    ImageCropHelper.cropImage(this, uri, croppedUri -> {
                        pendingPaymentProofUri = croppedUri;
                        if (ivPaymentProofPreview != null) {
                            ivPaymentProofPreview.setImageURI(croppedUri);
                            ivPaymentProofPreview.setVisibility(View.VISIBLE);
                        }
                        if (tvPaymentProofStatus != null) {
                            tvPaymentProofStatus.setText("✅ تصویر منتخب ہو گئی / Screenshot selected");
                        }
                    });
                }
            });

    /** Currently active product whose form is being filled */
    private Product currentlyActiveFormProduct = null;

    /** Products grouped by category for this seller */
    private final Map<String, List<Product>> sellerCategoryProductsMap = new LinkedHashMap<>();
    private final List<String> availableOrderCategories = new ArrayList<>();

    /** Checked product IDs across ALL categories */
    private final Set<String> checkedProductIds = new HashSet<>();

    /** Quantity per product ID */
    private final Map<String, Integer> productQuantitiesMap = new HashMap<>();

    private int currentCategoryIndex = 0;

    // View references for sample image upload card in active form
    private ImageView ivSamplePreview;
    private TextView tvSampleStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_place_order);

        session = new SessionManager(this);

        if (getIntent() != null) {
            cartCheckedProductIds = getIntent().getStringArrayListExtra(EXTRA_CART_CHECKED_IDS);
            sellerId         = getIntent().getStringExtra(EXTRA_SELLER_ID);
            if (sellerId != null) sellerId = sellerId.trim();
            productId        = getIntent().getStringExtra(EXTRA_PRODUCT_ID);
            if (productId != null) productId = productId.trim();
            productName      = getIntent().getStringExtra(EXTRA_PRODUCT_NAME);
            productPrice     = getIntent().getDoubleExtra(EXTRA_PRODUCT_PRICE, 0);
            productCategory  = getIntent().getStringExtra(EXTRA_PRODUCT_CATEGORY);
        }
        singleProductMode = (productId != null && !productId.isEmpty());
        if (singleProductMode) {
            initialLockedProductId = productId;
            checkedProductIds.add(initialLockedProductId);
            productQuantitiesMap.put(initialLockedProductId, 1);

            initialProduct = new Product();
            initialProduct.setId(productId);
            initialProduct.setName(productName != null ? productName : "Product");
            initialProduct.setPrice(productPrice);
            initialProduct.setCategory(productCategory != null ? productCategory : "Cooking");
            initialProduct.setSellerId(sellerId);
            currentlyActiveFormProduct = initialProduct;
        }

        tvSellerName        = findViewById(R.id.tvSellerName);
        tvSellerMeta        = findViewById(R.id.tvSellerMeta);
        tvSellerVerified    = findViewById(R.id.tvSellerVerified);
        tvSelectedItem      = findViewById(R.id.tvSelectedItem);
        tvDeliveryInfo      = findViewById(R.id.tvDeliveryInfo);
        tvCategoryHeader    = findViewById(R.id.tvCategoryHeader);
        tvPageIndicator     = findViewById(R.id.tvPageIndicator);
        tvProductFormTabHeader = findViewById(R.id.tvProductFormTabHeader);
        layoutProductFormTabs  = findViewById(R.id.layoutProductFormTabs);
        spinnerOrderCategory = findViewById(R.id.spinnerOrderCategory);
        tvAmountLockedNote  = findViewById(R.id.tvAmountLockedNote);
        tvChecklistHeader   = findViewById(R.id.tvChecklistHeader);
        tvChecklistEmpty    = findViewById(R.id.tvChecklistEmpty);
        layoutDynamicFields = findViewById(R.id.layoutDynamicFields);
        layoutBuyerAddress  = findViewById(R.id.layoutBuyerAddress);
        layoutProductChecklist = findViewById(R.id.layoutProductChecklist);
        layoutWizardNav     = findViewById(R.id.layoutWizardNav);
        btnOnlyThisOrder    = findViewById(R.id.btnOnlyThisOrder);
        btnBrowseMore       = findViewById(R.id.btnBrowseMore);
        btnBulkWhatsApp     = findViewById(R.id.btnBulkWhatsApp);
        etAmount            = findViewById(R.id.etAmount);
        etAddress           = findViewById(R.id.etAddress);
        etNotes             = findViewById(R.id.etNotes);
        btnSubmitOrder      = findViewById(R.id.btnSubmitOrder);

        rgPaymentMethod           = findViewById(R.id.rgPaymentMethod);
        rbPaymentCOD              = findViewById(R.id.rbPaymentCOD);
        rbPaymentEasyPaisa        = findViewById(R.id.rbPaymentEasyPaisa);
        rbPaymentJazzCash         = findViewById(R.id.rbPaymentJazzCash);
        layoutPaymentProofSection = findViewById(R.id.layoutPaymentProofSection);
        tvSellerAccountInfo       = findViewById(R.id.tvSellerAccountInfo);
        tvPaymentProofStatus      = findViewById(R.id.tvPaymentProofStatus);
        btnPickPaymentProof       = findViewById(R.id.btnPickPaymentProof);
        ivPaymentProofPreview     = findViewById(R.id.ivPaymentProofPreview);

        if (btnPickPaymentProof != null) {
            btnPickPaymentProof.setOnClickListener(v -> paymentProofPicker.launch("image/*"));
        }

        setupPaymentMethodSelection();

        if (!session.isLoggedIn()) {
            Toast.makeText(this, "پہلے لاگ ان کریں", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        if (sellerId == null || sellerId.isEmpty()) {
            Toast.makeText(this, "بیچنے والی نہیں ملی", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        BottomNavHelper.setupBackButton(this);

        // Price input is strictly locked — auto-calculated from selected items and quantities
        etAmount.setEnabled(false);
        etAmount.setFocusable(false);
        tvAmountLockedNote.setVisibility(View.VISIBLE);

        ImageButton btnSpeaker = findViewById(R.id.btnSpeaker);
        VoiceGuide.setupSpeakerButton(btnSpeaker, this,
                singleProductMode
                        ? "یہ آرڈر کی سکرین ہے۔ آپ کی منتخب کردہ چیز شامل کر دی گئی ہے۔ اس چیز کا فارم مکمل پر کریں اور جمع کروائیں۔ اگر کوئی اور چیز نہیں چاہیے تو دیگر چیزیں نہیں چاہیے کا بٹن دبائیں۔"
                        : "یہ آرڈر کی سکرین ہے۔ نیچے دی گئی فہرست میں سے وہ چیزیں منتخب کریں جو آپ منگوانا چاہتی ہیں، اور ہر چیز کا فارم مکمل پر کریں۔"
        );

        boolean isDark = isDarkMode();
        int headerColor = isDark ? 0xFFFFFFFF : 0xFF192A56;

        if (tvCategoryHeader != null) tvCategoryHeader.setTextColor(headerColor);
        if (tvChecklistHeader != null) tvChecklistHeader.setTextColor(headerColor);
        if (tvSelectedItem != null) tvSelectedItem.setTextColor(headerColor);

        if (singleProductMode) {
            tvSelectedItem.setText("📌 منتخب کردہ پروڈکٹ / Selected Item:\n" + productName + " (PKR " + (long) productPrice + ")");
            tvSelectedItem.setVisibility(View.VISIBLE);
            tvChecklistHeader.setVisibility(View.VISIBLE);
            etAmount.setText(String.valueOf((long) productPrice));
            tvAmountLockedNote.setText("🔒 یہ قیمت آپ کی منتخب کردہ چیزوں اور تعداد کے مطابق خود بخود بنتی ہے");
        } else {
            VoiceGuide.announceScreen(this,
                    "یہ آرڈر کی سکرین ہے۔ نیچے دی گئی فہرست میں سے وہ چیزیں منتخب کریں جو آپ منگوانا چاہتی ہیں، منتخب کردہ چیزوں کی کل قیمت خود بخود جمع ہو جائے گی۔");
            tvSelectedItem.setVisibility(View.GONE);
            tvChecklistHeader.setVisibility(View.VISIBLE);
            etAmount.setText("0");
            tvAmountLockedNote.setText("🔒 یہ رقم آپ کی منتخب کردہ چیزوں کے مطابق خود بخود بنتی ہے");
        }

        setupWizardNavListeners();
        setupMultiCategoryGuideButtons();
        setupBulkWhatsAppButton();
        loadSellerAndBuildForm();
        btnSubmitOrder.setOnClickListener(v -> submitOrder());
    }

    private void setupMultiCategoryGuideButtons() {
        if (btnOnlyThisOrder != null) {
            btnOnlyThisOrder.setOnClickListener(v -> {
                isOnlyThisOrderSelected = true;
                if (cardMultiCategoryGuide != null) cardMultiCategoryGuide.setVisibility(View.GONE);
                if (layoutWizardNav != null) layoutWizardNav.setVisibility(View.GONE);
                if (spinnerOrderCategory != null) spinnerOrderCategory.setVisibility(View.GONE);
                if (tvPageIndicator != null) tvPageIndicator.setVisibility(View.GONE);

                Toast.makeText(this, "صرف یہی آرڈر فائنل کر دیا گیا۔ نیچے فارم پر کر کے 'جمع کروائیں' دبائیں! ✅", Toast.LENGTH_LONG).show();
                VoiceGuide.explain(this, "آپ نے منتخب کردہ آرڈر فائنل کر لیا ہے۔ نیچے فارم پر کر کے آرڈر جمع کروائیں۔");
            });
        }
        if (btnBrowseMore != null) {
            btnBrowseMore.setOnClickListener(v -> {
                isOnlyThisOrderSelected = false;
                if (currentCategoryIndex < availableOrderCategories.size() - 1) {
                    spinnerOrderCategory.setSelection(currentCategoryIndex + 1);
                } else {
                    Toast.makeText(this, "آپ آخری کیٹیگری کے صفحے پر ہیں", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void setupBulkWhatsAppButton() {
        if (btnBulkWhatsApp != null) {
            btnBulkWhatsApp.setOnClickListener(v -> {
                if (currentSeller != null && currentSeller.getWhatsappNumber() != null) {
                    String msg = "السلام علیکم! میں آپ سے ایک بڑا یا خاص آرڈر بنوانے کی تفصیلات معلوم کرنا چاہتی ہوں۔";
                    WhatsAppHelper.openChat(this, currentSeller.getWhatsappNumber(), msg);
                } else {
                    Toast.makeText(this, "بیچنے والی کا واٹس ایپ نمبر دستیاب نہیں ہے", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void setupWizardNavListeners() {
        if (btnPrevCategory != null) {
            btnPrevCategory.setOnClickListener(v -> {
                if (currentCategoryIndex > 0) {
                    spinnerOrderCategory.setSelection(currentCategoryIndex - 1);
                }
            });
        }
        if (btnNextCategory != null) {
            btnNextCategory.setOnClickListener(v -> {
                if (currentCategoryIndex < availableOrderCategories.size() - 1) {
                    spinnerOrderCategory.setSelection(currentCategoryIndex + 1);
                }
            });
        }
    }

    private void loadSellerAndBuildForm() {
        final String targetSellerId = sellerId.trim();

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

                        // Ensure initial product exists in map if launched in singleProductMode
                        if (singleProductMode && productId != null) {
                            boolean found = false;
                            for (List<Product> list : sellerCategoryProductsMap.values()) {
                                for (Product p : list) {
                                    if (p != null && productId.equalsIgnoreCase(p.getId())) {
                                        currentlyActiveFormProduct = p;
                                        found = true;
                                        break;
                                    }
                                }
                                if (found) break;
                            }
                            if (!found && initialProduct != null) {
                                String cat = initialProduct.getCategory();
                                List<Product> catList = sellerCategoryProductsMap.get(cat);
                                if (catList == null) {
                                    catList = new ArrayList<>();
                                    sellerCategoryProductsMap.put(cat, catList);
                                }
                                catList.add(initialProduct);
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

                        // Filter categories to active seller categories
                        Map<String, List<Product>> filteredMap = new LinkedHashMap<>();
                        for (Map.Entry<String, List<Product>> entry : sellerCategoryProductsMap.entrySet()) {
                            if (isCategoryActiveForSeller(entry.getKey())) {
                                filteredMap.put(entry.getKey(), entry.getValue());
                            }
                        }
                        sellerCategoryProductsMap.clear();
                        sellerCategoryProductsMap.putAll(filteredMap);

                        // Add empty categories registered by seller
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

                        if (cartCheckedProductIds != null && !cartCheckedProductIds.isEmpty() && !singleProductMode) {
                            checkedProductIds.clear();
                            for (String cPid : cartCheckedProductIds) {
                                if (cPid != null && !cPid.trim().isEmpty()) {
                                    checkedProductIds.add(cPid.trim());
                                    if (!productQuantitiesMap.containsKey(cPid.trim())) {
                                        productQuantitiesMap.put(cPid.trim(), 1);
                                    }
                                }
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

    private void onCategoryChanged(String selectedCat) {
        if (selectedCat == null || selectedCat.isEmpty()) return;

        saveCurrentProductFormDetails();
        currentActiveCategory = selectedCat.trim();

        int idx = availableOrderCategories.indexOf(currentActiveCategory);
        if (idx >= 0) currentCategoryIndex = idx;

        boolean isDark = isDarkMode();
        int headerColor = isDark ? 0xFFFFFFFF : 0xFF192A56;
        if (tvCategoryHeader != null) {
            tvCategoryHeader.setText("📋 " + selectedCat + " Details\n" + Constants.getUrduLabel(selectedCat) + " کی تفصیلات");
            tvCategoryHeader.setTextColor(headerColor);
        }

        if (isOnlyThisOrderSelected) {
            if (cardMultiCategoryGuide != null) cardMultiCategoryGuide.setVisibility(View.GONE);
            if (layoutWizardNav != null) layoutWizardNav.setVisibility(View.GONE);
            if (spinnerOrderCategory != null) spinnerOrderCategory.setVisibility(View.GONE);
            if (tvPageIndicator != null) tvPageIndicator.setVisibility(View.GONE);
        } else {
            if (spinnerOrderCategory != null) spinnerOrderCategory.setVisibility(View.VISIBLE);
            if (cardMultiCategoryGuide != null) {
                cardMultiCategoryGuide.setVisibility(availableOrderCategories.size() > 1 ? View.VISIBLE : View.GONE);
            }

            if (tvPageIndicator != null && availableOrderCategories.size() > 1) {
                tvPageIndicator.setVisibility(View.VISIBLE);
                String urduCat = Constants.getUrduLabel(selectedCat);
                tvPageIndicator.setText("صفحہ " + (currentCategoryIndex + 1) + " از " + availableOrderCategories.size() + " — " + urduCat + " / " + selectedCat);
            } else if (tvPageIndicator != null) {
                tvPageIndicator.setVisibility(View.GONE);
            }

            if (layoutWizardNav != null && availableOrderCategories.size() > 1) {
                layoutWizardNav.setVisibility(View.VISIBLE);
                if (btnPrevCategory != null) {
                    btnPrevCategory.setVisibility(currentCategoryIndex > 0 ? View.VISIBLE : View.GONE);
                }
                if (btnNextCategory != null) {
                    btnNextCategory.setVisibility(currentCategoryIndex < availableOrderCategories.size() - 1 ? View.VISIBLE : View.GONE);
                }
            } else if (layoutWizardNav != null) {
                layoutWizardNav.setVisibility(View.GONE);
            }
        }

        updateSellerMetaCard(selectedCat);
        buildProductChecklistForCategory(selectedCat);
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

    private List<Product> getAllCheckedProducts() {
        List<Product> checkedList = new ArrayList<>();
        for (List<Product> prods : sellerCategoryProductsMap.values()) {
            if (prods == null) continue;
            for (Product p : prods) {
                if (p != null && p.getId() != null && checkedProductIds.contains(p.getId().trim())) {
                    checkedList.add(p);
                }
            }
        }
        return checkedList;
    }

    private void updateProductFormTabs() {
        if (layoutProductFormTabs == null) return;
        layoutProductFormTabs.removeAllViews();

        List<Product> checkedProducts = getAllCheckedProducts();

        if (checkedProducts.isEmpty()) {
            if (tvProductFormTabHeader != null) tvProductFormTabHeader.setVisibility(View.GONE);
            layoutProductFormTabs.setVisibility(View.GONE);
            layoutDynamicFields.removeAllViews();
            return;
        }

        if (tvProductFormTabHeader != null) tvProductFormTabHeader.setVisibility(View.VISIBLE);
        layoutProductFormTabs.setVisibility(View.VISIBLE);

        // Auto-select active product if null or not checked
        if (currentlyActiveFormProduct == null || !checkedProductIds.contains(currentlyActiveFormProduct.getId().trim())) {
            currentlyActiveFormProduct = checkedProducts.get(0);
        }

        boolean isDark = isDarkMode();

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
            tvName.setTextColor(isActive ? 0xFFFFFFFF : (isDark ? 0xFFFFFFFF : 0xFF192A56));
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
                restoreProductFormDetails(p.getId());
            });

            layoutProductFormTabs.addView(tabRow);
        }

        buildDynamicForm(currentlyActiveFormProduct.getCategory());
        restoreProductFormDetails(currentlyActiveFormProduct.getId());
    }

    private void buildProductChecklistForCategory(String selectedCat) {
        layoutProductChecklist.removeAllViews();

        List<Product> products = sellerCategoryProductsMap.get(selectedCat);

        if (products == null || products.isEmpty()) {
            tvChecklistEmpty.setVisibility(View.VISIBLE);
            tvChecklistEmpty.setText("اس زمرے (" + Constants.getUrduLabel(selectedCat) + ") میں فی الحال کوئی چیز موجود نہیں");
            recalcTotal();
            updateProductFormTabs();
            return;
        }
        tvChecklistEmpty.setVisibility(View.GONE);

        boolean isDark = isDarkMode();
        int textColor = isDark ? 0xFFFFFFFF : 0xFF192A56;
        int priceColor = isDark ? 0xFFF7D794 : 0xFF192A56;

        for (Product p : products) {
            if (p == null || p.getId() == null) continue;
            final String pid = p.getId().trim();
            final boolean isInitialProduct = singleProductMode && pid.equalsIgnoreCase(initialLockedProductId);

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

            boolean isChecked = checkedProductIds.contains(pid) || isInitialProduct;
            if (isChecked) {
                checkedProductIds.add(pid);
                if (!productQuantitiesMap.containsKey(pid)) {
                    productQuantitiesMap.put(pid, 1);
                }
            }
            cb.setChecked(isChecked);

            if (isInitialProduct) {
                cb.setEnabled(false); // Keeps the main clicked item checked
            }

            TextView tvPrice = new TextView(PlaceOrderActivity.this);
            tvPrice.setText("PKR " + (long) p.getPrice());
            tvPrice.setTextColor(priceColor);
            tvPrice.setTypeface(null, Typeface.BOLD);
            tvPrice.setPadding(12, 0, 12, 0);

            // Quantity Stepper ( ➖ 1 ➕ ) — Always active for increasing/decreasing quantities!
            LinearLayout stepperLayout = new LinearLayout(PlaceOrderActivity.this);
            stepperLayout.setOrientation(LinearLayout.HORIZONTAL);
            stepperLayout.setGravity(Gravity.CENTER_VERTICAL);

            Button btnMinus = new Button(PlaceOrderActivity.this);
            btnMinus.setText("➖");
            btnMinus.setTextSize(12);
            btnMinus.setPadding(0, 0, 0, 0);
            LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                    dpToPx(36), dpToPx(36));
            btnMinus.setLayoutParams(btnParams);

            TextView tvQty = new TextView(PlaceOrderActivity.this);
            int currentQty = productQuantitiesMap.getOrDefault(pid, 1);
            tvQty.setText(String.valueOf(currentQty));
            tvQty.setTextColor(textColor);
            tvQty.setTypeface(null, Typeface.BOLD);
            tvQty.setTextSize(14);
            tvQty.setPadding(12, 0, 12, 0);

            Button btnPlus = new Button(PlaceOrderActivity.this);
            btnPlus.setText("➕");
            btnPlus.setTextSize(12);
            btnPlus.setPadding(0, 0, 0, 0);
            btnPlus.setLayoutParams(btnParams);

            cb.setOnCheckedChangeListener((buttonView, checked) -> {
                if (checked) {
                    checkedProductIds.add(pid);
                    if (!productQuantitiesMap.containsKey(pid)) {
                        productQuantitiesMap.put(pid, 1);
                    }
                    currentlyActiveFormProduct = p;
                } else {
                    checkedProductIds.remove(pid);
                    productFormDetailsMap.remove(pid);
                    productFormImagesMap.remove(pid);
                    productWidgetInputsMap.remove(pid);
                }
                tvQty.setText(String.valueOf(productQuantitiesMap.getOrDefault(pid, 1)));
                recalcTotal();
                updateProductFormTabs();
            });

            btnPlus.setOnClickListener(v -> {
                int q = productQuantitiesMap.getOrDefault(pid, 1) + 1;
                productQuantitiesMap.put(pid, q);
                checkedProductIds.add(pid);
                cb.setChecked(true);
                currentlyActiveFormProduct = p;
                tvQty.setText(String.valueOf(q));
                recalcTotal();
                updateProductFormTabs();
            });

            btnMinus.setOnClickListener(v -> {
                int q = productQuantitiesMap.getOrDefault(pid, 1);
                if (q > 1) {
                    q--;
                    productQuantitiesMap.put(pid, q);
                    tvQty.setText(String.valueOf(q));
                } else if (!isInitialProduct) {
                    cb.setChecked(false);
                    checkedProductIds.remove(pid);
                    productFormDetailsMap.remove(pid);
                    productFormImagesMap.remove(pid);
                    productWidgetInputsMap.remove(pid);
                }
                recalcTotal();
                updateProductFormTabs();
            });

            stepperLayout.addView(btnMinus);
            stepperLayout.addView(tvQty);
            stepperLayout.addView(btnPlus);

            row.addView(cb);
            row.addView(tvPrice);
            row.addView(stepperLayout);
            layoutProductChecklist.addView(row);
        }

        recalcTotal();
        updateProductFormTabs();
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private void recalcTotal() {
        double total = 0;
        int totalItemsCount = 0;

        for (Map.Entry<String, List<Product>> entry : sellerCategoryProductsMap.entrySet()) {
            if (entry.getValue() == null) continue;
            for (Product p : entry.getValue()) {
                if (p != null && p.getId() != null) {
                    String pid = p.getId().trim();
                    if (checkedProductIds.contains(pid)) {
                        int qty = productQuantitiesMap.getOrDefault(pid, 1);
                        total += p.getPrice() * qty;
                        totalItemsCount += qty;
                    }
                }
            }
        }

        if (etAmount != null) {
            etAmount.setText(String.valueOf((long) total));
        }

        if (tvAmountLockedNote != null) {
            if (checkedProductIds.isEmpty()) {
                tvAmountLockedNote.setText("🔒 کوئی چیز منتخب نہیں ہے");
            } else {
                tvAmountLockedNote.setText("🔒 کل رقم: PKR " + (long) total + " (" + totalItemsCount + " items)");
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

    private void saveCurrentProductFormDetails() {
        if (currentlyActiveFormProduct == null || currentlyActiveFormProduct.getId() == null) return;
        String pid = currentlyActiveFormProduct.getId().trim();

        StringBuilder sb = new StringBuilder();
        boolean hasInputs = false;
        Map<String, Object> widgetValues = new HashMap<>();

        for (Map.Entry<String, EditText> entry : dynamicTextInputs.entrySet()) {
            String val = entry.getValue().getText().toString().trim();
            widgetValues.put(entry.getKey(), val);
            if (!val.isEmpty()) {
                hasInputs = true;
                sb.append(entry.getKey()).append(": ").append(val).append("\n");
            }
        }
        for (Map.Entry<String, Spinner> entry : dynamicSpinners.entrySet()) {
            int selIndex = entry.getValue().getSelectedItemPosition();
            widgetValues.put(entry.getKey(), selIndex);
            Object selObj = entry.getValue().getSelectedItem();
            if (selObj != null) {
                sb.append(entry.getKey()).append(": ").append(selObj).append("\n");
            }
        }
        for (Map.Entry<String, List<CheckBox>> entry : dynamicCheckboxGroups.entrySet()) {
            List<String> chosen = new ArrayList<>();
            for (CheckBox cb : entry.getValue()) {
                if (cb.isChecked()) chosen.add(cb.getText().toString());
            }
            widgetValues.put(entry.getKey(), chosen);
            if (!chosen.isEmpty()) {
                sb.append(entry.getKey()).append(": ").append(String.join(", ", chosen)).append("\n");
            }
        }

        productWidgetInputsMap.put(pid, widgetValues);

        if (hasInputs) {
            productFormDetailsMap.put(pid, sb.toString().trim());
        }
    }

    private void restoreProductFormDetails(String pid) {
        if (pid == null || pid.isEmpty()) return;
        Map<String, Object> widgetValues = productWidgetInputsMap.get(pid.trim());
        if (widgetValues == null) return;

        for (Map.Entry<String, EditText> entry : dynamicTextInputs.entrySet()) {
            Object val = widgetValues.get(entry.getKey());
            if (val instanceof String && entry.getValue() != null) {
                entry.getValue().setText((String) val);
            }
        }
        for (Map.Entry<String, Spinner> entry : dynamicSpinners.entrySet()) {
            Object val = widgetValues.get(entry.getKey());
            if (val instanceof Integer && entry.getValue() != null) {
                entry.getValue().setSelection((Integer) val);
            }
        }
        for (Map.Entry<String, List<CheckBox>> entry : dynamicCheckboxGroups.entrySet()) {
            Object val = widgetValues.get(entry.getKey());
            if (val instanceof List && entry.getValue() != null) {
                @SuppressWarnings("unchecked")
                List<String> chosenList = (List<String>) val;
                for (CheckBox cb : entry.getValue()) {
                    cb.setChecked(chosenList.contains(cb.getText().toString()));
                }
            }
        }
    }

    private void buildDynamicForm(String category) {
        layoutDynamicFields.removeAllViews();
        dynamicTextInputs.clear();
        dynamicSpinners.clear();
        dynamicCheckboxGroups.clear();

        if (currentlyActiveFormProduct == null) return;

        if (category == null || category.trim().isEmpty()) category = currentlyActiveFormProduct.getCategory();
        String catName = category.trim();

        List<OrderField> fields = CategoryFormConfig.getFieldsForCategory(catName);

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
                }
                fieldCard.addView(input);
                dynamicTextInputs.put(field.labelEnglish, input);
            }

            layoutDynamicFields.addView(fieldCard);
        }

        // Optional Sample Design Image Upload Card
        LinearLayout imgCard = new LinearLayout(this);
        imgCard.setOrientation(LinearLayout.VERTICAL);
        imgCard.setPadding(24, 18, 24, 18);
        imgCard.setBackgroundResource(R.drawable.bg_skeu_card);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 14, 0, 0);
        imgCard.setLayoutParams(cardParams);

        TextView tvImgLabel = new TextView(this);
        tvImgLabel.setText("📸 ڈیزائن کا نمونہ یا نمونہ تصویر (اختیاری) / Sample Design Image (Optional)");
        tvImgLabel.setTextColor(labelColor);
        tvImgLabel.setTextSize(15);
        tvImgLabel.setTypeface(null, Typeface.BOLD);
        tvImgLabel.setPadding(0, 0, 0, 8);
        imgCard.addView(tvImgLabel);

        Button btnUploadImg = new Button(this);
        btnUploadImg.setText("🖼️ تصویر منتخب کریں / Choose Sample Image");
        btnUploadImg.setTextColor(0xFFFFFFFF);
        btnUploadImg.setBackgroundResource(R.drawable.bg_skeu_button_primary);
        btnUploadImg.setPadding(20, 12, 20, 12);
        imgCard.addView(btnUploadImg);

        tvSampleStatus = new TextView(this);
        tvSampleStatus.setPadding(0, 8, 0, 8);
        tvSampleStatus.setTextSize(12);
        tvSampleStatus.setTextColor(hintColor);

        ivSamplePreview = new ImageView(this);
        LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(dpToPx(120), dpToPx(120));
        previewParams.setMargins(0, 8, 0, 0);
        ivSamplePreview.setLayoutParams(previewParams);
        ivSamplePreview.setScaleType(ImageView.ScaleType.CENTER_CROP);
        ivSamplePreview.setVisibility(View.GONE);

        String activePid = currentlyActiveFormProduct.getId().trim();
        if (productFormImagesMap.containsKey(activePid)) {
            String imgUrl = productFormImagesMap.get(activePid);
            if (imgUrl != null && !imgUrl.isEmpty()) {
                tvSampleStatus.setText("✅ تصویر اپلوڈ ہو گئی / Sample Image Uploaded");
                ivSamplePreview.setVisibility(View.VISIBLE);
                Glide.with(this).load(imgUrl).into(ivSamplePreview);
            }
        } else {
            tvSampleStatus.setText("کوئی تصویر منتخب نہیں ہے (اختیاری)");
        }

        btnUploadImg.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(Intent.createChooser(intent, "Select Sample Image"), REQUEST_PICK_SAMPLE_IMAGE);
        });

        imgCard.addView(tvSampleStatus);
        imgCard.addView(ivSamplePreview);
        layoutDynamicFields.addView(imgCard);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PICK_SAMPLE_IMAGE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri originalUri = data.getData();
            if (currentlyActiveFormProduct != null && currentlyActiveFormProduct.getId() != null) {
                final String pid = currentlyActiveFormProduct.getId().trim();
                ImageCropHelper.cropImage(this, originalUri, croppedUri -> {
                    if (tvSampleStatus != null) tvSampleStatus.setText("⏳ تصویر اپلوڈ ہو رہی ہے... / Uploading sample image...");

                    CloudinaryHelper.upload(this, croppedUri, "order_samples", (url, error) -> {
                        if (url != null) {
                            productFormImagesMap.put(pid, url);
                            if (tvSampleStatus != null) tvSampleStatus.setText("✅ تصویر اپلوڈ ہو گئی / Sample Image Uploaded");
                            if (ivSamplePreview != null) {
                                ivSamplePreview.setVisibility(View.VISIBLE);
                                Glide.with(PlaceOrderActivity.this).load(url).into(ivSamplePreview);
                            }
                            Toast.makeText(PlaceOrderActivity.this, "نمونہ تصویر کامیابی سے اپلوڈ ہو گئی! ✅", Toast.LENGTH_SHORT).show();
                        } else {
                            if (tvSampleStatus != null) tvSampleStatus.setText("❌ تصویر اپلوڈ نہ ہو سکی: " + error);
                            Toast.makeText(PlaceOrderActivity.this, "تصویر اپلوڈ میں خرابی: " + error, Toast.LENGTH_LONG).show();
                        }
                    });
                });
            }
        }
    }

    private void submitOrder() {
        saveCurrentProductFormDetails();

        List<Product> checkedProducts = getAllCheckedProducts();

        if (checkedProducts.isEmpty()) {
            Toast.makeText(this, "کم از کم ایک چیز منتخب کریں / Select at least one product", Toast.LENGTH_SHORT).show();
            VoiceGuide.explain(this, "کم از کم ایک چیز منتخب کریں۔");
            return;
        }

        // MANDATORY PRODUCT FORM CHECK: Enforce that form details exist for EVERY checked product!
        for (Product p : checkedProducts) {
            if (p == null || p.getId() == null) continue;
            String pid = p.getId().trim();

            if (!productFormDetailsMap.containsKey(pid) || productFormDetailsMap.get(pid) == null || productFormDetailsMap.get(pid).trim().isEmpty()) {
                currentlyActiveFormProduct = p;
                updateProductFormTabs();
                buildDynamicForm(p.getCategory());
                restoreProductFormDetails(pid);

                String prodName = p.getName() != null ? p.getName() : "Product";
                Toast.makeText(this, "براہ کرم پہلے پروڈکٹ '" + prodName + "' کا فارم بھی پر کریں!", Toast.LENGTH_LONG).show();
                VoiceGuide.explain(this, "براہ کرم پہلے پروڈکٹ " + prodName + " کا فارم پر کریں۔");
                return;
            }
        }

        // Address validation
        boolean needsBuyerAddress = layoutBuyerAddress.getVisibility() == View.VISIBLE;
        String address = needsBuyerAddress ? etAddress.getText().toString().trim()
                : (currentSeller.getShopAddress() != null ? currentSeller.getShopAddress() : currentSeller.getCity());

        if (needsBuyerAddress && TextUtils.isEmpty(address)) {
            etAddress.setError("پتہ لکھنا ضروری ہے / Address required");
            etAddress.requestFocus();
            Toast.makeText(this, "براہ کرم اپنا ڈیلیوری پتہ درج کریں", Toast.LENGTH_LONG).show();
            return;
        }

        // Calculate grand total and combined product names
        double grandTotal = 0;
        List<String> productNameList = new ArrayList<>();

        for (Product p : checkedProducts) {
            String pid = p.getId().trim();
            int qty = productQuantitiesMap.getOrDefault(pid, 1);
            grandTotal += p.getPrice() * qty;
            productNameList.add(p.getName() + " x" + qty);
        }

        if (grandTotal <= 0) {
            Toast.makeText(this, "رقم صفر نہیں ہو سکتی", Toast.LENGTH_LONG).show();
            return;
        }

        String finalProductNameCombined = String.join(", ", productNameList);
        String notes = etNotes.getText().toString().trim();

        // Build product-by-product breakdown details for Firebase & WhatsApp
        StringBuilder detailsBuilder = new StringBuilder();

        for (Product p : checkedProducts) {
            if (p == null || p.getId() == null) continue;
            String pid = p.getId().trim();
            int qty = productQuantitiesMap.getOrDefault(pid, 1);
            long lineTotal = (long) (p.getPrice() * qty);

            detailsBuilder.append("=== ").append(p.getName()).append(" (").append(Constants.getCategoryDisplayLabel(this, p.getCategory())).append(") ===\n");
            detailsBuilder.append("• Quantity: ").append(qty).append(" — PKR ").append(lineTotal).append("\n");

            if (productFormDetailsMap.containsKey(pid)) {
                detailsBuilder.append(productFormDetailsMap.get(pid)).append("\n");
            }
            if (productFormImagesMap.containsKey(pid)) {
                detailsBuilder.append("📸 Sample Image: ").append(productFormImagesMap.get(pid)).append("\n");
            }
            detailsBuilder.append("\n");
        }

        if (!notes.isEmpty()) {
            detailsBuilder.append("📝 نوٹس / Notes: ").append(notes);
        }

        String details = detailsBuilder.toString().trim();
        if (details.isEmpty()) {
            details = finalProductNameCombined;
        }

        btnSubmitOrder.setEnabled(false);

        String uid = session.getUid();
        if (uid == null || uid.trim().isEmpty()) {
            Toast.makeText(this, "پہلے لاگ ان کریں", Toast.LENGTH_SHORT).show();
            btnSubmitOrder.setEnabled(true);
            return;
        }

        final double finalGrandTotal = grandTotal;
        final String finalAddress = address;
        final String finalDeliveryMethod = resolvedDeliveryMethod;
        final String finalDetails = details;
        final String finalProductName = finalProductNameCombined;

        // Validation for Online Payment Proof Screenshot
        if (!"cod".equalsIgnoreCase(selectedPaymentMethod)) {
            if (pendingPaymentProofUri == null && (uploadedPaymentProofUrl == null || uploadedPaymentProofUrl.isEmpty())) {
                btnSubmitOrder.setEnabled(true);
                Toast.makeText(this, "براہ کرم ادائیگی کی رسید/اسکرین شاٹ اپلوڈ کریں / Please upload payment proof screenshot", Toast.LENGTH_LONG).show();
                if (layoutPaymentProofSection != null) layoutPaymentProofSection.requestFocus();
                return;
            }
        }

        // Upload payment proof screenshot to Cloudinary folder "payment_proofs"
        if (pendingPaymentProofUri != null && (uploadedPaymentProofUrl == null || uploadedPaymentProofUrl.isEmpty())) {
            btnSubmitOrder.setEnabled(false);
            if (tvPaymentProofStatus != null) tvPaymentProofStatus.setText("⏳ اسکرین شاٹ اپلوڈ ہو رہا ہے... / Uploading proof...");

            CloudinaryHelper.upload(this, pendingPaymentProofUri, "payment_proofs", (url, error) -> {
                if (url != null) {
                    uploadedPaymentProofUrl = url;
                    if (tvPaymentProofStatus != null) tvPaymentProofStatus.setText("✅ اسکرین شاٹ اپلوڈ ہو گیا / Proof uploaded");
                    proceedWithOrderSaving(finalGrandTotal, finalAddress, finalDeliveryMethod, finalDetails, notes, finalProductName, uid);
                } else {
                    btnSubmitOrder.setEnabled(true);
                    if (tvPaymentProofStatus != null) tvPaymentProofStatus.setText("❌ اپلوڈ ناکام: " + error);
                    Toast.makeText(this, "رسید اپلوڈ نہ ہو سکی: " + error, Toast.LENGTH_LONG).show();
                }
            });
            return;
        }

        proceedWithOrderSaving(finalGrandTotal, finalAddress, finalDeliveryMethod, finalDetails, notes, finalProductName, uid);
    }

    private void setupPaymentMethodSelection() {
        if (rgPaymentMethod == null) return;

        rgPaymentMethod.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbPaymentEasyPaisa) {
                selectedPaymentMethod = "easypaisa";
                selectedPaymentStatus = "submitted";
                if (layoutPaymentProofSection != null) layoutPaymentProofSection.setVisibility(View.VISIBLE);
                String accountNum = (currentSeller != null && currentSeller.getWhatsappNumber() != null && !currentSeller.getWhatsappNumber().isEmpty())
                        ? currentSeller.getWhatsappNumber() : "03001234567";
                String sName = (currentSeller != null && currentSeller.getOwnerName() != null) ? currentSeller.getOwnerName() : "Seller";
                if (tvSellerAccountInfo != null) {
                    tvSellerAccountInfo.setText("💳 EasyPaisa Account Number: " + accountNum + "\nAccount Title: " + sName);
                }
            } else if (checkedId == R.id.rbPaymentJazzCash) {
                selectedPaymentMethod = "jazzcash";
                selectedPaymentStatus = "submitted";
                if (layoutPaymentProofSection != null) layoutPaymentProofSection.setVisibility(View.VISIBLE);
                String accountNum = (currentSeller != null && currentSeller.getWhatsappNumber() != null && !currentSeller.getWhatsappNumber().isEmpty())
                        ? currentSeller.getWhatsappNumber() : "03001234567";
                String sName = (currentSeller != null && currentSeller.getOwnerName() != null) ? currentSeller.getOwnerName() : "Seller";
                if (tvSellerAccountInfo != null) {
                    tvSellerAccountInfo.setText("💳 JazzCash Account Number: " + accountNum + "\nAccount Title: " + sName);
                }
            } else {
                selectedPaymentMethod = "cod";
                selectedPaymentStatus = "pending";
                if (layoutPaymentProofSection != null) layoutPaymentProofSection.setVisibility(View.GONE);
            }
        });
    }

    private void proceedWithOrderSaving(double finalGrandTotal, String finalAddress, String finalDeliveryMethod, String finalDetails, String notes, String finalProductName, String uid) {
        FirebaseDatabase.getInstance().getReference().child(Constants.NODE_USERS).child(uid.trim())
                .get().addOnSuccessListener(userSnap -> {
                    String buyerName = userSnap.child("name").getValue(String.class);
                    saveOrder(finalGrandTotal, finalAddress, finalDeliveryMethod, finalDetails, notes, buyerName, finalProductName);
                }).addOnFailureListener(e -> saveOrder(finalGrandTotal, finalAddress, finalDeliveryMethod, finalDetails, notes, session.getName(), finalProductName));
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
        order.setPaymentMethod(selectedPaymentMethod);
        order.setPaymentStatus(selectedPaymentStatus);
        if (uploadedPaymentProofUrl != null) {
            order.setPaymentProofUrl(uploadedPaymentProofUrl);
            order.setPaymentProofUploadedAt(System.currentTimeMillis());
        }

        dbRef.child(Constants.NODE_ORDERS).child(orderId).setValue(order)
                .addOnSuccessListener(v -> {
                    DemoDataSeeder.clearDemoDataOnFirstRealUse(this);
                    Toast.makeText(this, "آرڈر جمع ہو گیا! ✅", Toast.LENGTH_LONG).show();
                    VoiceGuide.explain(this, "آپ کا آرڈر کامیابی سے جمع ہو گیا ہے۔ بیچنے والی سے واٹس ایپ پر رابطہ ہو رہا ہے۔");
                    NotificationHelper.createAndSendOrderNotification(this, order);
                    String msg = "السلام علیکم! میں نے شی ایمپاور ایپ سے آرڈر دیا ہے:\n"
                            + order.getProductName() + " — PKR " + (long) amount + "\n\n" + details;
                    WhatsAppHelper.openChat(this, currentSeller.getWhatsappNumber(), msg);
                    startActivity(new Intent(this, MyOrdersActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnSubmitOrder.setEnabled(true);
                    Toast.makeText(this, "خرابی: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

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