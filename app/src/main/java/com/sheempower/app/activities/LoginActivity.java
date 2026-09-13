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
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;
import com.sheempower.app.R;
import com.sheempower.app.models.User;
import com.sheempower.app.utils.Constants;
import com.sheempower.app.utils.SessionManager;
import com.sheempower.app.utils.VoiceGuide;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword;
    private Button btnLogin;
    private ImageButton togglePasswordVisibility, btnSpeaker;
    private ProgressBar passwordStrengthBar;
    private TextView passwordStrengthText;
    private TextView tvForgotPassword, tvGoBuyerRegister, tvGoSellerRegister;

    private FirebaseAuth mAuth;
    private SessionManager session;
    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        session = new SessionManager(this);

        VoiceGuide.announceScreen(this,
                "یہ لاگ ان سکرین ہے۔ اپنا ای میل اور پاس ورڈ درج کریں اور لاگ ان کا بٹن دبائیں۔");

        initializeViews();
        setupPasswordVisibilityToggle();
        setupPasswordStrengthListener();

        btnLogin.setOnClickListener(v -> attemptLogin());
        btnSpeaker.setOnClickListener(v -> speakText());

        tvForgotPassword.setOnClickListener(v -> handleForgotPassword());
        tvGoBuyerRegister.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, BuyerRegisterActivity.class)));
        tvGoSellerRegister.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, SellerRegistrationActivity.class)));
    }

    private void initializeViews() {
        etEmail                  = findViewById(R.id.etEmail);
        etPassword               = findViewById(R.id.etPassword);
        btnLogin                 = findViewById(R.id.btnLogin);
        btnSpeaker              = findViewById(R.id.btnSpeaker);
        togglePasswordVisibility = findViewById(R.id.togglePasswordVisibility);
        passwordStrengthBar      = findViewById(R.id.passwordStrengthBar);
        passwordStrengthText     = findViewById(R.id.passwordStrengthText);
        tvForgotPassword         = findViewById(R.id.tvForgotPassword);
        tvGoBuyerRegister        = findViewById(R.id.tvGoBuyerRegister);
        tvGoSellerRegister       = findViewById(R.id.tvGoSellerRegister);
    }

    private void setupPasswordVisibilityToggle() {
        togglePasswordVisibility.setOnClickListener(v -> {
            if (isPasswordVisible) {
                etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                togglePasswordVisibility.setImageResource(R.drawable.ic_eye_closed);
                isPasswordVisible = false;
                VoiceGuide.explain(this, "پاس ورڈ چھپایا گیا");
            } else {
                etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                togglePasswordVisibility.setImageResource(R.drawable.ic_eye_open);
                isPasswordVisible = true;
                VoiceGuide.explain(this, "پاس ورڈ دیکھایا گیا");
            }
            if (etPassword.getText() != null) {
                etPassword.setSelection(etPassword.getText().length());
            }
        });
    }

    private void setupPasswordStrengthListener() {
        etPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updatePasswordStrength(s != null ? s.toString() : "");
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

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

    private int getStrengthColor(int strength) {
        switch (strength) {
            case 0:
            case 1:
                return ContextCompat.getColor(this, android.R.color.holo_red_light);
            case 2:
                return ContextCompat.getColor(this, android.R.color.holo_orange_light);
            case 3:
                return ContextCompat.getColor(this, android.R.color.holo_green_light);
            case 4:
            case 5:
                return ContextCompat.getColor(this, android.R.color.holo_green_dark);
            default:
                return ContextCompat.getColor(this, android.R.color.darker_gray);
        }
    }

    private void updatePasswordStrength(String password) {
        int strength = getPasswordStrength(password);
        int progress = strength * 20;
        passwordStrengthBar.setProgress(progress);
        int color = getStrengthColor(strength);
        passwordStrengthBar.setProgressDrawable(
                new ColorDrawable(color));
        passwordStrengthText.setText(getStrengthLabel(strength));
        passwordStrengthText.setTextColor(color);
    }

    private void speakText() {
        VoiceGuide.announceScreen(this,
                "یہ لاگ ان سکرین ہے۔ اپنا ای میل اور پاس ورڈ درج کریں اور لاگ ان کا بٹن دبائیں۔");
    }

    private void handleForgotPassword() {
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("درست ای میل درج کریں");
            Toast.makeText(this, "براہ کرم اپنا ای میل درج کریں", Toast.LENGTH_SHORT).show();
            VoiceGuide.explain(this, "براہ کرم پاس ورڈ ری سیٹ کے لیے اپنا ای میل درج کریں");
            return;
        }

        mAuth.sendPasswordResetEmail(email)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "پاس ورڈ ری سیٹ لنک ای میل کر دیا گیا ہے", Toast.LENGTH_LONG).show();
                    VoiceGuide.explain(this, "پاس ورڈ ری سیٹ لنک آپ کے ای میل پر بھیج دیا گیا ہے");
                })
                .addOnFailureListener(e -> Toast.makeText(this, "ای میل بھیجنے میں ناکامی: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void attemptLogin() {
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String pass  = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("درست ای میل درج کریں");
            VoiceGuide.explain(this, "براہ کرم درست ای میل لکھیں");
            return;
        }

        if (TextUtils.isEmpty(pass)) {
            etPassword.setError("پاس ورڈ ضروری ہے");
            VoiceGuide.explain(this, "براہ کرم اپنا پاس ورڈ لکھیں");
            return;
        }

        // Admin Credentials Check
        if (Constants.ADMIN_EMAIL.equalsIgnoreCase(email) && Constants.ADMIN_PASSWORD.equals(pass)) {
            btnLogin.setEnabled(false);
            mAuth.signInWithEmailAndPassword(email, pass)
                    .addOnCompleteListener(task -> {
                        FirebaseUser user = mAuth.getCurrentUser();
                        String uid = (user != null) ? user.getUid() : "admin_uid";
                        session.saveUser(uid, Constants.ROLE_ADMIN, "Admin");
                        Toast.makeText(this, "ایڈمن لاگ ان کامیاب", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(LoginActivity.this, AdminDashboardActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    });
            return;
        }

        btnLogin.setEnabled(false);
        mAuth.signInWithEmailAndPassword(email, pass)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = authResult.getUser();
                    String uid = (user != null) ? user.getUid() : "";
                    fetchUserDataAndNavigate(uid, email);
                })
                .addOnFailureListener(e -> {
                    btnLogin.setEnabled(true);
                    Toast.makeText(this, "لاگ ان ناکام: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    VoiceGuide.explain(this, "لاگ ان ناکام ہو گیا۔ ای میل یا پاس ورڈ چیک کریں");
                });
    }

    private void fetchUserDataAndNavigate(String uid, String fallbackEmail) {
        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference()
                .child(Constants.NODE_USERS)
                .child(uid);

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String role = Constants.ROLE_BUYER;
                String name = fallbackEmail;

                if (snapshot.exists()) {
                    User user = snapshot.getValue(User.class);
                    if (user != null) {
                        if (user.getRole() != null && !user.getRole().isEmpty()) {
                            role = user.getRole();
                        }
                        if (user.getName() != null && !user.getName().isEmpty()) {
                            name = user.getName();
                        }
                    }
                }

                session.saveUser(uid, role, name);

                Intent intent;
                if (Constants.ROLE_ADMIN.equals(role)) {
                    intent = new Intent(LoginActivity.this, AdminDashboardActivity.class);
                } else if (Constants.ROLE_SELLER.equals(role)) {
                    intent = new Intent(LoginActivity.this, SellerDashboardActivity.class);
                } else {
                    intent = new Intent(LoginActivity.this, MainActivity.class);
                }
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                btnLogin.setEnabled(true);
                Toast.makeText(LoginActivity.this, "ڈیٹا لوڈ کرنے میں ناکامی", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
