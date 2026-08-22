
package com.sheempower.app.activities;

import android.content.Intent;
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
                    tvImageStatus.setText("✅ Uploading image…");
                    CloudinaryHelper.upload(this, uri, "products", url -> {
                        if (url != null) {
                            imageUrl = url;
                            tvImageStatus.setText("✅ Image uploaded successfully");
                            tvImageStatus.setTextColor(0xFF4CAF50);
                        } else {
                            tvImageStatus.setText("❌ Upload failed. Try again.");
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

        // Find Views
        etProductName = findViewById(R.id.etProductName);
        etDescription = findViewById(R.id.etDescription);
        etPrice = findViewById(R.id.etPrice);
        btnPickImage = findViewById(R.id.btnPickImage);
        btnSubmit = findViewById(R.id.btnSubmit);
        tvImageStatus = findViewById(R.id.tvImageStatus);

        // ✅ Speaker Icon for TTS Voice Guidance
        findViewById(R.id.btnSpeaker).setOnClickListener(v -> {
            String instruction = "Add Product screen. Enter product name, description, price, and upload an image. " +
                    "Only verified sellers can add products.";
            tts.speak(instruction);
        });

        // ✅ Check if seller is approved before allowing product addition
        checkSellerApproval();

        btnPickImage.setOnClickListener(v -> imagePicker.launch("image/*"));
        btnSubmit.setOnClickListener(v -> submitProduct());

        // ✅ Auto-speak screen instruction
        tts.speak("Add Product screen loaded.");
    }

    private void checkSellerApproval() {
        FirebaseDatabase.getInstance().getReference()
                .child(Constants.NODE_SELLERS)
                .child(sellerId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        Seller seller = snapshot.getValue(Seller.class);
                        if (seller == null) {
                            Toast.makeText(AddProductActivity.this, "Seller record not found", Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }

                        // ✅ Check if approved
                        if (!"approved".equals(seller.getVerificationStatus())) {
                            Toast.makeText(AddProductActivity.this,
                                    "You need admin approval to add products. Your status: " + seller.getVerificationStatus(),
                                    Toast.LENGTH_LONG).show();
                            btnSubmit.setEnabled(false);
                            btnSubmit.setBackgroundTintList(
                                    android.content.res.ColorStateList.valueOf(0xFF999999));
                            tts.speak("You are not approved to add products yet.");
                            finish();
                        } else {
                            btnSubmit.setEnabled(true);
                            btnSubmit.setBackgroundTintList(
                                    android.content.res.ColorStateList.valueOf(0xFFE91E8C));
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Toast.makeText(AddProductActivity.this, "Error checking status", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
    }

    private void submitProduct() {
        String name = etProductName.getText().toString().trim();
        String desc = etDescription.getText().toString().trim();
        String priceStr = etPrice.getText().toString().trim();

        // ✅ Validations
        if (TextUtils.isEmpty(name)) {
            etProductName.setError("Required");
            return;
        }
        if (TextUtils.isEmpty(priceStr)) {
            etPrice.setError("Required");
            return;
        }
        if (TextUtils.isEmpty(imageUrl)) {
            Toast.makeText(this, "Please upload a product image", Toast.LENGTH_SHORT).show();
            return;
        }

        double price;
        try {
            price = Double.parseDouble(priceStr);
        } catch (Exception e) {
            etPrice.setError("Invalid amount");
            return;
        }

        if (price <= 0) {
            etPrice.setError("Price must be greater than 0");
            return;
        }

        btnSubmit.setEnabled(false);
        btnSubmit.setText("Adding...");

        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();
        String productId = dbRef.child(Constants.NODE_PRODUCTS).push().getKey();
        if (productId == null) {
            btnSubmit.setEnabled(true);
            btnSubmit.setText("Add Product");
            return;
        }

        Product product = new Product(productId, sellerId, name, desc, price, imageUrl);
        dbRef.child(Constants.NODE_PRODUCTS).child(productId).setValue(product)
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, "✅ Product added successfully!", Toast.LENGTH_SHORT).show();
                    tts.speak("Product added successfully.");
                    btnSubmit.setEnabled(true);
                    btnSubmit.setText("Add Product");
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnSubmit.setEnabled(true);
                    btnSubmit.setText("Add Product");
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tts != null) tts.shutdown();
    }
}