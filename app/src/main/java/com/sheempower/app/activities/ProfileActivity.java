package com.sheempower.app.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.*;
import com.sheempower.app.R;
import com.sheempower.app.models.User;
import com.sheempower.app.utils.*;
import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileActivity extends AppCompatActivity {

    private CircleImageView ivProfilePhoto;
    private TextView tvName, tvEmail, tvRoleBadge;
    private TextInputEditText etFullName, etPhone;
    private Button btnSave, btnLogout, btnMyOrders, btnChangePhoto;
    private ImageView btnSpeaker;

    private SessionManager session;
    private TTSHelper tts;
    private String currentUid;
    private Uri selectedPhotoUri;

    private static final int REQUEST_PICK_IMAGE = 1002;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        session = new SessionManager(this);
        tts = TTSHelper.getInstance(this);
        currentUid = session.getUid();

        // Find Views
        ivProfilePhoto = findViewById(R.id.ivProfilePhoto);
        tvName = findViewById(R.id.tvName);
        tvEmail = findViewById(R.id.tvEmail);
        tvRoleBadge = findViewById(R.id.tvRoleBadge);
        etFullName = findViewById(R.id.etFullName);
        etPhone = findViewById(R.id.etPhone);
        btnSave = findViewById(R.id.btnSave);
        btnLogout = findViewById(R.id.btnLogout);
        btnMyOrders = findViewById(R.id.btnMyOrders);
        btnChangePhoto = findViewById(R.id.btnChangePhoto);
        btnSpeaker = findViewById(R.id.btnSpeaker);

        // ✅ Speaker Icon for TTS Voice Guidance
        btnSpeaker.setOnClickListener(v -> {
            String instruction = "Profile screen. You can view and edit your name, phone number, and profile photo. " +
                    "Use the My Orders button to see your order history, or logout to switch accounts.";
            tts.speak(instruction);
        });

        loadUser();

        // Buttons
        btnSave.setOnClickListener(v -> saveChanges());
        btnLogout.setOnClickListener(v -> {
            tts.speak("Logging out. Goodbye.");
            session.logout();
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
        btnMyOrders.setOnClickListener(v -> {
            tts.speak("Opening your orders.");
            startActivity(new Intent(this, MyOrdersActivity.class));
        });

        // ✅ Change Profile Photo
        btnChangePhoto.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");
            startActivityForResult(intent, REQUEST_PICK_IMAGE);
        });

        // ✅ Auto-speak screen instruction
        tts.speak("Profile screen loaded.");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PICK_IMAGE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            selectedPhotoUri = data.getData();
            ivProfilePhoto.setImageURI(selectedPhotoUri);

            // Upload photo to Cloudinary
            Toast.makeText(this, "Uploading photo...", Toast.LENGTH_SHORT).show();
            CloudinaryHelper.upload(this, selectedPhotoUri, "profile_photos", url -> {
                if (url != null) {
                    // Save URL to Firebase
                    FirebaseDatabase.getInstance().getReference()
                            .child(Constants.NODE_USERS).child(currentUid)
                            .child("photoUrl").setValue(url)
                            .addOnSuccessListener(v -> Toast.makeText(this, "Photo updated!", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(this, "Failed to save photo", Toast.LENGTH_SHORT).show());
                } else {
                    Toast.makeText(this, "Upload failed", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void loadUser() {
        FirebaseDatabase.getInstance().getReference()
                .child(Constants.NODE_USERS).child(currentUid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        User user = snapshot.getValue(User.class);
                        if (user == null) return;

                        tvName.setText(user.getName());
                        tvEmail.setText(user.getEmail());
                        tvRoleBadge.setText("🎁 " + capitalize(user.getRole()));
                        etFullName.setText(user.getName());
                        etPhone.setText(user.getPhone());

                        // Load profile photo
                        String photoUrl = user.getPhotoUrl();
                        if (photoUrl != null && !photoUrl.isEmpty()) {
                            Glide.with(ProfileActivity.this)
                                    .load(photoUrl)
                                    .placeholder(R.drawable.placeholder_avatar)
                                    .into(ivProfilePhoto);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {}
                });
    }

    private void saveChanges() {
        String name = etFullName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            etFullName.setError("Required");
            return;
        }

        // Save to Firebase
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference()
                .child(Constants.NODE_USERS).child(currentUid);

        userRef.child("name").setValue(name);
        userRef.child("phone").setValue(phone)
                .addOnSuccessListener(v -> {
                    // Update session
                    session.saveUser(currentUid, session.getRole(), name);
                    tvName.setText(name);
                    Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show();
                    tts.speak("Profile updated successfully.");
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return "";
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tts != null) tts.shutdown();
    }
}