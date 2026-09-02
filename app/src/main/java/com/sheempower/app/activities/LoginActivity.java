package com.sheempower.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.sheempower.app.R;
import com.sheempower.app.models.User;
import com.sheempower.app.utils.Constants;
import com.sheempower.app.utils.SessionManager;
import com.sheempower.app.utils.TTSHelper;
import com.sheempower.app.utils.VoiceGuide;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvGoBuyerRegister, tvGoSellerRegister, tvForgotPassword;

    private FirebaseAuth mAuth;
    private SessionManager session;
    private TTSHelper tts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        session = new SessionManager(this);
        tts = TTSHelper.getInstance(this);

        // Find Views
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvGoBuyerRegister = findViewById(R.id.tvGoBuyerRegister);
        tvGoSellerRegister = findViewById(R.id.tvGoSellerRegister);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);

        // ✅ Speaker Icon for Voice Guidance
        findViewById(R.id.btnSpeaker).setOnClickListener(v -> {
            VoiceGuide.announceScreen(this,
                    "یہ لاگ ان سکرین ہے۔ اپنا ای میل اور پاس ورڈ درج کریں۔");
        });

        // ✅ Auto Voice on Load
        VoiceGuide.announceScreen(this,
                "یہ لاگ ان سکرین ہے۔ اپنا ای میل اور پاس ورڈ درج کریں۔");

        // Click Listeners
        btnLogin.setOnClickListener(v -> attemptLogin());

        tvGoBuyerRegister.setOnClickListener(v -> {
            VoiceGuide.explain(this, "خریدار رجسٹریشن کھل رہی ہے");
            startActivity(new Intent(this, BuyerRegisterActivity.class));
        });

        tvGoSellerRegister.setOnClickListener(v -> {
            VoiceGuide.explain(this, "بیچنے والے کی رجسٹریشن کھل رہی ہے");
            startActivity(new Intent(this, SellerRegistrationActivity.class));
        });

        tvForgotPassword.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            if (TextUtils.isEmpty(email)) {
                Toast.makeText(this, "پہلے ای میل درج کریں", Toast.LENGTH_SHORT).show();
                VoiceGuide.explain(this, "براہ کرم پہلے ای میل درج کریں");
                return;
            }
            mAuth.sendPasswordResetEmail(email)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "ری سیٹ لنک ای میل پر بھیج دیا گیا", Toast.LENGTH_LONG).show();
                        VoiceGuide.explain(this, "پاس ورڈ ری سیٹ لنک ای میل پر بھیج دیا گیا");
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "خرابی: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });
    }

    // ============================================================
    // ✅ ATTEMPT LOGIN (WITH onCancelled Toast)
    // ============================================================
    private void attemptLogin() {
        String email = etEmail.getText().toString().trim();
        String pass = etPassword.getText().toString().trim();

        // Validations
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("درست ای میل درج کریں / Invalid email");
            VoiceGuide.explain(this, "براہ کرم درست ای میل درج کریں");
            return;
        }
        if (TextUtils.isEmpty(pass)) {
            etPassword.setError("ضروری ہے / Required");
            VoiceGuide.explain(this, "براہ کرم پاس ورڈ درج کریں");
            return;
        }

        // ✅ Admin shortcut
        if (email.equals(Constants.ADMIN_EMAIL) && pass.equals(Constants.ADMIN_PASSWORD)) {
            session.saveUser("admin", Constants.ROLE_ADMIN, "Admin");
            VoiceGuide.explain(this, "ایڈمن لاگ ان کامیاب");
            startActivity(new Intent(this, AdminDashboardActivity.class));
            finish();
            return;
        }

        btnLogin.setEnabled(false);
        btnLogin.setText("لاگ ان ہو رہا ہے...");

        mAuth.signInWithEmailAndPassword(email, pass)
                .addOnSuccessListener(authResult -> {
                    String uid = authResult.getUser().getUid();
                    FirebaseDatabase.getInstance().getReference()
                            .child(Constants.NODE_USERS).child(uid)
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot snapshot) {
                                    User user = snapshot.getValue(User.class);
                                    btnLogin.setEnabled(true);
                                    btnLogin.setText("🔑 لاگ ان");

                                    if (user == null) {
                                        Toast.makeText(LoginActivity.this,
                                                "صارف نہیں ملا / User not found", Toast.LENGTH_SHORT).show();
                                        VoiceGuide.explain(LoginActivity.this, "صارف نہیں ملا");
                                        return;
                                    }

                                    session.saveUser(uid, user.getRole(), user.getName());
                                    VoiceGuide.explain(LoginActivity.this, "خوش آمدید " + user.getName());

                                    // ✅ Role-based navigation
                                    Intent intent;
                                    if (Constants.ROLE_ADMIN.equals(user.getRole())) {
                                        intent = new Intent(LoginActivity.this, AdminDashboardActivity.class);
                                    } else if (Constants.ROLE_SELLER.equals(user.getRole())) {
                                        intent = new Intent(LoginActivity.this, SellerDashboardActivity.class);
                                    } else {
                                        intent = new Intent(LoginActivity.this, MainActivity.class);
                                    }
                                    startActivity(intent);
                                    finish();
                                }

                                // ✅ FIXED: onCancelled with Toast
                                @Override
                                public void onCancelled(DatabaseError error) {
                                    btnLogin.setEnabled(true);
                                    btnLogin.setText("🔑 لاگ ان");
                                    Toast.makeText(LoginActivity.this,
                                            "خرابی: " + error.getMessage(), Toast.LENGTH_LONG).show();
                                    VoiceGuide.explain(LoginActivity.this, "ڈیٹا لوڈ کرنے میں خرابی");
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    btnLogin.setEnabled(true);
                    btnLogin.setText("🔑 لاگ ان");
                    Toast.makeText(this, "لاگ ان ناکام: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    VoiceGuide.explain(this, "لاگ ان ناکام ہو گیا، دوبارہ کوشش کریں");
                });
    }

    // ✅ FIXED: Remove TTS shutdown (singleton issue)
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // TTSHelper is a singleton — don't shut it down here
    }
}