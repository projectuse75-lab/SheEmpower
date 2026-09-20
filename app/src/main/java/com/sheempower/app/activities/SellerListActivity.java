package com.sheempower.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.database.*;
import com.sheempower.app.R;
import com.sheempower.app.adapters.ProductAdapter;
import com.sheempower.app.adapters.SellerAdapter;
import com.sheempower.app.models.Product;
import com.sheempower.app.models.Seller;
import com.sheempower.app.utils.Constants;
import com.sheempower.app.utils.SessionManager;
import com.sheempower.app.utils.VoiceGuide;
import com.sheempower.app.utils.WhatsAppHelper;
import com.sheempower.app.utils.PreferencesManager;
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
    private String category;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seller_list);

        session = new SessionManager(this);
        category = getIntent().getStringExtra(EXTRA_CATEGORY);

        tvCategoryTitle = findViewById(R.id.tvCategoryTitle);
        tvSellerCount   = findViewById(R.id.tvSellerCount);
        rvSellers       = findViewById(R.id.rvSellers);
        rvProducts      = findViewById(R.id.rvProducts);

        tvCategoryTitle.setText(Constants.getUrduLabel(category) + " (" + category + ")");

        VoiceGuide.announceScreen(this,
                Constants.getUrduLabel(category) + " کی بیچنے والیاں اور اشیاء کی فہرست ہے۔ کسی بیچنے والی کی تصویر پر کلک کر کے اس کی پوری پروفائل دیکھیں، یا کسی چیز پر کلک کر کے اس کی تفصیل دیکھیں۔");

        rvSellers.setLayoutManager(new GridLayoutManager(this, 2));
        sellerAdapter = new SellerAdapter(sellerList, seller -> {
            if (seller == null || seller.getUid() == null || seller.getUid().trim().isEmpty()) {
                Toast.makeText(this, "بیچنے والی کی معلومات نہیں ملی", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent i = new Intent(this, SellerProfileActivity.class);
            i.putExtra(SellerProfileActivity.EXTRA_SELLER_ID, seller.getUid().trim());
            startActivity(i);
        });
        rvSellers.setAdapter(sellerAdapter);

        rvProducts.setLayoutManager(new GridLayoutManager(this, 2));
        productAdapter = new ProductAdapter(productList,
                product -> {
                    Intent i = new Intent(this, ProductDetailActivity.class);
                    i.putExtra(ProductDetailActivity.EXTRA_PRODUCT_ID, product.getId());
                    startActivity(i);
                },
                product -> requireBuyerLogin(() -> {
                    Intent i = new Intent(this, PlaceOrderActivity.class);
                    i.putExtra(PlaceOrderActivity.EXTRA_SELLER_ID, product.getSellerId());
                    i.putExtra(PlaceOrderActivity.EXTRA_PRODUCT_ID, product.getId());
                    i.putExtra(PlaceOrderActivity.EXTRA_PRODUCT_NAME, product.getName());
                    i.putExtra(PlaceOrderActivity.EXTRA_PRODUCT_PRICE, product.getPrice());
                    startActivity(i);
                }),
                product -> chatWithSellerOfProduct(product.getSellerId())
        );
        rvProducts.setAdapter(productAdapter);

        loadData();
    }

    @Override protected void onResume() { super.onResume(); loadData(); }

    private void loadData() {
        FirebaseDatabase.getInstance().getReference().child(Constants.NODE_SELLERS)
                .orderByChild("category").equalTo(category)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(DataSnapshot snapshot) {
                        sellerList.clear();
                        List<String> sellerIds = new ArrayList<>();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            Seller s = child.getValue(Seller.class);
                            if (s != null && Constants.isApproved(s.getVerificationStatus())) {
                                // ✅ FIX: UID fallback
                                if (s.getUid() == null || s.getUid().isEmpty()) s.setUid(child.getKey());
                                sellerList.add(s);
                                sellerIds.add(s.getUid());
                            }
                        }
                        sellerAdapter.notifyDataSetChanged();
                        tvSellerCount.setText(sellerList.size() + " تصدیق شدہ بیچنے والیاں");
                        loadProductsForSellers(sellerIds);
                    }
                    @Override public void onCancelled(DatabaseError error) {
                        Toast.makeText(SellerListActivity.this, "خرابی: " + error.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void loadProductsForSellers(List<String> sellerIds) {
        FirebaseDatabase.getInstance().getReference().child(Constants.NODE_PRODUCTS)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(DataSnapshot snapshot) {
                        productList.clear();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            Product p = child.getValue(Product.class);
                            if (p != null) {
                                // ✅ FIX: ID fallback
                                if (p.getId() == null || p.getId().isEmpty()) p.setId(child.getKey());

                                if (sellerIds.contains(p.getSellerId())) productList.add(p);
                            }
                        }
                        productAdapter.notifyDataSetChanged();
                    }
                    @Override public void onCancelled(DatabaseError error) {}
                });
    }

    private void chatWithSellerOfProduct(String sellerId) {
        if (sellerId == null || sellerId.trim().isEmpty()) return;
        FirebaseDatabase.getInstance().getReference().child(Constants.NODE_SELLERS).child(sellerId.trim())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(DataSnapshot snapshot) {
                        Seller s = snapshot.getValue(Seller.class);
                        if (s != null) WhatsAppHelper.openChat(SellerListActivity.this, s.getWhatsappNumber(), null);
                    }
                    @Override public void onCancelled(DatabaseError error) {}
                });
    }

    private void requireBuyerLogin(Runnable action) {
        if (session.isLoggedIn()) action.run();
        else { Toast.makeText(this, "پہلے لاگ ان کریں", Toast.LENGTH_SHORT).show(); startActivity(new Intent(this, LoginActivity.class)); }
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