package com.sheempower.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
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
import com.sheempower.app.utils.PreferencesManager;
import com.sheempower.app.utils.SessionManager;
import com.sheempower.app.utils.VoiceGuide;
import com.sheempower.app.utils.WhatsAppHelper;

import java.util.ArrayList;
import java.util.List;

public class SellerListActivity extends AppCompatActivity {

    public static final String EXTRA_CATEGORY = "category";

    private TextView tvCategoryTitle, tvSellerCount;
    private RecyclerView rvSellers, rvProducts;

    private SellerAdapter sellerAdapter;
    private ProductAdapter productAdapter;

    private final List<Seller> sellerList = new ArrayList<>();
    private final List<Product> productList = new ArrayList<>();

    private String selectedCategory;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seller_list);

        session = new SessionManager(this);
        selectedCategory = getIntent().getStringExtra(EXTRA_CATEGORY);

        if (selectedCategory != null) {
            selectedCategory = selectedCategory.trim();
        }

        if (TextUtils.isEmpty(selectedCategory)) {
            Toast.makeText(this, "زمرہ منتخب نہیں کیا گیا", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        initializeViews();
        BottomNavHelper.setupBackButton(this);
        setupSellerRecyclerView();
        setupProductRecyclerView();
        updateCategoryHeader();
        loadSellersThenProducts();
    }

    private void initializeViews() {
        tvCategoryTitle = findViewById(R.id.tvCategoryTitle);
        tvSellerCount   = findViewById(R.id.tvSellerCount);
        rvSellers       = findViewById(R.id.rvSellers);
        rvProducts      = findViewById(R.id.rvProducts);

        ImageButton btnSpeaker = findViewById(R.id.btnSpeaker);
        VoiceGuide.setupSpeakerButton(btnSpeaker, this,
                Constants.getUrduLabel(selectedCategory) + " کی بیچنے والیاں اور اشیاء دیکھیں۔"
        );

        if (tvCategoryTitle != null) {
            tvCategoryTitle.setText(
                    Constants.getUrduLabel(selectedCategory)
                            + " ("
                            + selectedCategory
                            + ")"
            );
        }
    }

    private void updateCategoryHeader() {
        if (tvCategoryTitle != null) {
            tvCategoryTitle.setText(
                    Constants.getUrduLabel(selectedCategory)
                            + " ("
                            + selectedCategory
                            + ")"
            );
        }

        VoiceGuide.announceScreen(
                this,
                Constants.getUrduLabel(selectedCategory)
                        + " کی بیچنے والیاں اور اشیاء دیکھیں۔"
        );
    }

    private void setupSellerRecyclerView() {
        if (rvSellers == null) return;

        rvSellers.setLayoutManager(new GridLayoutManager(this, 2));
        rvSellers.setNestedScrollingEnabled(false);

        sellerAdapter = new SellerAdapter(
                sellerList,
                seller -> {
                    if (seller == null || TextUtils.isEmpty(seller.getUid())) {
                        Toast.makeText(this, "بیچنے والی کی معلومات نہیں ملی", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Intent intent = new Intent(SellerListActivity.this, SellerProfileActivity.class);
                    intent.putExtra(SellerProfileActivity.EXTRA_SELLER_ID, seller.getUid().trim());
                    intent.putExtra(SellerProfileActivity.EXTRA_SELECTED_CATEGORY, selectedCategory);
                    startActivity(intent);
                }
        );

        rvSellers.setAdapter(sellerAdapter);
    }

    private void setupProductRecyclerView() {
        if (rvProducts == null) return;

        rvProducts.setLayoutManager(new GridLayoutManager(this, 2));
        rvProducts.setNestedScrollingEnabled(false);

        productAdapter = new ProductAdapter(
                productList,
                product -> {
                    Intent intent = new Intent(this, ProductDetailActivity.class);
                    intent.putExtra(ProductDetailActivity.EXTRA_PRODUCT_ID, product.getId());
                    startActivity(intent);
                },
                product -> requireBuyerLogin(() -> goToOrder(product)),
                product -> chatOnWhatsApp(product.getSellerId())
        );

        rvProducts.setAdapter(productAdapter);
    }

    private void loadSellersThenProducts() {
        if (TextUtils.isEmpty(selectedCategory)) return;

        FirebaseDatabase.getInstance().getReference().child(Constants.NODE_SELLERS)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        sellerList.clear();

                        for (DataSnapshot child : snapshot.getChildren()) {
                            Seller seller = child.getValue(Seller.class);
                            if (seller == null) continue;

                            if (TextUtils.isEmpty(seller.getUid())) {
                                seller.setUid(child.getKey());
                            }
                            seller.setCategories(Constants.parseCategoriesFromSnapshot(child));

                            if ("blocked".equalsIgnoreCase(seller.getVerificationStatus()) ||
                                "rejected".equalsIgnoreCase(seller.getVerificationStatus())) {
                                continue;
                            }

                            if (Constants.sellerProvidesCategory(seller, selectedCategory)) {
                                String directImg = child.child("imageUrl").getValue(String.class);
                                if (directImg != null && !directImg.trim().isEmpty()) {
                                    seller.setImageUrl(directImg.trim());
                                }

                                String uid = seller.getUid();
                                if ((seller.getImageUrl() == null || seller.getImageUrl().trim().isEmpty()) && uid != null) {
                                    FirebaseDatabase.getInstance().getReference().child(Constants.NODE_USERS)
                                            .child(uid.trim()).child("imageUrl").get()
                                            .addOnSuccessListener(imgSnap -> {
                                                String userImg = imgSnap.getValue(String.class);
                                                if (userImg != null && !userImg.trim().isEmpty()) {
                                                    seller.setImageUrl(userImg.trim());
                                                    if (sellerAdapter != null) sellerAdapter.notifyDataSetChanged();
                                                }
                                            });
                                }

                                sellerList.add(seller);
                            }
                        }

                        if (sellerAdapter != null) {
                            sellerAdapter.notifyDataSetChanged();
                        }

                        updateSellerCount();

                        // Load products AFTER sellerList is fully populated
                        loadProductsInternal();
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Toast.makeText(SellerListActivity.this, "خرابی: " + error.getMessage(), Toast.LENGTH_LONG).show();
                        loadProductsInternal();
                    }
                });
    }

    private void loadProductsInternal() {
        if (TextUtils.isEmpty(selectedCategory)) return;

        FirebaseDatabase.getInstance().getReference().child(Constants.NODE_PRODUCTS)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        productList.clear();

                        for (DataSnapshot child : snapshot.getChildren()) {
                            Product p = child.getValue(Product.class);
                            if (p != null) {
                                String pid = child.getKey();
                                if (pid != null) p.setId(pid.trim());

                                if (isProductFromActiveSeller(p)) {
                                    productList.add(p);
                                }
                            }
                        }

                        if (productAdapter != null) {
                            productAdapter.notifyDataSetChanged();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {}
                });
    }

    private boolean isProductFromActiveSeller(Product p) {
        if (p == null || p.getCategory() == null || p.getSellerId() == null || p.getSellerId().trim().isEmpty()) {
            return false;
        }

        // 1. Product category must match selected category
        if (!selectedCategory.equalsIgnoreCase(p.getCategory().trim())) {
            return false;
        }

        String pSellerId = p.getSellerId().trim();

        // 2. Must match an active seller in sellerList who provides this category
        for (Seller s : sellerList) {
            if (s == null) continue;
            boolean isOwner = (s.getUid() != null && pSellerId.equalsIgnoreCase(s.getUid().trim()))
                    || (s.getEmail() != null && pSellerId.equalsIgnoreCase(s.getEmail().trim()))
                    || (s.getPhone() != null && pSellerId.equalsIgnoreCase(s.getPhone().trim()))
                    || (s.getWhatsappNumber() != null && pSellerId.equalsIgnoreCase(s.getWhatsappNumber().trim()));

            if (isOwner) {
                return Constants.sellerProvidesCategory(s, selectedCategory);
            }
        }

        // If sellerList is empty or no match, do not display products from unverified/missing sellers
        return sellerList.isEmpty();
    }

    private void goToOrder(Product product) {
        Intent i = new Intent(this, PlaceOrderActivity.class);
        i.putExtra(PlaceOrderActivity.EXTRA_SELLER_ID, product.getSellerId());
        i.putExtra(PlaceOrderActivity.EXTRA_PRODUCT_ID, product.getId());
        i.putExtra(PlaceOrderActivity.EXTRA_PRODUCT_NAME, product.getName());
        i.putExtra(PlaceOrderActivity.EXTRA_PRODUCT_PRICE, product.getPrice());
        i.putExtra(PlaceOrderActivity.EXTRA_PRODUCT_CATEGORY, product.getCategory());
        startActivity(i);
    }

    private void chatOnWhatsApp(String sellerId) {
        if (sellerId == null || sellerId.isEmpty()) return;
        FirebaseDatabase.getInstance().getReference().child(Constants.NODE_SELLERS).child(sellerId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        Seller s = snapshot.getValue(Seller.class);
                        if (s != null) {
                            WhatsAppHelper.openChat(SellerListActivity.this, s.getWhatsappNumber(), null);
                        }
                    }
                    @Override public void onCancelled(DatabaseError error) {}
                });
    }

    private void requireBuyerLogin(Runnable action) {
        if (session == null) session = new SessionManager(this);
        if (session.isLoggedIn()) {
            action.run();
        } else {
            Toast.makeText(this, "پہلے لاگ ان کریں", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
        }
    }

    private void updateSellerCount() {
        if (tvSellerCount == null) return;
        tvSellerCount.setText(sellerList.size() + " تصدیق شدہ بیچنے والیاں");
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (selectedCategory != null && !selectedCategory.isEmpty()) {
            loadSellersThenProducts();
        }
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