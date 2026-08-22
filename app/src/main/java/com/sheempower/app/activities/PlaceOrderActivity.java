package com.sheempower.app.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.*;
import com.sheempower.app.R;
import com.sheempower.app.models.Order;
import com.sheempower.app.models.Seller;
import com.sheempower.app.utils.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PlaceOrderActivity extends AppCompatActivity {

    public static final String EXTRA_SELLER_ID = "sellerId";
    public static final String EXTRA_PRODUCT_ID = "productId";
    public static final String EXTRA_PRODUCT_NAME = "productName";
    public static final String EXTRA_PRODUCT_PRICE = "productPrice";

    private static final int REQUEST_RECEIPT_IMAGE = 1001;

    private TextView tvSellerName, tvSellerMeta, tvSellerVerified, tvSelectedItem;
    private TextInputEditText etDetails, etAmount, etAddress, etDeliveryDate, etNotes;
    private Button btnSubmitOrder, btnUploadReceipt;
    private ImageView ivReceiptPreview;

    private String sellerId, productId, productName;
    private double productPrice;
    private Seller currentSeller;
    private SessionManager session;
    private TTSHelper tts;
    private String receiptUrl = null;  // ✅ For Cloudinary upload

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_place_order);

        session = new SessionManager(this);
        tts = TTSHelper.getInstance(this);

        sellerId = getIntent().getStringExtra(EXTRA_SELLER_ID);
        productId = getIntent().getStringExtra(EXTRA_PRODUCT_ID);
        productName = getIntent().getStringExtra(EXTRA_PRODUCT_NAME);
        productPrice = getIntent().getDoubleExtra(EXTRA_PRODUCT_PRICE, 0);

        // ✅ Check if user is logged in AND is a buyer
        if (!session.isLoggedIn()) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        if (!session.isBuyer()) {
            Toast.makeText(this, "Only buyers can place orders. Please register as a buyer.", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, BuyerRegisterActivity.class));
            finish();
            return;
        }

        tvSellerName = findViewById(R.id.tvSellerName);
        tvSellerMeta = findViewById(R.id.tvSellerMeta);
        tvSellerVerified = findViewById(R.id.tvSellerVerified);
        tvSelectedItem = findViewById(R.id.tvSelectedItem);
        etDetails = findViewById(R.id.etDetails);
        etAmount = findViewById(R.id.etAmount);
        etAddress = findViewById(R.id.etAddress);
        etDeliveryDate = findViewById(R.id.etDeliveryDate);
        etNotes = findViewById(R.id.etNotes);
        btnSubmitOrder = findViewById(R.id.btnSubmitOrder);
        btnUploadReceipt = findViewById(R.id.btnUploadReceipt);
        ivReceiptPreview = findViewById(R.id.ivReceiptPreview);

        // ✅ Speaker Icon for TTS Voice Guidance
        findViewById(R.id.btnSpeaker).setOnClickListener(v -> {
            String instruction = "Place order screen. Enter order details, amount, address, and upload payment receipt. Tap submit to place your order.";
            tts.speak(instruction);
        });

        if (productName != null) {
            tvSelectedItem.setText("📦 " + productName);
            etAmount.setText(String.valueOf((long) productPrice));
            tvSelectedItem.setVisibility(View.VISIBLE);
        } else {
            tvSelectedItem.setVisibility(View.GONE);
        }

        // ✅ Set default delivery date to tomorrow
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        etDeliveryDate.setText(sdf.format(new Date(System.currentTimeMillis() + 86400000)));

        loadSeller();

        // ✅ Upload Receipt Button
        btnUploadReceipt.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, REQUEST_RECEIPT_IMAGE);
        });

        btnSubmitOrder.setOnClickListener(v -> submitOrder());

        // ✅ Auto-speak screen instruction
        tts.speak("Place order screen loaded.");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_RECEIPT_IMAGE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            ivReceiptPreview.setImageURI(imageUri);
            ivReceiptPreview.setVisibility(View.VISIBLE);

            // ✅ Upload to Cloudinary
            Toast.makeText(this, "Uploading receipt...", Toast.LENGTH_SHORT).show();
            CloudinaryHelper.upload(this, imageUri, "receipts", url -> {
                if (url != null) {
                    receiptUrl = url;
                    Toast.makeText(this, "Receipt uploaded!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Upload failed, try again", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void loadSeller() {
        FirebaseDatabase.getInstance().getReference()
                .child(Constants.NODE_SELLERS).child(sellerId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        currentSeller = snapshot.getValue(Seller.class);
                        if (currentSeller == null) return;
                        tvSellerName.setText(currentSeller.getBusinessName());
                        tvSellerMeta.setText(currentSeller.getCategory() + " · ⭐ " + currentSeller.getRating());
                        tvSellerVerified.setText("✓ Verified Seller");
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {}
                });
    }

    private void submitOrder() {
        String details = etDetails.getText().toString().trim();
        String amountStr = etAmount.getText().toString().trim();
        String address = etAddress.getText().toString().trim();
        String deliveryDate = etDeliveryDate.getText().toString().trim();
        String notes = etNotes.getText().toString().trim();

        // ✅ Validate all fields
        if (TextUtils.isEmpty(details)) {
            etDetails.setError("Required");
            return;
        }
        if (TextUtils.isEmpty(amountStr)) {
            etAmount.setError("Required");
            return;
        }
        if (TextUtils.isEmpty(address)) {
            etAddress.setError("Required");
            return;
        }
        if (TextUtils.isEmpty(deliveryDate)) {
            etDeliveryDate.setError("Required");
            return;
        }
        if (receiptUrl == null) {
            Toast.makeText(this, "Please upload payment receipt", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (Exception e) {
            etAmount.setError("Invalid amount");
            return;
        }

        btnSubmitOrder.setEnabled(false);
        btnSubmitOrder.setText("Submitting...");

        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();
        String orderId = dbRef.child(Constants.NODE_ORDERS).push().getKey();
        if (orderId == null) return;

        Order order = new Order();
        order.setId(orderId);
        order.setBuyerId(session.getUid());
        order.setSellerId(sellerId);
        order.setProductId(productId);
        order.setProductName(productName != null ? productName : details);
        order.setDetails(details);
        order.setAddress(address);
        order.setDeliveryDate(deliveryDate);
        order.setNotes(notes);
        order.setAmount(amount);
        order.setReceiptUrl(receiptUrl);  // ✅ Store receipt URL
        order.setStatus(Constants.STATUS_PENDING);
        order.setTimestamp(System.currentTimeMillis());

        dbRef.child(Constants.NODE_ORDERS).child(orderId).setValue(order)
                .addOnSuccessListener(v -> {
                    // ✅ First real order — clear demo data
                    DemoDataSeeder.clearDemoDataOnFirstRealUse(this);

                    Toast.makeText(this, "Order submitted! Seller will confirm.", Toast.LENGTH_LONG).show();

                    // ✅ Open WhatsApp to notify seller
                    if (currentSeller != null) {
                        String msg = "Assalam o Alaikum! A new order has been placed via She Empower app:\n" +
                                "Product: " + order.getProductName() + "\n" +
                                "Amount: PKR " + (long) amount + "\n" +
                                "Please check your orders in the app.";
                        WhatsAppHelper.openChat(this, currentSeller.getWhatsappNumber(), msg);
                    }

                    startActivity(new Intent(this, MyOrdersActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnSubmitOrder.setEnabled(true);
                    btnSubmitOrder.setText("Submit Order");
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tts != null) tts.shutdown();
    }
}