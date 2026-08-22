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
    private String category;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seller_list);

        session = new SessionManager(this);
        category = getIntent().getStringExtra(EXTRA_CATEGORY);

        tvCategoryTitle = findViewById(R.id.tvCategoryTitle);
        tvSellerCount   = findViewById(R.id.tvSellerCount);
        rvSellers       = findViewById(R.id.rvSellers);
        rvProducts      = findViewById(R.id.rvProducts);

        tvCategoryTitle.setText(category);

        rvSellers.setLayoutManager(new GridLayoutManager(this, 3));
        sellerAdapter = new SellerAdapter(sellerList, seller -> {
            Intent i = new Intent(this, SellerProfileActivity.class);
            i.putExtra(SellerProfileActivity.EXTRA_SELLER_ID, seller.getUid());
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

    private void loadData() {
        FirebaseDatabase.getInstance().getReference().child(Constants.NODE_SELLERS)
                .orderByChild("category").equalTo(category)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        sellerList.clear();
                        List<String> sellerIds = new ArrayList<>();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            Seller s = child.getValue(Seller.class);
                            // ✅ FIXED: "verified" → "approved"
                            if (s != null && "approved".equals(s.getVerificationStatus())) {
                                sellerList.add(s);
                                sellerIds.add(s.getUid());
                            }
                        }
                        sellerAdapter.notifyDataSetChanged();
                        tvSellerCount.setText(sellerList.size() + " verified seller(s)");
                        loadProductsForSellers(sellerIds);
                    }
                    @Override
                    public void onCancelled(DatabaseError error) {}
                });
    }

    private void loadProductsForSellers(List<String> sellerIds) {
        if (sellerIds.isEmpty()) {
            productList.clear();
            productAdapter.notifyDataSetChanged();
            return;
        }

        FirebaseDatabase.getInstance().getReference().child(Constants.NODE_PRODUCTS)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        productList.clear();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            Product p = child.getValue(Product.class);
                            if (p != null && sellerIds.contains(p.getSellerId())) {
                                productList.add(p);
                            }
                        }
                        productAdapter.notifyDataSetChanged();
                    }
                    @Override
                    public void onCancelled(DatabaseError error) {}
                });
    }

    private void chatWithSellerOfProduct(String sellerId) {
        FirebaseDatabase.getInstance().getReference().child(Constants.NODE_SELLERS).child(sellerId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        Seller s = snapshot.getValue(Seller.class);
                        if (s != null) {
                            WhatsAppHelper.openChat(SellerListActivity.this, s.getWhatsappNumber(), null);
                        }
                    }
                    @Override
                    public void onCancelled(DatabaseError error) {}
                });
    }

    private void requireBuyerLogin(Runnable action) {
        if (session.isLoggedIn()) {
            if (session.isSeller()) {
                Toast.makeText(this, "Sellers cannot place orders. Please login as Buyer.", Toast.LENGTH_SHORT).show();
                return;
            }
            action.run();
        } else {
            Toast.makeText(this, "Pehle login karein", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
        }
    }
}