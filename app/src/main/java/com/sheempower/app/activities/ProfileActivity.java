package com.sheempower.app.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.*;
import com.sheempower.app.R;
import com.sheempower.app.models.User;
import com.sheempower.app.utils.*;
import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileActivity extends AppCompatActivity {

    private CircleImageView ivProfilePic;
    private ImageButton btnChangePic;
    private TextView tvName, tvEmail, tvRoleBadge;
    private TextInputEditText etFullName, etPhone;
    private Button btnSave, btnLogout, btnMyOrders;

    private SessionManager session;

    private final ActivityResultLauncher<String> imagePicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) uploadProfilePic(uri);
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        session = new SessionManager(this);

        VoiceGuide.announceScreen(this,
                "یہ آپ کی پروفائل ہے۔ تصویر بدلنے کے لیے کیمرے کے آئیکن پر کلک کریں۔ نیچے اپنا نام اور فون نمبر بدل کر محفوظ کریں کا بٹن دبائیں۔ میرے آرڈرز پر کلک کر کے اپنے آرڈرز کی حالت دیکھیں۔");

        ivProfilePic = findViewById(R.id.ivProfilePic);
        btnChangePic = findViewById(R.id.btnChangePic);
        tvName      = findViewById(R.id.tvName);
        tvEmail     = findViewById(R.id.tvEmail);
        tvRoleBadge = findViewById(R.id.tvRoleBadge);
        etFullName  = findViewById(R.id.etFullName);
        etPhone     = findViewById(R.id.etPhone);
        btnSave     = findViewById(R.id.btnSave);
        btnLogout   = findViewById(R.id.btnLogout);
        btnMyOrders = findViewById(R.id.btnMyOrders);

        loadUser();

        btnChangePic.setOnClickListener(v -> imagePicker.launch("image/*"));

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

    private void uploadProfilePic(Uri uri) {
        Toast.makeText(this, "تصویر اپلوڈ ہو رہی ہے...", Toast.LENGTH_SHORT).show();
        CloudinaryHelper.upload(this, uri, "profile_pictures", (url, error) -> {
            if (url != null) {
                Glide.with(this).load(url).into(ivProfilePic);
                FirebaseDatabase.getInstance().getReference().child(Constants.NODE_USERS)
                        .child(session.getUid()).child("imageUrl").setValue(url)
                        .addOnSuccessListener(v -> Toast.makeText(this, "تصویر تبدیل ہو گئی", Toast.LENGTH_SHORT).show());
            } else {
                Toast.makeText(this, "اپلوڈ ناکام: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void loadUser() {
        FirebaseDatabase.getInstance().getReference().child(Constants.NODE_USERS).child(session.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(DataSnapshot snapshot) {
                        User user = snapshot.getValue(User.class);
                        if (user == null) return;
                        tvName.setText(user.getName());
                        tvEmail.setText(user.getEmail());
                        tvRoleBadge.setText(capitalize(user.getRole()));
                        etFullName.setText(user.getName());
                        etPhone.setText(user.getPhone());
                        if (user.getImageUrl() != null && !user.getImageUrl().isEmpty())
                            Glide.with(ProfileActivity.this).load(user.getImageUrl()).into(ivProfilePic);
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