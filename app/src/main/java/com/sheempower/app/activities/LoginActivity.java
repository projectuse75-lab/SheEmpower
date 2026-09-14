package com.sheempower.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Patterns;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.sheempower.app.R;
import com.sheempower.app.models.User;
import com.sheempower.app.utils.Constants;
import com.sheempower.app.utils.SessionManager;
import com.sheempower.app.utils.VoiceGuide;

public class LoginActivity extends AppCompatActivity {

    // ============ UI ELEMENTS ============
    private TextInputEditText etEmail, etPassword;
    private Button btnLogin, btnBuyerLogin, btnSellerLogin;
    private ImageButton togglePasswordVisibility, btnSpeaker;
    private ProgressBar passwordStrengthBar;
    private TextView passwordStrengthText, tvLoginTitle;
    private TextView tvGoBuyerRegister, tvGoSellerRegister, tvForgotPassword;

    // ============ FIREBASE ============
    private FirebaseAuth mAuth;
    private SessionManager session;

    // ============ STATE ============
    private boolean isPasswordVisible = false;
    private String loginType = Constants.ROLE_BUYER; // Default: Buyer

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        session = new SessionManager(this);

        // Voice announcement
        VoiceGuide.announceScreen(this,
                "لاگ ان سکرین۔ خریدار یا بیچنے والی منتخب کریں، پھر ای میل اور پاس ورڈ درج کریں۔");

        // Initialize
        initializeViews();
        setupLoginTypeToggle();
        setupPasswordVisibilityToggle();
        setupPasswordStrengthListener();
        setupClickListeners();
    }

    // ============ INITIALIZE ALL VIEWS ============
    private void initializeViews() {
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnBuyerLogin = findViewById(R.id.btnBuyerLogin);
        btnSellerLogin = findViewById(R.id.btnSellerLogin);
        btnSpeaker = findViewById(R.id.btnSpeaker);
        togglePasswordVisibility = findViewById(R.id.togglePasswordVisibility);
        passwordStrengthBar = findViewById(R.id.passwordStrengthBar);
        passwordStrengthText = findViewById(R.id.passwordStrengthText);
        tvLoginTitle = findViewById(R.id.tvLoginTitle);
        tvGoBuyerRegister = findViewById(R.id.tvGoBuyerRegister);
        tvGoSellerRegister = findViewById(R.id.tvGoSellerRegister);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
    }

    // ============ SETUP ALL CLICK LISTENERS ============
    private void setupClickListeners() {
        btnLogin.setOnClickListener(v -> attemptLogin());
        tvGoBuyerRegister.setOnClickListener(v ->
                startActivity(new Intent(this, BuyerRegisterActivity.class)));
        tvGoSellerRegister.setOnClickListener(v ->
                startActivity(new Intent(this, SellerRegistrationActivity.class)));
        tvForgotPassword.setOnClickListener(v -> handleForgotPassword());
        btnSpeaker.setOnClickListener(v -> speakText());
    }

    // ============ LOGIN TYPE TOGGLE ============
    private void setupLoginTypeToggle() {
        updateLoginTypeUI();

        btnBuyerLogin.setOnClickListener(v -> {
            loginType = Constants.ROLE_BUYER;
            updateLoginTypeUI();
            VoiceGuide.explain(this, "خریدار کے لیے لاگ ان");
        });

        btnSellerLogin.setOnClickListener(v -> {
            loginType = Constants.ROLE_SELLER;
            updateLoginTypeUI();
            VoiceGuide.explain(this, "بیچنے والی کے لیے لاگ ان");
        });
    }

    // ============ UPDATE LOGIN TYPE UI ============
    private void updateLoginTypeUI() {
        boolean isBuyer = Constants.ROLE_BUYER.equals(loginType);

        // Update button colors
        if (isBuyer) {
            // Buyer Selected
            btnBuyerLogin.setBackgroundTintList(
                    ContextCompat.getColorStateList(this, android.R.color.holo_blue_light));
            btnBuyerLogin.setTextColor(getResources().getColor(android.R.color.white));
            btnSellerLogin.setBackgroundColor(getResources().getColor(android.R.color.white));
            btnSellerLogin.setTextColor(getResources().getColor(android.R.color.holo_red_light));
            tvLoginTitle.setText("خریدار کے لیے لاگ ان / Buyer Login");
        } else {
            // Seller Selected
            btnSellerLogin.setBackgroundTintList(
                    ContextCompat.getColorStateList(this, android.R.color.holo_blue_light));
            btnSellerLogin.setTextColor(getResources().getColor(android.R.color.white));
            btnBuyerLogin.setBackgroundColor(getResources().getColor(android.R.color.white));
            btnBuyerLogin.setTextColor(getResources().getColor(android.R.color.holo_red_light));
            tvLoginTitle.setText("بیچنے والی کے لیے لاگ ان / Seller Login");
        }
    }

    // ============ PASSWORD VISIBILITY TOGGLE ============
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
            // Keep cursor at end
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

    // ============ CALCULATE PASSWORD STRENGTH ============
    private int getPasswordStrength(String password) {
        if (password == null || password.length() < 8) {
            return 0;
        }

        int strength = 0;
        if (password.matches(".*[A-Z].*")) strength++;
        if (password.matches(".*[a-z].*")) strength++;
        if (password.matches(".*[0-9].*")) strength++;
        if (password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};:'\",.<>?/\\\\|`~].*")) strength++;
        if (password.length() >= 12) strength++;

        return strength;
    }

    // ============ GET STRENGTH LABEL ============
    private String getStrengthLabel(int strength) {
        switch (strength) {
            case 0: return "بہت کمزور ❌❌";
            case 1: return "کمزور ❌";
            case 2: return "درمیانی ⚠️";
            case 3: return "اچھا ✓";
            case 4:
            case 5: return "بہت مضبوط ✅✅";
            default: return "نامعلوم";
        }
    }

    // ============ GET STRENGTH COLOR ============
    private int getStrengthColor(int strength) {
        switch (strength) {
            case 0:
            case 1: return getColor(android.R.color.holo_red_light);
            case 2: return getColor(android.R.color.holo_orange_light);
            case 3: return getColor(android.R.color.holo_green_light);
            case 4:
            case 5: return getColor(android.R.color.holo_green_dark);
            default: return getColor(android.R.color.darker_gray);
        }
    }

    // ============ UPDATE PASSWORD STRENGTH UI ============
    private void updatePasswordStrength(String password) {
        int strength = getPasswordStrength(password);
        int progress = strength * 20;

        passwordStrengthBar.setProgress(progress);
        int color = getStrengthColor(strength);
        passwordStrengthBar.setProgressDrawable(
                new android.graphics.drawable.ColorDrawable(color));
        passwordStrengthText.setText(getStrengthLabel(strength));
        passwordStrengthText.setTextColor(color);
    }

    // ============ LOGIN ATTEMPT ============
    private void attemptLogin() {
        String email = etEmail.getText().toString().trim();
        String pass = etPassword.getText().toString().trim();

        // Validate Email
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("درست ای میل درج کریں");
            VoiceGuide.explain(this, "براہ کرم درست ای میل درج کریں");
            return;
        }

        // Validate Password
        if (TextUtils.isEmpty(pass)) {
            etPassword.setError("پاس ورڈ ضروری ہے");
            VoiceGuide.explain(this, "براہ کرم پاس ورڈ درج کریں");
            return;
        }

        // Check Admin Credentials
        if (email.equalsIgnoreCase(Constants.ADMIN_EMAIL.trim()) &&
                pass.equals(Constants.ADMIN_PASSWORD)) {
            loginAsAdmin();
            return;
        }

        // Regular User Login
        btnLogin.setEnabled(false);
        mAuth.signInWithEmailAndPassword(email, pass)
                .addOnSuccessListener(authResult -> {
                    String uid = authResult.getUser().getUid();
                    checkUserRoleAndNavigate(uid);
                })
                .addOnFailureListener(e -> {
                    btnLogin.setEnabled(true);
                    String errorMsg = "لاگ ان ناکام: " + e.getMessage();
                    Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
                    VoiceGuide.explain(this, "لاگ ان ناکام ہو گیا، دوبارہ کوشش کریں");
                });
    }

    // ============ CHECK USER ROLE AND NAVIGATE ============
    private void checkUserRoleAndNavigate(String uid) {
        FirebaseDatabase.getInstance().getReference()
                .child(Constants.NODE_USERS)
                .child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        btnLogin.setEnabled(true);

                        if (!snapshot.exists()) {
                            Toast.makeText(LoginActivity.this,
                                    "صارف کا ریکارڈ نہیں ملا", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        User user = snapshot.getValue(User.class);
                        if (user == null) {
                            Toast.makeText(LoginActivity.this,
                                    "ڈیٹا لوڈ نہیں ہو سکا", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        String userRole = user.getRole();

                        // ============ VERIFY ROLE MATCHES LOGIN TYPE ============
                        if (!userRole.equals(loginType)) {
                            String errorMsg = Constants.ROLE_BUYER.equals(loginType)
                                    ? "❌ یہ بیچنے والی اکاؤنٹ ہے\n🏪 بیچنے والی لاگ ان استعمال کریں"
                                    : "❌ یہ خریدار اکاؤنٹ ہے\n🛍️ خریدار لاگ ان استعمال کریں";
                            Toast.makeText(LoginActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                            VoiceGuide.explain(LoginActivity.this, errorMsg);
                            return;
                        }

                        // ============ LOGIN SUCCESS ============
                        session.saveUser(uid, userRole, user.getName());
                        VoiceGuide.explain(LoginActivity.this, "خوش آمدید " + user.getName());

                        Intent intent = Constants.ROLE_SELLER.equals(userRole)
                                ? new Intent(LoginActivity.this, SellerDashboardActivity.class)
                                : new Intent(LoginActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        btnLogin.setEnabled(true);
                        Toast.makeText(LoginActivity.this,
                                "خرابی: " + error.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    // ============ ADMIN LOGIN ============
    private void loginAsAdmin() {
        btnLogin.setEnabled(false);
        mAuth.signInWithEmailAndPassword(Constants.ADMIN_EMAIL, Constants.ADMIN_PASSWORD)
                .addOnSuccessListener(res -> goToAdminDashboard())
                .addOnFailureListener(e -> {
                    // Try to create admin account
                    mAuth.createUserWithEmailAndPassword(Constants.ADMIN_EMAIL, Constants.ADMIN_PASSWORD)
                            .addOnSuccessListener(res2 -> goToAdminDashboard())
                            .addOnFailureListener(e2 -> {
                                btnLogin.setEnabled(true);
                                Toast.makeText(this, "ایڈمن لاگ ان خرابی", Toast.LENGTH_LONG).show();
                            });
                });
    }

    private void goToAdminDashboard() {
        session.saveUser(mAuth.getCurrentUser().getUid(), Constants.ROLE_ADMIN, "Admin");
        startActivity(new Intent(this, AdminDashboardActivity.class));
        finish();
    }

    // ============ FORGOT PASSWORD ============
    private void handleForgotPassword() {
        String email = etEmail.getText().toString().trim();

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("درست ای میل درج کریں");
            Toast.makeText(this, "براہ کرم اپنا ای میل درج کریں", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.sendPasswordResetEmail(email)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "🔗 ری سیٹ لنک ای میل کر دیا گیا", Toast.LENGTH_LONG).show();
                    VoiceGuide.explain(this, "پاس ورڈ ری سیٹ لنک آپ کے ای میل پر بھیج دیا گیا ہے");
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "خرابی: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    // ============ SPEAKER ============
    private void speakText() {
        String typeText = Constants.ROLE_BUYER.equals(loginType) ? "خریدار" : "بیچنے والی";
        VoiceGuide.explain(this, "یہ " + typeText + " کے لیے لاگ ان ہے۔ اپنا ای میل اور پاس ورڈ درج کریں۔");
    }
}