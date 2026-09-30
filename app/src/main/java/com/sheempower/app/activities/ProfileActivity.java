package com.sheempower.app.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import com.sheempower.app.R;
import com.sheempower.app.models.Seller;
import java.util.ArrayList;
import java.util.List;
import com.sheempower.app.models.User;
import com.sheempower.app.utils.BottomNavHelper;
import com.sheempower.app.utils.CloudinaryHelper;
import com.sheempower.app.utils.Constants;
import com.sheempower.app.utils.SessionManager;
import com.sheempower.app.utils.VoiceGuide;
import com.sheempower.app.models.Buyer;
import com.sheempower.app.utils.PreferencesManager;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileActivity extends AppCompatActivity {

    private CircleImageView ivProfilePic;
    private ImageButton btnChangePic;
    private ImageButton btnSpeaker;
    private ImageView ivVerifiedBadge;

    private TextView tvBusinessHeader;
    private TextView tvOwnerHeader;
    private TextView tvName;
    private TextView tvEmail;
    private TextView tvVerificationStatus;

    private TextView btnSwitchRole;

    private View layoutProfileView;
    private View layoutGuestState;

    private TextView tvPersonalName;
    private TextView tvPersonalEmail;
    private TextView tvWhatsApp;

    private TextView tvBusinessName;
    private TextView tvCategory;
    private TextView tvBio;

    private TextView tvCity;
    private TextView tvShopAddress;
    private TextView tvDelivery;
    private TextView tvCapacity;

    private TextView tvCnicStatus;
    private TextView tvVoiceStatus;
    private TextView tvRating;
    private TextView tvRatingCount;
    private TextView tvTopRated;

    private View layoutEditProfile;
    private View layoutSellerEditFields;

    private EditText etFullName;
    private EditText etPhone;

    private EditText etBusinessName;
    private EditText etBio;
    private EditText etCity;
    private TextView tvCategoryProfile;
    private final List<String> selectedProfileCategories = new ArrayList<>();
    private EditText etWhatsApp;
    private EditText etCapacity;
    private EditText etShopAddress;
    private Spinner spinnerCanDeliverProfile;

    private Button btnEditProfile;
    private Button btnSave;
    private Button btnCancelEdit;
    private Button btnMyOrders;
    private Button btnLogout;
    private Button btnGuestLogin;
    private Button btnGuestRegister;

    private SessionManager session;

    private User currentUser;
    private Seller currentSeller;

    private final ActivityResultLauncher<String> imagePicker =
            registerForActivityResult(
                    new ActivityResultContracts.GetContent(),
                    uri -> {
                        if (uri != null) uploadProfilePic(uri);
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        session = new SessionManager(this);

        initializeViews();

        if (!session.isLoggedIn()) {
            showGuestState();
            View bottomNav = findViewById(R.id.bottomNav);
            if (bottomNav != null) BottomNavHelper.setupForBuyerOrVisitor(this, bottomNav, 3);
            return;
        }

        VoiceGuide.announceScreen(this,
                "یہ آپ کی پروفائل کی سکرین ہے۔ یہاں آپ اپنی معلومات دیکھ اور تبدیل کر سکتی ہیں۔");

        // ✅ ASAL FIX — admin ke liye Firebase se "users" node dhoondhne ki koshish
        // hi nahi karte (jo waha kabhi likha hi nahi gaya), seedha admin ki apni
        // screen dikha dete hain
        if (session.isAdmin()) {
            setupDeliverySpinner();
            setupButtons();
            showAdminProfile();
            View bottomNav = findViewById(R.id.bottomNav);
            if (bottomNav != null) BottomNavHelper.setupForAdmin(this, bottomNav, 3);
            return;
        }

        setupDeliverySpinner();
        setupButtons();
        BottomNavHelper.setupBackButton(this);

        loadUser();

        View bottomNav = findViewById(R.id.bottomNav);
        if (bottomNav != null) {
            if (session.isSeller()) {
                BottomNavHelper.setupForSeller(this, bottomNav, 3);
            } else {
                BottomNavHelper.setupForBuyerOrVisitor(this, bottomNav, 3);
            }
        }
    }

    /** ✅ NAYI — admin ki apni saaf-suthri identity screen, koi seller/buyer field nahi */
    private void showAdminProfile() {
        tvBusinessHeader.setText("🛡️ Admin");
        tvOwnerHeader.setText("Administrator");
        tvName.setText(session.getName() != null && !session.getName().isEmpty() ? session.getName() : "Admin");
        tvEmail.setText(Constants.ADMIN_EMAIL);
        if (ivVerifiedBadge != null) ivVerifiedBadge.setVisibility(View.GONE);
        tvVerificationStatus.setVisibility(View.GONE);

        if (layoutSellerEditFields != null) layoutSellerEditFields.setVisibility(View.GONE);

        if (tvBusinessName != null) tvBusinessName.setVisibility(View.GONE);
        if (tvCategory != null) tvCategory.setVisibility(View.GONE);
        if (tvBio != null) tvBio.setVisibility(View.GONE);
        if (tvCity != null) tvCity.setVisibility(View.GONE);
        if (tvShopAddress != null) tvShopAddress.setVisibility(View.GONE);
        if (tvDelivery != null) tvDelivery.setVisibility(View.GONE);
        if (tvCapacity != null) tvCapacity.setVisibility(View.GONE);
        if (tvCnicStatus != null) tvCnicStatus.setVisibility(View.GONE);
        if (tvVoiceStatus != null) tvVoiceStatus.setVisibility(View.GONE);
        if (tvRating != null) tvRating.setVisibility(View.GONE);
        if (tvRatingCount != null) tvRatingCount.setVisibility(View.GONE);
        if (tvTopRated != null) tvTopRated.setVisibility(View.GONE);

        tvPersonalName.setText("Full Name\nAdmin");
        tvPersonalEmail.setText("Email\n" + Constants.ADMIN_EMAIL);
        tvWhatsApp.setVisibility(View.GONE);

        if (btnSwitchRole != null) btnSwitchRole.setVisibility(View.GONE);
        if (btnMyOrders != null) btnMyOrders.setVisibility(View.GONE); // admin ke apne orders nahi hote
    }

    private void showGuestState() {
        layoutGuestState.setVisibility(View.VISIBLE);
        layoutProfileView.setVisibility(View.GONE);
        layoutEditProfile.setVisibility(View.GONE);
        btnSwitchRole.setVisibility(View.GONE);
        btnEditProfile.setVisibility(View.GONE);
        btnMyOrders.setVisibility(View.GONE);
        btnLogout.setVisibility(View.GONE);
        if (ivVerifiedBadge != null) ivVerifiedBadge.setVisibility(View.GONE);

        tvBusinessHeader.setText("👋 خوش آمدید / Welcome");
        tvOwnerHeader.setText("Visitor");
        tvName.setText("مہمان / Guest");
        tvEmail.setVisibility(View.GONE);
        tvVerificationStatus.setVisibility(View.GONE);

        btnGuestLogin.setOnClickListener(v -> startActivity(new Intent(this, LoginActivity.class)));
        btnGuestRegister.setOnClickListener(v -> startActivity(new Intent(this, SellerRegistrationActivity.class)));
    }

    private void initializeViews() {

        ivProfilePic = findViewById(R.id.ivProfilePic);
        btnChangePic = findViewById(R.id.btnChangePic);
        btnSpeaker   = findViewById(R.id.btnSpeaker);
        ivVerifiedBadge = findViewById(R.id.ivVerifiedBadge);

        tvBusinessHeader = findViewById(R.id.tvBusinessHeader);
        tvOwnerHeader = findViewById(R.id.tvOwnerHeader);
        tvName = findViewById(R.id.tvName);
        tvEmail = findViewById(R.id.tvEmail);
        tvVerificationStatus = findViewById(R.id.tvVerificationStatus);

        btnSwitchRole = findViewById(R.id.btnSwitchRole);

        layoutProfileView = findViewById(R.id.layoutProfileView);
        layoutGuestState = findViewById(R.id.layoutGuestState);

        tvPersonalName = findViewById(R.id.tvPersonalName);
        tvPersonalEmail = findViewById(R.id.tvPersonalEmail);
        tvWhatsApp = findViewById(R.id.tvWhatsApp);

        tvBusinessName = findViewById(R.id.tvBusinessName);
        tvCategory = findViewById(R.id.tvCategory);
        tvBio = findViewById(R.id.tvBio);

        tvCity = findViewById(R.id.tvCity);
        tvShopAddress = findViewById(R.id.tvShopAddress);
        tvDelivery = findViewById(R.id.tvDelivery);
        tvCapacity = findViewById(R.id.tvCapacity);

        tvCnicStatus = findViewById(R.id.tvCnicStatus);
        tvVoiceStatus = findViewById(R.id.tvVoiceStatus);
        tvRating = findViewById(R.id.tvRating);
        tvRatingCount = findViewById(R.id.tvRatingCount);
        tvTopRated = findViewById(R.id.tvTopRated);

        layoutEditProfile = findViewById(R.id.layoutEditProfile);
        layoutSellerEditFields = findViewById(R.id.layoutSellerEditFields);

        etFullName = findViewById(R.id.etFullName);
        etPhone = findViewById(R.id.etPhone);

        etBusinessName = findViewById(R.id.etBusinessName);
        etBio = findViewById(R.id.etBio);
        etCity = findViewById(R.id.etCity);

        tvCategoryProfile = findViewById(R.id.tvCategoryProfile);
        if (tvCategoryProfile != null) tvCategoryProfile.setOnClickListener(v -> showCategoryDialog());

        etWhatsApp = findViewById(R.id.etWhatsApp);
        etCapacity = findViewById(R.id.etCapacity);
        etShopAddress = findViewById(R.id.etShopAddress);

        spinnerCanDeliverProfile = findViewById(R.id.spinnerCanDeliverProfile);

        btnEditProfile = findViewById(R.id.btnEditProfile);
        btnSave = findViewById(R.id.btnSave);
        btnCancelEdit = findViewById(R.id.btnCancelEdit);

        btnMyOrders = findViewById(R.id.btnMyOrders);
        btnLogout = findViewById(R.id.btnLogout);

        btnGuestLogin = findViewById(R.id.btnGuestLogin);
        btnGuestRegister = findViewById(R.id.btnGuestRegister);
    }

    private void setupDeliverySpinner() {
        String[] deliveryOptions = {"Yes", "No"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, deliveryOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        if (spinnerCanDeliverProfile != null) spinnerCanDeliverProfile.setAdapter(adapter);
    }

    private void setupButtons() {
        if (btnSpeaker != null) {
            VoiceGuide.setupSpeakerButton(btnSpeaker, this,
                    "یہ آپ کی پروفائل کی سکرین ہے۔ یہاں آپ اپنی معلومات دیکھ اور تبدیل کر سکتی ہیں۔"
            );
        }
        if (btnChangePic != null) btnChangePic.setOnClickListener(v -> imagePicker.launch("image/*"));
        if (btnEditProfile != null) btnEditProfile.setOnClickListener(v -> openEditMode());
        if (btnSave != null) btnSave.setOnClickListener(v -> saveChanges());
        if (btnCancelEdit != null) btnCancelEdit.setOnClickListener(v -> closeEditMode());

        if (btnMyOrders != null) {
            btnMyOrders.setOnClickListener(v -> startActivity(new Intent(ProfileActivity.this, MyOrdersActivity.class)));
        }

        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                session.logout();
                Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            });
        }

        if (btnSwitchRole != null) btnSwitchRole.setOnClickListener(v -> showRoleSwitchDialog());
    }

    private void showCategoryDialog() {
        String[] displayCategories = new String[Constants.CATEGORIES.length];
        boolean[] checkedItems = new boolean[Constants.CATEGORIES.length];

        for (int i = 0; i < Constants.CATEGORIES.length; i++) {
            displayCategories[i] = Constants.CATEGORY_LABELS_UR[i] + " / " + Constants.CATEGORIES[i];
            checkedItems[i] = selectedProfileCategories.contains(Constants.CATEGORIES[i]);
        }

        new AlertDialog.Builder(this)
                .setTitle("زمرے منتخب کریں / Select Categories")
                .setMultiChoiceItems(displayCategories, checkedItems, (dialog, which, isChecked) -> {
                    String cat = Constants.CATEGORIES[which];
                    if (isChecked) {
                        if (!selectedProfileCategories.contains(cat)) selectedProfileCategories.add(cat);
                    } else {
                        selectedProfileCategories.remove(cat);
                    }
                })
                .setPositiveButton("OK", (dialog, which) -> updateProfileCategoryText())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateProfileCategoryText() {
        if (tvCategoryProfile == null) return;
        if (selectedProfileCategories.isEmpty()) {
            tvCategoryProfile.setText("زمرے منتخب کریں / Select Categories");
        } else {
            StringBuilder sb = new StringBuilder();
            for (String cat : selectedProfileCategories) {
                if (sb.length() > 0) sb.append(" • ");
                sb.append(Constants.getUrduLabel(cat)).append(" (").append(cat).append(")");
            }
            tvCategoryProfile.setText(sb.toString());
        }
    }

    private void speakText() {
        VoiceGuide.announceScreen(this, "یہ آپ کی پروفائل کی سکرین ہے۔ یہاں آپ اپنی معلومات دیکھ اور تبدیل کر سکتی ہیں۔");
    }

    private void loadUser() {
        String uid = session.getUid();
        if (uid == null) return;

        boolean isSellerMode = session.isSeller();

        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child(Constants.NODE_USERS).child(uid);

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                currentUser = snapshot.getValue(User.class);
                if (currentUser != null) {
                    showUserInformation(currentUser);
                }
                if (isSellerMode || (currentUser != null && "seller".equalsIgnoreCase(safe(currentUser.getRole())))) {
                    loadSellerInformation(uid);
                } else {
                    showBuyerProfile();
                }
            }
            @Override
            public void onCancelled(DatabaseError error) {
                if (isSellerMode) {
                    loadSellerInformation(uid);
                } else {
                    Toast.makeText(ProfileActivity.this, "Profile load nahi ho saki", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void showUserInformation(User user) {
        if (user == null) return;
        String name = safe(user.getName());
        String email = safe(user.getEmail());
        String phone = safe(user.getPhone());

        if (tvName != null) tvName.setText(name);
        if (tvEmail != null) tvEmail.setText(email);
        if (tvPersonalName != null) tvPersonalName.setText("Full Name\n" + name);
        if (tvPersonalEmail != null) tvPersonalEmail.setText("Email\n" + email);
        if (tvWhatsApp != null) tvWhatsApp.setText("WhatsApp / Phone\n" + phone);
        if (etFullName != null) etFullName.setText(name);
        if (etPhone != null) etPhone.setText(phone);

        if (ivProfilePic != null && !safe(user.getImageUrl()).isEmpty()) {
            Glide.with(this).load(user.getImageUrl()).into(ivProfilePic);
        }
    }

    private void loadSellerInformation(String uid) {
        DatabaseReference sellerRef = FirebaseDatabase.getInstance().getReference().child(Constants.NODE_SELLERS).child(uid);

        sellerRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                currentSeller = snapshot.getValue(Seller.class);
                if (currentSeller == null) {
                    Toast.makeText(ProfileActivity.this, "Seller details nahi milin", Toast.LENGTH_SHORT).show();
                    return;
                }
                showSellerInformation(currentSeller);
            }
            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(ProfileActivity.this, "Seller details load nahi ho saken", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showSellerInformation(Seller seller) {
        String business = safe(seller.getBusinessName());
        String owner = safe(seller.getOwnerName());
        String email = safe(seller.getEmail());
        String phone = safe(seller.getPhone());
        String whatsapp = safe(seller.getWhatsappNumber());
        String category = safe(seller.getCategory());
        String bio = safe(seller.getBio());
        String city = safe(seller.getCity());
        String address = safe(seller.getShopAddress());
        String capacity = safe(seller.getCapacityDescription());

        selectedProfileCategories.clear();
        if (seller.getCategories() != null && !seller.getCategories().isEmpty()) {
            selectedProfileCategories.addAll(seller.getCategories());
        } else if (!safe(seller.getCategory()).isEmpty()) {
            selectedProfileCategories.add(seller.getCategory());
        } else {
            selectedProfileCategories.add(Constants.CATEGORIES[0]);
        }

        updateProfileCategoryText();

        StringBuilder catSb = new StringBuilder();
        for (String c : selectedProfileCategories) {
            if (catSb.length() > 0) catSb.append(" • ");
            catSb.append(Constants.getUrduLabel(c)).append(" (").append(c).append(")");
        }

        if (tvBusinessHeader != null) tvBusinessHeader.setText("🎨 " + business + " Profile");
        if (tvOwnerHeader != null) tvOwnerHeader.setText(owner);
        if (tvName != null) tvName.setText(owner);
        if (tvEmail != null) tvEmail.setText(email);

        if (tvPersonalName != null) tvPersonalName.setText("Full Name\n" + owner);
        if (tvPersonalEmail != null) tvPersonalEmail.setText("Email\n" + email);
        if (tvWhatsApp != null) tvWhatsApp.setText("WhatsApp\n" + whatsapp);

        if (tvBusinessName != null) tvBusinessName.setText("Business / Shop Name\n" + business);
        if (tvCategory != null) tvCategory.setText("Categories\n" + catSb.toString());
        if (tvBio != null) tvBio.setText("Bio / Description\n" + bio);

        if (tvCity != null) tvCity.setText("City\n" + city);
        if (tvShopAddress != null) tvShopAddress.setText("Shop Address\n" + address);
        if (tvDelivery != null) tvDelivery.setText("Can Deliver\n" + (seller.isCanDeliver() ? "Yes" : "No"));
        if (tvCapacity != null) tvCapacity.setText("Capacity / Daily Work\n" + capacity);

        String verification = safe(seller.getVerificationStatus());
        boolean approved = Constants.isApproved(verification);

        if (ivVerifiedBadge != null) ivVerifiedBadge.setVisibility(approved ? View.VISIBLE : View.GONE);
        if (tvVerificationStatus != null) tvVerificationStatus.setText(approved ? "✅ Verified" : "⏳ Verification: " + verification);

        if (tvCnicStatus != null) {
            tvCnicStatus.setText(!safe(seller.getCnicUrl()).isEmpty() ? "CNIC Verification\nDocument submitted" : "CNIC Verification\nNot submitted");
        }
        if (tvVoiceStatus != null) {
            tvVoiceStatus.setText(!safe(seller.getVoiceUrl()).isEmpty() ? "Voice Verification\nSubmitted" : "Voice Verification\nNot submitted");
        }
        if (tvRating != null) tvRating.setText("Rating\n" + seller.getRating());
        if (tvRatingCount != null) tvRatingCount.setText("Rating Count\n" + seller.getRatingCount());
        if (tvTopRated != null) tvTopRated.setText("Top Rated\n" + (seller.isTopRated() ? "Yes" : "No"));

        if (etFullName != null) etFullName.setText(owner);
        if (etPhone != null) etPhone.setText(phone);
        if (etBusinessName != null) etBusinessName.setText(business);
        if (etBio != null) etBio.setText(bio);
        if (etCity != null) etCity.setText(city);
        if (etWhatsApp != null) etWhatsApp.setText(whatsapp);
        if (etCapacity != null) etCapacity.setText(capacity);
        if (etShopAddress != null) etShopAddress.setText(address);

        if (spinnerCanDeliverProfile != null) setSpinnerValue(spinnerCanDeliverProfile, seller.isCanDeliver() ? "Yes" : "No");

        if (btnSwitchRole != null) {
            btnSwitchRole.setVisibility(View.VISIBLE);
            btnSwitchRole.setText("👤  SELLER  ▼");
        }

        if (ivProfilePic != null && !safe(seller.getImageUrl()).isEmpty()) {
            Glide.with(this).load(seller.getImageUrl()).into(ivProfilePic);
        }
    }

    private void showBuyerProfile() {
        if (tvBusinessHeader != null) tvBusinessHeader.setText("💗 Buyer Profile");

        String userName = currentUser != null ? safe(currentUser.getName()) : "";
        String userEmail = currentUser != null ? safe(currentUser.getEmail()) : "";

        if (tvOwnerHeader != null) tvOwnerHeader.setText(userName);
        if (tvName != null) tvName.setText(userName);
        if (tvEmail != null) tvEmail.setText(userEmail);

        if (ivVerifiedBadge != null) ivVerifiedBadge.setVisibility(View.GONE);
        if (tvVerificationStatus != null) tvVerificationStatus.setVisibility(View.GONE);
        if (layoutSellerEditFields != null) layoutSellerEditFields.setVisibility(View.GONE);

        if (tvBusinessName != null) tvBusinessName.setVisibility(View.GONE);
        if (tvCategory != null) tvCategory.setVisibility(View.GONE);
        if (tvBio != null) tvBio.setVisibility(View.GONE);
        if (tvCity != null) tvCity.setVisibility(View.GONE);
        if (tvShopAddress != null) tvShopAddress.setVisibility(View.GONE);
        if (tvDelivery != null) tvDelivery.setVisibility(View.GONE);
        if (tvCapacity != null) tvCapacity.setVisibility(View.GONE);
        if (tvCnicStatus != null) tvCnicStatus.setVisibility(View.GONE);
        if (tvVoiceStatus != null) tvVoiceStatus.setVisibility(View.GONE);
        if (tvRating != null) tvRating.setVisibility(View.GONE);
        if (tvRatingCount != null) tvRatingCount.setVisibility(View.GONE);
        if (tvTopRated != null) tvTopRated.setVisibility(View.GONE);

        if (btnSwitchRole != null) btnSwitchRole.setText("👤  BUYER  ▼");
    }

    private void openEditMode() {
        if (layoutProfileView != null) layoutProfileView.setVisibility(View.GONE);
        if (layoutEditProfile != null) layoutEditProfile.setVisibility(View.VISIBLE);
        if (btnEditProfile != null) btnEditProfile.setVisibility(View.GONE);

        if (layoutSellerEditFields != null) {
            layoutSellerEditFields.setVisibility(session.isSeller() ? View.VISIBLE : View.GONE);
        }
    }

    private void closeEditMode() {
        if (layoutEditProfile != null) layoutEditProfile.setVisibility(View.GONE);
        if (layoutProfileView != null) layoutProfileView.setVisibility(View.VISIBLE);
        if (btnEditProfile != null) btnEditProfile.setVisibility(View.VISIBLE);
        loadUser();
    }

    private void saveChanges() {
        String uid = session.getUid();
        if (uid == null) return;

        String name = etFullName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            etFullName.setError("Name required");
            return;
        }

        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child(Constants.NODE_USERS).child(uid);
        userRef.child("name").setValue(name);
        userRef.child("phone").setValue(phone)
                .addOnSuccessListener(v -> {
                    session.saveName(name);
                    if (session.isSeller()) {
                        saveSellerInformation(uid, name, phone);
                    } else {
                        Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                        closeEditMode();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void saveSellerInformation(String uid, String ownerName, String phone) {
        String business = etBusinessName.getText().toString().trim();
        String bio = etBio.getText().toString().trim();
        String city = etCity.getText().toString().trim();
        String whatsapp = etWhatsApp.getText().toString().trim();
        String capacity = etCapacity.getText().toString().trim();
        String address = etShopAddress.getText().toString().trim();
        String delivery = spinnerCanDeliverProfile.getSelectedItem().toString();
        boolean canDeliver = delivery.equalsIgnoreCase("Yes");

        if (selectedProfileCategories.isEmpty()) {
            Toast.makeText(this, "کم از کم ایک category منتخب کریں", Toast.LENGTH_SHORT).show();
            showCategoryDialog();
            return;
        }

        String primaryCategory = selectedProfileCategories.get(0);

        DatabaseReference sellerRef = FirebaseDatabase.getInstance().getReference().child(Constants.NODE_SELLERS).child(uid);

        sellerRef.child("ownerName").setValue(ownerName);
        sellerRef.child("phone").setValue(phone);
        sellerRef.child("businessName").setValue(business);
        sellerRef.child("bio").setValue(bio);
        sellerRef.child("city").setValue(city);
        sellerRef.child("category").setValue(primaryCategory);
        sellerRef.child("categories").setValue(new ArrayList<>(selectedProfileCategories));
        sellerRef.child("whatsappNumber").setValue(whatsapp);
        sellerRef.child("capacityDescription").setValue(capacity);
        sellerRef.child("shopAddress").setValue(address);
        sellerRef.child("canDeliver").setValue(canDeliver)
                .addOnSuccessListener(v -> {
                    session.saveName(ownerName);
                    Toast.makeText(this, "All profile information updated", Toast.LENGTH_SHORT).show();
                    closeEditMode();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Seller details save failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void uploadProfilePic(Uri uri) {
        Toast.makeText(this, "تصویر اپلوڈ ہو رہی ہے...", Toast.LENGTH_SHORT).show();
        CloudinaryHelper.upload(this, uri, "profile_pictures", (url, error) -> {
            if (url != null) {
                Glide.with(this).load(url).into(ivProfilePic);
                String uid = session.getUid();
                if (uid != null) {
                    DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
                    rootRef.child(Constants.NODE_USERS).child(uid).child("imageUrl").setValue(url);
                    if (session.isSeller()) {
                        rootRef.child(Constants.NODE_SELLERS).child(uid).child("imageUrl").setValue(url);
                    }
                    Toast.makeText(this, "تصویر تبدیل ہو گئی", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "اپلوڈ ناکام: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setSpinnerValue(Spinner spinner, String value) {
        if (value == null) return;
        for (int i = 0; i < spinner.getCount(); i++) {
            Object item = spinner.getItemAtPosition(i);
            if (item != null && value.equalsIgnoreCase(item.toString())) {
                spinner.setSelection(i);
                return;
            }
        }
    }

    private void showRoleSwitchDialog() {
        String currentRole = session.getRole();

        if ("seller".equalsIgnoreCase(currentRole)) {
            String uid = session.getUid();
            if (uid == null) {
                Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_LONG).show();
                return;
            }

            DatabaseReference buyerRef = FirebaseDatabase.getInstance().getReference().child(Constants.NODE_BUYERS).child(uid);
            buyerRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        new AlertDialog.Builder(ProfileActivity.this)
                                .setTitle("Switch Account")
                                .setItems(new String[]{"👤 Switch to Buyer", "Cancel"}, (dialog, which) -> {
                                    if (which == 0) switchToBuyer();
                                }).show();
                    } else {
                        new AlertDialog.Builder(ProfileActivity.this)
                                .setTitle("Switch Account")
                                .setItems(new String[]{"➕ Create Buyer Profile", "Cancel"}, (dialog, which) -> {
                                    if (which == 0) startActivity(new Intent(ProfileActivity.this, BuyerProfileActivity.class));
                                }).show();
                    }
                }
                @Override
                public void onCancelled(DatabaseError error) {
                    Toast.makeText(ProfileActivity.this, "Buyer profile check failed: " + error.getMessage(), Toast.LENGTH_LONG).show();
                }
            });

        } else if ("buyer".equalsIgnoreCase(currentRole)) {
            new AlertDialog.Builder(this)
                    .setTitle("Switch Account")
                    .setMessage("Switch to your Seller account?")
                    .setPositiveButton("Switch to Seller", (dialog, which) -> switchToSeller())
                    .setNegativeButton("Cancel", null)
                    .show();
        }
    }

    private void switchToBuyer() {
        String uid = session.getUid();
        if (uid == null) {
            Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_LONG).show();
            return;
        }

        DatabaseReference buyerRef = FirebaseDatabase.getInstance().getReference().child(Constants.NODE_BUYERS).child(uid);
        buyerRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Toast.makeText(ProfileActivity.this, "Buyer profile not found.", Toast.LENGTH_LONG).show();
                    return;
                }
                Buyer buyer = snapshot.getValue(Buyer.class);
                if (buyer != null && !TextUtils.isEmpty(buyer.getName())) {
                    session.saveName(buyer.getName());
                }
                session.switchRole(Constants.ROLE_BUYER);
                Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(ProfileActivity.this, "Buyer switch failed: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void switchToSeller() {
        String uid = session.getUid();
        if (uid == null) {
            Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_LONG).show();
            return;
        }

        DatabaseReference sellerRef = FirebaseDatabase.getInstance().getReference().child(Constants.NODE_SELLERS).child(uid);
        sellerRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Toast.makeText(ProfileActivity.this, "Seller profile not found.", Toast.LENGTH_LONG).show();
                    return;
                }
                session.switchRole(Constants.ROLE_SELLER);
                Intent intent = new Intent(ProfileActivity.this, SellerDashboardActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(ProfileActivity.this, "Seller profile check failed.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private void applyTheme() {
        PreferencesManager prefs = new PreferencesManager(this);
        if (prefs.isDarkThemeEnabled()) {
            setTheme(R.style.Theme_SheEmpower_Dark);
        } else {
            setTheme(R.style.Theme_SheEmpower);
        }
    }
}