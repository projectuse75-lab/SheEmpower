package com.sheempower.app.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.sheempower.app.R;
import com.sheempower.app.models.Seller;
import com.sheempower.app.models.User;
import com.sheempower.app.utils.*;
import java.io.File;

public class SellerRegistrationActivity extends AppCompatActivity {

    private static final int MIC_PERMISSION_CODE = 101;

    private TextInputEditText etName, etEmail, etPassword, etBusiness,
            etBio, etCity, etWhatsApp, etCapacity;
    private Spinner spinnerCategory;
    private Button btnPickCnic, btnRecord, btnPickAudio, btnRegister;
    private TextView tvCnicStatus, tvVoiceStatus, tvWhatsAppHint;

    private Uri cnicUri;
    private String cnicCloudUrl = "", voiceCloudUrl = "";
    private String audioFilePath;
    private boolean isRecording = false;
    private MediaRecorder mediaRecorder;

    private FirebaseAuth mAuth;
    private DatabaseReference dbRef;
    private SessionManager session;
    private TTSHelper tts;

    private final ActivityResultLauncher<String> cnicPicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    cnicUri = uri;
                    tvCnicStatus.setText("✅ تصویر منتخب — اپلوڈ ہو رہی ہے...");
                    tvCnicStatus.setTextColor(0xFF4CAF50);
                    uploadCnic(uri);
                }
            });

    private final ActivityResultLauncher<String> audioPicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    tvVoiceStatus.setText("✅ آڈیو منتخب — اپلوڈ ہو رہی ہے...");
                    uploadVoice(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seller_registration);

        mAuth = FirebaseAuth.getInstance();
        dbRef = FirebaseDatabase.getInstance().getReference();
        session = new SessionManager(this);
        tts = TTSHelper.getInstance(this);

        // Find Views
        etName = findViewById(R.id.etSellerName);
        etEmail = findViewById(R.id.etSellerEmail);
        etPassword = findViewById(R.id.etSellerPassword);
        etBusiness = findViewById(R.id.etBusinessName);
        etBio = findViewById(R.id.etBio);
        etCity = findViewById(R.id.etCity);
        etWhatsApp = findViewById(R.id.etWhatsApp);
        etCapacity = findViewById(R.id.etCapacity);
        tvWhatsAppHint = findViewById(R.id.tvWhatsAppHint);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        btnPickCnic = findViewById(R.id.btnPickCnic);
        btnRecord = findViewById(R.id.btnRecord);
        btnPickAudio = findViewById(R.id.btnPickAudio);
        btnRegister = findViewById(R.id.btnSellerRegister);
        tvCnicStatus = findViewById(R.id.tvCnicStatus);
        tvVoiceStatus = findViewById(R.id.tvVoiceStatus);

        // ✅ Speaker Icon for Voice Guidance
        findViewById(R.id.btnSpeaker).setOnClickListener(v -> {
            VoiceGuide.announceScreen(this,
                    "یہ رجسٹریشن کی سکرین ہے۔ اپنا نام، کاروبار، شہر اور واٹس ایپ نمبر لکھیں۔");
        });

        // ✅ Auto Voice on Load
        VoiceGuide.announceScreen(this,
                "یہ رجسٹریشن کی سکرین ہے۔ اپنا نام، کاروبار، شہر اور واٹس ایپ نمبر لکھیں۔");

        tvWhatsAppHint.setText("✅ خریدار اسی نمبر پر واٹس ایپ سے آپ سے بات کریں گے۔");

        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, Constants.CATEGORIES);
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(catAdapter);

        btnPickCnic.setOnClickListener(v -> {
            VoiceGuide.explain(this, "اپنا شناختی کارڈ کی تصویر منتخب کریں");
            cnicPicker.launch("image/*");
        });
        btnRecord.setOnClickListener(v -> toggleRecording());
        btnPickAudio.setOnClickListener(v -> audioPicker.launch("audio/*"));
        btnRegister.setOnClickListener(v -> attemptRegister());
    }

    private void uploadCnic(Uri uri) {
        CloudinaryHelper.upload(this, uri, "cnic_images", url -> {
            if (url != null) {
                cnicCloudUrl = url;
                tvCnicStatus.setText("✅ شناختی کارڈ اپلوڈ ہو گیا");
                tvCnicStatus.setTextColor(0xFF4CAF50);
            } else {
                tvCnicStatus.setText("❌ اپلوڈ ناکام — انٹرنیٹ چیک کریں یا دوبارہ کوشش کریں");
                tvCnicStatus.setTextColor(0xFFF44336);
            }
        });
    }

    private void uploadVoice(Uri uri) {
        CloudinaryHelper.upload(this, uri, "voice_samples", url -> {
            if (url != null) {
                voiceCloudUrl = url;
                tvVoiceStatus.setText("✅ آواز اپلوڈ ہو گئی");
                tvVoiceStatus.setTextColor(0xFF4CAF50);
            } else {
                tvVoiceStatus.setText("❌ اپلوڈ ناکام");
                tvVoiceStatus.setTextColor(0xFFF44336);
            }
        });
    }

    // ✅ RECORD_AUDIO permission check
    private void toggleRecording() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO}, MIC_PERMISSION_CODE);
            return;
        }
        startOrStopRecording();
    }

    private void startOrStopRecording() {
        if (!isRecording) {
            try {
                audioFilePath = getExternalCacheDir() + "/voice_" + System.currentTimeMillis() + ".mp3";
                mediaRecorder = new MediaRecorder();
                mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                mediaRecorder.setOutputFile(audioFilePath);
                mediaRecorder.prepare();
                mediaRecorder.start();
                isRecording = true;
                btnRecord.setText("⏹ ریکارڈنگ بند کریں");
                btnRecord.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFF44336));
                VoiceGuide.explain(this, "اب بولیں");
            } catch (Exception e) {
                Toast.makeText(this, "ریکارڈنگ شروع نہیں ہو سکی: " + e.getMessage(), Toast.LENGTH_LONG).show();
                isRecording = false;
            }
        } else {
            try {
                mediaRecorder.stop();
                mediaRecorder.release();
                mediaRecorder = null;
                isRecording = false;
                btnRecord.setText("🎙 آواز ریکارڈ کریں");
                btnRecord.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFFF4081));
                Uri voiceUri = Uri.fromFile(new File(audioFilePath));
                tvVoiceStatus.setText("✅ ریکارڈنگ محفوظ — اپلوڈ ہو رہی ہے...");
                uploadVoice(voiceUri);
            } catch (Exception e) {
                Toast.makeText(this, "ریکارڈنگ روکنے میں مسئلہ", Toast.LENGTH_SHORT).show();
                isRecording = false;
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MIC_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startOrStopRecording();
            } else {
                Toast.makeText(this, "آواز ریکارڈ کرنے کے لیے اجازت درکار ہے", Toast.LENGTH_LONG).show();
                VoiceGuide.explain(this, "براہ کرم مائیکروفون کی اجازت دیں");
            }
        }
    }

    private void attemptRegister() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String business = etBusiness.getText().toString().trim();
        String bio = etBio.getText().toString().trim();
        String city = etCity.getText().toString().trim();
        String capacity = etCapacity.getText().toString().trim();
        String whatsapp = etWhatsApp.getText().toString().trim().replaceAll("[^0-9]", "");
        String category = spinnerCategory.getSelectedItem().toString();

        // Validations
        if (TextUtils.isEmpty(name)) {
            etName.setError("ضروری ہے");
            VoiceGuide.explain(this, "براہ کرم اپنا نام لکھیں");
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("درست ای میل");
            VoiceGuide.explain(this, "براہ کرم درست ای میل لکھیں");
            return;
        }
        if (password.length() < 6) {
            etPassword.setError("کم از کم 6 حروف");
            VoiceGuide.explain(this, "پاس ورڈ کم از کم 6 حروف کا ہونا چاہیے");
            return;
        }
        if (TextUtils.isEmpty(business)) {
            etBusiness.setError("ضروری ہے");
            return;
        }
        if (TextUtils.isEmpty(city)) {
            etCity.setError("ضروری ہے");
            return;
        }
        if (whatsapp.length() < 10) {
            etWhatsApp.setError("درست واٹس ایپ نمبر (مثال: 923001234567)");
            VoiceGuide.explain(this, "براہ کرم درست واٹس ایپ نمبر لکھیں");
            return;
        }
        if (cnicCloudUrl.isEmpty()) {
            Toast.makeText(this, "پہلے شناختی کارڈ اپلوڈ کریں", Toast.LENGTH_SHORT).show();
            VoiceGuide.explain(this, "براہ کرم پہلے شناختی کارڈ اپلوڈ کریں");
            return;
        }

        btnRegister.setEnabled(false);
        btnRegister.setText("اکاؤنٹ بن رہا ہے...");

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    String uid = authResult.getUser().getUid();

                    User user = new User(uid, name, email, whatsapp, Constants.ROLE_SELLER);
                    dbRef.child(Constants.NODE_USERS).child(uid).setValue(user);

                    Seller seller = new Seller();
                    seller.setUid(uid);
                    seller.setOwnerName(name);
                    seller.setEmail(email);
                    seller.setBusinessName(business);
                    seller.setBio(bio);
                    seller.setCity(city);
                    seller.setCategory(category);
                    seller.setCnicUrl(cnicCloudUrl);
                    seller.setVoiceUrl(voiceCloudUrl);
                    seller.setWhatsappNumber(whatsapp);
                    seller.setCapacityDescription(capacity);
                    seller.setVerificationStatus("pending");
                    seller.setTopRated(false);
                    seller.setRating(0);
                    seller.setRatingCount(0);

                    dbRef.child(Constants.NODE_SELLERS).child(uid).setValue(seller)
                            .addOnSuccessListener(v -> {
                                DemoDataSeeder.clearDemoDataOnFirstRealUse(this);
                                session.saveUser(uid, Constants.ROLE_SELLER, name);
                                VoiceGuide.explain(this, "رجسٹریشن جمع ہو گئی، ایڈمن کی منظوری کا انتظار کریں");
                                Toast.makeText(this, "رجسٹریشن جمع! منظوری کا انتظار کریں۔", Toast.LENGTH_LONG).show();
                                startActivity(new Intent(this, SellerDashboardActivity.class));
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                btnRegister.setEnabled(true);
                                btnRegister.setText("🚀 رجسٹریشن جمع کروائیں");
                                Toast.makeText(this, "خرابی: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    btnRegister.setEnabled(true);
                    btnRegister.setText("🚀 رجسٹریشن جمع کروائیں");
                    Toast.makeText(this, "خرابی: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    VoiceGuide.explain(this, "رجسٹریشن ناکام ہو گئی، دوبارہ کوشش کریں");
                });
    }

    // ✅ FIXED: Remove TTS shutdown (singleton issue)
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaRecorder != null) {
            try {
                mediaRecorder.release();
                mediaRecorder = null;
            } catch (Exception e) { /* ignore */ }
        }
        // TTSHelper is a singleton — don't shut it down here
    }
}