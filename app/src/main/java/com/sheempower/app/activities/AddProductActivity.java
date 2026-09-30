package com.sheempower.app.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.*;
import com.sheempower.app.R;
import com.sheempower.app.models.Product;
import com.sheempower.app.models.Seller;
import com.sheempower.app.utils.BottomNavHelper;
import com.sheempower.app.utils.CloudinaryHelper;
import com.sheempower.app.utils.Constants;
import com.sheempower.app.utils.SessionManager;
import com.sheempower.app.utils.VoiceGuide;
import com.sheempower.app.utils.PreferencesManager;
import com.sheempower.app.utils.LanguageHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AddProductActivity extends AppCompatActivity {

    public static final String EXTRA_PRODUCT_ID = "productId";

    private Spinner spinnerCategory;
    private TextInputEditText etProductName, etDescription, etPrice;
    private Button btnPickImage, btnSubmit;
    private TextView tvImageStatus;
    private String imageUrl = "";
    private SessionManager session;
    private final List<String> availableSellerCategories = new ArrayList<>();

    private String editProductId = null;
    private Product editingProduct = null;

    private final ActivityResultLauncher<String> imagePicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    tvImageStatus.setText(LanguageHelper.processDualString(this, "✅ اپلوڈ ہو رہی ہے... / Uploading..."));
                    CloudinaryHelper.upload(this, uri, "products", (url, error) -> {
                        if (url != null) {
                            imageUrl = url;
                            tvImageStatus.setText(LanguageHelper.processDualString(this, "✅ تصویر اپلوڈ ہو گئی / Image Uploaded"));
                        } else {
                            tvImageStatus.setText(LanguageHelper.processDualString(this, "❌ اپلوڈ ناکام: / Upload Failed: ") + error);
                        }
                    });
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_product);

        LanguageHelper.processViewHierarchy(findViewById(android.R.id.content), this);

        session = new SessionManager(this);

        if (getIntent() != null && getIntent().hasExtra(EXTRA_PRODUCT_ID)) {
            editProductId = getIntent().getStringExtra(EXTRA_PRODUCT_ID);
        }

        spinnerCategory = findViewById(R.id.spinnerCategory);
        etProductName   = findViewById(R.id.etProductName);
        etDescription   = findViewById(R.id.etDescription);
        etPrice         = findViewById(R.id.etPrice);
        btnPickImage    = findViewById(R.id.btnPickImage);
        btnSubmit       = findViewById(R.id.btnSubmit);
        tvImageStatus   = findViewById(R.id.tvImageStatus);

        BottomNavHelper.setupBackButton(this);

        if (editProductId != null) {
            btnSubmit.setText(LanguageHelper.processDualString(this, "💾 تبدیلی محفوظ کریں / Save Changes"));
        }

        ImageButton btnSpeaker = findViewById(R.id.btnSpeaker);
        if (btnSpeaker != null) {
            VoiceGuide.setupSpeakerButton(btnSpeaker, this,
                    editProductId != null
                            ? "یہاں اپنی چیز کی معلومات تبدیل کریں۔ ترمیم مکمل کر کے محفوظ کریں۔"
                            : "یہاں اپنی نئی چیز شامل کریں۔ پہلے زمرہ منتخب کریں، پھر چیز کا نام لکھیں، تفصیل، اور حتمی قیمت درج کریں۔"
            );
        }

        setupCategorySpinner();

        if (editProductId != null) {
            loadExistingProduct(editProductId);
        }

        btnPickImage.setOnClickListener(v -> imagePicker.launch("image/*"));
        btnSubmit.setOnClickListener(v -> submitProduct());
    }

    @Override
    protected void onResume() {
        super.onResume();
        LanguageHelper.processViewHierarchy(findViewById(android.R.id.content), this);
        ImageButton btnSpeaker = findViewById(R.id.btnSpeaker);
        if (btnSpeaker != null) {
            VoiceGuide.updateSpeakerIcon(btnSpeaker, VoiceGuide.isMuted(this));
        }
    }

    private void loadExistingProduct(String pid) {
        FirebaseDatabase.getInstance().getReference().child(Constants.NODE_PRODUCTS).child(pid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        editingProduct = snapshot.getValue(Product.class);
                        if (editingProduct == null) return;

                        if (etProductName != null) etProductName.setText(editingProduct.getName());
                        if (etDescription != null) etDescription.setText(editingProduct.getDescription());
                        if (etPrice != null) etPrice.setText(String.valueOf((long) editingProduct.getPrice()));

                        if (editingProduct.getImageUrl() != null && !editingProduct.getImageUrl().isEmpty()) {
                            imageUrl = editingProduct.getImageUrl();
                            if (tvImageStatus != null) tvImageStatus.setText("✅ تصویر موجود ہے");
                        }

                        selectCategoryInSpinner(editingProduct.getCategory());
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {}
                });
    }

    private void selectCategoryInSpinner(String category) {
        if (category == null || category.trim().isEmpty() || availableSellerCategories.isEmpty()) return;
        for (int i = 0; i < availableSellerCategories.size(); i++) {
            if (availableSellerCategories.get(i).equalsIgnoreCase(category.trim())) {
                if (spinnerCategory != null) spinnerCategory.setSelection(i);
                break;
            }
        }
    }

    private void setupCategorySpinner() {
        if (session.getUid() == null) {
            setupFallbackCategorySpinner();
            return;
        }

        FirebaseDatabase.getInstance().getReference().child(Constants.NODE_SELLERS)
                .child(session.getUid()).get()
                .addOnSuccessListener(snapshot -> {
                    Seller seller = snapshot.getValue(Seller.class);
                    availableSellerCategories.clear();

                    if (snapshot.hasChild("categories")) {
                        for (DataSnapshot catSnap : snapshot.child("categories").getChildren()) {
                            String cat = catSnap.getValue(String.class);
                            if (cat != null && !cat.trim().isEmpty() && !availableSellerCategories.contains(cat.trim())) {
                                availableSellerCategories.add(cat.trim());
                            }
                        }
                    }

                    if (availableSellerCategories.isEmpty() && seller != null && seller.getCategories() != null && !seller.getCategories().isEmpty()) {
                        for (String cat : seller.getCategories()) {
                            if (cat != null && !cat.trim().isEmpty() && !availableSellerCategories.contains(cat.trim())) {
                                availableSellerCategories.add(cat.trim());
                            }
                        }
                    }

                    if (availableSellerCategories.isEmpty() && seller != null && seller.getCategory() != null && !seller.getCategory().trim().isEmpty()) {
                        availableSellerCategories.add(seller.getCategory().trim());
                    }

                    populateCategorySpinner();
                })
                .addOnFailureListener(e -> setupFallbackCategorySpinner());
    }

    private void setupFallbackCategorySpinner() {
        availableSellerCategories.clear();
        availableSellerCategories.addAll(Arrays.asList(Constants.CATEGORIES));
        populateCategorySpinner();
    }

    private void populateCategorySpinner() {
        if (availableSellerCategories.isEmpty()) {
            availableSellerCategories.addAll(Arrays.asList(Constants.CATEGORIES));
        }

        String[] categoryDisplayNames = new String[availableSellerCategories.size()];
        for (int i = 0; i < availableSellerCategories.size(); i++) {
            String cat = availableSellerCategories.get(i);
            categoryDisplayNames[i] = Constants.getUrduLabel(cat) + " / " + cat;
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categoryDisplayNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        if (spinnerCategory != null) spinnerCategory.setAdapter(adapter);

        if (editingProduct != null) {
            selectCategoryInSpinner(editingProduct.getCategory());
        }
    }

    private void submitProduct() {
        String name  = etProductName.getText().toString().trim();
        String desc  = etDescription.getText().toString().trim();
        String priceStr = etPrice.getText().toString().trim();

        // ✅ ALL 4 FIELDS ARE STRICTLY MANDATORY
        if (TextUtils.isEmpty(name)) {
            etProductName.setError("نام درج کرنا ضروری ہے");
            etProductName.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(desc)) {
            etDescription.setError("تفصیل درج کرنا ضروری ہے");
            etDescription.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(priceStr)) {
            etPrice.setError("قیمت درج کرنا ضروری ہے");
            etPrice.requestFocus();
            return;
        }

        double price;
        try {
            price = Double.parseDouble(priceStr);
            if (price <= 0) {
                etPrice.setError("درست قیمت درج کریں");
                etPrice.requestFocus();
                return;
            }
        } catch (Exception e) {
            etPrice.setError("غلط قیمت");
            etPrice.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(imageUrl)) {
            Toast.makeText(this, "براہ کرم پروڈکٹ کی تصویر اپلوڈ کریں (تصویر لازمی ہے)", Toast.LENGTH_LONG).show();
            return;
        }

        int selectedIndex = spinnerCategory != null ? spinnerCategory.getSelectedItemPosition() : 0;
        String category = (selectedIndex >= 0 && selectedIndex < availableSellerCategories.size())
                ? availableSellerCategories.get(selectedIndex)
                : "Cooking";

        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();
        String targetProductId = (editProductId != null) ? editProductId : dbRef.child(Constants.NODE_PRODUCTS).push().getKey();
        if (targetProductId == null) return;

        String uid = session.getUid();
        if (uid == null || uid.trim().isEmpty()) {
            Toast.makeText(this, "سیشن ختم ہو گیا، دوبارہ لاگ ان کریں", Toast.LENGTH_LONG).show();
            return;
        }

        Product product = new Product(targetProductId, uid.trim(), name, desc, price, imageUrl);
        product.setCategory(category.trim());

        btnSubmit.setEnabled(false);

        dbRef.child(Constants.NODE_PRODUCTS).child(targetProductId).setValue(product)
                .addOnSuccessListener(v -> {
                    String msg = editProductId != null ? "تبدیلی محفوظ ہو گئی! ✅" : "چیز شامل ہو گئی! ✅";
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                    VoiceGuide.explain(this, msg);
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnSubmit.setEnabled(true);
                    Toast.makeText(this, "خرابی: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
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