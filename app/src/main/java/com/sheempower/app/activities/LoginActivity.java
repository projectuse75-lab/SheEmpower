package com.sheempower.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Patterns;
import android.widget.*;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.sheempower.app.R;
import com.sheempower.app.models.User;
import com.sheempower.app.utils.Constants;
import com.sheempower.app.utils.SessionManager;
import com.sheempower.app.utils.PreferencesManager;
import com.sheempower.app.utils.VoiceGuide;
import com.sheempower.app.utils.BottomNavHelper;
public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword;
    private Button btnLogin;
    private ImageButton togglePasswordVisibility, btnSpeaker;
    private ProgressBar passwordStrengthBar;
    private TextView passwordStrengthText;
    private TextView tvGoBuyerRegister, tvGoSellerRegister;

    private FirebaseAuth mAuth;
    private SessionManager session;
    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        setupLoginBottomNavigation();

        mAuth   = FirebaseAuth.getInstance();
        session = new SessionManager(this);

        VoiceGuide.announceScreen(this,
                "یہ لاگ ان سکرین ہے۔ سب سے پہلے خانے میں اپنا ای میل لکھیں، پھر نیچے والے خانے میں اپنا پاس ورڈ لکھیں، اور آخر میں لاگ ان کے بٹن پر کلک کریں۔ اگر آپ کا اکاؤنٹ نہیں ہے تو نیچے دیے گئے آپشن سے نیا اکاؤنٹ بنائیں۔");

        // ============ Initialize Views ============
        initializeViews();

        // ============ Setup Password Visibility Toggle ============
        setupPasswordVisibilityToggle();

        // ============ Setup Password Strength Listener ============
        setupPasswordStrengthListener();

        // ============ Setup Button Listeners ============
        btnLogin.setOnClickListener(v -> attemptLogin());
        tvGoBuyerRegister.setOnClickListener(v -> {

            Intent intent = new Intent(
                    this,
                    BuyerRegisterActivity.class
            );

            String destination = getIntent().getStringExtra(
                    BottomNavHelper.EXTRA_LOGIN_DESTINATION
            );

            if (destination != null) {
                intent.putExtra(
                        BottomNavHelper.EXTRA_LOGIN_DESTINATION,
                        destination
                );
            }

            startActivity(intent);
        });

        tvGoSellerRegister.setOnClickListener(v -> {

            Intent intent = new Intent(
                    this,
                    SellerRegistrationActivity.class
            );

            String destination = getIntent().getStringExtra(
                    BottomNavHelper.EXTRA_LOGIN_DESTINATION
            );

            if (destination != null) {
                intent.putExtra(
                        BottomNavHelper.EXTRA_LOGIN_DESTINATION,
                        destination
                );
            }

            startActivity(intent);
        });

        btnSpeaker.setOnClickListener(v -> speakText());

        TextView tvContinueVisitor = findViewById(R.id.tvContinueVisitor);
        if (tvContinueVisitor != null) {
            tvContinueVisitor.setOnClickListener(v -> {
                session.saveUser("guest", Constants.ROLE_BUYER, "Visitor");
                Intent intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }
    }

    // ============ Initialize All Views ============
    private void initializeViews() {
        etEmail                     = findViewById(R.id.etEmail);
        etPassword                  = findViewById(R.id.etPassword);
        btnLogin                    = findViewById(R.id.btnLogin);
        btnSpeaker                  = findViewById(R.id.btnSpeaker);
        togglePasswordVisibility    = findViewById(R.id.togglePasswordVisibility);
        passwordStrengthBar         = findViewById(R.id.passwordStrengthBar);
        passwordStrengthText        = findViewById(R.id.passwordStrengthText);
        tvGoBuyerRegister           = findViewById(R.id.tvGoBuyerRegister);
        tvGoSellerRegister          = findViewById(R.id.tvGoSellerRegister);
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
                new android.graphics.drawable.ColorDrawable(color));

        // Update Label Text
        passwordStrengthText.setText(getStrengthLabel(strength));
        passwordStrengthText.setTextColor(color);
    }

    // ============ LOGIN BUTTON LOGIC (Firebase) ============
    private void attemptLogin() {
        String email = etEmail.getText().toString().trim();
        String pass  = etPassword.getText().toString().trim();

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("درست ای میل لکھیں");
            VoiceGuide.explain(this, "براہ کرم درست ای میل درج کریں");
            return;
        }

        if (TextUtils.isEmpty(pass)) {
            etPassword.setError("ضروری ہے");
            VoiceGuide.explain(this, "براہ کرم پاس ورڈ درج کریں");
            return;
        }

        // Check for admin credentials
        if (email.equalsIgnoreCase(Constants.ADMIN_EMAIL.trim()) && pass.equals(Constants.ADMIN_PASSWORD)) {
            loginOrBootstrapAdmin(email, pass);
            return;
        }

        btnLogin.setEnabled(false);
        mAuth.signInWithEmailAndPassword(email, pass)
                .addOnSuccessListener(authResult -> {
                    String uid = authResult.getUser().getUid();
                    FirebaseDatabase.getInstance().getReference().child(Constants.NODE_USERS).child(uid)
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot snapshot) {
                                    User user = snapshot.getValue(User.class);
                                    btnLogin.setEnabled(true);
                                    if (user == null) {
                                        Toast.makeText(LoginActivity.this, "صارف کا ریکارڈ نہیں ملا", Toast.LENGTH_SHORT).show();
                                        return;
                                    }
                                    session.saveUser(uid, user.getRole(), user.getName());
                                    session.saveEmail(user.getEmail());
                                    VoiceGuide.explain(LoginActivity.this, "خوش آمدید " + user.getName());
                                    String loginDestination = getIntent().getStringExtra(
                                            BottomNavHelper.EXTRA_LOGIN_DESTINATION
                                    );

                                    Intent intent;

                                    if (BottomNavHelper.DESTINATION_PROFILE.equals(loginDestination)) {

                                        // Profile button se login hua tha
                                        // Login ke baad apni role-based ProfileActivity open hogi.
                                        intent = new Intent(
                                                LoginActivity.this,
                                                ProfileActivity.class
                                        );

                                    } else if (Constants.ROLE_SELLER.equals(user.getRole())) {

                                        // Normal seller login
                                        intent = new Intent(
                                                LoginActivity.this,
                                                SellerDashboardActivity.class
                                        );

                                    } else {

                                        // Normal buyer login
                                        intent = new Intent(
                                                LoginActivity.this,
                                                MainActivity.class
                                        );
                                    }

                                    startActivity(intent);
                                    finish();
                                }

                                @Override
                                public void onCancelled(DatabaseError error) {
                                    btnLogin.setEnabled(true);
                                    Toast.makeText(LoginActivity.this, "خرابی: " + error.getMessage(), Toast.LENGTH_LONG).show();
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    btnLogin.setEnabled(true);
                    Toast.makeText(this, "لاگ ان ناکام: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    VoiceGuide.explain(this, "لاگ ان ناکام ہو گیا، دوبارہ کوشش کریں");
                });
    }

    // ============ ADMIN LOGIN LOGIC ============
    private void loginOrBootstrapAdmin(String email, String pass) {
        btnLogin.setEnabled(false);
        mAuth.signInWithEmailAndPassword(email, pass)
                .addOnSuccessListener(res -> goToAdminDashboard())
                .addOnFailureListener(e -> {
                    mAuth.createUserWithEmailAndPassword(email, pass)
                            .addOnSuccessListener(res2 -> goToAdminDashboard())
                            .addOnFailureListener(e2 -> {
                                btnLogin.setEnabled(true);
                                Toast.makeText(this, "ایڈمن لاگ ان کی خرابی: " + e2.getMessage(), Toast.LENGTH_LONG).show();
                            });
                });
    }

    private void goToAdminDashboard() {
        session.saveUser(mAuth.getCurrentUser().getUid(), Constants.ROLE_ADMIN, "Admin");
        session.saveEmail(Constants.ADMIN_EMAIL);
        startActivity(new Intent(this, AdminDashboardActivity.class));
        finish();
    }

    // ============ SPEAKER BUTTON LOGIC ============
    private void speakText() {
        String textToSpeak = "لاگ ان کریں، اپنا ای میل اور مضبوط پاس ورڈ درج کریں";
        VoiceGuide.explain(this, textToSpeak);
    }
    private void setupLoginBottomNavigation() {

        FrameLayout root = findViewById(android.R.id.content);

        if (root == null) return;

        View bottomNav = LayoutInflater.from(this)
                .inflate(R.layout.bottom_nav_bar, root, false);

        FrameLayout.LayoutParams params =
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        (int) (64 * getResources().getDisplayMetrics().density)
                );

        params.gravity = android.view.Gravity.BOTTOM;

        root.addView(bottomNav, params);

        BottomNavHelper.setupForBuyerOrVisitor(
                this,
                bottomNav,
                3
        );

        // Content ko bottom navigation ke peeche jane se bachane ke liye
        View content = root.getChildAt(0);

        if (content != null && content != bottomNav) {

            int currentBottomPadding = content.getPaddingBottom();

            int extraPadding =
                    (int) (64 * getResources().getDisplayMetrics().density);

            content.setPadding(
                    content.getPaddingLeft(),
                    content.getPaddingTop(),
                    content.getPaddingRight(),
                    currentBottomPadding + extraPadding
            );
        }
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