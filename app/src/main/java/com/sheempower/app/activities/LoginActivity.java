package com.sheempower.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.sheempower.app.R;
import com.sheempower.app.models.User;
import com.sheempower.app.utils.Constants;
import com.sheempower.app.utils.SessionManager;
import com.sheempower.app.utils.VoiceGuide;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvGoBuyerRegister, tvGoSellerRegister;
    private FirebaseAuth mAuth;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth   = FirebaseAuth.getInstance();
        session = new SessionManager(this);

        etEmail    = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin   = findViewById(R.id.btnLogin);
        tvGoBuyerRegister  = findViewById(R.id.tvGoBuyerRegister);
        tvGoSellerRegister = findViewById(R.id.tvGoSellerRegister);

        findViewById(R.id.btnSpeaker).setOnClickListener(v ->
                VoiceGuide.announceScreen(this, "لاگ ان سکرین: یہاں اپنا ای میل اور پاس ورڈ لکھ کر لاگ ان کریں۔"));

        btnLogin.setOnClickListener(v -> attemptLogin());
        tvGoBuyerRegister.setOnClickListener(v -> startActivity(new Intent(this, BuyerRegisterActivity.class)));
        tvGoSellerRegister.setOnClickListener(v -> startActivity(new Intent(this, SellerRegistrationActivity.class)));
    }

    private void attemptLogin() {
        String email = etEmail.getText().toString().trim();
        String pass  = etPassword.getText().toString().trim();

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) { etEmail.setError("درست ای میل"); return; }
        if (TextUtils.isEmpty(pass)) { etPassword.setError("ضروری ہے"); return; }

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
                                @Override public void onDataChange(DataSnapshot snapshot) {
                                    User user = snapshot.getValue(User.class);
                                    btnLogin.setEnabled(true);
                                    if (user == null) {
                                        Toast.makeText(LoginActivity.this, "صارف کا ریکارڈ نہیں ملا (UID mismatch)\nUID: " + uid, Toast.LENGTH_LONG).show();
                                        return;
                                    }
                                    session.saveUser(uid, user.getRole(), user.getName());
                                    Intent intent = Constants.ROLE_SELLER.equals(user.getRole())
                                            ? new Intent(LoginActivity.this, SellerDashboardActivity.class)
                                            : new Intent(LoginActivity.this, MainActivity.class);
                                    startActivity(intent);
                                    finish();
                                }
                                @Override public void onCancelled(DatabaseError error) {
                                    btnLogin.setEnabled(true);
                                    Toast.makeText(LoginActivity.this, "خرابی: " + error.getMessage(), Toast.LENGTH_LONG).show();
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    btnLogin.setEnabled(true);
                    Toast.makeText(this, "لاگ ان ناکام: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

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
        startActivity(new Intent(this, AdminDashboardActivity.class));
        finish();
    }
}