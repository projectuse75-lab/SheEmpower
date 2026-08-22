package com.sheempower.app.activities;

import android.content.Intent;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.sheempower.app.R;
import com.sheempower.app.models.Seller;
import com.sheempower.app.models.User;
import com.sheempower.app.utils.*;
import java.io.File;
import java.io.IOException;

public class SellerRegistrationActivity extends AppCompatActivity {

    private TextInputEditText etName, etEmail, etPassword, etBusiness,
            etBio, etCity, etWhatsApp;
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
                    tvCnicStatus.setText("✅ CNIC selected — uploading…");
                    tvCnicStatus.setTextColor(0xFF4CAF50);
                    uploadCnic(uri);
                }
            });

    private final ActivityResultLauncher<String> audioPicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    tvVoiceStatus.setText("✅ Audio selected — uploading…");
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

        // ✅ Find Views
        etName = findViewById(R.id.etSellerName);
        etEmail = findViewById(R.id.etSellerEmail);
        etPassword = findViewById(R.id.etSellerPassword);
        etBusiness = findViewById(R.id.etBusinessName);
        etBio = findViewById(R.id.etBio);
        etCity = findViewById(R.id.etCity);
        etWhatsApp = findViewById(R.id.etWhatsApp);
        tvWhatsAppHint = findViewById(R.id.tvWhatsAppHint);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        btnPickCnic = findViewById(R.id.btnPickCnic);
        btnRecord = findViewById(R.id.btnRecord);
        btnPickAudio = findViewById(R.id.btnPickAudio);
        btnRegister = findViewById(R.id.btnSellerRegister);
        tvCnicStatus = findViewById(R.id.tvCnicStatus);
        tvVoiceStatus = findViewById(R.id.tvVoiceStatus);

        // ✅ Speaker Icon for TTS Voice Guidance
        findViewById(R.id.btnSpeaker).setOnClickListener(v -> {
            String instruction = "Seller Registration. Fill in your name, email, password, business name, bio, city, " +
                    "and WhatsApp number. Upload your CNIC image and record a voice sample. Then submit for admin approval.";
            tts.speak(instruction);
        });

        // ✅ Clear hint: This number will be used for WhatsApp Chat
        tvWhatsAppHint.setText("✅ Buyers will use this number to chat with you on WhatsApp. " +
                "There is no in-app chat system — only this WhatsApp number will be shown to buyers.");

        // ✅ Setup Category Spinner
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, Constants.CATEGORIES);
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(catAdapter);

        // ✅ Click Listeners
        btnPickCnic.setOnClickListener(v -> cnicPicker.launch("image/*"));
        btnRecord.setOnClickListener(v -> toggleRecording());
        btnPickAudio.setOnClickListener(v -> audioPicker.launch("audio/*"));
        btnRegister.setOnClickListener(v -> attemptRegister());

        // ✅ Auto-speak screen instructions on load
        tts.speak("Seller registration screen. Fill in all details and submit.");
    }

    private void uploadCnic(Uri uri) {
        CloudinaryHelper.upload(this, uri, "cnic_images", url -> {
            if (url != null) {
                cnicCloudUrl = url;
                tvCnicStatus.setText("✅ CNIC uploaded successfully");
                tvCnicStatus.setTextColor(0xFF4CAF50);
            } else {
                tvCnicStatus.setText("❌ Upload failed. Try again.");
                tvCnicStatus.setTextColor(0xFFF44336);
            }
        });
    }

    private void uploadVoice(Uri uri) {
        CloudinaryHelper.upload(this, uri, "voice_samples", url -> {
            if (url != null) {
                voiceCloudUrl = url;
                tvVoiceStatus.setText("✅ Voice uploaded successfully");
                tvVoiceStatus.setTextColor(0xFF4CAF50);
            } else {
                tvVoiceStatus.setText("❌ Upload failed. Try again.");
                tvVoiceStatus.setTextColor(0xFFF44336);
            }
        });
    }

    private void toggleRecording() {
        if (!isRecording) {
            audioFilePath = getExternalCacheDir() + "/voice_" + System.currentTimeMillis() + ".mp3";
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setOutputFile(audioFilePath);
            try {
                mediaRecorder.prepare();
                mediaRecorder.start();
                isRecording = true;
                btnRecord.setText("⏹ Stop Recording");
                btnRecord.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFF44336));
                Toast.makeText(this, "Recording... Tap Stop when done", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                Toast.makeText(this, "Recording failed", Toast.LENGTH_SHORT).show();
            }
        } else {
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;
            isRecording = false;
            btnRecord.setText("🎙 Record Voice");
            btnRecord.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFE91E8C));
            Uri voiceUri = Uri.fromFile(new File(audioFilePath));
            tvVoiceStatus.setText("✅ Recording saved — uploading…");
            uploadVoice(voiceUri);
        }
    }

    private void attemptRegister() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String business = etBusiness.getText().toString().trim();
        String bio = etBio.getText().toString().trim();
        String city = etCity.getText().toString().trim();
        String whatsapp = etWhatsApp.getText().toString().trim().replaceAll("[^0-9]", "");
        String category = spinnerCategory.getSelectedItem().toString();

        // ✅ Validations
        if (TextUtils.isEmpty(name)) {
            etName.setError("Required");
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Invalid email");
            return;
        }
        if (password.length() < 6) {
            etPassword.setError("Min 6 chars");
            return;
        }
        if (TextUtils.isEmpty(business)) {
            etBusiness.setError("Required");
            return;
        }
        if (TextUtils.isEmpty(bio)) {
            etBio.setError("Required");
            return;
        }
        if (TextUtils.isEmpty(city)) {
            etCity.setError("Required");
            return;
        }
        if (whatsapp.length() < 10) {
            etWhatsApp.setError("Correct WhatsApp number (e.g., 923001234567)");
            return;
        }
        if (cnicCloudUrl.isEmpty()) {
            Toast.makeText(this, "Please upload CNIC image", Toast.LENGTH_SHORT).show();
            return;
        }
        if (voiceCloudUrl.isEmpty()) {
            Toast.makeText(this, "Please record or upload voice sample", Toast.LENGTH_SHORT).show();
            return;
        }

        btnRegister.setEnabled(false);
        btnRegister.setText("Creating account…");

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    String uid = authResult.getUser().getUid();

                    // ✅ Save User
                    User user = new User(uid, name, email, whatsapp, Constants.ROLE_SELLER);
                    dbRef.child(Constants.NODE_USERS).child(uid).setValue(user);

                    // ✅ Save Seller
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
                    seller.setVerificationStatus("pending");
                    seller.setTopRated(false);
                    seller.setRating(0);
                    seller.setRatingCount(0);

                    dbRef.child(Constants.NODE_SELLERS).child(uid).setValue(seller)
                            .addOnSuccessListener(v -> {
                                // ✅ Clear demo data on first real registration
                                DemoDataSeeder.clearDemoDataOnFirstRealUse(this);

                                session.saveUser(uid, Constants.ROLE_SELLER, name);
                                tts.speak("Registration submitted successfully. Waiting for admin approval.");
                                Toast.makeText(this, "Registration submitted! Waiting for admin approval.", Toast.LENGTH_LONG).show();
                                startActivity(new Intent(this, SellerDashboardActivity.class));
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                btnRegister.setEnabled(true);
                                btnRegister.setText("🚀 Submit Registration");
                                Toast.makeText(this, "DB Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    btnRegister.setEnabled(true);
                    btnRegister.setText("🚀 Submit Registration");
                    Toast.makeText(this, "Auth Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tts != null) tts.shutdown();
        if (mediaRecorder != null) {
            try {
                mediaRecorder.release();
            } catch (Exception e) { /* ignore */ }
        }
    }
}