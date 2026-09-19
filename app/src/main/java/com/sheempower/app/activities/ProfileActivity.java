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
import com.sheempower.app.models.User;
import com.sheempower.app.utils.BottomNavHelper;
import com.sheempower.app.utils.CloudinaryHelper;
import com.sheempower.app.utils.Constants;
import com.sheempower.app.utils.SessionManager;
import com.sheempower.app.models.Buyer;


import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileActivity extends AppCompatActivity {

    // Header
    private CircleImageView ivProfilePic;
    private ImageButton btnChangePic;

    private TextView tvBusinessHeader;
    private TextView tvOwnerHeader;
    private TextView tvName;
    private TextView tvEmail;
    private TextView tvVerificationStatus;

    // Role switch
    private TextView btnSwitchRole;

    // View mode
    private View layoutProfileView;

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

    // Edit mode
    private View layoutEditProfile;
    private View layoutSellerEditFields;

    private EditText etFullName;
    private EditText etPhone;

    private EditText etBusinessName;
    private EditText etBio;
    private EditText etCity;
    private Spinner spinnerCategoryProfile;
    private EditText etWhatsApp;
    private EditText etCapacity;
    private EditText etShopAddress;
    private Spinner spinnerCanDeliverProfile;

    private Button btnEditProfile;
    private Button btnSave;
    private Button btnCancelEdit;
    private Button btnMyOrders;
    private Button btnLogout;

    private SessionManager session;

    private User currentUser;
    private Seller currentSeller;

    private final ActivityResultLauncher<String> imagePicker =
            registerForActivityResult(
                    new ActivityResultContracts.GetContent(),
                    uri -> {
                        if (uri != null) {
                            uploadProfilePic(uri);
                        }
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        session = new SessionManager(this);

        initializeViews();
        setupCategorySpinner();
        setupDeliverySpinner();
        setupButtons();

        loadUser();

        if (session.isSeller()) {

            BottomNavHelper.setupForSeller(
                    this,
                    findViewById(R.id.bottomNav),
                    3
            );

        } else {

            BottomNavHelper.setupForBuyerOrVisitor(
                    this,
                    findViewById(R.id.bottomNav),
                    3
            );
        }
    }

    private void initializeViews() {

        // Header
        ivProfilePic = findViewById(R.id.ivProfilePic);
        btnChangePic = findViewById(R.id.btnChangePic);

        tvBusinessHeader = findViewById(R.id.tvBusinessHeader);
        tvOwnerHeader = findViewById(R.id.tvOwnerHeader);
        tvName = findViewById(R.id.tvName);
        tvEmail = findViewById(R.id.tvEmail);
        tvVerificationStatus = findViewById(R.id.tvVerificationStatus);

        // Role
        btnSwitchRole = findViewById(R.id.btnSwitchRole);

        // View mode
        layoutProfileView = findViewById(R.id.layoutProfileView);

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

        // Edit mode
        layoutEditProfile = findViewById(R.id.layoutEditProfile);
        layoutSellerEditFields = findViewById(R.id.layoutSellerEditFields);

        etFullName = findViewById(R.id.etFullName);
        etPhone = findViewById(R.id.etPhone);

        etBusinessName = findViewById(R.id.etBusinessName);
        etBio = findViewById(R.id.etBio);
        etCity = findViewById(R.id.etCity);

        spinnerCategoryProfile =
                findViewById(R.id.spinnerCategoryProfile);

        etWhatsApp = findViewById(R.id.etWhatsApp);
        etCapacity = findViewById(R.id.etCapacity);
        etShopAddress = findViewById(R.id.etShopAddress);

        spinnerCanDeliverProfile =
                findViewById(R.id.spinnerCanDeliverProfile);

        btnEditProfile = findViewById(R.id.btnEditProfile);
        btnSave = findViewById(R.id.btnSave);
        btnCancelEdit = findViewById(R.id.btnCancelEdit);

        btnMyOrders = findViewById(R.id.btnMyOrders);
        btnLogout = findViewById(R.id.btnLogout);
    }

    private void setupCategorySpinner() {

        String[] categories;

        if (Constants.CATEGORIES != null &&
                Constants.CATEGORIES.length > 0) {

            categories = Constants.CATEGORIES;

        } else {

            categories = new String[]{
                    "Select Category",
                    "Beauty",
                    "Clothing",
                    "Food",
                    "Handmade",
                    "Education",
                    "Other"
            };
        }

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        categories
                );

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinnerCategoryProfile.setAdapter(adapter);
    }

    private void setupDeliverySpinner() {

        String[] deliveryOptions = {
                "Yes",
                "No"
        };

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        deliveryOptions
                );

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinnerCanDeliverProfile.setAdapter(adapter);
    }

    private void setupButtons() {

        // Change profile picture
        btnChangePic.setOnClickListener(v ->
                imagePicker.launch("image/*")
        );

        // Open Edit Profile
        btnEditProfile.setOnClickListener(v ->
                openEditMode()
        );

        // Save changes
        btnSave.setOnClickListener(v ->
                saveChanges()
        );

        // Cancel edit
        btnCancelEdit.setOnClickListener(v ->
                closeEditMode()
        );

        // My Orders
        btnMyOrders.setOnClickListener(v ->
                startActivity(
                        new Intent(
                                ProfileActivity.this,
                                MyOrdersActivity.class
                        )
                )
        );

        // Logout
        btnLogout.setOnClickListener(v -> {

            session.logout();

            Intent intent =
                    new Intent(
                            ProfileActivity.this,
                            MainActivity.class
                    );

            intent.addFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TOP |
                            Intent.FLAG_ACTIVITY_NEW_TASK
            );

            startActivity(intent);
            finish();
        });

        // Role switch
        btnSwitchRole.setOnClickListener(v ->
                showRoleSwitchDialog()
        );
    }

    private void loadUser() {

        String uid = session.getUid();

        if (uid == null) {
            return;
        }

        DatabaseReference userRef =
                FirebaseDatabase.getInstance()
                        .getReference()
                        .child(Constants.NODE_USERS)
                        .child(uid);

        userRef.addListenerForSingleValueEvent(
                new ValueEventListener() {

                    @Override
                    public void onDataChange(
                            DataSnapshot snapshot
                    ) {

                        currentUser =
                                snapshot.getValue(User.class);

                        if (currentUser == null) {
                            return;
                        }

                        showUserInformation(currentUser);

                        if ("seller".equalsIgnoreCase(
                                safe(currentUser.getRole())
                        )) {

                            loadSellerInformation(uid);

                        } else {

                            showBuyerProfile();
                        }
                    }

                    @Override
                    public void onCancelled(
                            DatabaseError error
                    ) {

                        Toast.makeText(
                                ProfileActivity.this,
                                "Profile load nahi ho saki",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
        );
    }

    private void showUserInformation(User user) {

        String name = safe(user.getName());
        String email = safe(user.getEmail());
        String phone = safe(user.getPhone());

        tvName.setText(name);
        tvEmail.setText(email);

        tvPersonalName.setText(
                "Full Name\n" + name
        );

        tvPersonalEmail.setText(
                "Email\n" + email
        );

        tvWhatsApp.setText(
                "WhatsApp / Phone\n" + phone
        );

        etFullName.setText(name);
        etPhone.setText(phone);

        if (!safe(user.getImageUrl()).isEmpty()) {

            Glide.with(this)
                    .load(user.getImageUrl())
                    .into(ivProfilePic);
        }
    }

    private void loadSellerInformation(String uid) {

        DatabaseReference sellerRef =
                FirebaseDatabase.getInstance()
                        .getReference()
                        .child(Constants.NODE_SELLERS)
                        .child(uid);

        sellerRef.addListenerForSingleValueEvent(
                new ValueEventListener() {

                    @Override
                    public void onDataChange(
                            DataSnapshot snapshot
                    ) {

                        currentSeller =
                                snapshot.getValue(Seller.class);

                        if (currentSeller == null) {

                            Toast.makeText(
                                    ProfileActivity.this,
                                    "Seller details nahi milin",
                                    Toast.LENGTH_SHORT
                            ).show();

                            return;
                        }

                        showSellerInformation(
                                currentSeller
                        );
                    }

                    @Override
                    public void onCancelled(
                            DatabaseError error
                    ) {

                        Toast.makeText(
                                ProfileActivity.this,
                                "Seller details load nahi ho saken",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
        );
    }

    private void showSellerInformation(
            Seller seller
    ) {

        String business =
                safe(seller.getBusinessName());

        String owner =
                safe(seller.getOwnerName());

        String email =
                safe(seller.getEmail());

        String phone =
                safe(seller.getPhone());

        String whatsapp =
                safe(seller.getWhatsappNumber());

        String category =
                safe(seller.getCategory());

        String bio =
                safe(seller.getBio());

        String city =
                safe(seller.getCity());

        String address =
                safe(seller.getShopAddress());

        String capacity =
                safe(seller.getCapacityDescription());

        // Header
        tvBusinessHeader.setText(
                "🎨 " + business + " Profile"
        );

        tvOwnerHeader.setText(owner);
        tvName.setText(owner);
        tvEmail.setText(email);

        // Personal
        tvPersonalName.setText(
                "Full Name\n" + owner
        );

        tvPersonalEmail.setText(
                "Email\n" + email
        );

        tvWhatsApp.setText(
                "WhatsApp\n" + whatsapp
        );

        // Business
        tvBusinessName.setText(
                "Business / Shop Name\n" + business
        );

        tvCategory.setText(
                "Category\n" + category
        );

        tvBio.setText(
                "Bio / Description\n" + bio
        );

        // Location
        tvCity.setText(
                "City\n" + city
        );

        tvShopAddress.setText(
                "Shop Address\n" + address
        );

        tvDelivery.setText(
                "Can Deliver\n" +
                        (seller.isCanDeliver()
                                ? "Yes"
                                : "No")
        );

        tvCapacity.setText(
                "Capacity / Daily Work\n" +
                        capacity
        );

        // Verification
        String verification =
                safe(seller.getVerificationStatus());

        tvVerificationStatus.setText(
                "⏳ Verification: " +
                        verification
        );

        if (!safe(seller.getCnicUrl()).isEmpty()) {

            tvCnicStatus.setText(
                    "CNIC Verification\n" +
                            "Document submitted"
            );

        } else {

            tvCnicStatus.setText(
                    "CNIC Verification\n" +
                            "Not submitted"
            );
        }

        if (!safe(seller.getVoiceUrl()).isEmpty()) {

            tvVoiceStatus.setText(
                    "Voice Verification\n" +
                            "Submitted"
            );

        } else {

            tvVoiceStatus.setText(
                    "Voice Verification\n" +
                            "Not submitted"
            );
        }

        tvRating.setText(
                "Rating\n" +
                        seller.getRating()
        );

        tvRatingCount.setText(
                "Rating Count\n" +
                        seller.getRatingCount()
        );

        tvTopRated.setText(
                "Top Rated\n" +
                        (seller.isTopRated()
                                ? "Yes"
                                : "No")
        );

        // Fill Edit fields
        etFullName.setText(owner);
        etPhone.setText(phone);

        etBusinessName.setText(business);
        etBio.setText(bio);
        etCity.setText(city);
        etWhatsApp.setText(whatsapp);
        etCapacity.setText(capacity);
        etShopAddress.setText(address);

        setSpinnerValue(
                spinnerCategoryProfile,
                category
        );

        setSpinnerValue(
                spinnerCanDeliverProfile,
                seller.isCanDeliver()
                        ? "Yes"
                        : "No"
        );

        // Seller image
        if (!safe(seller.getImageUrl()).isEmpty()) {

            Glide.with(this)
                    .load(seller.getImageUrl())
                    .into(ivProfilePic);
        }
    }

    private void showBuyerProfile() {

        tvBusinessHeader.setText(
                "💗 Buyer Profile"
        );

        tvOwnerHeader.setText(
                safe(currentUser.getName())
        );

        tvName.setText(
                safe(currentUser.getName())
        );

        tvEmail.setText(
                safe(currentUser.getEmail())
        );

        tvVerificationStatus.setVisibility(
                View.GONE
        );

        layoutSellerEditFields.setVisibility(
                View.GONE
        );

        // Hide seller information
        tvBusinessName.setVisibility(View.GONE);
        tvCategory.setVisibility(View.GONE);
        tvBio.setVisibility(View.GONE);

        tvCity.setVisibility(View.GONE);
        tvShopAddress.setVisibility(View.GONE);
        tvDelivery.setVisibility(View.GONE);
        tvCapacity.setVisibility(View.GONE);

        tvCnicStatus.setVisibility(View.GONE);
        tvVoiceStatus.setVisibility(View.GONE);
        tvRating.setVisibility(View.GONE);
        tvRatingCount.setVisibility(View.GONE);
        tvTopRated.setVisibility(View.GONE);

        btnSwitchRole.setText(
                "👤  BUYER  ▼"
        );
    }

    private void openEditMode() {

        layoutProfileView.setVisibility(
                View.GONE
        );

        layoutEditProfile.setVisibility(
                View.VISIBLE
        );

        btnEditProfile.setVisibility(
                View.GONE
        );

        if (session.isSeller()) {

            layoutSellerEditFields.setVisibility(
                    View.VISIBLE
            );

        } else {

            layoutSellerEditFields.setVisibility(
                    View.GONE
            );
        }
    }

    private void closeEditMode() {

        layoutEditProfile.setVisibility(
                View.GONE
        );

        layoutProfileView.setVisibility(
                View.VISIBLE
        );

        btnEditProfile.setVisibility(
                View.VISIBLE
        );

        loadUser();
    }

    private void saveChanges() {

        String uid = session.getUid();

        if (uid == null) {
            return;
        }

        String name =
                etFullName.getText()
                        .toString()
                        .trim();

        String phone =
                etPhone.getText()
                        .toString()
                        .trim();

        if (TextUtils.isEmpty(name)) {

            etFullName.setError(
                    "Name required"
            );

            return;
        }

        DatabaseReference userRef =
                FirebaseDatabase.getInstance()
                        .getReference()
                        .child(Constants.NODE_USERS)
                        .child(uid);

        userRef.child("name")
                .setValue(name);

        userRef.child("phone")
                .setValue(phone)
                .addOnSuccessListener(v -> {

                    session.saveName(name);

                    if (session.isSeller()) {

                        saveSellerInformation(
                                uid,
                                name,
                                phone
                        );

                    } else {

                        Toast.makeText(
                                this,
                                "Profile updated successfully",
                                Toast.LENGTH_SHORT
                        ).show();

                        closeEditMode();
                    }
                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            this,
                            "Save failed: " +
                                    e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void saveSellerInformation(
            String uid,
            String ownerName,
            String phone
    ) {

        String business =
                etBusinessName.getText()
                        .toString()
                        .trim();

        String bio =
                etBio.getText()
                        .toString()
                        .trim();

        String city =
                etCity.getText()
                        .toString()
                        .trim();

        String whatsapp =
                etWhatsApp.getText()
                        .toString()
                        .trim();

        String capacity =
                etCapacity.getText()
                        .toString()
                        .trim();

        String address =
                etShopAddress.getText()
                        .toString()
                        .trim();

        String category =
                spinnerCategoryProfile
                        .getSelectedItem()
                        .toString();

        String delivery =
                spinnerCanDeliverProfile
                        .getSelectedItem()
                        .toString();

        boolean canDeliver =
                delivery.equalsIgnoreCase("Yes");

        DatabaseReference sellerRef =
                FirebaseDatabase.getInstance()
                        .getReference()
                        .child(Constants.NODE_SELLERS)
                        .child(uid);

        sellerRef.child("ownerName")
                .setValue(ownerName);

        sellerRef.child("phone")
                .setValue(phone);

        sellerRef.child("businessName")
                .setValue(business);

        sellerRef.child("bio")
                .setValue(bio);

        sellerRef.child("city")
                .setValue(city);

        sellerRef.child("category")
                .setValue(category);

        sellerRef.child("whatsappNumber")
                .setValue(whatsapp);

        sellerRef.child("capacityDescription")
                .setValue(capacity);

        sellerRef.child("shopAddress")
                .setValue(address);

        sellerRef.child("canDeliver")
                .setValue(canDeliver)
                .addOnSuccessListener(v -> {

                    session.saveName(ownerName);

                    Toast.makeText(
                            this,
                            "All profile information updated",
                            Toast.LENGTH_SHORT
                    ).show();

                    closeEditMode();
                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            this,
                            "Seller details save failed: " +
                                    e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void uploadProfilePic(Uri uri) {

        Toast.makeText(
                this,
                "تصویر اپلوڈ ہو رہی ہے...",
                Toast.LENGTH_SHORT
        ).show();

        CloudinaryHelper.upload(
                this,
                uri,
                "profile_pictures",
                (url, error) -> {

                    if (url != null) {

                        Glide.with(this)
                                .load(url)
                                .into(ivProfilePic);

                        FirebaseDatabase
                                .getInstance()
                                .getReference()
                                .child(Constants.NODE_USERS)
                                .child(session.getUid())
                                .child("imageUrl")
                                .setValue(url)
                                .addOnSuccessListener(v ->
                                        Toast.makeText(
                                                this,
                                                "تصویر تبدیل ہو گئی",
                                                Toast.LENGTH_SHORT
                                        ).show()
                                );

                    } else {

                        Toast.makeText(
                                this,
                                "اپلوڈ ناکام: " + error,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    private void setSpinnerValue(
            Spinner spinner,
            String value
    ) {

        if (value == null) {
            return;
        }

        for (int i = 0;
             i < spinner.getCount();
             i++) {

            Object item =
                    spinner.getItemAtPosition(i);

            if (item != null &&
                    value.equalsIgnoreCase(
                            item.toString()
                    )) {

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
                Toast.makeText(
                        this,
                        "Session expired. Please login again.",
                        Toast.LENGTH_LONG
                ).show();
                return;
            }

            // Check whether Buyer profile already exists
            DatabaseReference buyerRef =
                    FirebaseDatabase.getInstance()
                            .getReference()
                            .child(Constants.NODE_BUYERS)
                            .child(uid);

            buyerRef.addListenerForSingleValueEvent(
                    new ValueEventListener() {

                        @Override
                        public void onDataChange(DataSnapshot snapshot) {

                            if (snapshot.exists()) {

                                // Buyer profile already exists
                                new androidx.appcompat.app.AlertDialog.Builder(
                                        ProfileActivity.this
                                )
                                        .setTitle("Switch Account")
                                        .setItems(
                                                new String[]{
                                                        "👤 Switch to Buyer",
                                                        "Cancel"
                                                },
                                                (dialog, which) -> {

                                                    if (which == 0) {
                                                        switchToBuyer();
                                                    }
                                                }
                                        )
                                        .show();

                            } else {

                                // Buyer profile does not exist
                                new androidx.appcompat.app.AlertDialog.Builder(
                                        ProfileActivity.this
                                )
                                        .setTitle("Switch Account")
                                        .setItems(
                                                new String[]{
                                                        "➕ Create Buyer Profile",
                                                        "Cancel"
                                                },
                                                (dialog, which) -> {

                                                    if (which == 0) {

                                                        Intent intent =
                                                                new Intent(
                                                                        ProfileActivity.this,
                                                                        BuyerProfileActivity.class
                                                                );

                                                        startActivity(intent);
                                                    }
                                                }
                                        )
                                        .show();
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError error) {

                            Toast.makeText(
                                    ProfileActivity.this,
                                    "Buyer profile check failed: "
                                            + error.getMessage(),
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    }
            );

        } else if ("buyer".equalsIgnoreCase(currentRole)) {

            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Switch Account")
                    .setMessage("Switch to your Seller account?")
                    .setPositiveButton(
                            "Switch to Seller",
                            (dialog, which) -> switchToSeller()
                    )
                    .setNegativeButton("Cancel", null)
                    .show();
        }
    }
    private void switchToBuyer() {

        String uid = session.getUid();

        if (uid == null) {
            Toast.makeText(
                    this,
                    "Session expired. Please login again.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        DatabaseReference buyerRef =
                FirebaseDatabase.getInstance()
                        .getReference()
                        .child(Constants.NODE_BUYERS)
                        .child(uid);

        buyerRef.addListenerForSingleValueEvent(
                new ValueEventListener() {

                    @Override
                    public void onDataChange(DataSnapshot snapshot) {

                        if (!snapshot.exists()) {

                            Toast.makeText(
                                    ProfileActivity.this,
                                    "Buyer profile not found.",
                                    Toast.LENGTH_LONG
                            ).show();

                            return;
                        }

                        Buyer buyer = snapshot.getValue(Buyer.class);

                        if (buyer != null &&
                                !TextUtils.isEmpty(buyer.getName())) {

                            session.saveName(buyer.getName());
                        }

                        // Only switch current mode.
                        // Logout nahi hoga.
                        session.switchRole(Constants.ROLE_BUYER);

                        Intent intent = new Intent(
                                ProfileActivity.this,
                                MainActivity.class
                        );

                        intent.addFlags(
                                Intent.FLAG_ACTIVITY_CLEAR_TOP |
                                        Intent.FLAG_ACTIVITY_NEW_TASK
                        );

                        startActivity(intent);
                        finish();
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {

                        Toast.makeText(
                                ProfileActivity.this,
                                "Buyer switch failed: "
                                        + error.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }
    private void switchToSeller() {

        String uid = session.getUid();

        if (uid == null) {
            Toast.makeText(
                    this,
                    "Session expired. Please login again.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        DatabaseReference sellerRef =
                FirebaseDatabase.getInstance()
                        .getReference()
                        .child(Constants.NODE_SELLERS)
                        .child(uid);

        sellerRef.addListenerForSingleValueEvent(
                new ValueEventListener() {

                    @Override
                    public void onDataChange(DataSnapshot snapshot) {

                        if (!snapshot.exists()) {

                            Toast.makeText(
                                    ProfileActivity.this,
                                    "Seller profile not found.",
                                    Toast.LENGTH_LONG
                            ).show();

                            return;
                        }

                        session.switchRole(Constants.ROLE_SELLER);

                        Intent intent = new Intent(
                                ProfileActivity.this,
                                SellerDashboardActivity.class
                        );

                        intent.addFlags(
                                Intent.FLAG_ACTIVITY_CLEAR_TOP |
                                        Intent.FLAG_ACTIVITY_NEW_TASK
                        );

                        startActivity(intent);
                        finish();
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {

                        Toast.makeText(
                                ProfileActivity.this,
                                "Seller profile check failed.",
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }


    private String safe(String value) {

        return value == null
                ? ""
                : value;
    }
}