package com.sheempower.app.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.database.FirebaseDatabase;
import com.sheempower.app.R;
import com.sheempower.app.dialogs.ProfileDetailsDialog;
import com.sheempower.app.helpers.DarkThemeHelper;
import com.sheempower.app.utils.Constants;
import com.sheempower.app.utils.SessionManager;

public class ProfilePictureActivity extends AppCompatActivity {

    private Button btnSelectImage;
    private ImageView ivProfileImage;
    private Uri selectedImageUri;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Dark theme apply BEFORE setContentView
        DarkThemeHelper.applyTheme(this);

        setContentView(R.layout.activity_profile_picture);

        // Initialize
        btnSelectImage = findViewById(R.id.btn_select_image);
        ivProfileImage = findViewById(R.id.iv_profile_image);
        sessionManager = new SessionManager(this);

        // Button click - Select Image from Gallery
        btnSelectImage.setOnClickListener(v -> openGallery());

        // Image click - Show details dialog or open gallery if none selected
        ivProfileImage.setOnClickListener(v -> {
            if (selectedImageUri != null) {
                showProfileDetails();
            } else {
                Toast.makeText(this,
                        "گیلری سے تصویر منتخب کریں / Select image from gallery",
                        Toast.LENGTH_SHORT).show();
                openGallery();
            }
        });

        // Load existing user profile image from Firebase if available
        loadFirebaseProfileImage();
    }

    private void loadFirebaseProfileImage() {
        String uid = sessionManager.getUid();
        if (uid != null && !uid.trim().isEmpty()) {
            FirebaseDatabase.getInstance().getReference()
                    .child(Constants.NODE_USERS)
                    .child(uid.trim())
                    .child("imageUrl")
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        String imgUrl = snapshot.getValue(String.class);
                        if (imgUrl != null && !imgUrl.trim().isEmpty()) {
                            selectedImageUri = Uri.parse(imgUrl.trim());
                            if (!isFinishing() && !isDestroyed()) {
                                Glide.with(this).load(imgUrl.trim()).into(ivProfileImage);
                            }
                        }
                    });
        }
    }

    // =========================================================
    // GALLERY SE IMAGE LENA
    // =========================================================

    private void openGallery() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, 100);  // Request code = 100
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();

            if (selectedImageUri != null) {
                ivProfileImage.setImageURI(selectedImageUri);

                Toast.makeText(this,
                        "تصویر منتخب ہو گئی! اپنی معلومات درج کریں / Image selected! Enter your details",
                        Toast.LENGTH_SHORT).show();

                // Open edit details dialog immediately
                showEditDetailsDialog();
            }
        }
    }

    // =========================================================
    // EDIT DETAILS DIALOG (NAME, FATHER NAME, CLASS)
    // =========================================================

    private void showEditDetailsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("📝 اپنی معلومات درج کریں / Enter Your Details");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 24);

        // Name field
        final EditText etName = new EditText(this);
        etName.setHint("👤 نام / Name");
        etName.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        String currentName = sessionManager.getUserName();
        if (!TextUtils.isEmpty(currentName)) {
            etName.setText(currentName);
        }
        layout.addView(etName);

        // Father Name field
        final EditText etFatherName = new EditText(this);
        etFatherName.setHint("👨‍👦 والد کا نام / Father Name");
        etFatherName.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        String currentFatherName = sessionManager.getFatherName();
        if (!TextUtils.isEmpty(currentFatherName)) {
            etFatherName.setText(currentFatherName);
        }
        layout.addView(etFatherName);

        // Class field
        final EditText etClass = new EditText(this);
        etClass.setHint("🎓 کلاس / Class (e.g. 10th, BS CS)");
        etClass.setInputType(InputType.TYPE_CLASS_TEXT);
        String currentClass = sessionManager.getUserClass();
        if (!TextUtils.isEmpty(currentClass)) {
            etClass.setText(currentClass);
        }
        layout.addView(etClass);

        builder.setView(layout);

        builder.setPositiveButton("محفوظ کریں / Save", (dialog, which) -> {
            String name = etName.getText().toString().trim();
            String fatherName = etFatherName.getText().toString().trim();
            String userClass = etClass.getText().toString().trim();

            if (!TextUtils.isEmpty(name)) {
                sessionManager.saveName(name);
            }
            if (!TextUtils.isEmpty(fatherName)) {
                sessionManager.saveFatherName(fatherName);
            }
            if (!TextUtils.isEmpty(userClass)) {
                sessionManager.saveUserClass(userClass);
            }

            // Save to Firebase database if user is logged in
            String uid = sessionManager.getUid();
            if (uid != null && !uid.trim().isEmpty()) {
                FirebaseDatabase.getInstance().getReference()
                        .child(Constants.NODE_USERS)
                        .child(uid.trim())
                        .child("fatherName")
                        .setValue(fatherName);

                FirebaseDatabase.getInstance().getReference()
                        .child(Constants.NODE_USERS)
                        .child(uid.trim())
                        .child("studentClass")
                        .setValue(userClass);
            }

            Toast.makeText(this, "معلومات محفوظ ہو گئیں! / Details Saved!", Toast.LENGTH_SHORT).show();

            // Open Profile Details Dialog immediately
            showProfileDetails();
        });

        builder.setNegativeButton("منسوخ کریں / Cancel", (dialog, which) -> dialog.dismiss());

        builder.show();
    }

    // =========================================================
    // FULL SCREEN DIALOG ME DETAILS DIKHANA
    // =========================================================

    private void showProfileDetails() {
        if (selectedImageUri == null) {
            Toast.makeText(this,
                    "پہلے تصویر منتخب کریں / Select image first",
                    Toast.LENGTH_SHORT).show();
            openGallery();
            return;
        }

        String userName = sessionManager.getUserName();
        String fatherName = sessionManager.getFatherName();
        String userClass = sessionManager.getUserClass();

        ProfileDetailsDialog dialog = new ProfileDetailsDialog(
                this,
                userName,
                fatherName,
                userClass,
                selectedImageUri,
                this::shareProfileImage,
                this::showEditDetailsDialog
        );

        dialog.show();
    }

    // =========================================================
    // SHARE KAREIN
    // =========================================================

    private void shareProfileImage() {
        if (selectedImageUri == null) {
            Toast.makeText(this,
                    "تصویر منتخب نہیں ہے",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        String userName = sessionManager.getUserName();
        String fatherName = sessionManager.getFatherName();
        String userClass = sessionManager.getUserClass();

        if (TextUtils.isEmpty(userName)) userName = "She Empower Student";
        if (TextUtils.isEmpty(fatherName)) fatherName = "---";
        if (TextUtils.isEmpty(userClass)) userClass = "---";

        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);

        String scheme = selectedImageUri.getScheme();
        String details = "👤 نام / Name: " + userName + "\n" +
                "👨‍👦 والد کا نام / Father Name: " + fatherName + "\n" +
                "🎓 کلاس / Class: " + userClass + "\n\n" +
                "🌟 She Empower - پاکستانی خواتین کے لیے\n" +
                "💪 Empowering Pakistani Women";

        if (scheme != null && (scheme.equals("http") || scheme.equals("https"))) {
            shareIntent.setType("text/plain");
            details += "\n🖼️ تصویر / Image: " + selectedImageUri.toString();
            shareIntent.putExtra(Intent.EXTRA_TEXT, details);
        } else {
            shareIntent.setType("image/*");
            shareIntent.putExtra(Intent.EXTRA_STREAM, selectedImageUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            shareIntent.putExtra(Intent.EXTRA_TEXT, details);
        }

        startActivity(Intent.createChooser(shareIntent,
                "اپنی پروفائل شیئر کریں / Share Your Profile"));
    }
}
