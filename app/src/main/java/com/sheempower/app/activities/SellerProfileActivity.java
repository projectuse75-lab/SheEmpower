package com.sheempower.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.firebase.database.*;
import com.sheempower.app.R;
import com.sheempower.app.adapters.ProductAdapter;
import com.sheempower.app.models.Product;
import com.sheempower.app.models.Seller;
import com.sheempower.app.utils.*;
import de.hdodenhof.circleimageview.CircleImageView;
import java.util.ArrayList;
import java.util.List;

public class SellerProfileActivity extends AppCompatActivity {

    public static final String EXTRA_SELLER_ID = "sellerId";

    private CircleImageView ivSellerAvatar;
    private TextView tvName, tvCategory, tvCity, tvBio, tvRating, tvStatus, tvCapacity, tvDeliveryBadge;
    private Button btnPlaceOrder, btnWhatsAppChat;
    private RecyclerView rvProducts;
    private ProductAdapter productAdapter;
    private final List<Product> productList = new ArrayList<>();

    private String sellerId;
    private Seller currentSeller;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seller_profile);

        session  = new SessionManager(this);
        sellerId = getIntent().getStringExtra(EXTRA_SELLER_ID);

        if (sellerId == null || sellerId.isEmpty()) {
            Toast.makeText(this, "بیچنے والی کی شناخت نہیں ملی", Toast.LENGTH_LONG).show();
            finish(); return;
        }

        VoiceGuide.announceScreen(this,
                "یہ بیچنے والی کی پروفائل ہے۔ نیچے اس کی تمام اشیاء نظر آئیں گی۔ آرڈر دیں کا بٹن دبا کر آپ کئی چیزیں ایک ساتھ منتخب کر سکتی ہیں، یا کسی ایک چیز پر آرڈر کا بٹن دبا کر صرف وہی چیز منگوا سکتی ہیں۔ چیٹ کے بٹن سے واٹس ایپ پر براہ راست بات کریں۔");

        ivSellerAvatar  = findViewById(R.id.ivSellerAvatar);
        tvName          = findViewById(R.id.tvSellerName);
        tvCategory      = findViewById(R.id.tvSellerCategory);
        tvCity          = findViewById(R.id.tvSellerCity);
        tvBio           = findViewById(R.id.tvSellerBio);
        tvRating        = findViewById(R.id.tvSellerRating);
        tvStatus        = findViewById(R.id.tvSellerStatus);
        tvCapacity      = findViewById(R.id.tvSellerCapacity);
        tvDeliveryBadge = findViewById(R.id.tvSellerDeliveryBadge);
        btnPlaceOrder   = findViewById(R.id.btnPlaceOrder);
        btnWhatsAppChat = findViewById(R.id.btnWhatsAppChat);
        rvProducts      = findViewById(R.id.rvSellerProducts);

        rvProducts.setLayoutManager(new GridLayoutManager(this, 2));
        productAdapter = new ProductAdapter(productList,
                product -> {
                    Intent i = new Intent(this, ProductDetailActivity.class);
                    i.putExtra(ProductDetailActivity.EXTRA_PRODUCT_ID, product.getId());
                    startActivity(i);
                },
                product -> requireBuyerLogin(() -> goToSingleProductOrder(product)),
                product -> WhatsAppHelper.openChat(this,
                        currentSeller != null ? currentSeller.getWhatsappNumber() : null,
                        "السلام علیکم! مجھے " + product.getName() + " کے بارے میں پوچھنا تھا۔")
        );
        rvProducts.setAdapter(productAdapter);

        loadSellerData();

        // Yahan sirf sellerId jata hai (koi ek product nahi) — is se PlaceOrderActivity
        // saari products ki checklist dikhaye gi, price-with-checkbox tareeqe se
        btnPlaceOrder.setOnClickListener(v -> requireBuyerLogin(() -> {
            Intent i = new Intent(this, PlaceOrderActivity.class);
            i.putExtra(PlaceOrderActivity.EXTRA_SELLER_ID, sellerId);
            startActivity(i);
        }));

        btnWhatsAppChat.setOnClickListener(v -> WhatsAppHelper.openChat(this,
                currentSeller != null ? currentSeller.getWhatsappNumber() : null, null));
    }

    private void loadSellerData() {
        FirebaseDatabase.getInstance().getReference().child(Constants.NODE_SELLERS).child(sellerId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(DataSnapshot snapshot) {
                        currentSeller = snapshot.getValue(Seller.class);
                        if (currentSeller == null) {
                            Toast.makeText(SellerProfileActivity.this, "یہ بیچنے والی اب دستیاب نہیں ہے", Toast.LENGTH_LONG).show();
                            return;
                        }
                        // ✅ FIX: UID fallback
                        if (currentSeller.getUid() == null || currentSeller.getUid().isEmpty())
                            currentSeller.setUid(snapshot.getKey());

                        bindSellerData();
                        loadSellerProducts();
                    }
                    @Override public void onCancelled(DatabaseError error) {
                        Toast.makeText(SellerProfileActivity.this, "ڈیٹا لوڈ نہیں ہو سکا: " + error.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void bindSellerData() {
        tvName.setText(currentSeller.getBusinessName());
        tvCategory.setText(Constants.getUrduLabel(currentSeller.getCategory()) + " (" + currentSeller.getCategory() + ")");
        tvCity.setText(currentSeller.getCity());
        tvBio.setText(currentSeller.getBio());
        tvRating.setText(String.format("⭐ %.1f (%d)", currentSeller.getRating(), currentSeller.getRatingCount()));
        tvStatus.setText("✓ تصدیق شدہ");

        boolean travelType = Constants.isTravelType(currentSeller.getCategory());
        if (currentSeller.isCanDeliver()) {
            tvDeliveryBadge.setText(travelType ? "✅ یہ خود آپ کے پاس آ سکتی ہیں" : "✅ گھر ڈیلیوری دستیاب ہے");
            tvDeliveryBadge.setBackgroundResource(R.drawable.bg_rounded_green);
            tvDeliveryBadge.setTextColor(0xFF2E7D32);
        } else {
            tvDeliveryBadge.setText("⚠️ صرف خود آ کر لینا ہوگا\n📍 " + (currentSeller.getShopAddress() != null ? currentSeller.getShopAddress() : currentSeller.getCity()));
            tvDeliveryBadge.setBackgroundResource(R.drawable.bg_rounded_pink);
            tvDeliveryBadge.setTextColor(0xFFB25E00);
        }

        if (currentSeller.getCapacityDescription() != null && !currentSeller.getCapacityDescription().isEmpty()) {
            tvCapacity.setText("🙋‍♀️ " + currentSeller.getCapacityDescription());
            tvCapacity.setVisibility(android.view.View.VISIBLE);
        } else tvCapacity.setVisibility(android.view.View.GONE);

        if (currentSeller.getImageUrl() != null && !currentSeller.getImageUrl().isEmpty())
            Glide.with(this).load(currentSeller.getImageUrl()).into(ivSellerAvatar);
    }

    private void loadSellerProducts() {
        FirebaseDatabase.getInstance().getReference().child(Constants.NODE_PRODUCTS)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(DataSnapshot snapshot) {
                        productList.clear();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            Product p = child.getValue(Product.class);
                            if (p != null) {
                                // ✅ FIX: ID fallback & Trim
                                String pid = child.getKey() != null ? child.getKey().trim() : "";
                                p.setId(pid);

                                // Filter by sellerId (trimmed for safety)
                                String pSellerId = p.getSellerId() != null ? p.getSellerId().trim() : "";
                                if (pSellerId.equals(sellerId.trim())) {
                                    productList.add(p);
                                }
                            }
                        }
                        productAdapter.notifyDataSetChanged();
                    }
                    @Override public void onCancelled(DatabaseError error) {}
                });
    }

    private void goToSingleProductOrder(Product product) {
        Intent i = new Intent(this, PlaceOrderActivity.class);
        i.putExtra(PlaceOrderActivity.EXTRA_SELLER_ID, sellerId);
        i.putExtra(PlaceOrderActivity.EXTRA_PRODUCT_ID, product.getId());
        i.putExtra(PlaceOrderActivity.EXTRA_PRODUCT_NAME, product.getName());
        i.putExtra(PlaceOrderActivity.EXTRA_PRODUCT_PRICE, product.getPrice());
        startActivity(i);
    }

    private void requireBuyerLogin(Runnable action) {
        if (session.isLoggedIn()) action.run();
        else { Toast.makeText(this, "پہلے لاگ ان کریں", Toast.LENGTH_SHORT).show(); startActivity(new Intent(this, LoginActivity.class)); }
    }
}