package com.sheempower.app.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.*;
import com.sheempower.app.R;
import com.sheempower.app.models.Product;
import com.sheempower.app.models.Seller;
import com.sheempower.app.utils.*;

public class AddProductActivity extends AppCompatActivity {

    private TextInputEditText etProductName, etDescription, etPrice;
    private Button btnPickImage, btnSubmit;
    private TextView tvImageStatus;
    private String imageUrl = "";
    private SessionManager session;
    private TTSHelper tts;
    private String sellerId;

    private final ActivityResultLauncher<String> imagePicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    tvImageStatus.setText("✅ تصویر اپلوڈ ہو رہی ہے...");
                    tvImageStatus.setTextColor(0xFF4CAF50);
                    CloudinaryHelper.upload(this, uri, "products", url -> {
                        if (url != null) {
                            imageUrl = url;
                            tvImageStatus.setText("✅ تصویر اپلوڈ ہو گئی");
                        } else {
                            tvImageStatus.setText("❌ اپلوڈ ناکام");
                            tvImageStatus.setTextColor(0xFFF44336);
                        }
                    });
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_product);

        session = new SessionManager(this);
        tts = TTSHelper.getInstance(this);
        sellerId = session.getUid();

        // ✅ Check if seller is approved before allowing product addition
        if (!session.isLoggedIn() || !session.isSeller()) {
            Toast.makeText(this, "Access denied", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Find Views
        etProductName = findViewById(R.id.etProductName);
        etDescription = findViewById(R.id.etDescription);
        etPrice = findViewById(R.id.etPrice);
        btnPickImage = findViewById(R.id.btnPickImage);
        btnSubmit = findViewById(R.id.btnSubmit);
        tvImageStatus = findViewById(R.id.tvImageStatus);

        // ✅ Speaker Icon for Voice Guidance
        findViewById(R.id.btnSpeaker).setOnClickListener(v -> {
            VoiceGuide.announceScreen(this,
                    "یہاں اپنی نئی چیز کا نام، تفصیل اور قیمت لکھیں، اور اس کی تصویر اپلوڈ کریں۔");
        });

        // ✅ Auto Voice on Load
        VoiceGuide.announceScreen(this,
                "یہاں اپنی نئی چیز کا نام، تفصیل اور قیمت لکھیں، اور اس کی تصویر اپلوڈ کریں۔");

        // ✅ Check seller approval before enabling form
        checkSellerApproval();

        btnPickImage.setOnClickListener(v -> {
            VoiceGuide.explain(this, "تصویر منتخب کریں");
            imagePicker.launch("image/*");
        });
        btnSubmit.setOnClickListener(v -> submitProduct());
    }

    // ============================================================
    // ✅ CHECK SELLER APPROVAL
    // ============================================================
    private void checkSellerApproval() {
        FirebaseDatabase.getInstance().getReference()
                .child(Constants.NODE_SELLERS)
                .child(sellerId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        Seller seller = snapshot.getValue(Seller.class);
                        if (seller == null) {
                            Toast.makeText(AddProductActivity.this, "Seller not found", Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }

                        // ✅ Only approved sellers can add products
                        if ("approved".equals(seller.getVerificationStatus())) {
                            btnSubmit.setEnabled(true);
                            btnSubmit.setBackgroundTintList(
                                    android.content.res.ColorStateList.valueOf(0xFFFF4081));
                            btnSubmit.setText("✅ چیز شامل کریں / Add Product");
                        } else {
                            Toast.makeText(AddProductActivity.this,
                                    "آپ کو ایڈمن کی منظوری نہیں ملی / Not approved by admin",
                                    Toast.LENGTH_LONG).show();
                            btnSubmit.setEnabled(false);
                            btnSubmit.setBackgroundTintList(
                                    android.content.res.ColorStateList.valueOf(0xFF999999));
                            btnSubmit.setText("⏳ منظوری کا انتظار / Awaiting Approval");
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Toast.makeText(AddProductActivity.this, "Error checking approval", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
    }

    // ============================================================
    // SUBMIT PRODUCT
    // ============================================================
    private void submitProduct() {
        String name = etProductName.getText().toString().trim();
        String desc = etDescription.getText().toString().trim();
        String priceStr = etPrice.getText().toString().trim();

        // Validations
        if (TextUtils.isEmpty(name)) {
            etProductName.setError("ضروری ہے / Required");
            VoiceGuide.explain(this, "براہ کرم چیز کا نام لکھیں");
            return;
        }
        if (TextUtils.isEmpty(priceStr)) {
            etPrice.setError("ضروری ہے / Required");
            VoiceGuide.explain(this, "براہ کرم قیمت درج کریں");
            return;
        }
        if (TextUtils.isEmpty(imageUrl)) {
            Toast.makeText(this, "براہ کرم چیز کی تصویر اپلوڈ کریں", Toast.LENGTH_SHORT).show();
            VoiceGuide.explain(this, "براہ کرم تصویر اپلوڈ کریں");
            return;
        }

        double price;
        try {
            price = Double.parseDouble(priceStr);
        } catch (Exception e) {
            etPrice.setError("غلط قیمت / Invalid price");
            return;
        }

        if (price <= 0) {
            etPrice.setError("قیمت صفر سے زیادہ ہونی چاہیے / Price must be > 0");
            return;
        }

        btnSubmit.setEnabled(false);
        btnSubmit.setText("شامل کر رہے ہیں...");

        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();
        String productId = dbRef.child(Constants.NODE_PRODUCTS).push().getKey();
        if (productId == null) {
            btnSubmit.setEnabled(true);
            btnSubmit.setText("✅ چیز شامل کریں / Add Product");
            return;
        }

        Product product = new Product(productId, sellerId, name, desc, price, imageUrl);
        dbRef.child(Constants.NODE_PRODUCTS).child(productId).setValue(product)
                .addOnSuccessListener(v -> {
                    // ✅ Clear demo data on first real product
                    DemoDataSeeder.clearDemoDataOnFirstRealUse(this);

                    Toast.makeText(this, "✅ چیز شامل ہو گئی! / Product added!", Toast.LENGTH_LONG).show();
                    VoiceGuide.explain(this, "آپ کی نئی چیز کامیابی سے شامل ہو گئی ہے");

                    btnSubmit.setEnabled(true);
                    btnSubmit.setText("✅ چیز شامل کریں / Add Product");
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnSubmit.setEnabled(true);
                    btnSubmit.setText("✅ چیز شامل کریں / Add Product");
                    Toast.makeText(this, "خرابی / Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    // ============================================================
    // ✅ ON DESTROY (FIXED)
    // ============================================================
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // TTSHelper is a singleton — don't shut it down here
    }
}