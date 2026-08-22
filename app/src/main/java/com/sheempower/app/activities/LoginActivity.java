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

        mAuth = FirebaseAuth.getInstance();
        session = new SessionManager(this);

        // ✅ IDs match YOUR XML exactly
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvGoBuyerRegister = findViewById(R.id.tvGoBuyerRegister);
        tvGoSellerRegister = findViewById(R.id.tvGoSellerRegister);

        btnLogin.setOnClickListener(v -> attemptLogin());

        tvGoBuyerRegister.setOnClickListener(v ->
                startActivity(new Intent(this, BuyerRegisterActivity.class)));

        tvGoSellerRegister.setOnClickListener(v ->
                startActivity(new Intent(this, SellerRegistrationActivity.class)));
    }

    private void attemptLogin() {
        String email = etEmail.getText().toString().trim();
        String pass = etPassword.getText().toString().trim();

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Invalid email");
            return;
        }
        if (TextUtils.isEmpty(pass)) {
            etPassword.setError("Required");
            return;
        }

        // ✅ Admin shortcut login
        if (email.equals(Constants.ADMIN_EMAIL) && pass.equals(Constants.ADMIN_PASSWORD)) {
            session.saveUser("admin", Constants.ROLE_ADMIN, "Admin");
            startActivity(new Intent(this, AdminDashboardActivity.class));
            finish();
            return;
        }

        btnLogin.setEnabled(false);
        btnLogin.setText("Logging in...");

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
                                    btnLogin.setText("🔑 Login");

                                    if (user == null) {
                                        Toast.makeText(LoginActivity.this,
                                                "User record not found", Toast.LENGTH_SHORT).show();
                                        return;
                                    }

                                    session.saveUser(uid, user.getRole(), user.getName());

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

                                @Override
                                public void onCancelled(DatabaseError error) {
                                    btnLogin.setEnabled(true);
                                    btnLogin.setText("🔑 Login");
                                    Toast.makeText(LoginActivity.this,
                                            "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    btnLogin.setEnabled(true);
                    btnLogin.setText("🔑 Login");
                    Toast.makeText(this, "Login failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}