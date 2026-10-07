package com.sheempower.app.activities;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.google.firebase.database.*;
import com.sheempower.app.R;
import com.sheempower.app.models.CartItem;
import com.sheempower.app.models.Product;
import com.sheempower.app.models.Seller;
import com.sheempower.app.utils.CartManager;
import com.sheempower.app.utils.*;
import com.sheempower.app.utils.PreferencesManager;

public class ProductDetailActivity extends AppCompatActivity {

    public static final String EXTRA_PRODUCT_ID = "productId";
    public static final String EXTRA_IS_SELLER_VIEW = "isSellerView";

    private ImageView ivProduct;
    private TextView tvName, tvPrice, tvSellerName, tvSellerMeta, tvSellerRating, tvDescription;
    private Button btnViewSeller, btnPlaceOrder, btnWhatsApp, btnAddToCart;

    private Product currentProduct;
    private Seller currentSeller;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);

        LanguageHelper.processViewHierarchy(findViewById(android.R.id.content), this);

        session = new SessionManager(this);
        String productId = getIntent().getStringExtra(EXTRA_PRODUCT_ID);

        if (productId == null) { Toast.makeText(this, "چیز نہیں ملی", Toast.LENGTH_LONG).show(); finish(); return; }

        ImageButton btnSpeaker = findViewById(R.id.btnSpeaker);
        VoiceGuide.setupSpeakerButton(btnSpeaker, this,
                "یہ چیز کی مکمل تفصیل ہے۔ اس کی قیمت پہلے سے مقرر ہے۔ آرڈر دینے کے لیے نیچے بٹن دبائیں، یا واٹس ایپ پر بات کرنے کے لیے سبز بٹن دبائیں۔"
        );

        ivProduct      = findViewById(R.id.ivProduct);
        tvName         = findViewById(R.id.tvProductName);
        tvPrice        = findViewById(R.id.tvProductPrice);
        tvSellerName   = findViewById(R.id.tvSellerName);
        tvSellerMeta   = findViewById(R.id.tvSellerMeta);
        tvSellerRating = findViewById(R.id.tvSellerRating);
        tvDescription  = findViewById(R.id.tvDescription);
        btnViewSeller  = findViewById(R.id.btnViewSeller);
        btnPlaceOrder  = findViewById(R.id.btnPlaceOrder);
        btnWhatsApp    = findViewById(R.id.btnWhatsApp);
        btnAddToCart   = findViewById(R.id.btnAddToCart);

        BottomNavHelper.setupBackButton(this);

        boolean isSellerView = getIntent().getBooleanExtra(EXTRA_IS_SELLER_VIEW, false);
        if (isSellerView) {
            if (btnPlaceOrder != null) btnPlaceOrder.setVisibility(View.GONE);
            if (btnWhatsApp != null) btnWhatsApp.setVisibility(View.GONE);
            if (btnViewSeller != null) btnViewSeller.setVisibility(View.GONE);
            if (btnAddToCart != null) btnAddToCart.setVisibility(View.GONE);
        }

        if (btnAddToCart != null) {
            boolean isDark = isDarkMode();
            btnAddToCart.setTextColor(isDark ? 0xFFFFFFFF : 0xFF192A56);
            btnAddToCart.setOnClickListener(v -> requireBuyerLogin(this::addToCart));
        }

        loadProduct(productId);

        btnWhatsApp.setOnClickListener(v -> WhatsAppHelper.openChat(this,
                currentSeller != null ? currentSeller.getWhatsappNumber() : null,
                currentProduct != null ? "السلام علیکم! مجھے " + currentProduct.getName() + " کے بارے میں پوچھنا تھا۔" : null));

        btnPlaceOrder.setOnClickListener(v -> requireBuyerLogin(() -> {
            if (currentProduct == null) return;
            Intent i = new Intent(this, PlaceOrderActivity.class);
            i.putExtra(PlaceOrderActivity.EXTRA_SELLER_ID, currentProduct.getSellerId());
            i.putExtra(PlaceOrderActivity.EXTRA_PRODUCT_ID, currentProduct.getId());
            i.putExtra(PlaceOrderActivity.EXTRA_PRODUCT_NAME, currentProduct.getName());
            i.putExtra(PlaceOrderActivity.EXTRA_PRODUCT_PRICE, currentProduct.getPrice());
            i.putExtra(PlaceOrderActivity.EXTRA_PRODUCT_CATEGORY, currentProduct.getCategory());
            startActivity(i);
        }));
    }

    private void loadProduct(String productId) {
        DatabaseReference prodRef = FirebaseDatabase.getInstance().getReference().child(Constants.NODE_PRODUCTS);
        
        // 1. Pehle direct key se check karein
        prodRef.child(productId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(DataSnapshot snapshot) {
                currentProduct = snapshot.getValue(Product.class);
                if (currentProduct != null) {
                    setupProductUI(snapshot.getKey());
                } else {
                    // 2. Agar direct nahi mila (ID mismatch), toh poori list scan karein
                    scanAllProductsForId(productId);
                }
            }
            @Override public void onCancelled(DatabaseError error) {}
        });
    }

    private void scanAllProductsForId(String targetId) {
        FirebaseDatabase.getInstance().getReference().child(Constants.NODE_PRODUCTS)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(DataSnapshot snapshot) {
                        for (DataSnapshot child : snapshot.getChildren()) {
                            String key = child.getKey();
                            if (key != null && key.trim().equals(targetId.trim())) {
                                currentProduct = child.getValue(Product.class);
                                if (currentProduct != null) {
                                    setupProductUI(key);
                                    return;
                                }
                            }
                        }
                        Toast.makeText(ProductDetailActivity.this, "چیز اب دستیاب نہیں ہے", Toast.LENGTH_LONG).show();
                    }
                    @Override public void onCancelled(DatabaseError error) {}
                });
    }

    private void setupProductUI(String actualId) {
        if (currentProduct.getId() == null) currentProduct.setId(actualId);
        tvName.setText(currentProduct.getName());
        tvPrice.setText("PKR " + (long) currentProduct.getPrice());
        tvDescription.setText(currentProduct.getDescription());
        if (currentProduct.getImageUrl() != null && !currentProduct.getImageUrl().isEmpty())
            Glide.with(ProductDetailActivity.this).load(currentProduct.getImageUrl()).into(ivProduct);

        if (ivProduct != null) {
            ivProduct.setOnClickListener(v -> {
                if (currentProduct != null && currentProduct.getImageUrl() != null && !currentProduct.getImageUrl().trim().isEmpty()) {
                    showFullScreenImageDialog(currentProduct.getImageUrl().trim(), currentProduct.getName());
                }
            });
        }

        bindSellerMeta();

        boolean isSellerView = getIntent().getBooleanExtra(EXTRA_IS_SELLER_VIEW, false);

        if (isSellerView) {
            if (btnPlaceOrder != null) btnPlaceOrder.setVisibility(View.GONE);
            if (btnAddToCart != null) btnAddToCart.setVisibility(View.GONE);
            if (btnWhatsApp != null) btnWhatsApp.setVisibility(View.GONE);
            if (btnViewSeller != null) btnViewSeller.setVisibility(View.GONE);
        } else {
            if (btnPlaceOrder != null) btnPlaceOrder.setVisibility(View.VISIBLE);
            if (btnAddToCart != null) btnAddToCart.setVisibility(View.VISIBLE);
            if (btnWhatsApp != null) btnWhatsApp.setVisibility(View.VISIBLE);
            if (btnViewSeller != null) btnViewSeller.setVisibility(View.VISIBLE);
        }

        if (currentProduct.getSellerId() != null && !currentProduct.getSellerId().trim().isEmpty()) {
            loadSeller(currentProduct.getSellerId().trim());
        }
    }

    private void bindSellerMeta() {
        String prodCat = (currentProduct != null && currentProduct.getCategory() != null && !currentProduct.getCategory().trim().isEmpty())
                ? currentProduct.getCategory().trim()
                : (currentSeller != null && currentSeller.getCategory() != null ? currentSeller.getCategory().trim() : "Cooking");

        String displayCat = Constants.getCategoryDisplayLabel(this, prodCat);
        String city = (currentSeller != null && currentSeller.getCity() != null) ? currentSeller.getCity() : "";

        tvSellerMeta.setText(displayCat + (city.isEmpty() ? "" : " · " + city));
    }

    private void loadSeller(String sellerId) {
        if (sellerId == null || sellerId.trim().isEmpty()) return;
        FirebaseDatabase.getInstance().getReference().child(Constants.NODE_SELLERS).child(sellerId.trim())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(DataSnapshot snapshot) {
                        currentSeller = snapshot.getValue(Seller.class);
                        if (currentSeller == null) return;

                        if (currentSeller.getUid() == null || currentSeller.getUid().isEmpty())
                            currentSeller.setUid(snapshot.getKey());

                        currentSeller.setCategories(Constants.parseCategoriesFromSnapshot(snapshot));

                        tvSellerName.setText(currentSeller.getBusinessName());
                        bindSellerMeta();
                        tvSellerRating.setText("⭐ " + currentSeller.getRating() + (currentSeller.isTopRated() ? "  🏅 بہترین" : ""));

                        // Detect if product category is no longer active for seller
                        if (currentProduct != null && !Constants.sellerProvidesCategory(currentSeller, currentProduct.getCategory())) {
                            Toast.makeText(ProductDetailActivity.this, "یہ چیز اب فعال نہیں ہے / Category no longer active", Toast.LENGTH_SHORT).show();
                            if (btnPlaceOrder != null) btnPlaceOrder.setEnabled(false);
                        }

                        btnViewSeller.setOnClickListener(v -> {
                            if (sellerId.isEmpty()) {
                                Toast.makeText(ProductDetailActivity.this, "بیچنے والی کی معلومات نہیں ملی", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            Intent i = new Intent(ProductDetailActivity.this, SellerProfileActivity.class);
                            i.putExtra(SellerProfileActivity.EXTRA_SELLER_ID, sellerId);
                            if (currentProduct != null && currentProduct.getCategory() != null) {
                                i.putExtra(SellerProfileActivity.EXTRA_SELECTED_CATEGORY, currentProduct.getCategory().trim());
                            }
                            startActivity(i);
                        });
                    }
                    @Override public void onCancelled(DatabaseError error) {}
                });
    }

    private void requireBuyerLogin(Runnable action) {
        if (!session.isLoggedIn()) {
            Toast.makeText(this, LanguageHelper.processDualString(this, "پہلے لاگ ان کریں / Please Login First"), Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            return;
        }

        action.run();
    }

    private void showFullScreenImageDialog(String imageUrl, String title) {
        if (isFinishing() || isDestroyed()) return;

        Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_fullscreen_image);

        ImageView ivFull = dialog.findViewById(R.id.ivFullScreenProduct);
        TextView tvTitle = dialog.findViewById(R.id.tvImageTitle);
        ImageButton btnClose = dialog.findViewById(R.id.btnCloseImage);

        if (tvTitle != null) tvTitle.setText(title != null ? title : "");
        if (btnClose != null) btnClose.setOnClickListener(v -> dialog.dismiss());

        if (ivFull != null) {
            Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.placeholder_product)
                    .error(R.drawable.placeholder_product)
                    .into(ivFull);

            ivFull.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }

    private void addToCart() {
        if (currentProduct == null || session.getUid() == null) return;

        CartItem item = new CartItem(currentProduct.getId(), currentProduct.getId(), currentProduct.getName(),
                currentProduct.getImageUrl(), currentProduct.getCategory(), currentProduct.getSellerId(),
                currentSeller != null ? currentSeller.getBusinessName() : "", currentProduct.getPrice(), 1);

        CartManager.getInstance(this).addToCart(item);
        Toast.makeText(this, "کارٹ میں شامل ہو گیا! 🛒 / Added to Cart!", Toast.LENGTH_SHORT).show();
    }

    private boolean isDarkMode() {
        PreferencesManager prefs = new PreferencesManager(this);
        return prefs.isDarkThemeEnabled();
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