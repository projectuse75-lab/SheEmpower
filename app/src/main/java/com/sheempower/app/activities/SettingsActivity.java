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
import com.sheempower.app.utils.BottomNavHelper;
import com.sheempower.app.utils.Constants;
import com.sheempower.app.utils.LanguageHelper;
import com.sheempower.app.utils.PreferencesManager;
import com.sheempower.app.utils.VoiceGuide;
import android.widget.ImageButton;
import com.sheempower.app.utils.SessionManager;

public class SettingsActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private PreferencesManager preferencesManager;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference database;
    private TextView tvUserName, tvUserEmail, tvUserContact, tvCurrentLanguage;
    private Switch switchNotifications, switchDarkTheme;
    private LinearLayout llChangeName, llChangeContact, llChangePassword;
    private LinearLayout llPrivacyPolicy, llTermsConditions, llAboutApp, llShareApp, llLanguage;
    private Button btnLogout, btnDeleteAccount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyTheme();
        super.onCreate(savedInstanceState);
        preferencesManager = new PreferencesManager(this);
        setContentView(R.layout.activity_settings);

        sessionManager = new SessionManager(this);
        firebaseAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance().getReference();

        initializeViews();
        loadUserInformation();
        loadSettings();
        setupListeners();
        setupBottomNavigation();

        ImageButton btnSpeaker = findViewById(R.id.btnSpeaker);
        VoiceGuide.setupSpeakerButton(btnSpeaker, this,
                "یہ سیٹنگز کی سکرین ہے۔ یہاں سے آپ اپنا نام، واٹس ایپ نمبر، پاس ورڈ اور ایپ کا ڈارک موڈ تبدیل کر سکتی ہیں۔"
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        LanguageHelper.processViewHierarchy(findViewById(android.R.id.content), this);
        ImageButton btnSpeaker = findViewById(R.id.btnSpeaker);
        if (btnSpeaker != null) {
            VoiceGuide.updateSpeakerIcon(btnSpeaker, VoiceGuide.isMuted(this));
        }
        VoiceGuide.announceScreen(this,
                "یہ سیٹنگز کی سکرین ہے۔ یہاں سے آپ اپنا نام، واٹس ایپ نمبر، پاس ورڈ اور ایپ کا ڈارک موڈ تبدیل کر سکتی ہیں۔"
        );
    }

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
        llShareApp = findViewById(R.id.llShareApp);
        llLanguage = findViewById(R.id.llLanguage);
        tvCurrentLanguage = findViewById(R.id.tvCurrentLanguage);
        btnLogout = findViewById(R.id.btnLogout);
        btnDeleteAccount = findViewById(R.id.btnDeleteAccount);
    }

    private void setupBottomNavigation() {
        View bottomNav = findViewById(R.id.bottomNav);
        if (bottomNav == null) return;

        if (sessionManager.isSeller()) {
            BottomNavHelper.setupForSeller(this, bottomNav, 4);
        } else if (sessionManager.isAdmin()) {
            BottomNavHelper.setupForAdmin(this, bottomNav, 4);
        } else {
            BottomNavHelper.setupForBuyerOrVisitor(this, bottomNav, 4);
        }
    }

    private void loadUserInformation() {
        String name = sessionManager.getName();
        if (name == null || name.trim().isEmpty()) name = "She Empower User";
        tvUserName.setText(name);

        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        if (firebaseUser != null) {
            String email = firebaseUser.getEmail();
            if (email == null || email.isEmpty()) email = "Email not available";
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

        database.child(Constants.NODE_USERS).child(uid).child("phone").get()
                .addOnSuccessListener(snapshot -> {
                    String phone = snapshot.getValue(String.class);
                    if (phone == null || phone.trim().isEmpty()) {
                        tvUserContact.setText("Contact not added");
                    } else {
                        tvUserContact.setText(phone);
                    }
                })
                .addOnFailureListener(e -> tvUserContact.setText("Contact not available"));
    }

    private void loadSettings() {
        switchNotifications.setChecked(preferencesManager.areNotificationsEnabled());
        switchDarkTheme.setChecked(preferencesManager.isDarkThemeEnabled());
    }

    private void setupListeners() {
        llChangeName.setOnClickListener(v -> showChangeNameDialog());
        llChangeContact.setOnClickListener(v -> showChangeContactDialog());
        llChangePassword.setOnClickListener(v -> showChangePasswordDialog());

        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferencesManager.setNotificationsEnabled(isChecked);
            Toast.makeText(this, isChecked ? "🔔 Notifications ON" : "🔕 Notifications OFF", Toast.LENGTH_SHORT).show();
        });

        // ✅ DARK THEME - APP RESTART FIX
        switchDarkTheme.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferencesManager.setDarkThemeEnabled(isChecked);
            AppCompatDelegate.setDefaultNightMode(isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
            Toast.makeText(this, isChecked ? "🌙 Dark Theme ON" : "☀️ Light Theme ON", Toast.LENGTH_SHORT).show();

            // APP RESTART
            Intent restartIntent = new Intent(SettingsActivity.this, MainActivity.class);
            restartIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(restartIntent);
            finish();
        });

        if (llLanguage != null) {
            llLanguage.setVisibility(View.GONE);
        }

        llPrivacyPolicy.setOnClickListener(v -> showPrivacyPolicy());
        llTermsConditions.setOnClickListener(v -> showTermsConditions());
        llAboutApp.setOnClickListener(v -> showAboutDialog());
        if (llShareApp != null) llShareApp.setOnClickListener(v -> shareApp());
        btnLogout.setOnClickListener(v -> showLogoutConfirmation());
        btnDeleteAccount.setOnClickListener(v -> showDeleteAccountConfirmation());
    }

    private void shareApp() {
        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.setType("text/plain");
        String shareBody = "السلام علیکم! شی ایمپاور ایپ ڈاؤن لوڈ کریں اور خواتین کے ہنر، اشیاء اور خدمات سے فائدہ اٹھائیں:\n\n" +
                "✨ She Empower App — Empowering Women, One Sale at a Time\n" +
                "https://play.google.com/store/apps/details?id=" + getPackageName();
        sendIntent.putExtra(Intent.EXTRA_SUBJECT, "She Empower App");
        sendIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
        startActivity(Intent.createChooser(sendIntent, "ایپ شیئر کریں / Share App via"));
    }

    private void showChangeNameDialog() {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint("Enter your name");
        input.setText(sessionManager.getName());
        input.setSelectAllOnFocus(true);
        input.setPadding(30, 30, 30, 30);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("👤 Change Name")
                .setMessage("Apna naam enter karein")
                .setView(input)
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String newName = input.getText().toString().trim();
                if (newName.isEmpty()) {
                    input.setError("Name required");
                    return;
                }
                updateName(newName, dialog);
            });
        });
        dialog.show();
    }

    private void updateName(String newName, AlertDialog dialog) {
        String uid = sessionManager.getUid();
        if (uid == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        database.child(Constants.NODE_USERS).child(uid).child("name").setValue(newName)
                .addOnSuccessListener(unused -> {
                    sessionManager.saveName(newName);
                    tvUserName.setText(newName);
                    Toast.makeText(this, "Name updated successfully ✓", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Name update failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void showChangeContactDialog() {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint("03XXXXXXXXX");
        input.setInputType(InputType.TYPE_CLASS_PHONE);

        String currentContact = tvUserContact.getText().toString();
        if (!currentContact.equals("Contact not added") && !currentContact.equals("Contact not available")) {
            input.setText(currentContact);
            input.setSelectAllOnFocus(true);
        }
        input.setPadding(30, 30, 30, 30);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("📱 Change Contact")
                .setMessage("Apna WhatsApp / contact number enter karein")
                .setView(input)
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String newContact = input.getText().toString().trim();
                if (newContact.isEmpty()) {
                    input.setError("Contact required");
                    return;
                }
                updateContact(newContact, dialog);
            });
        });
        dialog.show();
    }

    private void updateContact(String newContact, AlertDialog dialog) {
        String uid = sessionManager.getUid();
        if (uid == null) return;

        database.child(Constants.NODE_USERS).child(uid).child("phone").setValue(newContact)
                .addOnSuccessListener(unused -> {
                    if (sessionManager.isSeller()) {
                        database.child(Constants.NODE_SELLERS).child(uid).child("phone").setValue(newContact);
                        database.child(Constants.NODE_SELLERS).child(uid).child("whatsappNumber").setValue(newContact);
                    }
                    tvUserContact.setText(newContact);
                    Toast.makeText(this, "Contact updated successfully ✓", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Contact update failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void showChangePasswordDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(35, 10, 35, 0);

        EditText oldPassword = new EditText(this);
        oldPassword.setHint("Current Password");
        oldPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        EditText newPassword = new EditText(this);
        newPassword.setHint("New Password");
        newPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        EditText confirmPassword = new EditText(this);
        confirmPassword.setHint("Confirm New Password");
        confirmPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

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
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String oldPass = oldPassword.getText().toString();
                String newPass = newPassword.getText().toString();
                String confirmPass = confirmPassword.getText().toString();

                if (oldPass.isEmpty()) {
                    oldPassword.setError("Current password required");
                    return;
                }
                if (newPass.length() < 6) {
                    newPassword.setError("Password must be at least 6 characters");
                    return;
                }
                if (!newPass.equals(confirmPass)) {
                    confirmPassword.setError("Passwords do not match");
                    return;
                }
                changePassword(oldPass, newPass, dialog);
            });
        });
        dialog.show();
    }

    private void changePassword(String oldPassword, String newPassword, AlertDialog dialog) {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null || user.getEmail() == null) {
            Toast.makeText(this, "User session not found", Toast.LENGTH_SHORT).show();
            return;
        }

        firebaseAuth.signInWithEmailAndPassword(user.getEmail(), oldPassword)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser currentUser = firebaseAuth.getCurrentUser();
                    if (currentUser == null) return;

                    currentUser.updatePassword(newPassword)
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(this, "Password changed successfully ✓", Toast.LENGTH_LONG).show();
                                dialog.dismiss();
                            })
                            .addOnFailureListener(e -> Toast.makeText(this, "Password change failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Current password is incorrect ✗", Toast.LENGTH_LONG).show());
    }

    private void showPrivacyPolicy() {
        new AlertDialog.Builder(this)
                .setTitle("🔒 Privacy Policy")
                .setMessage("She Empower respects your privacy.\n\n" +
                        "• Your account information is used to provide app services.\n\n" +
                        "• Seller information is used for seller profile and verification.\n\n" +
                        "• Payment proof is used only for order verification.\n\n" +
                        "• Your information is not intentionally shared for unrelated purposes.\n\n" +
                        "Please contact She Empower administration for privacy concerns.")
                .setPositiveButton("OK", null)
                .show();
    }

    private void showTermsConditions() {
        new AlertDialog.Builder(this)
                .setTitle("📜 Terms & Conditions")
                .setMessage("By using She Empower, you agree that:\n\n" +
                        "• Users must provide accurate account information.\n\n" +
                        "• Sellers are responsible for business information accuracy.\n\n" +
                        "• Buyers should verify order details before placing orders.\n\n" +
                        "• Payment and delivery arrangements follow app options.\n\n" +
                        "• Users must not misuse the platform.\n\n" +
                        "• She Empower may restrict accounts violating rules.")
                .setPositiveButton("I Understand", null)
                .show();
    }

    private void showAboutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("💗 About She Empower")
                .setMessage("She Empower\n\nVersion 1.0\n\n" +
                        "A platform connecting women sellers with buyers, helping women showcase skills and small businesses.\n\n" +
                        "Empowering women through skills, business and opportunities. 💗")
                .setPositiveButton("OK", null)
                .show();
    }

    private void showLogoutConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("🚪 Logout")
                .setMessage("Kya aap logout karna chahti hain?")
                .setPositiveButton("Yes", (dialog, which) -> logout())
                .setNegativeButton("No", null)
                .show();
    }

    private void logout() {
        firebaseAuth.signOut();
        sessionManager.logout();
        preferencesManager.clear();

        Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showDeleteAccountConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("🗑️ Delete Account")
                .setMessage("⚠️ Yeh action permanent hai.\n\nAapka account aur related data delete ho jayega.\n\nKya aap continue karna chahti hain?")
                .setPositiveButton("Delete", (dialog, which) -> showPasswordConfirmation())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showPasswordConfirmation() {
        EditText input = new EditText(this);
        input.setHint("Password");
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Confirm Password")
                .setMessage("Account delete karne ke liye password enter karein.")
                .setView(input)
                .setPositiveButton("Delete", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String password = input.getText().toString();
                if (password.isEmpty()) {
                    input.setError("Password required");
                    return;
                }
                deleteAccount(password, dialog);
            });
        });
        dialog.show();
    }

    private void deleteAccount(String password, AlertDialog dialog) {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null || user.getEmail() == null) {
            Toast.makeText(this, "User session not found", Toast.LENGTH_SHORT).show();
            return;
        }

        firebaseAuth.signInWithEmailAndPassword(user.getEmail(), password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser currentUser = firebaseAuth.getCurrentUser();
                    if (currentUser == null) return;

                    String uid = sessionManager.getUid();
                    currentUser.delete()
                            .addOnSuccessListener(unused -> {
                                if (uid != null) {
                                    database.child(Constants.NODE_USERS).child(uid).removeValue();
                                    database.child(Constants.NODE_SELLERS).child(uid).removeValue();
                                    database.child("buyers").child(uid).removeValue();
                                }
                                sessionManager.logout();
                                preferencesManager.clear();

                                Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            })
                            .addOnFailureListener(e -> Toast.makeText(this, "Delete failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Wrong password ✗", Toast.LENGTH_SHORT).show());
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