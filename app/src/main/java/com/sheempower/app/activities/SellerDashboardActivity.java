package com.sheempower.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;
import com.sheempower.app.R;
import com.sheempower.app.adapters.ProductAdapter;
import com.sheempower.app.models.Order;
import com.sheempower.app.models.Product;
import com.sheempower.app.models.Seller;
import com.sheempower.app.utils.*;
import com.sheempower.app.utils.PreferencesManager;
import java.util.ArrayList;
import java.util.List;

public class SellerDashboardActivity extends AppCompatActivity {

    private TextView tvCategory, tvWelcome, tvStatus, tvTotalOrders, tvPendingOrders, tvCompletedOrders, tvEarnings;
    private Button btnAddProduct;
    private SessionManager session;

    // ✅ NAYA — "My Products" section
    private RecyclerView rvMyProducts;
    private TextView tvNoProducts;
    private ProductAdapter myProductAdapter;
    private final List<Product> myProductList = new ArrayList<>();
    private Seller currentSeller;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyTheme();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seller_dashboard);

        session = new SessionManager(this);

        // ============ Initialize Views ============
        tvCategory        = findViewById(R.id.tvCategory);
        tvWelcome         = findViewById(R.id.tvWelcome);
        tvStatus          = findViewById(R.id.tvStatus);
        tvTotalOrders     = findViewById(R.id.tvTotalOrders);
        tvPendingOrders   = findViewById(R.id.tvPendingOrders);
        tvCompletedOrders = findViewById(R.id.tvCompletedOrders);
        tvEarnings        = findViewById(R.id.tvEarnings);
        btnAddProduct     = findViewById(R.id.btnAddProduct);
        rvMyProducts      = findViewById(R.id.rvMyProducts);
        tvNoProducts      = findViewById(R.id.tvNoProducts);

        ImageButton btnSpeaker = findViewById(R.id.btnSpeaker);
        VoiceGuide.setupSpeakerButton(btnSpeaker, this,
                "یہ آپ کی دکان کا ڈیش بورڈ ہے۔ یہاں آپ اپنی کل کمائی اور آرڈرز کی تعداد دیکھ سکتی ہیں۔ نیچے آپ کی شامل کردہ تمام اشیاء بھی نظر آئیں گی۔ نئی چیز شامل کرنے کے لیے بٹن دبائیں۔"
        );

        tvWelcome.setText("👋 " + session.getName());

        BottomNavHelper.setupBackButton(this);

        btnAddProduct.setOnClickListener(v -> startActivity(new Intent(this, AddProductActivity.class)));

        setupMyProductsList();

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            OneSignalHelper.updateSellerPlayerId(currentUser.getUid());
            BatteryOptimizationHelper.checkAndPrompt(this);
        }

        loadSellerStatus();
        loadStats();
        loadMyProducts();

        BottomNavHelper.setupForSeller(this, findViewById(R.id.bottomNav), 1);
    }


    @Override
    protected void onResume() {
        super.onResume();
        VoiceGuide.announceScreen(this,
                "یہ آپ کی دکان کا ڈیش بورڈ ہے۔ یہاں آپ اپنی کل کمائی اور آرڈرز کی تعداد دیکھ سکتی ہیں۔ نیچے آپ کی شامل کردہ تمام اشیاء بھی نظر آئیں گی۔ نئی چیز شامل کرنے کے لیے بٹن دبائیں۔"
        );
        loadSellerStatus();
        loadStats();
        loadMyProducts();
    }

    // ============ SETUP MY PRODUCTS RECYCLERVIEW ============
    private void setupMyProductsList() {
        rvMyProducts.setLayoutManager(new GridLayoutManager(this, 2));
        myProductAdapter = new ProductAdapter(
                myProductList,
                this::showProductOptionsDialog
        );
        rvMyProducts.setAdapter(myProductAdapter);
    }

    private void showProductOptionsDialog(Product product) {
        if (product == null) return;
        String[] options = {"✏️ ترمیم کریں / Edit Product", "🗑️ ختم کریں / Delete Product", "👁️ تفصیلات دیکھیں / View Details"};
        new AlertDialog.Builder(this)
                .setTitle(product.getName())
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        Intent intent = new Intent(this, AddProductActivity.class);
                        intent.putExtra(AddProductActivity.EXTRA_PRODUCT_ID, product.getId());
                        startActivity(intent);
                    } else if (which == 1) {
                        confirmDeleteProduct(product);
                    } else if (which == 2) {
                        Intent i = new Intent(this, ProductDetailActivity.class);
                        i.putExtra(ProductDetailActivity.EXTRA_PRODUCT_ID, product.getId());
                        i.putExtra(ProductDetailActivity.EXTRA_IS_SELLER_VIEW, true);
                        startActivity(i);
                    }
                })
                .show();
    }

    private void confirmDeleteProduct(Product product) {
        new AlertDialog.Builder(this)
                .setTitle("حذف کی تصدیق")
                .setMessage("کیا آپ واقعی \"" + product.getName() + "\" کو حذف کرنا چاہتی ہیں؟")
                .setPositiveButton("ہاں، ختم کریں", (dialog, which) -> {
                    FirebaseDatabase.getInstance().getReference().child(Constants.NODE_PRODUCTS)
                            .child(product.getId()).removeValue()
                            .addOnSuccessListener(v -> {
                                Toast.makeText(this, "پروڈکٹ حذف ہو گئی! ✅", Toast.LENGTH_SHORT).show();
                                loadMyProducts();
                            })
                            .addOnFailureListener(e -> Toast.makeText(this, "خرابی: " + e.getMessage(), Toast.LENGTH_LONG).show());
                })
                .setNegativeButton("منسوخ کریں", null)
                .show();
    }

    private boolean isProductOwnedBySeller(Product p, String targetUid) {
        if (p == null || p.getSellerId() == null) return false;
        String pSellerId = p.getSellerId().trim();
        if (pSellerId.isEmpty()) return false;

        if (targetUid.equalsIgnoreCase(pSellerId)) return true;

        FirebaseUser authUser = FirebaseAuth.getInstance().getCurrentUser();
        if (authUser != null) {
            if (authUser.getUid() != null && authUser.getUid().trim().equalsIgnoreCase(pSellerId)) return true;
            if (authUser.getEmail() != null && authUser.getEmail().trim().equalsIgnoreCase(pSellerId)) return true;
        }

        if (currentSeller != null) {
            if (currentSeller.getUid() != null && currentSeller.getUid().trim().equalsIgnoreCase(pSellerId)) return true;
            if (currentSeller.getEmail() != null && currentSeller.getEmail().trim().equalsIgnoreCase(pSellerId)) return true;
            if (currentSeller.getPhone() != null && currentSeller.getPhone().trim().equalsIgnoreCase(pSellerId)) return true;
            if (currentSeller.getWhatsappNumber() != null && currentSeller.getWhatsappNumber().trim().equalsIgnoreCase(pSellerId)) return true;
        }

        return false;
    }

    private boolean containsProduct(List<Product> list, String id) {
        if (id == null || list == null) return false;
        for (Product item : list) {
            if (item != null && item.getId() != null && id.trim().equalsIgnoreCase(item.getId().trim())) {
                return true;
            }
        }
        return false;
    }

    // ============ LOAD SELLER'S OWN PRODUCTS ============
    private void loadMyProducts() {
        String uid = session.getUid();
        if (uid == null || uid.trim().isEmpty()) return;

        final String targetUid = uid.trim();

        FirebaseDatabase.getInstance().getReference().child(Constants.NODE_SELLERS).child(targetUid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot sellerSnap) {
                        currentSeller = sellerSnap.getValue(Seller.class);
                        if (currentSeller != null) {
                            if (currentSeller.getUid() == null || currentSeller.getUid().isEmpty()) {
                                currentSeller.setUid(targetUid);
                            }
                            currentSeller.setCategories(Constants.parseCategoriesFromSnapshot(sellerSnap));
                        }

                        FirebaseDatabase.getInstance().getReference().child(Constants.NODE_PRODUCTS)
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot snapshot) {
                                        myProductList.clear();
                                        for (DataSnapshot child : snapshot.getChildren()) {
                                            Product p = child.getValue(Product.class);
                                            if (p != null) {
                                                String pid = child.getKey();
                                                if (pid != null) p.setId(pid.trim());

                                                if (isProductOwnedBySeller(p, targetUid)) {
                                                    if (!containsProduct(myProductList, p.getId())) {
                                                        myProductList.add(p);
                                                    }
                                                }
                                            }
                                        }
                                        if (myProductAdapter != null) myProductAdapter.notifyDataSetChanged();
                                        tvNoProducts.setVisibility(myProductList.isEmpty() ? View.VISIBLE : View.GONE);
                                        rvMyProducts.setVisibility(myProductList.isEmpty() ? View.GONE : View.VISIBLE);
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError error) {
                                        Toast.makeText(SellerDashboardActivity.this, "خرابی: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {}
                });
    }

    // ============ LOAD SELLER STATUS + CATEGORY ============
    private void loadSellerStatus() {
        String uid = session.getUid();
        if (uid == null || uid.trim().isEmpty()) return;

        FirebaseDatabase.getInstance().getReference()
                .child(Constants.NODE_SELLERS)
                .child(uid.trim())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        Seller s = snapshot.getValue(Seller.class);
                        if (s == null) return;

                        s.setCategories(Constants.parseCategoriesFromSnapshot(snapshot));

                        List<String> sellerCategories = Constants.getActiveSellerCategories(s);
                        if (!sellerCategories.isEmpty()) {
                            StringBuilder sb = new StringBuilder();
                            sb.append("🛍️ آپ کے منتخب کردہ زمرے (Categories):\n");
                            for (int i = 0; i < sellerCategories.size(); i++) {
                                if (i > 0) sb.append("\n");
                                sb.append(i + 1).append(". ").append(getCategoryDisplayName(sellerCategories.get(i)));
                            }
                            tvCategory.setText(sb.toString());
                        } else if (s.getCategory() != null && !s.getCategory().isEmpty()) {
                            tvCategory.setText("🛍️ آپ کا زمرہ (Category):\n1. " + getCategoryDisplayName(s.getCategory()));
                        }

                        if (Constants.isApproved(s.getVerificationStatus())) {
                            tvStatus.setText("✅ تصدیق شدہ — آپ کی دکان خریداروں کو نظر آ رہی ہے / Verified");
                            tvStatus.setTextColor(0xFF4CAF50);
                            btnAddProduct.setEnabled(true);
                        } else {
                            tvStatus.setText("⏳ ایڈمن کی منظوری کا انتظار / Pending Approval");
                            tvStatus.setTextColor(0xFFFFA000);
                            btnAddProduct.setEnabled(false);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {}
                });
    }

    private String getCategoryDisplayName(String categoryCode) {
        int index = -1;
        for (int i = 0; i < Constants.CATEGORIES.length; i++) {
            if (Constants.CATEGORIES[i].equals(categoryCode)) {
                index = i;
                break;
            }
        }

        if (index >= 0 && index < Constants.CATEGORIES.length) {
            String urduName = Constants.CATEGORY_LABELS_UR[index];
            String englishName = Constants.CATEGORIES[index];
            String emoji = getCategoryEmoji(englishName);
            return emoji + " " + urduName + " / " + englishName;
        }

        return "📦 " + categoryCode;
    }

    private String getCategoryEmoji(String category) {
        switch (category.toLowerCase()) {
            case "stitching": return "🧵";
            case "cooking": return "👨‍🍳";
            case "embroidery": return "🎨";
            case "tailoring": return "✂️";
            case "jewelry": return "💎";
            case "beauty": return "💄";
            case "crafts": return "🎭";
            case "handmade": return "🖐️";
            case "henna": return "🎨";
            case "home_decor": return "🏠";
            case "cosmetics": return "💅";
            case "clothing": return "👗";
            case "accessories": return "👜";
            case "baking": return "🍰";
            case "handicrafts": return "🛠️";
            default: return "📦";
        }
    }

    // ============ LOAD ORDER STATISTICS ============
    private void loadStats() {
        String uid = session.getUid();
        if (uid == null || uid.trim().isEmpty()) return;

        FirebaseDatabase.getInstance().getReference()
                .child(Constants.NODE_ORDERS)
                .orderByChild("sellerId")
                .equalTo(uid.trim())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        int total = 0, pending = 0, completed = 0;
                        double earnings = 0;

                        for (DataSnapshot child : snapshot.getChildren()) {
                            Order o = child.getValue(Order.class);
                            if (o == null) continue;

                            total++;

                            if (Constants.STATUS_PENDING.equals(o.getStatus())) {
                                pending++;
                            }

                            if (Constants.STATUS_COMPLETED.equals(o.getStatus())) {
                                completed++;
                                earnings += o.getAmount();
                            }
                        }

                        tvTotalOrders.setText(String.valueOf(total));
                        tvPendingOrders.setText(String.valueOf(pending));
                        tvCompletedOrders.setText(String.valueOf(completed));
                        tvEarnings.setText("PKR " + (long) earnings);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {}
                });
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