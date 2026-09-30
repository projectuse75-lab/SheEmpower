package com.sheempower.app.activities;

import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Patterns;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.sheempower.app.R;
import com.sheempower.app.models.User;
import com.sheempower.app.utils.Constants;
import com.sheempower.app.utils.SessionManager;
import com.sheempower.app.utils.PreferencesManager;
import com.sheempower.app.utils.VoiceGuide;
import com.sheempower.app.utils.BottomNavHelper;

public class BuyerRegisterActivity extends AppCompatActivity {

    private TextInputEditText etName, etEmail, etPassword, etPhone;
    private Button btnRegister;
    private ImageButton togglePasswordVisibility, btnSpeaker;
    private ProgressBar passwordStrengthBar;
    private TextView passwordStrengthText;

    private FirebaseAuth mAuth;
    private SessionManager session;
    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buyer_register);

        mAuth   = FirebaseAuth.getInstance();
        session = new SessionManager(this);

        VoiceGuide.announceScreen(this,
                "یہ خریدار اکاؤنٹ بنانے کی سکرین ہے۔ باری باری اپنا نام، ای میل، اور مضبوط پاس ورڈ لکھیں، پھر اکاؤنٹ بنائیں کا بٹن دبائیں۔");

        // ============ Initialize Views ============
        initializeViews();

        BottomNavHelper.setupBackButton(this);

        // ============ Setup Password Visibility Toggle ============
        setupPasswordVisibilityToggle();

        // ============ Setup Password Strength Listener ============
        setupPasswordStrengthListener();

        // ============ Setup Register Button ============
        btnRegister.setOnClickListener(v -> attemptRegister());

        // ============ Setup Speaker Button ============
        btnSpeaker.setOnClickListener(v -> speakText());
    }

    // ============ Initialize All Views ============
    private void initializeViews() {
        etName                      = findViewById(R.id.etName);
        etEmail                     = findViewById(R.id.etEmail);
        etPassword                  = findViewById(R.id.etPassword);
        etPhone                     = findViewById(R.id.etPhone);
        btnRegister                 = findViewById(R.id.btnRegister);
        btnSpeaker                  = findViewById(R.id.btnSpeaker);
        togglePasswordVisibility    = findViewById(R.id.togglePasswordVisibility);
        passwordStrengthBar         = findViewById(R.id.passwordStrengthBar);
        passwordStrengthText        = findViewById(R.id.passwordStrengthText);
    }

    // ============ PASSWORD SHOW/HIDE LOGIC ============
    private void setupPasswordVisibilityToggle() {
        togglePasswordVisibility.setOnClickListener(v -> {
            if (isPasswordVisible) {
                // Hide Password
                etPassword.setInputType(InputType.TYPE_CLASS_TEXT |
                        InputType.TYPE_TEXT_VARIATION_PASSWORD);
                togglePasswordVisibility.setImageResource(R.drawable.ic_eye_closed);
                isPasswordVisible = false;
                VoiceGuide.explain(this, "پاس ورڈ چھپایا گیا");
            } else {
                // Show Password
                etPassword.setInputType(InputType.TYPE_CLASS_TEXT |
                        InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                togglePasswordVisibility.setImageResource(R.drawable.ic_eye_open);
                isPasswordVisible = true;
                VoiceGuide.explain(this, "پاس ورڈ دیکھایا گیا");
            }
            // Cursor ko end mein rakhne ke liye
            etPassword.setSelection(etPassword.length());
        });
    }

    // ============ PASSWORD STRENGTH LISTENER ============
    private void setupPasswordStrengthListener() {
        etPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updatePasswordStrength(s.toString());
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    // ============ PASSWORD STRENGTH CALCULATION ============
    /**
     * Password Strength ke rules:
     * - Minimum 8 characters ✓
     * - 1 uppercase letter (A-Z) ✓
     * - 1 lowercase letter (a-z) ✓
     * - 1 number (0-9) ✓
     * - 1 special character (!@#$%^&*) ✓
     */
    private int getPasswordStrength(String password) {
        if (password == null || password.length() < 8) {
            return 0; // WEAK
        }

        int strength = 0;

        // Check uppercase letter
        if (password.matches(".*[A-Z].*")) {
            strength++;
        }

        // Check lowercase letter
        if (password.matches(".*[a-z].*")) {
            strength++;
        }

        // Check numbers
        if (password.matches(".*[0-9].*")) {
            strength++;
        }

        // Check special characters
        if (password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};:'\",.<>?/\\\\|`~].*")) {
            strength++;
        }

        // Length bonus (12+ characters)
        if (password.length() >= 12) {
            strength++;
        }

        return strength;
    }

    // ============ PASSWORD STRENGTH LABEL ============
    private String getStrengthLabel(int strength) {
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

    // ============ GET STRENGTH COLOR ============
    private int getStrengthColor(int strength) {
        switch (strength) {
            case 0:
            case 1:
                return getResources().getColor(android.R.color.holo_red_light); // Red
            case 2:
                return getResources().getColor(android.R.color.holo_orange_light); // Orange
            case 3:
                return getResources().getColor(android.R.color.holo_green_light); // Yellow-Green
            case 4:
            case 5:
                return getResources().getColor(android.R.color.holo_green_dark); // Dark Green
            default:
                return getResources().getColor(android.R.color.darker_gray);
        }
    }

    // ============ UPDATE PASSWORD STRENGTH UI ============
    private void updatePasswordStrength(String password) {
        int strength = getPasswordStrength(password);

        // Update Progress Bar
        int progress = strength * 20; // 0, 20, 40, 60, 80, 100
        passwordStrengthBar.setProgress(progress);

        // Update Color
        int color = getStrengthColor(strength);
        passwordStrengthBar.setProgressDrawable(
                new ColorDrawable(color));

        // Update Label Text
        passwordStrengthText.setText(getStrengthLabel(strength));
        passwordStrengthText.setTextColor(color);
    }

    // ============ REGISTER BUTTON LOGIC (Firebase) ============
    private void attemptRegister() {
        String name  = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String pass  = etPassword.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        // ============ VALIDATION ============
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

        // ============ PASSWORD VALIDATION (8+ characters) ============
        if (pass.length() < 8) {
            etPassword.setError("کم از کم 8 حروف");
            VoiceGuide.explain(this, "پاس ورڈ کم از کم آٹھ حروف کا ہونا چاہیے");
            return;
        }

        // ============ PASSWORD STRENGTH CHECK ============
        int strength = getPasswordStrength(pass);
        if (strength < 3) {
            Toast.makeText(this, "براہ کرم ایک مضبوط پاس ورڈ بنائیں", Toast.LENGTH_SHORT).show();
            VoiceGuide.explain(this, "پاس ورڈ میں حروف، نمبر اور علامت شامل کریں");
            return;
        }

        // ============ FIREBASE REGISTRATION ============
        btnRegister.setEnabled(false);
        mAuth.createUserWithEmailAndPassword(email, pass)
                .addOnSuccessListener(authResult -> {
                    String uid = authResult.getUser().getUid();
                    User user = new User(uid, name, email, phone, Constants.ROLE_BUYER);
                    FirebaseDatabase.getInstance().getReference()
                            .child(Constants.NODE_USERS).child(uid).setValue(user)
                            .addOnSuccessListener(v -> {
                                session.saveUser(uid, Constants.ROLE_BUYER, name);
                                VoiceGuide.explain(this, "خوش آمدید " + name + "، آپ کا اکاؤنٹ بن گیا ہے");
                                String destination = getIntent().getStringExtra(
                                        BottomNavHelper.EXTRA_LOGIN_DESTINATION
                                );

                                if (BottomNavHelper.DESTINATION_PROFILE.equals(destination)) {

                                    startActivity(new Intent(
                                            this,
                                            ProfileActivity.class
                                    ));

                                } else {

                                    startActivity(new Intent(
                                            this,
                                            MainActivity.class
                                    ));
                                }

                                finish();
                            })
                            .addOnFailureListener(e -> {
                                btnRegister.setEnabled(true);
                                Toast.makeText(this, "ڈیٹا محفوظ نہیں ہوا: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                VoiceGuide.explain(this, "معافی چاہتے ہیں، ڈیٹا محفوظ نہیں ہو سکا");
                            });
                })
                .addOnFailureListener(e -> {
                    btnRegister.setEnabled(true);
                    Toast.makeText(this, "خرابی: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    VoiceGuide.explain(this, "یہ ای میل پہلے سے موجود ہے یا کوئی مسئلہ ہے");
                });
    }

    // ============ SPEAKER BUTTON LOGIC ============
    private void speakText() {
        String textToSpeak = "خریدار بنیں اور شروع کریں۔ نام، ای میل، مضبوط پاس ورڈ درج کریں";
        VoiceGuide.explain(this, textToSpeak);
    }

    // ✅ DARK THEME - ADD AT END OF CLASS
    private void applyTheme() {
        PreferencesManager prefs = new PreferencesManager(this);
        if (prefs.isDarkThemeEnabled()) {
            setTheme(R.style.Theme_SheEmpower_Dark);
        } else {
            setTheme(R.style.Theme_SheEmpower);
        }
    }
}
