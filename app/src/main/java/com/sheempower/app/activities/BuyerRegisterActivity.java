package com.sheempower.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.sheempower.app.R;
import com.sheempower.app.models.User;
import com.sheempower.app.utils.Constants;
import com.sheempower.app.utils.SessionManager;
import com.sheempower.app.utils.VoiceGuide;

public class BuyerRegisterActivity extends AppCompatActivity {

    private TextInputEditText etName, etEmail, etPassword, etPhone;
    private Button btnRegister;
    private FirebaseAuth mAuth;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buyer_register);

        mAuth   = FirebaseAuth.getInstance();
        session = new SessionManager(this);

        VoiceGuide.announceScreen(this,
                "یہ نیا اکاؤنٹ بنانے کی سکرین ہے۔ باری باری اپنا نام، ای میل، اور کم از کم چھ حروف کا پاس ورڈ لکھیں، پھر اکاؤنٹ بنائیں کا بٹن دبائیں۔");

        etName      = findViewById(R.id.etName);
        etEmail     = findViewById(R.id.etEmail);
        etPassword  = findViewById(R.id.etPassword);
        etPhone     = findViewById(R.id.etPhone);
        btnRegister = findViewById(R.id.btnRegister);

        btnRegister.setOnClickListener(v -> attemptRegister());
    }

    private void attemptRegister() {
        String name  = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String pass  = etPassword.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            etName.setError("ضروری ہے");
            VoiceGuide.explain(this, "براہ کرم اپنا نام لکھیں");
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) { etEmail.setError("درست ای میل"); return; }
        if (pass.length() < 6) {
            etPassword.setError("کم از کم 6 حروف");
            VoiceGuide.explain(this, "پاس ورڈ کم از کم چھ حروف کا ہونا چاہیے");
            return;
        }

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
                                startActivity(new Intent(this, MainActivity.class));
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                btnRegister.setEnabled(true);
                                Toast.makeText(this, "ڈیٹا محفوظ نہیں ہوا: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    btnRegister.setEnabled(true);
                    Toast.makeText(this, "خرابی: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}