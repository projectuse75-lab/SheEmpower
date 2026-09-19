package com.sheempower.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.sheempower.app.R;
import com.sheempower.app.utils.PreferencesManager;
import com.sheempower.app.utils.SessionManager;

public class SettingsActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private PreferencesManager preferencesManager;
    private FirebaseAuth firebaseAuth;

    // UI Components
    private Switch switchNotifications;
    private Switch switchDarkTheme;
    private LinearLayout llPrivacyPolicy;
    private LinearLayout llAboutApp;
    private Button btnLogout;
    private Button btnDeleteAccount;
    private TextView tvUserEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Initialize
        sessionManager = new SessionManager(this);
        preferencesManager = new PreferencesManager(this);
        firebaseAuth = FirebaseAuth.getInstance();

        // Back Button
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Settings");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Find Views
        tvUserEmail = findViewById(R.id.tvUserEmail);
        switchNotifications = findViewById(R.id.switchNotifications);
        switchDarkTheme = findViewById(R.id.switchDarkTheme);
        llPrivacyPolicy = findViewById(R.id.llPrivacyPolicy);
        llAboutApp = findViewById(R.id.llAboutApp);
        btnLogout = findViewById(R.id.btnLogout);
        btnDeleteAccount = findViewById(R.id.btnDeleteAccount);

        // Load Settings
        loadSettings();

        // Set Listeners
        setupListeners();
    }

    private void loadSettings() {

        // Show User Email
        String email = sessionManager.getEmail();

        tvUserEmail.setText(
                email != null ? email : "Not set"
        );

        // Load Preferences
        boolean notificationsEnabled =
                preferencesManager.areNotificationsEnabled();

        boolean darkThemeEnabled =
                preferencesManager.isDarkThemeEnabled();

        switchNotifications.setChecked(notificationsEnabled);
        switchDarkTheme.setChecked(darkThemeEnabled);
    }

    private void setupListeners() {

        // =========================================================
        // Notifications Toggle
        // =========================================================

        switchNotifications.setOnCheckedChangeListener(
                (buttonView, isChecked) -> {

                    preferencesManager.setNotificationsEnabled(isChecked);

                    Toast.makeText(
                            this,
                            isChecked
                                    ? "Notifications ON ✓"
                                    : "Notifications OFF",
                            Toast.LENGTH_SHORT
                    ).show();
                }
        );


        // =========================================================
        // Dark Theme Toggle
        // =========================================================

        switchDarkTheme.setOnCheckedChangeListener(
                (buttonView, isChecked) -> {

                    preferencesManager.setDarkThemeEnabled(isChecked);

                    Toast.makeText(
                            this,
                            isChecked
                                    ? "Dark Theme ON 🌙"
                                    : "Light Theme ON ☀️",
                            Toast.LENGTH_SHORT
                    ).show();

                    // App restart optional
                    // recreate();
                }
        );


        // =========================================================
        // Privacy Policy
        // =========================================================

        llPrivacyPolicy.setOnClickListener(v -> {

            Toast.makeText(
                    this,
                    "Privacy Policy not available yet.",
                    Toast.LENGTH_SHORT
            ).show();
        });


        // =========================================================
        // About App
        // =========================================================

        llAboutApp.setOnClickListener(v -> {
            showAboutDialog();
        });


        // =========================================================
        // Logout
        // =========================================================

        btnLogout.setOnClickListener(v -> {
            showLogoutConfirmation();
        });


        // =========================================================
        // Delete Account
        // =========================================================

        btnDeleteAccount.setOnClickListener(v -> {
            showDeleteAccountConfirmation();
        });
    }


    // =============================================================
    // LOGOUT CONFIRMATION
    // =============================================================

    private void showLogoutConfirmation() {

        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("کیا آپ logout کرنا چاہتے ہیں؟")
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


    // =============================================================
    // LOGOUT
    // =============================================================

    private void logout() {

        try {

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

            Toast.makeText(
                    this,
                    "Logout successful ✓",
                    Toast.LENGTH_SHORT
            ).show();

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Error: " + e.getMessage(),
                    Toast.LENGTH_SHORT
            ).show();
        }
    }


    // =============================================================
    // DELETE ACCOUNT CONFIRMATION
    // =============================================================

    private void showDeleteAccountConfirmation() {

        new AlertDialog.Builder(this)
                .setTitle("Delete Account")
                .setMessage(
                        "⚠️ یہ عمل مستقل ہے۔ آپ کا تمام ڈیٹا حذف ہو جائے گا۔\n\n" +
                                "کیا آپ اپنا اکاؤنٹ حذف کرنا چاہتے ہیں؟"
                )
                .setPositiveButton(
                        "Delete",
                        (dialog, which) -> showPasswordConfirmation()
                )
                .setNegativeButton(
                        "Cancel",
                        null
                )
                .show();
    }


    // =============================================================
    // PASSWORD CONFIRMATION
    // =============================================================

    private void showPasswordConfirmation() {

        AlertDialog.Builder builder =
                new AlertDialog.Builder(this);

        builder.setTitle("Confirm Password");

        builder.setMessage(
                "اپنا پاس ورڈ درج کریں:"
        );

        final EditText input =
                new EditText(this);

        input.setInputType(
                InputType.TYPE_CLASS_TEXT |
                        InputType.TYPE_TEXT_VARIATION_PASSWORD
        );

        builder.setView(input);

        builder.setPositiveButton(
                "Delete",
                (dialog, which) -> {

                    String password =
                            input.getText()
                                    .toString();

                    deleteAccount(password);
                }
        );

        builder.setNegativeButton(
                "Cancel",
                null
        );

        builder.show();
    }


    // =============================================================
    // DELETE ACCOUNT
    // =============================================================

    private void deleteAccount(String password) {

        if (firebaseAuth.getCurrentUser() == null) {

            Toast.makeText(
                    this,
                    "User session not found.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String email =
                firebaseAuth.getCurrentUser()
                        .getEmail();

        if (email == null || email.isEmpty()) {

            Toast.makeText(
                    this,
                    "Email not found.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        firebaseAuth
                .signInWithEmailAndPassword(
                        email,
                        password
                )
                .addOnSuccessListener(authResult -> {

                    if (firebaseAuth.getCurrentUser() == null) {
                        return;
                    }

                    // Password verified
                    firebaseAuth.getCurrentUser()
                            .delete()
                            .addOnSuccessListener(aVoid -> {

                                // Delete user from database
                                String uid =
                                        sessionManager.getUid();

                                if (uid != null) {

                                    FirebaseDatabase
                                            .getInstance()
                                            .getReference("users")
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

                                Toast.makeText(
                                        SettingsActivity.this,
                                        "Account deleted successfully ✓",
                                        Toast.LENGTH_SHORT
                                ).show();
                            })
                            .addOnFailureListener(e -> {

                                Toast.makeText(
                                        SettingsActivity.this,
                                        "Error: " + e.getMessage(),
                                        Toast.LENGTH_SHORT
                                ).show();
                            });
                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            SettingsActivity.this,
                            "Wrong password ✗",
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }


    // =============================================================
    // ABOUT APP
    // =============================================================

    private void showAboutDialog() {

        new AlertDialog.Builder(this)
                .setTitle("About SheEmpower")
                .setMessage(
                        "SheEmpower App v1.0\n\n" +
                                "Empowering women sellers & buyers\n\n" +
                                "© 2024 SheEmpower\n" +
                                "All rights reserved"
                )
                .setPositiveButton(
                        "OK",
                        null
                )
                .show();
    }


    // =============================================================
    // BACK BUTTON
    // =============================================================

    @Override
    public boolean onSupportNavigateUp() {

        onBackPressed();

        return true;
    }
}