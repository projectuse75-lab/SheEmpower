package com.sheempower.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.sheempower.app.R;
import com.sheempower.app.models.User;
import com.sheempower.app.utils.Constants;
import com.sheempower.app.utils.DemoDataSeeder;
import com.sheempower.app.utils.SessionManager;
import com.sheempower.app.utils.TTSHelper;
import com.sheempower.app.utils.VoiceGuide;

public class BuyerRegisterActivity extends AppCompatActivity {

    private TextInputEditText etName, etEmail, etPassword, etConfirmPassword, etPhone;
    private Button btnRegister;
    private FirebaseAuth mAuth;
    private SessionManager session;
    private TTSHelper tts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buyer_register);

        mAuth = FirebaseAuth.getInstance();
        session = new SessionManager(this);
        tts = TTSHelper.getInstance(this);

        // ✅ Check if already logged in (redirect to MainActivity)
        if (session.isLoggedIn()) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        // Find Views
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        etPhone = findViewById(R.id.etPhone);
        btnRegister = findViewById(R.id.btnRegister);

        // ✅ Speaker Icon for Voice Guidance
        findViewById(R.id.btnSpeaker).setOnClickListener(v -> {
            VoiceGuide.announceScreen(this,
                    "یہ خریدار کی رجسٹریشن سکرین ہے۔ اپنا نام، ای میل اور پاس ورڈ لکھ کر اکاؤنٹ بنائیں۔");
        });

        // ✅ Auto Voice on Load
        VoiceGuide.announceScreen(this,
                "یہ خریدار کی رجسٹریشن سکرین ہے۔ اپنا نام، ای میل اور پاس ورڈ لکھ کر اکاؤنٹ بنائیں۔");

        btnRegister.setOnClickListener(v -> attemptRegister());
    }

    // ============================================================
    // ✅ ATTEMPT REGISTER (WITH FAILURE HANDLING)
    // ============================================================
    private void attemptRegister() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String pass = etPassword.getText().toString().trim();
        String confirmPass = etConfirmPassword.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        // ✅ Validations
        if (TextUtils.isEmpty(name)) {
            etName.setError("ضروری ہے / Required");
            VoiceGuide.explain(this, "براہ کرم اپنا نام لکھیں");
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("درست ای میل درج کریں / Invalid email");
            VoiceGuide.explain(this, "براہ کرم درست ای میل درج کریں");
            return;
        }
        if (pass.length() < 6) {
            etPassword.setError("کم از کم 6 حروف / Min 6 chars");
            VoiceGuide.explain(this, "پاس ورڈ کم از کم 6 حروف کا ہونا چاہیے");
            return;
        }
        if (!pass.equals(confirmPass)) {
            etConfirmPassword.setError("پاس ورڈ مماثل نہیں / Passwords do not match");
            VoiceGuide.explain(this, "پاس ورڈ مماثل نہیں، دوبارہ لکھیں");
            return;
        }

        btnRegister.setEnabled(false);
        btnRegister.setText("اکاؤنٹ بن رہا ہے...");

        mAuth.createUserWithEmailAndPassword(email, pass)
                .addOnSuccessListener(authResult -> {
                    String uid = authResult.getUser().getUid();
                    User user = new User(uid, name, email, phone, Constants.ROLE_BUYER);

                    // ✅ Database write with failure listener
                    FirebaseDatabase.getInstance().getReference()
                            .child(Constants.NODE_USERS).child(uid).setValue(user)
                            .addOnSuccessListener(v -> {
                                DemoDataSeeder.clearDemoDataOnFirstRealUse(this);
                                session.saveUser(uid, Constants.ROLE_BUYER, name);
                                VoiceGuide.explain(this, "خوش آمدید " + name + "، آپ کا اکاؤنٹ بن گیا ہے");
                                Toast.makeText(this, "✅ اکاؤنٹ بن گیا! / Account created!", Toast.LENGTH_LONG).show();

                                startActivity(new Intent(this, MainActivity.class));
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                btnRegister.setEnabled(true);
                                btnRegister.setText("✅ اکاؤنٹ بنائیں");
                                Toast.makeText(this, "❌ ڈیٹا محفوظ نہیں ہوا: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                VoiceGuide.explain(this, "ڈیٹا محفوظ نہیں ہوا، دوبارہ کوشش کریں");
                            });
                })
                .addOnFailureListener(e -> {
                    btnRegister.setEnabled(true);
                    btnRegister.setText("✅ اکاؤنٹ بنائیں");
                    Toast.makeText(this, "❌ رجسٹریشن ناکام: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    VoiceGuide.explain(this, "رجسٹریشن ناکام ہو گئی، دوبارہ کوشش کریں");
                });
    }

    // ✅ FIXED: Remove TTS shutdown (singleton issue)
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // TTSHelper is a singleton — don't shut it down here
    }
}