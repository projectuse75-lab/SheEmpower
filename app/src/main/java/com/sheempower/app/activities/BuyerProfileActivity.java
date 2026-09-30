package com.sheempower.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import com.sheempower.app.R;
import com.sheempower.app.models.Buyer;
import com.sheempower.app.utils.BottomNavHelper;
import com.sheempower.app.utils.Constants;
import com.sheempower.app.utils.SessionManager;
import com.sheempower.app.utils.PreferencesManager;

public class BuyerProfileActivity extends AppCompatActivity {

    private EditText etBuyerName;
    private EditText etBuyerEmail;
    private EditText etBuyerPhone;

    private Button btnCreateBuyerProfile;
    private Button btnCancelBuyerProfile;

    private FirebaseAuth auth;
    private DatabaseReference buyersRef;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buyer_profile);

        auth = FirebaseAuth.getInstance();

        buyersRef = FirebaseDatabase.getInstance()
                .getReference()
                .child(Constants.NODE_BUYERS);

        session = new SessionManager(this);

        initializeViews();
        BottomNavHelper.setupBackButton(this);
        loadExistingInformation();
        setupButtons();
    }

    private void initializeViews() {

        etBuyerName = findViewById(R.id.etBuyerName);
        etBuyerEmail = findViewById(R.id.etBuyerEmail);
        etBuyerPhone = findViewById(R.id.etBuyerPhone);

        btnCreateBuyerProfile =
                findViewById(R.id.btnCreateBuyerProfile);

        btnCancelBuyerProfile =
                findViewById(R.id.btnCancelBuyerProfile);
    }

    private void loadExistingInformation() {

        String name = session.getName();

        if (!TextUtils.isEmpty(name)) {
            etBuyerName.setText(name);
        }

        if (auth.getCurrentUser() != null) {

            String email = auth.getCurrentUser().getEmail();

            if (email != null) {
                etBuyerEmail.setText(email);
            }
        }
    }

    private void setupButtons() {

        btnCreateBuyerProfile.setOnClickListener(v ->
                createBuyerProfile()
        );

        btnCancelBuyerProfile.setOnClickListener(v ->
                finish()
        );
    }

    private void createBuyerProfile() {

        String uid = session.getUid();

        if (uid == null) {
            Toast.makeText(
                    this,
                    "Session expired. Please login again.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        String name =
                etBuyerName.getText().toString().trim();

        String email =
                etBuyerEmail.getText().toString().trim();

        String phone =
                etBuyerPhone.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            etBuyerName.setError("Name required");
            etBuyerName.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(email)) {
            etBuyerEmail.setError("Email required");
            etBuyerEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(phone)) {
            etBuyerPhone.setError("Phone / WhatsApp required");
            etBuyerPhone.requestFocus();
            return;
        }

        btnCreateBuyerProfile.setEnabled(false);

        Buyer buyer = new Buyer(
                uid,
                name,
                email,
                phone,
                "",
                true
        );

        buyersRef.child(uid)
                .setValue(buyer)
                .addOnSuccessListener(unused -> {

                    // Buyer profile successfully created
                    session.saveName(name);

                    // IMPORTANT:
                    // Ab seller logout nahi hoga.
                    // Sirf current mode buyer hoga.
                    session.switchRole(Constants.ROLE_BUYER);

                    Toast.makeText(
                            BuyerProfileActivity.this,
                            "Buyer profile created successfully",
                            Toast.LENGTH_SHORT
                    ).show();

                    Intent intent = new Intent(
                            BuyerProfileActivity.this,
                            MainActivity.class
                    );

                    intent.addFlags(
                            Intent.FLAG_ACTIVITY_CLEAR_TOP |
                                    Intent.FLAG_ACTIVITY_NEW_TASK
                    );

                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {

                    btnCreateBuyerProfile.setEnabled(true);

                    Toast.makeText(
                            BuyerProfileActivity.this,
                            "Profile save failed: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
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