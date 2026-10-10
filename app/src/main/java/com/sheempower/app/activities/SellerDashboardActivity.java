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
import com.sheempower.app.models.Review;
import com.sheempower.app.models.Seller;
import com.sheempower.app.utils.*;
import com.sheempower.app.utils.PreferencesManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SellerDashboardActivity extends AppCompatActivity {

    private TextView tvCategory, tvWelcome, tvStatus, tvTotalOrders, tvPendingOrders, tvCompletedOrders, tvEarnings;
    private Button btnAddProduct;
    private SessionManager session;

    private RecyclerView rvMyProducts;
    private TextView tvNoProducts, tvSellerDashboardRating;
    private Button btnViewSellerReviews;
    private ProductAdapter myProductAdapter;
    private final List<Product> myProductList = new ArrayList<>();
    private Seller currentSeller;

    private DatabaseReference notifRef;
    private ValueEventListener notifListener;
    private boolean isInitialNotifLoad = true;

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
        tvSellerDashboardRating = findViewById(R.id.tvSellerDashboardRating);
        View cardSellerReviews  = findViewById(R.id.cardSellerReviews);
        btnAddProduct     = findViewById(R.id.btnAddProduct);
        rvMyProducts      = findViewById(R.id.rvMyProducts);
        tvNoProducts      = findViewById(R.id.tvNoProducts);

        if (cardSellerReviews != null) {
            cardSellerReviews.setOnClickListener(v -> {
                Intent i = new Intent(this, ProductReviewsActivity.class);
                i.putExtra(ProductReviewsActivity.EXTRA_SELLER_ID, session.getUid());
                startActivity(i);
            });
        }

        ImageButton btnSpeaker = findViewById(R.id.btnSpeaker);
        VoiceGuide.setupSpeakerButton(btnSpeaker, this,
                "یہ آپ کی دکان کا ڈیش بورڈ ہے۔ یہاں آپ اپنی کل کمائی اور آرڈرز کی تعداد دیکھ سکتی ہیں۔ نیچے آپ کی شامل کردہ تمام اشیاء بھی نظر آئیں گی۔ نئی چیز شامل کرنے کے لیے بٹن دبائیں۔"
        );

        tvWelcome.setText("👋 " + session.getName());

        BottomNavHelper.setupBackButton(this);

        btnAddProduct.setOnClickListener(v -> startActivity(new Intent(this, AddProductActivity.class)));

        setupMyProductsList();

        // Android 13+ Notification Permission Request
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 501);
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            FcmTokenHelper.saveCurrentTokenWithContext(this, currentUser.getUid( ));
            BatteryOptimizationHelper.checkAndPrompt(this);
        }

        loadSellerStatus();
        loadStats();
        loadMyProducts();
        setupRealtimeOrderNotificationListener();

        BottomNavHelper.setupForSeller(this, findViewById(R.id.bottomNav), 1);
    }

    private void setupRealtimeOrderNotificationListener() {
        String uid = session.getUid();
        if (uid == null || uid.trim().isEmpty()) return;

        notifRef = FirebaseDatabase.getInstance().getReference()
                .child(Constants.NODE_NOTIFICATIONS)
                .child(uid.trim());

        notifListener = notifRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (isInitialNotifLoad) {
                    isInitialNotifLoad = false;
                    return;
                }

                int unread = 0;
                String latestMsg = "";

                for (DataSnapshot child : snapshot.getChildren()) {
                    Boolean read = child.child("read").getValue(Boolean.class);
                    if (read == null || !read) {
                        unread++;
                        String m = child.child("message").getValue(String.class);
                        if (m != null) latestMsg = m;
                    }
                }

                if (unread > 0) {
                    loadStats(); // Auto-refresh order dashboard stats!
                    TTSHelper.getInstance(SellerDashboardActivity.this).speak("آپ کو ایک نیا آرڈر موصول ہوا ہے");
                    if (!latestMsg.isEmpty()) {
                        Toast.makeText(SellerDashboardActivity.this, "🛒 " + latestMsg, Toast.LENGTH_LONG).show();
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {}
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (notifRef != null && notifListener != null) {
            notifRef.removeEventListener(notifListener);
        }
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

                        // Dynamically scan reviews for real-time accurate rating on Seller Dashboard
                        final DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();
                        final String sellerUid = uid.trim();

                        dbRef.child("reviews").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot snapshot) {
                                List<Review> revList = new ArrayList<>();
                                for (DataSnapshot child : snapshot.getChildren()) {
                                    Review r = RatingHelper.parseReview(child);
                                    if (r != null && r.getSellerId() != null && sellerUid.equalsIgnoreCase(r.getSellerId().trim())) {
                                        revList.add(r);
                                    }
                                }

                                dbRef.child(Constants.NODE_SELLERS).child(sellerUid).child("reviews")
                                        .addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(DataSnapshot subSnap) {
                                                for (DataSnapshot child : subSnap.getChildren()) {
                                                    Review r = RatingHelper.parseReview(child);
                                                    if (r != null) {
                                                        boolean exists = false;
                                                        for (Review ex : revList) {
                                                            if (ex.getReviewId() != null && ex.getReviewId().equalsIgnoreCase(child.getKey())) {
                                                                exists = true; break;
                                                            }
                                                        }
                                                        if (!exists) revList.add(r);
                                                    }
                                                }

                                                double sum = 0;
                                                long count = revList.size();
                                                for (Review r : revList) {
                                                    sum += r.getRating();
                                                }

                                                if (count > 0) {
                                                    double avg = sum / count;
                                                    double roundedAvg = Math.round(avg * 10.0) / 10.0;
                                                    boolean isTop = roundedAvg >= 4.5;
                                                    if (tvSellerDashboardRating != null) {
                                                        tvSellerDashboardRating.setText(String.format(Locale.ENGLISH, "⭐ %.1f (%d reviews)", roundedAvg, count));
                                                    }
                                                    DatabaseReference sellerRef = dbRef.child(Constants.NODE_SELLERS).child(sellerUid);
                                                    sellerRef.child("rating").setValue(roundedAvg);
                                                    sellerRef.child("ratingCount").setValue(count);
                                                    sellerRef.child("topRated").setValue(isTop);
                                                } else {
                                                    if (tvSellerDashboardRating != null) {
                                                        tvSellerDashboardRating.setText("⭐ 5.0 (0 reviews)");
                                                    }
                                                }
                                            }

                                            @Override public void onCancelled(DatabaseError error) {}
                                        });
                            }

                            @Override public void onCancelled(DatabaseError error) {}
                        });
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