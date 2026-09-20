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
import com.sheempower.app.utils.CloudinaryHelper;
import com.sheempower.app.utils.Constants;
import com.sheempower.app.utils.SessionManager;
import com.sheempower.app.utils.VoiceGuide;
import com.sheempower.app.utils.PreferencesManager;

public class AddProductActivity extends AppCompatActivity {

    private TextInputEditText etProductName, etDescription, etPrice;
    private Button btnPickImage, btnSubmit;
    private TextView tvImageStatus;
    private String imageUrl = "";
    private SessionManager session;

    private final ActivityResultLauncher<String> imagePicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    tvImageStatus.setText("✅ اپلوڈ ہو رہی ہے...");
                    CloudinaryHelper.upload(this, uri, "products", (url, error) -> {
                        if (url != null) { imageUrl = url; tvImageStatus.setText("✅ تصویر اپلوڈ ہو گئی"); }
                        else tvImageStatus.setText("❌ اپلوڈ ناکام: " + error);
                    });
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_product);

        session = new SessionManager(this);

        VoiceGuide.announceScreen(this,
                "یہاں اپنی نئی چیز شامل کریں۔ پہلے چیز کا نام لکھیں، پھر تفصیل، پھر اس کی حتمی قیمت لکھیں — یہ قیمت خریدار تبدیل نہیں کر سکے گا۔ اگر ایک سے زیادہ اقسام ہیں، جیسے مختلف سائز یا مختلف خدمات، تو ہر ایک کے لیے الگ چیز شامل کریں۔");

        etProductName = findViewById(R.id.etProductName);
        etDescription = findViewById(R.id.etDescription);
        etPrice       = findViewById(R.id.etPrice);
        btnPickImage  = findViewById(R.id.btnPickImage);
        btnSubmit     = findViewById(R.id.btnSubmit);
        tvImageStatus = findViewById(R.id.tvImageStatus);

        btnPickImage.setOnClickListener(v -> imagePicker.launch("image/*"));
        btnSubmit.setOnClickListener(v -> submitProduct());
    }

    private void submitProduct() {
        String name  = etProductName.getText().toString().trim();
        String desc  = etDescription.getText().toString().trim();
        String priceStr = etPrice.getText().toString().trim();

        if (TextUtils.isEmpty(name)) { etProductName.setError("ضروری ہے"); return; }
        if (TextUtils.isEmpty(priceStr)) { etPrice.setError("ضروری ہے"); return; }

        double price;
        try { price = Double.parseDouble(priceStr); } catch (Exception e) { etPrice.setError("غلط قیمت"); return; }

        FirebaseDatabase.getInstance().getReference().child(Constants.NODE_SELLERS).child(session.getUid())
                .child("category").get().addOnSuccessListener(snapshot -> {
                    String category = snapshot.getValue(String.class);

                    DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();
                    String productId = dbRef.child(Constants.NODE_PRODUCTS).push().getKey();
                    if (productId == null) return;

                    Product product = new Product(productId, session.getUid(), name, desc, price, imageUrl);
                    product.setCategory(category);
                    dbRef.child(Constants.NODE_PRODUCTS).child(productId).setValue(product)
                            .addOnSuccessListener(v -> {
                                Toast.makeText(this, "چیز شامل ہو گئی! یہ قیمت خریدار تبدیل نہیں کر سکے گا۔", Toast.LENGTH_LONG).show();
                                VoiceGuide.explain(this, "نئی چیز کامیابی سے شامل ہو گئی ہے");
                                finish();
                            })
                            .addOnFailureListener(e -> Toast.makeText(this, "خرابی: " + e.getMessage(), Toast.LENGTH_LONG).show());
                })
                .addOnFailureListener(e -> Toast.makeText(this, "خرابی: " + e.getMessage(), Toast.LENGTH_LONG).show());
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