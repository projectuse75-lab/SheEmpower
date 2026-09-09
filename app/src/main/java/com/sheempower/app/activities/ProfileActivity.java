package com.sheempower.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.*;
import com.sheempower.app.R;
import com.sheempower.app.models.User;
import com.sheempower.app.utils.BottomNavHelper;
import com.sheempower.app.utils.Constants;
import com.sheempower.app.utils.SessionManager;

public class ProfileActivity extends AppCompatActivity {

    private TextView tvName, tvEmail, tvRoleBadge;
    private TextInputEditText etFullName, etPhone;
    private Button btnSave, btnLogout, btnMyOrders;

    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        session = new SessionManager(this);

        tvName      = findViewById(R.id.tvName);
        tvEmail     = findViewById(R.id.tvEmail);
        tvRoleBadge = findViewById(R.id.tvRoleBadge);
        etFullName  = findViewById(R.id.etFullName);
        etPhone     = findViewById(R.id.etPhone);
        btnSave     = findViewById(R.id.btnSave);
        btnLogout   = findViewById(R.id.btnLogout);
        btnMyOrders = findViewById(R.id.btnMyOrders);

        loadUser();

        btnSave.setOnClickListener(v -> saveChanges());

        btnLogout.setOnClickListener(v -> {
            session.logout();
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });

        btnMyOrders.setOnClickListener(v -> startActivity(new Intent(this, MyOrdersActivity.class)));

        if (session.isSeller())
            BottomNavHelper.setupForSeller(this, findViewById(R.id.bottomNav), 3);
        else
            BottomNavHelper.setupForBuyerOrVisitor(this, findViewById(R.id.bottomNav), 3);
    }

    private void loadUser() {
        FirebaseDatabase.getInstance().getReference().child(Constants.NODE_USERS).child(session.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(DataSnapshot snapshot) {
                        User user = snapshot.getValue(User.class);
                        if (user == null) {
                            Toast.makeText(ProfileActivity.this, "صارف کا ڈیٹا بیس میں ریکارڈ نہیں ملا\nUID: " + session.getUid(), Toast.LENGTH_LONG).show();
                            return;
                        }
                        tvName.setText(user.getName());
                        tvEmail.setText(user.getEmail());
                        tvRoleBadge.setText(capitalize(user.getRole()));
                        etFullName.setText(user.getName());
                        etPhone.setText(user.getPhone());
                    }
                    @Override public void onCancelled(DatabaseError error) {}
                });
    }

    private void saveChanges() {
        String name  = etFullName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        if (TextUtils.isEmpty(name)) { etFullName.setError("ضروری ہے"); return; }

        FirebaseDatabase.getInstance().getReference().child(Constants.NODE_USERS).child(session.getUid())
                .child("name").setValue(name);
        FirebaseDatabase.getInstance().getReference().child(Constants.NODE_USERS).child(session.getUid())
                .child("phone").setValue(phone)
                .addOnSuccessListener(v -> {
                    session.saveUser(session.getUid(), session.getRole(), name);
                    Toast.makeText(this, "معلومات محفوظ ہو گئیں", Toast.LENGTH_SHORT).show();
                    tvName.setText(name);
                });
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return "";
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}