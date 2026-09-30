package com.sheempower.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.sheempower.app.R;
import com.sheempower.app.adapters.ProductAdapter;
import com.sheempower.app.adapters.SellerAdapter;
import com.sheempower.app.models.Product;
import com.sheempower.app.models.Seller;
import com.sheempower.app.utils.BottomNavHelper;
import com.sheempower.app.utils.Constants;
import com.sheempower.app.utils.LanguageHelper;
import com.sheempower.app.utils.PreferencesManager;
import com.sheempower.app.utils.SessionManager;
import com.sheempower.app.utils.VoiceGuide;
import com.sheempower.app.utils.WhatsAppHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends AppCompatActivity {

    private EditText etSearch;
    private Button btnLogin, btnJoin;
    private Button btnSearch;
    private Button btnCategoryFilter;
    private Button btnPriceFilter;
    private Button btnRatingFilter;
    private Button btnClearFilters;

    private LinearLayout layoutCategories;

    private RecyclerView rvTopSellers;
    private RecyclerView rvLatestProducts;

    private TextView tvProductsTab;
    private TextView tvSellersTab;

    private SellerAdapter topSellerAdapter;
    private SellerAdapter allSellerAdapter;
    private ProductAdapter productAdapter;

    private final List<Seller> topSellers = new ArrayList<>();
    private final List<Product> allProducts = new ArrayList<>();
    private final List<Seller> allSellers = new ArrayList<>();

    private final Set<String> verifiedSellerIds = new HashSet<>();

    private SessionManager session;

    private boolean voiceAnnounced = false;

    // Current filters
    private String selectedCategory = "";
    private String selectedPrice = "";
    private String selectedRating = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        applyTheme();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        session = new SessionManager(this);

        PreferencesManager prefs = new PreferencesManager(this);
        if (!prefs.hasSelectedLanguage()) {
            LanguageHelper.showLanguageSelectionDialog(this, langMode -> recreate());
        }

        etSearch = findViewById(R.id.etSearch);
        btnLogin = findViewById(R.id.btnLogin);
        btnJoin = findViewById(R.id.btnJoin);

        btnSearch = findViewById(R.id.btnSearch);
        btnCategoryFilter = findViewById(R.id.btnCategoryFilter);
        btnPriceFilter = findViewById(R.id.btnPriceFilter);
        btnRatingFilter = findViewById(R.id.btnRatingFilter);
        btnClearFilters = findViewById(R.id.btnClearFilters);

        layoutCategories = findViewById(R.id.layoutCategories);

        rvTopSellers = findViewById(R.id.rvTopSellers);
        rvLatestProducts = findViewById(R.id.rvLatestProducts);

        tvProductsTab = findViewById(R.id.tvProductsTab);
        tvSellersTab = findViewById(R.id.tvSellersTab);

        setupTopButtons();
        setupCategoryButtons();
        setupRecyclerViews();
        setupSearchAndFilters();

        ImageButton btnSpeaker = findViewById(R.id.btnSpeaker);
        VoiceGuide.setupSpeakerButton(btnSpeaker, this,
                session.isLoggedIn()
                        ? "شی ایمپاور میں خوش آمدید۔ یہاں سے کسی زمرے کو منتخب کر کے بیچنے والیاں اور اشیاء دیکھیں، اور آرڈر دیں۔"
                        : "شی ایمپاور میں خوش آمدید۔ خریداری کے لیے لاگ ان کریں، یا اپنا کام شروع کرنے کے لیے رجسٹر کریں۔"
        );

        loadSellersThenProducts();

        BottomNavHelper.setupForBuyerOrVisitor(
                this,
                findViewById(R.id.bottomNav),
                1
        );
    }

    @Override
    protected void onResume() {
        super.onResume();

        setupTopButtons();
        loadSellersThenProducts();

        ImageButton btnSpeaker = findViewById(R.id.btnSpeaker);
        if (btnSpeaker != null) {
            VoiceGuide.updateSpeakerIcon(btnSpeaker, VoiceGuide.isMuted(this));
        }

        if (!voiceAnnounced) {
            voiceAnnounced = true;
            VoiceGuide.announceScreen(
                    this,
                    session.isLoggedIn()
                            ? "شی ایمپاور میں خوش آمدید۔ یہاں سے کسی زمرے کو منتخب کر کے بیچنے والیاں اور اشیاء دیکھیں، اور آرڈر دیں۔"
                            : "شی ایمپاور میں خوش آمدید۔ خریداری کے لیے لاگ ان کریں، یا اپنا کام شروع کرنے کے لیے رجسٹر کریں۔"
            );
        }
    }

    // ---------------------------------------------------------
    // SEARCH + FILTERS
    // ---------------------------------------------------------

    private void setupSearchAndFilters() {

        // Search button
        btnSearch.setOnClickListener(v -> {

            String query = etSearch.getText().toString().trim();

            if (query.isEmpty() &&
                    selectedCategory.isEmpty() &&
                    selectedPrice.isEmpty() &&
                    selectedRating.isEmpty()) {

                Toast.makeText(
                        this,
                        "تلاش کے لیے کچھ لکھیں یا Filter منتخب کریں",
                        Toast.LENGTH_SHORT
                ).show();

                showAllData();

            } else {

                applyAllFilters();

                Toast.makeText(
                        this,
                        "Search applied ✓",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });

        // Keyboard Search button
        etSearch.setOnEditorActionListener((v, actionId, event) -> {

            if (actionId == EditorInfo.IME_ACTION_SEARCH) {

                btnSearch.performClick();
                return true;
            }

            return false;
        });

        // Category
        btnCategoryFilter.setOnClickListener(v -> showCategoryFilter());

        // Price
        btnPriceFilter.setOnClickListener(v -> showPriceFilter());

        // Rating
        btnRatingFilter.setOnClickListener(v -> showRatingFilter());

        // Clear
        btnClearFilters.setOnClickListener(v -> clearAllFilters());
    }

    private void showCategoryFilter() {

        String[] categories = new String[]{
                "All Categories",
                "Cooking",
                "Stitching",
                "Mehndi",
                "Makeup",
                "Embroidery"
        };

        int checked = 0;

        for (int i = 0; i < categories.length; i++) {

            if (categories[i].equals(selectedCategory)) {
                checked = i;
                break;
            }
        }

        final int[] selected = {checked};

        new AlertDialog.Builder(this)
                .setTitle("🛍️ Select Category")
                .setSingleChoiceItems(
                        categories,
                        checked,
                        (dialog, which) -> selected[0] = which
                )
                .setPositiveButton("Apply", (dialog, which) -> {

                    if (selected[0] == 0) {
                        selectedCategory = "";
                        btnCategoryFilter.setText("Category ▾");
                    } else {

                        selectedCategory = categories[selected[0]];

                        btnCategoryFilter.setText(
                                categories[selected[0]] + " ✓"
                        );
                    }

                    applyAllFilters();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showPriceFilter() {

        String[] prices = {
                "All Prices",
                "Low → High",
                "High → Low",
                "Under Rs. 500",
                "Rs. 500 – 1000",
                "Above Rs. 1000"
        };

        int checked = 0;

        for (int i = 0; i < prices.length; i++) {

            if (prices[i].equals(selectedPrice)) {
                checked = i;
                break;
            }
        }

        final int[] selected = {checked};

        new AlertDialog.Builder(this)
                .setTitle("💰 Price Filter")
                .setSingleChoiceItems(
                        prices,
                        checked,
                        (dialog, which) -> selected[0] = which
                )
                .setPositiveButton("Apply", (dialog, which) -> {

                    if (selected[0] == 0) {

                        selectedPrice = "";
                        btnPriceFilter.setText("Price ▾");

                    } else {

                        selectedPrice = prices[selected[0]];

                        btnPriceFilter.setText(
                                prices[selected[0]] + " ✓"
                        );
                    }

                    applyAllFilters();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showRatingFilter() {

        String[] ratings = {
                "All Ratings",
                "4★ & Above",
                "3★ & Above"
        };

        int checked = 0;

        for (int i = 0; i < ratings.length; i++) {

            if (ratings[i].equals(selectedRating)) {
                checked = i;
                break;
            }
        }

        final int[] selected = {checked};

        new AlertDialog.Builder(this)
                .setTitle("⭐ Rating Filter")
                .setSingleChoiceItems(
                        ratings,
                        checked,
                        (dialog, which) -> selected[0] = which
                )
                .setPositiveButton("Apply", (dialog, which) -> {

                    if (selected[0] == 0) {

                        selectedRating = "";
                        btnRatingFilter.setText("Rating ▾");

                    } else {

                        selectedRating = ratings[selected[0]];

                        btnRatingFilter.setText(
                                ratings[selected[0]] + " ✓"
                        );
                    }

                    applyAllFilters();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void clearAllFilters() {

        selectedCategory = "";
        selectedPrice = "";
        selectedRating = "";

        etSearch.setText("");

        btnCategoryFilter.setText("Category ▾");
        btnPriceFilter.setText("Price ▾");
        btnRatingFilter.setText("Rating ▾");

        showAllData();

        Toast.makeText(
                this,
                "All filters cleared ✓",
                Toast.LENGTH_SHORT
        ).show();
    }

    private void showAllData() {

        productAdapter.updateList(new ArrayList<>(allProducts));
        allSellerAdapter.updateList(new ArrayList<>(allSellers));
    }

    private void applyAllFilters() {

        String query = etSearch.getText()
                .toString()
                .toLowerCase()
                .trim();

        List<Product> filteredProducts = new ArrayList<>();
        List<Seller> filteredSellers = new ArrayList<>();

        // -------------------------------------------------
        // PRODUCTS
        // -------------------------------------------------

        for (Product p : allProducts) {

            if (p == null) continue;

            String productName = p.getName() == null
                    ? ""
                    : p.getName().toLowerCase();

            // Search
            if (!query.isEmpty() &&
                    !productName.contains(query)) {
                continue;
            }

            double price = p.getPrice();

            // Price filter
            if (!selectedPrice.isEmpty()) {

                if (selectedPrice.equals("Under Rs. 500")
                        && price >= 500) {
                    continue;
                }

                if (selectedPrice.equals("Rs. 500 – 1000")
                        && (price < 500 || price > 1000)) {
                    continue;
                }

                if (selectedPrice.equals("Above Rs. 1000")
                        && price <= 1000) {
                    continue;
                }
            }

            filteredProducts.add(p);
        }

        // Price sorting
        if (selectedPrice.equals("Low → High")) {

            Collections.sort(
                    filteredProducts,
                    (a, b) -> Double.compare(
                            a.getPrice(),
                            b.getPrice()
                    )
            );

        } else if (selectedPrice.equals("High → Low")) {

            Collections.sort(
                    filteredProducts,
                    (a, b) -> Double.compare(
                            b.getPrice(),
                            a.getPrice()
                    )
            );
        }

        // -------------------------------------------------
        // SELLERS
        // -------------------------------------------------

        for (Seller s : allSellers) {

            if (s == null) continue;

            String businessName = s.getBusinessName() == null
                    ? ""
                    : s.getBusinessName().toLowerCase();

            String primaryCategory = s.getCategory() == null
                    ? ""
                    : s.getCategory().toLowerCase();

            boolean matchesSearch = query.isEmpty()
                    || businessName.contains(query)
                    || primaryCategory.contains(query);

            // Multiple categories support
            if (!matchesSearch &&
                    s.getCategories() != null) {

                for (String category : s.getCategories()) {

                    if (category != null &&
                            category.toLowerCase().contains(query)) {

                        matchesSearch = true;
                        break;
                    }
                }
            }

            if (!matchesSearch) continue;

            // Category filter
            if (!selectedCategory.isEmpty()) {

                boolean categoryMatched =
                        selectedCategory.equalsIgnoreCase(
                                s.getCategory()
                        );

                if (!categoryMatched &&
                        s.getCategories() != null) {

                    for (String category : s.getCategories()) {

                        if (selectedCategory.equalsIgnoreCase(category)) {
                            categoryMatched = true;
                            break;
                        }
                    }
                }

                if (!categoryMatched) continue;
            }

            // Rating filter
            double rating = s.getRating();

            if (selectedRating.equals("4★ & Above")
                    && rating < 4.0) {
                continue;
            }

            if (selectedRating.equals("3★ & Above")
                    && rating < 3.0) {
                continue;
            }

            filteredSellers.add(s);
        }

        // Rating sorting
        if (selectedRating.equals("4★ & Above")
                || selectedRating.equals("3★ & Above")) {

            Collections.sort(
                    filteredSellers,
                    (a, b) -> Double.compare(
                            b.getRating(),
                            a.getRating()
                    )
            );
        }

        productAdapter.updateList(filteredProducts);
        allSellerAdapter.updateList(filteredSellers);
    }

    // ---------------------------------------------------------
    // TOP BUTTONS
    // ---------------------------------------------------------

    private void setupTopButtons() {

        if (session.isLoggedIn()) {

            btnLogin.setVisibility(View.GONE);

            btnJoin.setText(
                    "👤 " + session.getName()
            );

            btnJoin.setOnClickListener(
                    v -> startActivity(
                            new Intent(
                                    this,
                                    ProfileActivity.class
                            )
                    )
            );

        } else {

            btnLogin.setVisibility(View.VISIBLE);

            btnLogin.setText("🔑 لاگ ان");
            btnJoin.setText("📝 رجسٹر");

            btnLogin.setOnClickListener(
                    v -> startActivity(
                            new Intent(
                                    this,
                                    LoginActivity.class
                            )
                    )
            );

            btnJoin.setOnClickListener(
                    v -> startActivity(
                            new Intent(
                                    this,
                                    SellerRegistrationActivity.class
                            )
                    )
            );
        }
    }

    // ---------------------------------------------------------
    // CATEGORY BUTTONS
    // ---------------------------------------------------------

    private void setupCategoryButtons() {

        layoutCategories.removeAllViews();

        for (int i = 0;
             i < Constants.CATEGORIES.length;
             i++) {

            String cat = Constants.CATEGORIES[i];
            String urduLabel =
                    Constants.CATEGORY_LABELS_UR[i];

            View card = LayoutInflater
                    .from(this)
                    .inflate(
                            R.layout.item_category,
                            layoutCategories,
                            false
                    );

            CircleImageView iv =
                    card.findViewById(
                            R.id.ivCategoryImage
                    );

            TextView tvName =
                    card.findViewById(
                            R.id.tvCategoryName
                    );

            TextView tvNameEn =
                    card.findViewById(
                            R.id.tvCategoryNameEn
                    );

            PreferencesManager prefsManager = new PreferencesManager(this);
            String langMode = prefsManager.getAppLanguage();

            if (PreferencesManager.LANG_URDU.equals(langMode)) {
                tvName.setText(urduLabel);
                tvNameEn.setVisibility(View.GONE);
            } else if (PreferencesManager.LANG_ENGLISH.equals(langMode)) {
                tvName.setText(cat);
                tvNameEn.setVisibility(View.GONE);
            } else {
                tvName.setText(urduLabel);
                tvNameEn.setText(cat);
                tvNameEn.setVisibility(View.VISIBLE);
            }

            iv.setImageResource(
                    Constants.CATEGORY_IMAGE_RES[i]
            );

            card.setOnClickListener(v -> {

                Intent intent =
                        new Intent(
                                this,
                                SellerListActivity.class
                        );

                intent.putExtra(
                        SellerListActivity.EXTRA_CATEGORY,
                        cat
                );

                startActivity(intent);
            });

            layoutCategories.addView(card);
        }
    }

    // ---------------------------------------------------------
    // RECYCLER VIEWS
    // ---------------------------------------------------------

    private void setupRecyclerViews() {

        rvTopSellers.setLayoutManager(
                new LinearLayoutManager(
                        this,
                        LinearLayoutManager.HORIZONTAL,
                        false
                )
        );

        topSellerAdapter =
                new SellerAdapter(
                        topSellers,
                        this::openSellerProfile
                );

        rvTopSellers.setAdapter(
                topSellerAdapter
        );

        rvLatestProducts.setLayoutManager(
                new GridLayoutManager(this, 2)
        );

        productAdapter =
                new ProductAdapter(
                        allProducts,
                        this::openProductDetail,
                        product ->
                                requireBuyerLogin(
                                        () -> goToOrder(product)
                                ),
                        product ->
                                chatOnWhatsApp(
                                        product.getSellerId()
                                )
                );

        rvLatestProducts.setAdapter(
                productAdapter
        );

        allSellerAdapter =
                new SellerAdapter(
                        allSellers,
                        this::openSellerProfile
                );

        tvProductsTab.setOnClickListener(
                v -> switchTab(true)
        );

        tvSellersTab.setOnClickListener(
                v -> switchTab(false)
        );

        rvTopSellers.setNestedScrollingEnabled(false);
        rvLatestProducts.setNestedScrollingEnabled(false);
    }

    private void switchTab(boolean products) {
        if (products) {
            tvProductsTab.setBackgroundResource(R.drawable.bg_skeu_button_primary);
            tvProductsTab.setTextColor(0xFFFFFFFF);
            tvSellersTab.setBackgroundResource(R.drawable.bg_skeu_button_cream);
            tvSellersTab.setTextColor(ContextCompat.getColor(this, R.color.text_primary));

            rvLatestProducts.setLayoutManager(new GridLayoutManager(this, 2));
            rvLatestProducts.setAdapter(productAdapter);
        } else {
            tvSellersTab.setBackgroundResource(R.drawable.bg_skeu_button_primary);
            tvSellersTab.setTextColor(0xFFFFFFFF);
            tvProductsTab.setBackgroundResource(R.drawable.bg_skeu_button_cream);
            tvProductsTab.setTextColor(ContextCompat.getColor(this, R.color.text_primary));

            rvLatestProducts.setLayoutManager(new GridLayoutManager(this, 2));
            rvLatestProducts.setAdapter(allSellerAdapter);
        }
        rvLatestProducts.setNestedScrollingEnabled(false);
    }

    // ---------------------------------------------------------
    // FIREBASE
    // ---------------------------------------------------------

    private void loadSellersThenProducts() {

        FirebaseDatabase
                .getInstance()
                .getReference()
                .child(Constants.NODE_SELLERS)
                .addListenerForSingleValueEvent(
                        new ValueEventListener() {

                            @Override
                            public void onDataChange(DataSnapshot snapshot) {
                                allSellers.clear();
                                topSellers.clear();
                                verifiedSellerIds.clear();

                                for (DataSnapshot child : snapshot.getChildren()) {
                                    Seller s = child.getValue(Seller.class);
                                    if (s == null) continue;

                                    if ("blocked".equalsIgnoreCase(s.getVerificationStatus()) ||
                                        "rejected".equalsIgnoreCase(s.getVerificationStatus())) {
                                        continue;
                                    }

                                    String uid = child.getKey();
                                    if (uid == null) continue;

                                    s.setUid(uid.trim());

                                    String directImg = child.child("imageUrl").getValue(String.class);
                                    if (directImg != null && !directImg.trim().isEmpty()) {
                                        s.setImageUrl(directImg.trim());
                                    }

                                    if (s.getImageUrl() == null || s.getImageUrl().trim().isEmpty()) {
                                        FirebaseDatabase.getInstance().getReference().child(Constants.NODE_USERS)
                                                .child(uid.trim()).child("imageUrl").get()
                                                .addOnSuccessListener(imgSnap -> {
                                                    String img = imgSnap.getValue(String.class);
                                                    if (img != null && !img.trim().isEmpty()) {
                                                        s.setImageUrl(img.trim());
                                                        if (topSellerAdapter != null) topSellerAdapter.notifyDataSetChanged();
                                                        if (allSellerAdapter != null) allSellerAdapter.notifyDataSetChanged();
                                                    }
                                                });
                                    }

                                    allSellers.add(s);
                                    verifiedSellerIds.add(uid.trim());

                                    if (s.isTopRated() || Constants.isApproved(s.getVerificationStatus())) {
                                        topSellers.add(s);
                                    }
                                }

                                if (topSellers.isEmpty()) {
                                    topSellers.addAll(allSellers);
                                }

                                Collections.sort(topSellers, (a, b) -> Double.compare(b.getRating(), a.getRating()));

                                topSellerAdapter.notifyDataSetChanged();
                                allSellerAdapter.notifyDataSetChanged();

                                loadProducts();

                                // Re-apply search/filter
                                if (!etSearch.getText().toString().trim().isEmpty()
                                        || !selectedCategory.isEmpty()
                                        || !selectedPrice.isEmpty()
                                        || !selectedRating.isEmpty()) {
                                    applyAllFilters();
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError error) {
                                Toast.makeText(MainActivity.this, "ڈیٹا لوڈ نہیں ہوا: " + error.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        }
                );
    }

    private void loadProducts() {
        FirebaseDatabase.getInstance().getReference().child(Constants.NODE_PRODUCTS)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        allProducts.clear();

                        for (DataSnapshot child : snapshot.getChildren()) {
                            Product p = child.getValue(Product.class);
                            if (p != null) {
                                String pid = child.getKey();
                                if (pid != null) {
                                    p.setId(pid.trim());
                                }
                                if (p.getSellerId() != null) {
                                    p.setSellerId(p.getSellerId().trim());
                                }
                                allProducts.add(p);
                            }
                        }

                        productAdapter.notifyDataSetChanged();

                        // Apply active filters
                        if (!etSearch.getText().toString().trim().isEmpty()
                                || !selectedCategory.isEmpty()
                                || !selectedPrice.isEmpty()
                                || !selectedRating.isEmpty()) {
                            applyAllFilters();
                        }
                    }

                            @Override
                            public void onCancelled(
                                    DatabaseError error) {
                            }
                        }
                );
    }

    // ---------------------------------------------------------
    // OPEN SCREENS
    // ---------------------------------------------------------

    private void openSellerProfile(Seller seller) {

        if (seller == null ||
                seller.getUid() == null ||
                seller.getUid().trim().isEmpty()) {

            Toast.makeText(
                    this,
                    "بیچنے والی کی معلومات نہیں ملی",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        Intent i =
                new Intent(
                        this,
                        SellerProfileActivity.class
                );

        i.putExtra(
                SellerProfileActivity.EXTRA_SELLER_ID,
                seller.getUid().trim()
        );

        startActivity(i);
    }

    private void openProductDetail(Product product) {

        Intent i =
                new Intent(
                        this,
                        ProductDetailActivity.class
                );

        i.putExtra(
                ProductDetailActivity.EXTRA_PRODUCT_ID,
                product.getId()
        );

        startActivity(i);
    }

    private void goToOrder(Product product) {

        Intent i =
                new Intent(
                        this,
                        PlaceOrderActivity.class
                );

        i.putExtra(
                PlaceOrderActivity.EXTRA_SELLER_ID,
                product.getSellerId()
        );

        i.putExtra(
                PlaceOrderActivity.EXTRA_PRODUCT_ID,
                product.getId()
        );

        i.putExtra(
                PlaceOrderActivity.EXTRA_PRODUCT_NAME,
                product.getName()
        );

        i.putExtra(
                PlaceOrderActivity.EXTRA_PRODUCT_PRICE,
                product.getPrice()
        );

        i.putExtra(
                PlaceOrderActivity.EXTRA_PRODUCT_CATEGORY,
                product.getCategory()
        );

        startActivity(i);
    }

    private void chatOnWhatsApp(String sellerId) {

        FirebaseDatabase
                .getInstance()
                .getReference()
                .child(Constants.NODE_SELLERS)
                .child(sellerId)
                .addListenerForSingleValueEvent(
                        new ValueEventListener() {

                            @Override
                            public void onDataChange(
                                    DataSnapshot snapshot) {

                                Seller s =
                                        snapshot.getValue(
                                                Seller.class
                                        );

                                if (s != null) {

                                    WhatsAppHelper.openChat(
                                            MainActivity.this,
                                            s.getWhatsappNumber(),
                                            null
                                    );
                                }
                            }

                            @Override
                            public void onCancelled(
                                    DatabaseError error) {
                            }
                        }
                );
    }

    private void requireBuyerLogin(
            Runnable action) {

        if (session.isLoggedIn()) {

            action.run();

        } else {

            Toast.makeText(
                    this,
                    "پہلے لاگ ان کریں",
                    Toast.LENGTH_SHORT
            ).show();

            startActivity(
                    new Intent(
                            this,
                            LoginActivity.class
                    )
            );
        }
    }

    // ---------------------------------------------------------
    // DARK THEME
    // ---------------------------------------------------------

    private void applyTheme() {

        PreferencesManager prefs =
                new PreferencesManager(this);

        if (prefs.isDarkThemeEnabled()) {

            setTheme(
                    R.style.Theme_SheEmpower_Dark
            );

        } else {

            setTheme(
                    R.style.Theme_SheEmpower
            );
        }
    }
}