package com.sheempower.app.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.widget.*;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import com.sheempower.app.R;
import com.sheempower.app.models.Seller;
import com.sheempower.app.models.User;
import com.sheempower.app.utils.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SellerRegistrationActivity extends AppCompatActivity {

    public static final String EXTRA_CREATE_SELLER_PROFILE =
            "create_seller_profile";

    private static final int MIC_PERMISSION_CODE = 101;

    private TextInputEditText etName, etEmail, etPassword, etBusiness,
            etBio, etCity, etWhatsApp, etCapacity, etShopAddress;

    // IMPORTANT:
    // Category is now a TextView which opens a multi-select dialog.
    private TextView spinnerCategory;

    private Spinner spinnerCanDeliver;

    private TextView tvDeliverLabel, tvWhatsAppHint;
    private Button btnPickCnic, btnRecord, btnPickAudio, btnRegister;
    private TextView tvCnicStatus, tvVoiceStatus;
    private ImageButton togglePasswordVisibility, btnSpeaker;
    private ProgressBar passwordStrengthBar;
    private TextView passwordStrengthText;

    private Uri cnicUri;
    private String cnicCloudUrl = "";
    private String voiceCloudUrl = "";

    private String audioFilePath;
    private boolean isRecording = false;
    private boolean isPasswordVisible = false;

    private MediaRecorder mediaRecorder;

    private FirebaseAuth mAuth;
    private DatabaseReference dbRef;
    private SessionManager session;

    // Selected categories
    private final ArrayList<String> selectedCategories =
            new ArrayList<>();

    // ============================================================
    // CNIC PICKER
    // ============================================================

    private final ActivityResultLauncher<String> cnicPicker =
            registerForActivityResult(
                    new ActivityResultContracts.GetContent(),
                    uri -> {
                        if (uri != null) {
                            cnicUri = uri;

                            tvCnicStatus.setText(
                                    "✅ اپلوڈ ہو رہی ہے..."
                            );

                            tvCnicStatus.setTextColor(
                                    0xFF4CAF50
                            );

                            uploadCnic(uri);
                        }
                    }
            );

    // ============================================================
    // AUDIO PICKER
    // ============================================================

    private final ActivityResultLauncher<String> audioPicker =
            registerForActivityResult(
                    new ActivityResultContracts.GetContent(),
                    uri -> {
                        if (uri != null) {

                            tvVoiceStatus.setText(
                                    "✅ اپلوڈ ہو رہی ہے..."
                            );

                            uploadVoice(uri);
                        }
                    }
            );

    // ============================================================
    // ON CREATE
    // ============================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        applyTheme();

        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_seller_registration
        );

        mAuth = FirebaseAuth.getInstance();

        dbRef = FirebaseDatabase
                .getInstance()
                .getReference();

        session = new SessionManager(this);

        initializeViews();

        setupPasswordVisibilityToggle();

        setupPasswordStrengthListener();

        tvWhatsAppHint.setText(
                "✅ خریدار اسی نمبر پر واٹس ایپ سے آپ سے بات کریں گے۔"
        );

        setupCategorySelector();

        setupDeliverySpinner();

        updateDeliverLabel(
                Constants.CATEGORIES[0]
        );

        setupExistingAccountMode();

        btnSpeaker.setOnClickListener(v ->
                VoiceGuide.announceScreen(
                        this,
                        "یہ رجسٹریشن کی سکرین ہے۔ اپنا نام، کاروبار اور واٹس ایپ نمبر لکھیں۔"
                )
        );

        btnPickCnic.setOnClickListener(v ->
                cnicPicker.launch("image/*")
        );

        btnRecord.setOnClickListener(v ->
                toggleRecording()
        );

        btnPickAudio.setOnClickListener(v ->
                audioPicker.launch("audio/*")
        );

        btnRegister.setOnClickListener(v ->
                attemptRegister()
        );
    }

    // ============================================================
    // INITIALIZE VIEWS
    // ============================================================

    private void initializeViews() {

        etName = findViewById(R.id.etSellerName);
        etEmail = findViewById(R.id.etSellerEmail);
        etPassword = findViewById(R.id.etSellerPassword);

        etBusiness = findViewById(R.id.etBusinessName);
        etBio = findViewById(R.id.etBio);
        etCity = findViewById(R.id.etCity);
        etWhatsApp = findViewById(R.id.etWhatsApp);
        etCapacity = findViewById(R.id.etCapacity);
        etShopAddress = findViewById(R.id.etShopAddress);

        spinnerCategory =
                findViewById(R.id.spinnerCategory);

        spinnerCanDeliver =
                findViewById(R.id.spinnerCanDeliver);

        tvWhatsAppHint =
                findViewById(R.id.tvWhatsAppHint);

        tvDeliverLabel =
                findViewById(R.id.tvDeliverLabel);

        btnPickCnic =
                findViewById(R.id.btnPickCnic);

        btnRecord =
                findViewById(R.id.btnRecord);

        btnPickAudio =
                findViewById(R.id.btnPickAudio);

        btnRegister =
                findViewById(R.id.btnSellerRegister);

        tvCnicStatus =
                findViewById(R.id.tvCnicStatus);

        tvVoiceStatus =
                findViewById(R.id.tvVoiceStatus);

        btnSpeaker =
                findViewById(R.id.btnSpeaker);

        togglePasswordVisibility =
                findViewById(R.id.togglePasswordVisibility);

        passwordStrengthBar =
                findViewById(R.id.passwordStrengthBar);

        passwordStrengthText =
                findViewById(R.id.passwordStrengthText);
    }

    // ============================================================
    // MULTIPLE CATEGORY SELECTOR
    // ============================================================

    private void setupCategorySelector() {

        spinnerCategory.setText(
                "زمرہ منتخب کریں / Select Categories"
        );

        spinnerCategory.setOnClickListener(v ->
                showCategoryDialog()
        );
    }

    private void showCategoryDialog() {

        String[] displayCategories =
                new String[Constants.CATEGORIES.length];

        boolean[] checked =
                new boolean[Constants.CATEGORIES.length];

        for (int i = 0;
             i < Constants.CATEGORIES.length;
             i++) {

            displayCategories[i] =
                    Constants.CATEGORY_LABELS_UR[i]
                            + " ("
                            + Constants.CATEGORIES[i]
                            + ")";

            checked[i] =
                    selectedCategories.contains(
                            Constants.CATEGORIES[i]
                    );
        }

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle(
                                "زمرے منتخب کریں / Select Categories"
                        )
                        .setMultiChoiceItems(
                                displayCategories,
                                checked,
                                (dialogInterface, which, isChecked) -> {

                                    String category =
                                            Constants.CATEGORIES[which];

                                    if (isChecked) {

                                        if (!selectedCategories
                                                .contains(category)) {

                                            selectedCategories.add(
                                                    category
                                            );
                                        }

                                    } else {

                                        selectedCategories.remove(
                                                category
                                        );
                                    }
                                }
                        )
                        .setPositiveButton(
                                "Done / مکمل",
                                (dialogInterface, which) -> {

                                    updateCategoryText();

                                    if (!selectedCategories.isEmpty()) {

                                        updateDeliverLabel(
                                                selectedCategories.get(0)
                                        );
                                    }
                                }
                        )
                        .setNegativeButton(
                                "Cancel",
                                null
                        )
                        .create();

        dialog.show();
    }

    private void updateCategoryText() {

        if (selectedCategories.isEmpty()) {

            spinnerCategory.setText(
                    "زمرہ منتخب کریں / Select Categories"
            );

            return;
        }

        StringBuilder text =
                new StringBuilder();

        for (int i = 0;
             i < selectedCategories.size();
             i++) {

            if (i > 0) {
                text.append(", ");
            }

            text.append(
                    Constants.getUrduLabel(
                            selectedCategories.get(i)
                    )
            );
        }

        spinnerCategory.setText(
                text.toString()
        );
    }

    // ============================================================
    // EXISTING ACCOUNT MODE
    // ============================================================

    private boolean isCreatingSellerForExistingAccount() {

        return getIntent().getBooleanExtra(
                EXTRA_CREATE_SELLER_PROFILE,
                false
        );
    }

    private void setupExistingAccountMode() {

        if (!isCreatingSellerForExistingAccount()) {
            return;
        }

        FirebaseUser currentUser =
                mAuth.getCurrentUser();

        if (currentUser == null) {
            return;
        }

        // Existing Firebase account
        String email = currentUser.getEmail();

        if (email != null) {

            etEmail.setText(email);

            // Existing account ka email change nahi hoga
            etEmail.setEnabled(false);
        }

        // Existing account ke liye password dobara nahi chahiye
        etPassword.setText("");
        etPassword.setHint(
                "Existing account — password required nahi"
        );

        // Password validation registration mein skip hogi.
        togglePasswordVisibility.setVisibility(
                View.GONE
        );

        passwordStrengthBar.setVisibility(
                View.GONE
        );

        passwordStrengthText.setVisibility(
                View.GONE
        );

        // Existing buyer ka name available ho to fill kar dein
        String name = session.getName();

        if (name != null && !name.isEmpty()) {
            etName.setText(name);
        }

        btnRegister.setText(
                "🚀 Seller Profile بنائیں"
        );
    }

    // ============================================================
    // PASSWORD VISIBILITY
    // ============================================================

    private void setupPasswordVisibilityToggle() {

        togglePasswordVisibility.setOnClickListener(v -> {

            if (isPasswordVisible) {

                etPassword.setInputType(
                        InputType.TYPE_CLASS_TEXT |
                                InputType.TYPE_TEXT_VARIATION_PASSWORD
                );

                togglePasswordVisibility.setImageResource(
                        R.drawable.ic_eye_closed
                );

                isPasswordVisible = false;

                VoiceGuide.explain(
                        this,
                        "پاس ورڈ چھپایا گیا"
                );

            } else {

                etPassword.setInputType(
                        InputType.TYPE_CLASS_TEXT |
                                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                );

                togglePasswordVisibility.setImageResource(
                        R.drawable.ic_eye_open
                );

                isPasswordVisible = true;

                VoiceGuide.explain(
                        this,
                        "پاس ورڈ دکھایا گیا"
                );
            }

            etPassword.setSelection(
                    etPassword.length()
            );
        });
    }

    // ============================================================
    // PASSWORD STRENGTH
    // ============================================================

    private void setupPasswordStrengthListener() {

        etPassword.addTextChangedListener(
                new TextWatcher() {

                    @Override
                    public void onTextChanged(
                            CharSequence s,
                            int start,
                            int before,
                            int count
                    ) {
                        updatePasswordStrength(
                                s.toString()
                        );
                    }

                    @Override
                    public void beforeTextChanged(
                            CharSequence s,
                            int start,
                            int count,
                            int after
                    ) {
                    }

                    @Override
                    public void afterTextChanged(
                            Editable s
                    ) {
                    }
                }
        );
    }

    private int getPasswordStrength(
            String password
    ) {

        if (password == null ||
                password.length() < 8) {

            return 0;
        }

        int strength = 0;

        if (password.matches(".*[A-Z].*"))
            strength++;

        if (password.matches(".*[a-z].*"))
            strength++;

        if (password.matches(".*[0-9].*"))
            strength++;

        if (password.matches(
                ".*[!@#$%^&*()_+\\-=\\[\\]{};:'\",.<>?/\\\\|`~].*"
        ))
            strength++;

        if (password.length() >= 12)
            strength++;

        return strength;
    }

    private String getStrengthLabel(
            int strength
    ) {

        switch (strength) {

            case 0:
                return "بہت کمزور ❌❌";

            case 1:
                return "کمزور ❌";

            case 2:
                return "درمیانی ⚠️";

            case 3:
                return "اچھا ✓";

            case 4:
            case 5:
                return "بہت مضبوط ✅✅";

            default:
                return "نامعلوم";
        }
    }

    private int getStrengthColor(
            int strength
    ) {

        switch (strength) {

            case 0:
            case 1:
                return getResources().getColor(
                        android.R.color.holo_red_light
                );

            case 2:
                return getResources().getColor(
                        android.R.color.holo_orange_light
                );

            case 3:
                return getResources().getColor(
                        android.R.color.holo_green_light
                );

            case 4:
            case 5:
                return getResources().getColor(
                        android.R.color.holo_green_dark
                );

            default:
                return getResources().getColor(
                        android.R.color.darker_gray
                );
        }
    }

    private void updatePasswordStrength(
            String password
    ) {

        int strength =
                getPasswordStrength(password);

        int progress =
                strength * 20;

        passwordStrengthBar.setProgress(
                progress
        );

        int color =
                getStrengthColor(strength);

        passwordStrengthBar.setProgressDrawable(
                new android.graphics.drawable.ColorDrawable(
                        color
                )
        );

        passwordStrengthText.setText(
                getStrengthLabel(strength)
        );

        passwordStrengthText.setTextColor(
                color
        );
    }

    // ============================================================
    // DELIVERY LABEL
    // ============================================================

    private void updateDeliverLabel(
            String category
    ) {

        if (Constants.isTravelType(category)) {

            tvDeliverLabel.setText(
                    "🚗 کیا آپ خود گاہک کے گھر جا سکتی ہیں؟\n" +
                            "Can you go to customer's location?"
            );

        } else {

            tvDeliverLabel.setText(
                    "📦 کیا آپ گھر ڈیلیوری کر سکتی ہیں؟\n" +
                            "Can you deliver to buyer's home?"
            );
        }
    }

    // ============================================================
    // DELIVERY SPINNER
    // ============================================================

    private void setupDeliverySpinner() {

        ArrayAdapter<String> deliverAdapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        new String[]{
                                "جی ہاں",
                                "نہیں"
                        }
                );

        deliverAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinnerCanDeliver.setAdapter(
                deliverAdapter
        );
    }

    // ============================================================
    // CNIC UPLOAD
    // ============================================================

    private void uploadCnic(Uri uri) {

        CloudinaryHelper.upload(
                this,
                uri,
                "cnic_images",
                (url, error) -> {

                    if (url != null) {

                        cnicCloudUrl = url;

                        tvCnicStatus.setText(
                                "✅ شناختی کارڈ اپلوڈ ہو گیا"
                        );

                        tvCnicStatus.setTextColor(
                                0xFF4CAF50
                        );

                    } else {

                        tvCnicStatus.setText(
                                "❌ اپلوڈ ناکام: " + error
                        );

                        tvCnicStatus.setTextColor(
                                0xFFF44336
                        );
                    }
                }
        );
    }

    // ============================================================
    // VOICE UPLOAD
    // ============================================================

    private void uploadVoice(Uri uri) {

        CloudinaryHelper.upload(
                this,
                uri,
                "voice_samples",
                (url, error) -> {

                    if (url != null) {

                        voiceCloudUrl = url;

                        tvVoiceStatus.setText(
                                "✅ آواز اپلوڈ ہو گئی"
                        );

                    } else {

                        tvVoiceStatus.setText(
                                "❌ اپلوڈ ناکام: " + error
                        );
                    }
                }
        );
    }

    // ============================================================
    // RECORDING
    // ============================================================

    private void toggleRecording() {

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
        ) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.RECORD_AUDIO
                    },
                    MIC_PERMISSION_CODE
            );

            return;
        }

        startOrStopRecording();
    }

    private void startOrStopRecording() {

        if (!isRecording) {

            try {

                audioFilePath =
                        getExternalCacheDir()
                                + "/voice_"
                                + System.currentTimeMillis()
                                + ".mp3";

                mediaRecorder =
                        new MediaRecorder();

                mediaRecorder.setAudioSource(
                        MediaRecorder.AudioSource.MIC
                );

                mediaRecorder.setOutputFormat(
                        MediaRecorder.OutputFormat.MPEG_4
                );

                mediaRecorder.setAudioEncoder(
                        MediaRecorder.AudioEncoder.AAC
                );

                mediaRecorder.setOutputFile(
                        audioFilePath
                );

                mediaRecorder.prepare();

                mediaRecorder.start();

                isRecording = true;

                btnRecord.setText(
                        "⏹ ریکارڈنگ بند کریں"
                );

            } catch (Exception e) {

                Toast.makeText(
                        this,
                        "ریکارڈنگ شروع نہیں ہو سکی: "
                                + e.getMessage(),
                        Toast.LENGTH_LONG
                ).show();

                isRecording = false;
            }

        } else {

            try {

                mediaRecorder.stop();

                mediaRecorder.release();

                mediaRecorder = null;

                isRecording = false;

                btnRecord.setText(
                        "🎙 آواز ریکارڈ کریں"
                );

                Uri voiceUri =
                        Uri.fromFile(
                                new File(audioFilePath)
                        );

                tvVoiceStatus.setText(
                        "✅ ریکارڈنگ محفوظ — اپلوڈ ہو رہی ہے..."
                );

                uploadVoice(voiceUri);

            } catch (Exception e) {

                Toast.makeText(
                        this,
                        "ریکارڈنگ روکنے میں مسئلہ",
                        Toast.LENGTH_SHORT
                ).show();

                isRecording = false;
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults
    ) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        if (requestCode == MIC_PERMISSION_CODE) {

            if (grantResults.length > 0 &&
                    grantResults[0] ==
                            PackageManager.PERMISSION_GRANTED) {

                startOrStopRecording();

            } else {

                Toast.makeText(
                        this,
                        "آواز ریکارڈ کرنے کے لیے اجازت درکار ہے",
                        Toast.LENGTH_LONG
                ).show();
            }
        }
    }

    // ============================================================
    // REGISTER
    // ============================================================

    private void attemptRegister() {

        String name =
                etName.getText()
                        .toString()
                        .trim();

        String email =
                etEmail.getText()
                        .toString()
                        .trim();

        String password =
                etPassword.getText()
                        .toString()
                        .trim();

        String business =
                etBusiness.getText()
                        .toString()
                        .trim();

        String bio =
                etBio.getText()
                        .toString()
                        .trim();

        String city =
                etCity.getText()
                        .toString()
                        .trim();

        String capacity =
                etCapacity.getText()
                        .toString()
                        .trim();

        String shopAddr =
                etShopAddress.getText()
                        .toString()
                        .trim();

        String whatsapp =
                etWhatsApp.getText()
                        .toString()
                        .trim()
                        .replaceAll("[^0-9]", "");

        boolean canDeliver =
                spinnerCanDeliver
                        .getSelectedItemPosition() == 0;

        // ========================================================
        // VALIDATION
        // ========================================================

        if (TextUtils.isEmpty(name)) {

            etName.setError(
                    "ضروری ہے"
            );

            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(
                email
        ).matches()) {

            etEmail.setError(
                    "درست ای میل"
            );

            return;
        }

        // Existing account mein password ki zaroorat nahi
        if (!isCreatingSellerForExistingAccount()) {

            if (password.length() < 8) {

                etPassword.setError(
                        "کم از کم 8 حروف"
                );

                VoiceGuide.explain(
                        this,
                        "پاس ورڈ کم از کم آٹھ حروف کا ہونا چاہیے"
                );

                return;
            }

            int strength =
                    getPasswordStrength(password);

            if (strength < 3) {

                Toast.makeText(
                        this,
                        "براہ کرم ایک مضبوط پاس ورڈ بنائیں",
                        Toast.LENGTH_SHORT
                ).show();

                VoiceGuide.explain(
                        this,
                        "پاس ورڈ میں حروف، نمبر اور علامت شامل کریں"
                );

                return;
            }
        }

        if (TextUtils.isEmpty(business)) {

            etBusiness.setError(
                    "ضروری ہے"
            );

            return;
        }

        if (TextUtils.isEmpty(city)) {

            etCity.setError(
                    "ضروری ہے"
            );

            return;
        }

        if (whatsapp.length() < 10) {

            etWhatsApp.setError(
                    "درست واٹس ایپ نمبر"
            );

            return;
        }

        if (TextUtils.isEmpty(shopAddr)) {

            etShopAddress.setError(
                    "ضروری ہے — یہ خریدار کو دکھایا جائے گا"
            );

            return;
        }

        // Multiple category validation
        if (selectedCategories.isEmpty()) {

            Toast.makeText(
                    this,
                    "کم از کم ایک category منتخب کریں",
                    Toast.LENGTH_SHORT
            ).show();

            showCategoryDialog();

            return;
        }

        if (cnicCloudUrl.isEmpty()) {

            Toast.makeText(
                    this,
                    "پہلے شناختی کارڈ اپلوڈ کریں",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        btnRegister.setEnabled(false);

        btnRegister.setText(
                "اکاؤنٹ محفوظ ہو رہا ہے..."
        );

        // ========================================================
        // EXISTING BUYER ACCOUNT
        // ========================================================

        if (isCreatingSellerForExistingAccount()) {

            FirebaseUser currentUser =
                    mAuth.getCurrentUser();

            if (currentUser == null) {

                Toast.makeText(
                        this,
                        "Login session ختم ہو گیا۔ دوبارہ login کریں۔",
                        Toast.LENGTH_LONG
                ).show();

                btnRegister.setEnabled(true);

                return;
            }

            String uid =
                    currentUser.getUid();

            String existingEmail =
                    currentUser.getEmail();

            String finalEmail = (existingEmail != null) ? existingEmail : email;

            saveSellerProfile(
                    uid,
                    name,
                    finalEmail,
                    business,
                    bio,
                    city,
                    capacity,
                    shopAddr,
                    whatsapp,
                    canDeliver
            );

            return;
        }

        // ========================================================
        // BRAND NEW SELLER ACCOUNT
        // ========================================================

        mAuth.createUserWithEmailAndPassword(
                        email,
                        password
                )
                .addOnSuccessListener(authResult -> {

                    FirebaseUser user =
                            authResult.getUser();

                    if (user == null) {

                        showRegistrationError(
                                "User create nahi hua"
                        );

                        return;
                    }

                    String uid =
                            user.getUid();

                    User newUser =
                            new User(
                                    uid,
                                    name,
                                    email,
                                    whatsapp,
                                    Constants.ROLE_SELLER
                            );

                    dbRef.child(
                                    Constants.NODE_USERS
                            )
                            .child(uid)
                            .setValue(newUser)
                            .addOnSuccessListener(v -> {

                                saveSellerProfile(
                                        uid,
                                        name,
                                        email,
                                        business,
                                        bio,
                                        city,
                                        capacity,
                                        shopAddr,
                                        whatsapp,
                                        canDeliver
                                );
                            })
                            .addOnFailureListener(
                                    e -> showRegistrationError(
                                            e.getMessage()
                                    )
                            );
                })
                .addOnFailureListener(
                        e -> showRegistrationError(
                                e.getMessage()
                        )
                );
    }

    // ============================================================
    // SAVE SELLER PROFILE
    // ============================================================

    private void saveSellerProfile(
            String uid,
            String name,
            String email,
            String business,
            String bio,
            String city,
            String capacity,
            String shopAddr,
            String whatsapp,
            boolean canDeliver
    ) {

        Seller seller =
                new Seller();

        seller.setUid(uid);
        seller.setOwnerName(name);
        seller.setEmail(email);
        seller.setBusinessName(business);
        seller.setBio(bio);
        seller.setCity(city);
        String whatsappNumber =
                etWhatsApp.getText()
                        .toString()
                        .trim();
        seller.setPhone(whatsappNumber);
        seller.setWhatsappNumber(whatsappNumber);

        // First selected category remains
        // primary category for old code compatibility.
        seller.setCategory(
                selectedCategories.get(0)
        );

        // All selected categories
        seller.setCategories(
                new ArrayList<>(
                        selectedCategories
                )
        );

        seller.setCnicUrl(
                cnicCloudUrl
        );

        seller.setVoiceUrl(
                voiceCloudUrl
        );

        seller.setWhatsappNumber(
                whatsapp
        );

        seller.setCapacityDescription(
                capacity
        );

        seller.setShopAddress(
                shopAddr
        );

        seller.setCanDeliver(
                canDeliver
        );

        seller.setVerificationStatus(
                "pending"
        );

        seller.setTopRated(false);

        seller.setRating(0);

        seller.setRatingCount(0);

        dbRef.child(
                        Constants.NODE_SELLERS
                )
                .child(uid)
                .setValue(seller)
                .addOnSuccessListener(v -> {

                    DemoDataSeeder
                            .clearDemoDataOnFirstRealUse(
                                    this
                            );

                    session.saveUser(
                            uid,
                            Constants.ROLE_SELLER,
                            name
                    );

                    session.saveEmail(
                            email
                    );

                    Toast.makeText(
                            this,
                            "Seller profile successfully created! اب آپ Seller mode میں ہیں۔",
                            Toast.LENGTH_LONG
                    ).show();

                    String destination =
                            getIntent()
                                    .getStringExtra(
                                            BottomNavHelper
                                                    .EXTRA_LOGIN_DESTINATION
                                    );

                    if (BottomNavHelper
                            .DESTINATION_PROFILE
                            .equals(destination)) {

                        startActivity(
                                new Intent(
                                        this,
                                        ProfileActivity.class
                                )
                        );

                    } else {

                        startActivity(
                                new Intent(
                                        this,
                                        SellerDashboardActivity.class
                                )
                        );
                    }

                    finish();
                })
                .addOnFailureListener(
                        e -> showRegistrationError(
                                e.getMessage()
                        )
                );
    }

    // ============================================================
    // ERROR
    // ============================================================

    private void showRegistrationError(
            String error
    ) {

        btnRegister.setEnabled(true);

        btnRegister.setText(
                "🚀 رجسٹریشن جمع کروائیں"
        );

        Toast.makeText(
                this,
                "خرابی: " + error,
                Toast.LENGTH_LONG
        ).show();
    }

    // ============================================================
    // DESTROY
    // ============================================================

    @Override
    protected void onDestroy() {

        if (mediaRecorder != null) {

            try {
                mediaRecorder.stop();
            } catch (Exception ignored) {
            }

            try {
                mediaRecorder.release();
            } catch (Exception ignored) {
            }

            mediaRecorder = null;
        }

        super.onDestroy();
    }

    // ============================================================
    // DARK THEME
    // ============================================================

    private void applyTheme() {

        PreferencesManager prefs =
                new PreferencesManager(this);

        if (prefs.isDarkThemeEnabled()) {

            setTheme(
                    R.style.Theme_SheEmpower_Dark
            );

        } else {

            setTheme(
                    R.style.Theme_SheEmpower
            );
        }
    }
}