package com.sheempower.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.sheempower.app.R;
import com.sheempower.app.utils.Constants;
import com.sheempower.app.utils.PreferencesManager;
import com.sheempower.app.utils.SessionManager;

public class SettingsActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private PreferencesManager preferencesManager;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference database;

    // Account
    private TextView tvUserName;
    private TextView tvUserEmail;
    private TextView tvUserContact;

    // Switches
    private Switch switchNotifications;
    private Switch switchDarkTheme;

    // Account options
    private LinearLayout llChangeName;
    private LinearLayout llChangeContact;
    private LinearLayout llChangePassword;

    // Information
    private LinearLayout llPrivacyPolicy;
    private LinearLayout llTermsConditions;
    private LinearLayout llAboutApp;

    // Buttons
    private Button btnLogout;
    private Button btnDeleteAccount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Apply saved theme before loading UI
        preferencesManager = new PreferencesManager(this);
        preferencesManager.applyTheme();

        setContentView(R.layout.activity_settings);

        sessionManager = new SessionManager(this);
        firebaseAuth = FirebaseAuth.getInstance();

        database = FirebaseDatabase
                .getInstance()
                .getReference();

        initializeViews();
        loadUserInformation();
        loadSettings();
        setupListeners();
    }

    // =========================================================
    // INITIALIZE
    // =========================================================

    private void initializeViews() {

        tvUserName = findViewById(R.id.tvUserName);
        tvUserEmail = findViewById(R.id.tvUserEmail);
        tvUserContact = findViewById(R.id.tvUserContact);

        switchNotifications = findViewById(R.id.switchNotifications);
        switchDarkTheme = findViewById(R.id.switchDarkTheme);

        llChangeName = findViewById(R.id.llChangeName);
        llChangeContact = findViewById(R.id.llChangeContact);
        llChangePassword = findViewById(R.id.llChangePassword);

        llPrivacyPolicy = findViewById(R.id.llPrivacyPolicy);
        llTermsConditions = findViewById(R.id.llTermsConditions);
        llAboutApp = findViewById(R.id.llAboutApp);

        btnLogout = findViewById(R.id.btnLogout);
        btnDeleteAccount = findViewById(R.id.btnDeleteAccount);
    }

    // =========================================================
    // LOAD USER
    // =========================================================

    private void loadUserInformation() {

        String name = sessionManager.getName();

        if (name == null || name.trim().isEmpty()) {
            name = "She Empower User";
        }

        tvUserName.setText(name);

        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();

        if (firebaseUser != null) {

            String email = firebaseUser.getEmail();

            if (email == null || email.isEmpty()) {
                email = "Email not available";
            }

            tvUserEmail.setText(email);

            loadContactFromDatabase();
        } else {
            tvUserEmail.setText("Not logged in");
            tvUserContact.setText("Contact not available");
        }
    }

    private void loadContactFromDatabase() {

        String uid = sessionManager.getUid();

        if (uid == null || uid.isEmpty()) {
            tvUserContact.setText("Contact not available");
            return;
        }

        database.child(Constants.NODE_USERS)
                .child(uid)
                .child("phone")
                .get()
                .addOnSuccessListener(snapshot -> {

                    String phone = snapshot.getValue(String.class);

                    if (phone == null || phone.trim().isEmpty()) {
                        tvUserContact.setText("Contact not added");
                    } else {
                        tvUserContact.setText(phone);
                    }
                })
                .addOnFailureListener(e ->
                        tvUserContact.setText("Contact not available")
                );
    }

    // =========================================================
    // LOAD SETTINGS
    // =========================================================

    private void loadSettings() {

        switchNotifications.setChecked(
                preferencesManager.areNotificationsEnabled()
        );

        switchDarkTheme.setChecked(
                preferencesManager.isDarkThemeEnabled()
        );
    }

    // =========================================================
    // LISTENERS
    // =========================================================

    private void setupListeners() {

        // Change Name
        llChangeName.setOnClickListener(v ->
                showChangeNameDialog()
        );

        // Change Contact
        llChangeContact.setOnClickListener(v ->
                showChangeContactDialog()
        );

        // Change Password
        llChangePassword.setOnClickListener(v ->
                showChangePasswordDialog()
        );

        // Notifications
        switchNotifications.setOnCheckedChangeListener(
                (buttonView, isChecked) -> {

                    preferencesManager.setNotificationsEnabled(isChecked);

                    if (isChecked) {
                        Toast.makeText(
                                this,
                                "🔔 Notifications ON",
                                Toast.LENGTH_SHORT
                        ).show();
                    } else {
                        Toast.makeText(
                                this,
                                "🔕 Notifications OFF",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
        );

        // Dark Theme
        switchDarkTheme.setOnCheckedChangeListener(
                (buttonView, isChecked) -> {

                    preferencesManager.setDarkThemeEnabled(isChecked);

                    if (isChecked) {

                        AppCompatDelegate.setDefaultNightMode(
                                AppCompatDelegate.MODE_NIGHT_YES
                        );

                    } else {

                        AppCompatDelegate.setDefaultNightMode(
                                AppCompatDelegate.MODE_NIGHT_NO
                        );
                    }
                }
        );

        // Privacy
        llPrivacyPolicy.setOnClickListener(v ->
                showPrivacyPolicy()
        );

        // Terms
        llTermsConditions.setOnClickListener(v ->
                showTermsConditions()
        );

        // About
        llAboutApp.setOnClickListener(v ->
                showAboutDialog()
        );

        // Logout
        btnLogout.setOnClickListener(v ->
                showLogoutConfirmation()
        );

        // Delete
        btnDeleteAccount.setOnClickListener(v ->
                showDeleteAccountConfirmation()
        );
    }

    // =========================================================
    // CHANGE NAME
    // =========================================================

    private void showChangeNameDialog() {

        EditText input = new EditText(this);

        input.setSingleLine(true);
        input.setHint("Enter your name");
        input.setText(sessionManager.getName());
        input.setSelectAllOnFocus(true);

        int padding = 30;
        input.setPadding(padding, padding, padding, padding);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("👤 Change Name")
                .setMessage("Apna naam enter karein")
                .setView(input)
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(d -> {

            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setOnClickListener(v -> {

                        String newName =
                                input.getText().toString().trim();

                        if (newName.isEmpty()) {

                            input.setError("Name required");
                            return;
                        }

                        updateName(newName, dialog);
                    });
        });

        dialog.show();
    }

    private void updateName(
            String newName,
            AlertDialog dialog
    ) {

        String uid = sessionManager.getUid();

        if (uid == null) {
            Toast.makeText(
                    this,
                    "User not logged in",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        database.child(Constants.NODE_USERS)
                .child(uid)
                .child("name")
                .setValue(newName)
                .addOnSuccessListener(unused -> {

                    sessionManager.saveName(newName);

                    tvUserName.setText(newName);

                    Toast.makeText(
                            this,
                            "Name updated successfully ✓",
                            Toast.LENGTH_SHORT
                    ).show();

                    dialog.dismiss();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(
                                this,
                                "Name update failed: " + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    // =========================================================
    // CHANGE CONTACT
    // =========================================================

    private void showChangeContactDialog() {

        EditText input = new EditText(this);

        input.setSingleLine(true);
        input.setHint("03XXXXXXXXX");
        input.setInputType(InputType.TYPE_CLASS_PHONE);

        String currentContact =
                tvUserContact.getText().toString();

        if (!currentContact.equals("Contact not added") &&
                !currentContact.equals("Contact not available")) {

            input.setText(currentContact);
            input.setSelectAllOnFocus(true);
        }

        int padding = 30;
        input.setPadding(padding, padding, padding, padding);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("📱 Change Contact")
                .setMessage("Apna WhatsApp / contact number enter karein")
                .setView(input)
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(d -> {

            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setOnClickListener(v -> {

                        String newContact =
                                input.getText().toString().trim();

                        if (newContact.isEmpty()) {

                            input.setError("Contact required");
                            return;
                        }

                        updateContact(newContact, dialog);
                    });
        });

        dialog.show();
    }

    private void updateContact(
            String newContact,
            AlertDialog dialog
    ) {

        String uid = sessionManager.getUid();

        if (uid == null) {
            return;
        }

        database.child(Constants.NODE_USERS)
                .child(uid)
                .child("phone")
                .setValue(newContact)
                .addOnSuccessListener(unused -> {

                    // Seller ke liye seller profile bhi update karo
                    if (sessionManager.isSeller()) {

                        database.child(Constants.NODE_SELLERS)
                                .child(uid)
                                .child("phone")
                                .setValue(newContact);

                        database.child(Constants.NODE_SELLERS)
                                .child(uid)
                                .child("whatsappNumber")
                                .setValue(newContact);
                    }

                    tvUserContact.setText(newContact);

                    Toast.makeText(
                            this,
                            "Contact updated successfully ✓",
                            Toast.LENGTH_SHORT
                    ).show();

                    dialog.dismiss();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(
                                this,
                                "Contact update failed: " + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    // =========================================================
    // CHANGE PASSWORD
    // =========================================================

    private void showChangePasswordDialog() {

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        int padding = 35;
        layout.setPadding(padding, 10, padding, 0);

        EditText oldPassword = new EditText(this);
        oldPassword.setHint("Current Password");
        oldPassword.setInputType(
                InputType.TYPE_CLASS_TEXT |
                        InputType.TYPE_TEXT_VARIATION_PASSWORD
        );

        EditText newPassword = new EditText(this);
        newPassword.setHint("New Password");
        newPassword.setInputType(
                InputType.TYPE_CLASS_TEXT |
                        InputType.TYPE_TEXT_VARIATION_PASSWORD
        );

        EditText confirmPassword = new EditText(this);
        confirmPassword.setHint("Confirm New Password");
        confirmPassword.setInputType(
                InputType.TYPE_CLASS_TEXT |
                        InputType.TYPE_TEXT_VARIATION_PASSWORD
        );

        layout.addView(oldPassword);
        layout.addView(newPassword);
        layout.addView(confirmPassword);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("🔐 Change Password")
                .setMessage("Naya password set karein")
                .setView(layout)
                .setPositiveButton("Change", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(d -> {

            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setOnClickListener(v -> {

                        String oldPass =
                                oldPassword.getText().toString();

                        String newPass =
                                newPassword.getText().toString();

                        String confirmPass =
                                confirmPassword.getText().toString();

                        if (oldPass.isEmpty()) {
                            oldPassword.setError(
                                    "Current password required"
                            );
                            return;
                        }

                        if (newPass.length() < 6) {
                            newPassword.setError(
                                    "Password must be at least 6 characters"
                            );
                            return;
                        }

                        if (!newPass.equals(confirmPass)) {
                            confirmPassword.setError(
                                    "Passwords do not match"
                            );
                            return;
                        }

                        changePassword(
                                oldPass,
                                newPass,
                                dialog
                        );
                    });
        });

        dialog.show();
    }

    private void changePassword(
            String oldPassword,
            String newPassword,
            AlertDialog dialog
    ) {

        FirebaseUser user = firebaseAuth.getCurrentUser();

        if (user == null ||
                user.getEmail() == null) {

            Toast.makeText(
                    this,
                    "User session not found",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String email = user.getEmail();

        firebaseAuth
                .signInWithEmailAndPassword(
                        email,
                        oldPassword
                )
                .addOnSuccessListener(authResult -> {

                    FirebaseUser currentUser =
                            firebaseAuth.getCurrentUser();

                    if (currentUser == null) {
                        return;
                    }

                    currentUser.updatePassword(newPassword)
                            .addOnSuccessListener(unused -> {

                                Toast.makeText(
                                        this,
                                        "Password changed successfully ✓",
                                        Toast.LENGTH_LONG
                                ).show();

                                dialog.dismiss();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(
                                            this,
                                            "Password change failed: "
                                                    + e.getMessage(),
                                            Toast.LENGTH_LONG
                                    ).show()
                            );
                })
                .addOnFailureListener(e ->
                        Toast.makeText(
                                this,
                                "Current password is incorrect ✗",
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    // =========================================================
    // PRIVACY POLICY
    // =========================================================

    private void showPrivacyPolicy() {

        new AlertDialog.Builder(this)
                .setTitle("🔒 Privacy Policy")
                .setMessage(
                        "She Empower respects your privacy.\n\n" +
                                "• Your account information is used to provide app services.\n\n" +
                                "• Seller information is used for seller profile and verification.\n\n" +
                                "• Payment proof is used only for order verification.\n\n" +
                                "• Your information is not intentionally shared for unrelated purposes.\n\n" +
                                "Please contact the She Empower administration for any privacy-related concern."
                )
                .setPositiveButton("OK", null)
                .show();
    }

    // =========================================================
    // TERMS & CONDITIONS
    // =========================================================

    private void showTermsConditions() {

        new AlertDialog.Builder(this)
                .setTitle("📜 Terms & Conditions")
                .setMessage(
                        "By using She Empower, you agree that:\n\n" +
                                "• Users must provide accurate account information.\n\n" +
                                "• Sellers are responsible for the accuracy of their business information and products.\n\n" +
                                "• Buyers should verify order details before placing an order.\n\n" +
                                "• Payment and delivery arrangements must follow the options provided by the app.\n\n" +
                                "• Users must not misuse the platform or provide misleading information.\n\n" +
                                "• She Empower may restrict accounts that violate the app rules."
                )
                .setPositiveButton("I Understand", null)
                .show();
    }

    // =========================================================
    // ABOUT
    // =========================================================

    private void showAboutDialog() {

        new AlertDialog.Builder(this)
                .setTitle("💗 About She Empower")
                .setMessage(
                        "She Empower\n\n" +
                                "Version 1.0\n\n" +
                                "A platform designed to connect women sellers " +
                                "with buyers and help women showcase their skills " +
                                "and small businesses.\n\n" +
                                "Empowering women through skills, business and opportunities. 💗"
                )
                .setPositiveButton("OK", null)
                .show();
    }

    // =========================================================
    // LOGOUT
    // =========================================================

    private void showLogoutConfirmation() {

        new AlertDialog.Builder(this)
                .setTitle("🚪 Logout")
                .setMessage(
                        "Kya aap logout karna chahti hain?"
                )
                .setPositiveButton(
                        "Yes",
                        (dialog, which) -> logout()
                )
                .setNegativeButton(
                        "No",
                        null
                )
                .show();
    }

    private void logout() {

        firebaseAuth.signOut();
        sessionManager.logout();
        preferencesManager.clear();

        Intent intent = new Intent(
                SettingsActivity.this,
                LoginActivity.class
        );

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }

    // =========================================================
    // DELETE ACCOUNT
    // =========================================================

    private void showDeleteAccountConfirmation() {

        new AlertDialog.Builder(this)
                .setTitle("🗑️ Delete Account")
                .setMessage(
                        "⚠️ Yeh action permanent hai.\n\n" +
                                "Aapka account aur related data delete ho jayega.\n\n" +
                                "Kya aap continue karna chahti hain?"
                )
                .setPositiveButton(
                        "Delete",
                        (dialog, which) ->
                                showPasswordConfirmation()
                )
                .setNegativeButton(
                        "Cancel",
                        null
                )
                .show();
    }

    private void showPasswordConfirmation() {

        EditText input = new EditText(this);

        input.setHint("Password");

        input.setInputType(
                InputType.TYPE_CLASS_TEXT |
                        InputType.TYPE_TEXT_VARIATION_PASSWORD
        );

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Confirm Password")
                .setMessage(
                        "Account delete karne ke liye password enter karein."
                )
                .setView(input)
                .setPositiveButton("Delete", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(d -> {

            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setOnClickListener(v -> {

                        String password =
                                input.getText().toString();

                        if (password.isEmpty()) {
                            input.setError("Password required");
                            return;
                        }

                        deleteAccount(password, dialog);
                    });
        });

        dialog.show();
    }

    private void deleteAccount(
            String password,
            AlertDialog dialog
    ) {

        FirebaseUser user =
                firebaseAuth.getCurrentUser();

        if (user == null ||
                user.getEmail() == null) {

            Toast.makeText(
                    this,
                    "User session not found",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String email = user.getEmail();

        firebaseAuth
                .signInWithEmailAndPassword(
                        email,
                        password
                )
                .addOnSuccessListener(authResult -> {

                    FirebaseUser currentUser =
                            firebaseAuth.getCurrentUser();

                    if (currentUser == null) {
                        return;
                    }

                    String uid =
                            sessionManager.getUid();

                    currentUser.delete()
                            .addOnSuccessListener(unused -> {

                                if (uid != null) {

                                    database.child(Constants.NODE_USERS)
                                            .child(uid)
                                            .removeValue();

                                    database.child(Constants.NODE_SELLERS)
                                            .child(uid)
                                            .removeValue();

                                    database.child("buyers")
                                            .child(uid)
                                            .removeValue();
                                }

                                sessionManager.logout();
                                preferencesManager.clear();

                                Intent intent =
                                        new Intent(
                                                SettingsActivity.this,
                                                LoginActivity.class
                                        );

                                intent.setFlags(
                                        Intent.FLAG_ACTIVITY_NEW_TASK |
                                                Intent.FLAG_ACTIVITY_CLEAR_TASK
                                );

                                startActivity(intent);
                                finish();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(
                                            this,
                                            "Delete failed: "
                                                    + e.getMessage(),
                                            Toast.LENGTH_LONG
                                    ).show()
                            );
                })
                .addOnFailureListener(e ->
                        Toast.makeText(
                                this,
                                "Wrong password ✗",
                                Toast.LENGTH_SHORT
                        ).show()
                );
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}