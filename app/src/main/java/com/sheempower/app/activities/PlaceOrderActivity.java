package com.sheempower.app.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.*;
import com.sheempower.app.R;
import com.sheempower.app.models.Order;
import com.sheempower.app.models.OrderField;
import com.sheempower.app.models.Seller;
import com.sheempower.app.utils.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class PlaceOrderActivity extends AppCompatActivity {

    public static final String EXTRA_SELLER_ID     = "sellerId";
    public static final String EXTRA_PRODUCT_ID    = "productId";
    public static final String EXTRA_PRODUCT_NAME  = "productName";
    public static final String EXTRA_PRODUCT_PRICE = "productPrice";
    public static final String EXTRA_CATEGORY = "category";

    private static final int REQUEST_RECEIPT_IMAGE = 1001;

    // Views
    private TextView tvSellerName, tvSellerMeta, tvSellerVerified, tvSelectedItem;
    private LinearLayout llFormFields;          // ✅ matches XML
    private TextInputEditText etAmount, etAddress, etDeliveryDate, etNotes;
    private Button btnUploadReceipt, btnSubmitOrder;
    private ImageView ivReceiptPreview;

    private String sellerId, productId, productName;
    private double productPrice;
    private Seller currentSeller;
    private SessionManager session;
    private TTSHelper tts;
    private String receiptUrl = null;

    private final Map<String, EditText> dynamicTextInputs = new HashMap<>();
    private final Map<String, Spinner> dynamicSpinners = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_place_order);

        session = new SessionManager(this);
        tts = TTSHelper.getInstance(this);

        // Get Intent Data
        sellerId     = getIntent().getStringExtra(EXTRA_SELLER_ID);
        productId    = getIntent().getStringExtra(EXTRA_PRODUCT_ID);
        productName  = getIntent().getStringExtra(EXTRA_PRODUCT_NAME);
        productPrice = getIntent().getDoubleExtra(EXTRA_PRODUCT_PRICE, 0);

        // Find Views
        tvSellerName     = findViewById(R.id.tvSellerName);
        tvSellerMeta     = findViewById(R.id.tvSellerMeta);
        tvSellerVerified = findViewById(R.id.tvSellerVerified);
        tvSelectedItem   = findViewById(R.id.tvSelectedItem);
        llFormFields     = findViewById(R.id.llFormFields);   // ✅ correct ID
        etAmount         = findViewById(R.id.etAmount);
        etAddress        = findViewById(R.id.etAddress);
        etDeliveryDate   = findViewById(R.id.etDeliveryDate);
        etNotes          = findViewById(R.id.etNotes);
        btnUploadReceipt = findViewById(R.id.btnUploadReceipt);
        btnSubmitOrder   = findViewById(R.id.btnSubmitOrder);
        ivReceiptPreview = findViewById(R.id.ivReceiptPreview);

        // Check Login & Role
        if (!session.isLoggedIn()) {
            Toast.makeText(this, "پہلے لاگ ان کریں / Please login first", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        if (!session.isBuyer()) {
            Toast.makeText(this, "صرف خریدار آرڈر دے سکتے ہیں / Only buyers can order", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, BuyerRegisterActivity.class));
            finish();
            return;
        }

        // Speaker Icon
        findViewById(R.id.btnSpeaker).setOnClickListener(v -> {
            VoiceGuide.announceScreen(this,
                    "آرڈر دینے کا فارم۔ براہ کرم تمام تفصیلات بھریں، رسید اپ لوڈ کریں، اور آرڈر جمع کروائیں۔");
        });

        VoiceGuide.announceScreen(this,
                "یہ آرڈر دینے کی سکرین ہے۔ اپنی تفصیلات بھریں اور نیچے آرڈر جمع کروائیں والے بٹن پر کلک کریں۔");

        // Product Info
        if (productName != null) {
            tvSelectedItem.setText("منتخب کردہ چیز / Selected:\n" + productName);
            etAmount.setText(String.valueOf((long) productPrice));
            tvSelectedItem.setVisibility(View.VISIBLE);
        } else {
            tvSelectedItem.setVisibility(View.GONE);
        }

        // Default Delivery Date = Tomorrow
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        etDeliveryDate.setText(sdf.format(new Date(System.currentTimeMillis() + 86400000)));

        loadSellerAndBuildForm();

        btnUploadReceipt.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");
            startActivityForResult(intent, REQUEST_RECEIPT_IMAGE);
        });

        btnSubmitOrder.setOnClickListener(v -> submitOrder());
    }

    // ============================================================
    // LOAD SELLER & BUILD DYNAMIC FORM
    // ============================================================
    private void loadSellerAndBuildForm() {
        FirebaseDatabase.getInstance().getReference()
                .child(Constants.NODE_SELLERS).child(sellerId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        currentSeller = snapshot.getValue(Seller.class);
                        if (currentSeller == null) {
                            Toast.makeText(PlaceOrderActivity.this, "بیچنے والا نہیں ملا / Seller not found", Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }
                        tvSellerName.setText(currentSeller.getBusinessName());
                        tvSellerMeta.setText(currentSeller.getCategory() + " · ⭐ " + currentSeller.getRating());
                        tvSellerVerified.setText("✓ تصدیق شدہ / Verified");
                        buildDynamicForm(currentSeller.getCategory());
                    }
                    @Override
                    public void onCancelled(DatabaseError error) {}
                });
    }

    // ============================================================
    // BUILD DYNAMIC FORM (Category-Based)
    // ============================================================
    private void buildDynamicForm(String category) {
        llFormFields.removeAllViews();
        dynamicTextInputs.clear();
        dynamicSpinners.clear();

        List<OrderField> fields = CategoryFormConfig.getFieldsForCategory(category);

        for (OrderField field : fields) {
            // Label
            TextView label = new TextView(this);
            label.setText(field.labelUrdu + " / " + field.labelEnglish);
            label.setTextColor(0xFFE91E8C);
            label.setTextSize(15);
            label.setPadding(0, 24, 0, 6);
            llFormFields.addView(label);

            if ("spinner".equals(field.type)) {
                Spinner spinner = new Spinner(this);
                ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                        android.R.layout.simple_spinner_item, field.options);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinner.setAdapter(adapter);
                llFormFields.addView(spinner);
                dynamicSpinners.put(field.labelEnglish, spinner);
            } else {
                EditText input = new EditText(this);
                input.setHint(field.hintUrdu);
                input.setTextSize(16);
                input.setPadding(28, 28, 28, 28);
                input.setBackgroundResource(R.drawable.bg_rounded_pink);
                if ("number".equals(field.type)) {
                    input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
                }
                llFormFields.addView(input);
                dynamicTextInputs.put(field.labelEnglish, input);
            }
        }
    }

    // ============================================================
    // UPLOAD RECEIPT (Cloudinary)
    // ============================================================
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_RECEIPT_IMAGE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            ivReceiptPreview.setImageURI(imageUri);
            ivReceiptPreview.setVisibility(View.VISIBLE);

            Toast.makeText(this, "رسید اپ لوڈ ہو رہی ہے...", Toast.LENGTH_SHORT).show();
            CloudinaryHelper.upload(this, imageUri, "receipts", url -> {
                if (url != null) {
                    receiptUrl = url;
                    Toast.makeText(this, "✅ رسید اپ لوڈ ہو گئی!", Toast.LENGTH_SHORT).show();
                    VoiceGuide.explain(this, "رسید کامیابی سے اپ لوڈ ہو گئی۔");
                } else {
                    Toast.makeText(this, "❌ رسید اپ لوڈ نہیں ہوئی، دوبارہ کوشش کریں", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    // ============================================================
    // SUBMIT ORDER (FIXED: final variables)
    // ============================================================
    private void submitOrder() {
        String amountStr = etAmount.getText().toString().trim();
        String address = etAddress.getText().toString().trim();
        String deliveryDate = etDeliveryDate.getText().toString().trim();
        String notes = etNotes.getText().toString().trim();

        if (TextUtils.isEmpty(amountStr)) {
            etAmount.setError("ضروری ہے / Required");
            VoiceGuide.explain(this, "براہ کرم رقم درج کریں");
            return;
        }
        if (TextUtils.isEmpty(address)) {
            etAddress.setError("ضروری ہے / Required");
            VoiceGuide.explain(this, "براہ کرم اپنا پتہ درج کریں");
            return;
        }
        if (receiptUrl == null) {
            Toast.makeText(this, "براہ کرم ادائیگی کی رسید اپ لوڈ کریں", Toast.LENGTH_SHORT).show();
            VoiceGuide.explain(this, "براہ کرم رسید اپ لوڈ کریں");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (Exception e) {
            etAmount.setError("غلط رقم");
            return;
        }

        // Collect dynamic fields
        StringBuilder detailsBuilder = new StringBuilder();
        for (Map.Entry<String, EditText> entry : dynamicTextInputs.entrySet()) {
            String value = entry.getValue().getText().toString().trim();
            if (!value.isEmpty()) {
                detailsBuilder.append(entry.getKey()).append(": ").append(value).append("\n");
            }
        }
        for (Map.Entry<String, Spinner> entry : dynamicSpinners.entrySet()) {
            Object selected = entry.getValue().getSelectedItem();
            if (selected != null) {
                detailsBuilder.append(entry.getKey()).append(": ").append(selected).append("\n");
            }
        }
        if (!notes.isEmpty()) {
            detailsBuilder.append("نوٹس / Notes: ").append(notes);
        }

        // ✅ FIX: Make details final
        String detailsTemp = detailsBuilder.toString().trim();
        if (detailsTemp.isEmpty()) {
            detailsTemp = productName != null ? productName : "آرڈر / Order";
        }
        final String details = detailsTemp;
        final double finalAmount = amount;

        btnSubmitOrder.setEnabled(false);
        btnSubmitOrder.setText("جمع کروایا جا رہا ہے...");

        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();
        String orderId = dbRef.child(Constants.NODE_ORDERS).push().getKey();
        if (orderId == null) {
            btnSubmitOrder.setEnabled(true);
            btnSubmitOrder.setText("آرڈر جمع کروائیں");
            return;
        }

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
        order.setAmount(finalAmount);
        order.setReceiptUrl(receiptUrl);
        order.setStatus(Constants.STATUS_PENDING);
        order.setTimestamp(System.currentTimeMillis());

        dbRef.child(Constants.NODE_ORDERS).child(orderId).setValue(order)
                .addOnSuccessListener(v -> {
                    DemoDataSeeder.clearDemoDataOnFirstRealUse(this);

                    Toast.makeText(this, "آرڈر جمع ہو گیا! ✅ / Order submitted!", Toast.LENGTH_LONG).show();
                    VoiceGuide.explain(this, "آپ کا آرڈر کامیابی سے جمع ہو گیا ہے۔ بیچنے والی آپ سے واٹس ایپ پر رابطہ کرے گی۔");

                    if (currentSeller != null) {
                        String msg = "السلام علیکم! میں نے شی ایمپاور ایپ سے آرڈر دیا ہے:\n"
                                + order.getProductName() + " — PKR " + (long) finalAmount + "\n"
                                + "تفصیلات / Details:\n" + details;
                        WhatsAppHelper.openChat(this, currentSeller.getWhatsappNumber(), msg);
                    }

                    startActivity(new Intent(this, MyOrdersActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnSubmitOrder.setEnabled(true);
                    btnSubmitOrder.setText("آرڈر جمع کروائیں");
                    Toast.makeText(this, "خرابی / Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // TTS is singleton — no shutdown needed here
    }
}