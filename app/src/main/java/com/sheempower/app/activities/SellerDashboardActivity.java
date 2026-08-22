package com.sheempower.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.database.*;
import com.sheempower.app.R;
import com.sheempower.app.adapters.OrderAdapter;
import com.sheempower.app.models.Order;
import com.sheempower.app.models.Seller;
import com.sheempower.app.utils.Constants;
import com.sheempower.app.utils.DemoDataSeeder;
import com.sheempower.app.utils.SessionManager;
import com.sheempower.app.utils.TTSHelper;
import java.util.ArrayList;
import java.util.List;

public class SellerDashboardActivity extends AppCompatActivity {

    private TextView tvWelcome, tvStatus, tvOrderCount, tvEmpty;
    private RecyclerView rvOrders;
    private Button btnLogout, btnAddProduct;
    private OrderAdapter adapter;
    private final List<Order> orderList = new ArrayList<>();
    private SessionManager session;
    private TTSHelper tts;

    // Store listener references for cleanup
    private ValueEventListener ordersListener;
    private ValueEventListener sellerStatusListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seller_dashboard);

        session = new SessionManager(this);
        tts = TTSHelper.getInstance(this);

        // Find Views
        tvWelcome = findViewById(R.id.tvWelcome);
        tvStatus = findViewById(R.id.tvStatus);
        tvOrderCount = findViewById(R.id.tvOrderCount);
        tvEmpty = findViewById(R.id.tvEmpty);
        rvOrders = findViewById(R.id.rvOrders);
        btnLogout = findViewById(R.id.btnLogout);
        btnAddProduct = findViewById(R.id.btnAddProduct);

        tvWelcome.setText("👋 " + session.getName());

        // Speaker Icon for TTS Voice Guidance
        findViewById(R.id.btnSpeaker).setOnClickListener(v -> {
            String instruction = "Seller Dashboard. View and manage your incoming orders. " +
                    "Update order status to: Pending, Payment Verified, In Progress, or Completed.";
            tts.speak(instruction);
        });

        // Setup RecyclerView with OrderAdapter (isSellerView = true)
        rvOrders.setLayoutManager(new LinearLayoutManager(this));
        adapter = new OrderAdapter(orderList, true);
        adapter.setOnStatusChangeListener((order, newStatus) -> {
            // Update order status in Firebase
            FirebaseDatabase.getInstance().getReference()
                    .child(Constants.NODE_ORDERS)
                    .child(order.getId())
                    .child("status")
                    .setValue(newStatus)
                    .addOnSuccessListener(v -> {
                        Toast.makeText(this, "Status updated to: " + newStatus, Toast.LENGTH_SHORT).show();
                        tts.speak("Order status updated to " + newStatus);
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to update status", Toast.LENGTH_SHORT).show();
                    });
        });
        rvOrders.setAdapter(adapter);

        // Buttons
        btnLogout.setOnClickListener(v -> {
            tts.speak("Logging out. Goodbye.");
            session.logout();
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });

        // ✅ btnAddProduct ka click listener (AddProductActivity open karega)
        btnAddProduct.setOnClickListener(v -> {
            startActivity(new Intent(this, AddProductActivity.class));
        });

        loadSellerStatus();
        loadOrders();

        // Auto-speak screen instruction
        tts.speak("Seller Dashboard loaded.");
    }

    // ============================================================
    // ✅ STEP 2: YAHAN PAR btnAddProduct ENABLE/DISABLE HOTA HAI
    // ============================================================
    private void loadSellerStatus() {
        // Real-time listener for seller verification status
        sellerStatusListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Seller s = snapshot.getValue(Seller.class);
                if (s == null) return;

                // ✅ Check if seller is approved
                if ("approved".equals(s.getVerificationStatus())) {
                    tvStatus.setText("✅ Verified Seller");
                    tvStatus.setTextColor(0xFF4CAF50);

                    // ✅ ENABLE Add Product button
                    btnAddProduct.setEnabled(true);
                    btnAddProduct.setBackgroundTintList(
                            android.content.res.ColorStateList.valueOf(0xFFE91E8C));
                    btnAddProduct.setText("➕ Add New Product");

                } else if ("pending".equals(s.getVerificationStatus())) {
                    tvStatus.setText("⏳ Your account is under review by admin");
                    tvStatus.setTextColor(0xFFFFA000);

                    // ✅ DISABLE Add Product button
                    btnAddProduct.setEnabled(false);
                    btnAddProduct.setBackgroundTintList(
                            android.content.res.ColorStateList.valueOf(0xFF999999));
                    btnAddProduct.setText("⏳ Awaiting Approval");

                } else {
                    tvStatus.setText("❌ Account rejected. Please contact support.");
                    tvStatus.setTextColor(0xFFF44336);

                    // ✅ DISABLE Add Product button
                    btnAddProduct.setEnabled(false);
                    btnAddProduct.setBackgroundTintList(
                            android.content.res.ColorStateList.valueOf(0xFF999999));
                    btnAddProduct.setText("⛔ Access Denied");
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(SellerDashboardActivity.this, "Error loading status", Toast.LENGTH_SHORT).show();
            }
        };

        FirebaseDatabase.getInstance().getReference()
                .child(Constants.NODE_SELLERS)
                .child(session.getUid())
                .addValueEventListener(sellerStatusListener);
    }

    // ============================================================
    // LOAD ORDERS (Real-time)
    // ============================================================
    private void loadOrders() {
        ordersListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                orderList.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Order o = child.getValue(Order.class);
                    if (o != null) {
                        orderList.add(o);
                    }
                }
                // Sort by timestamp (newest first)
                orderList.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
                adapter.notifyDataSetChanged();

                tvOrderCount.setText(orderList.size() + " order(s)");

                if (orderList.isEmpty()) {
                    tvEmpty.setVisibility(View.VISIBLE);
                    tvEmpty.setText("📭 No orders yet\n\nBuyers will place orders here.");
                    addDemoOrders();
                } else {
                    tvEmpty.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(SellerDashboardActivity.this, "Error loading orders", Toast.LENGTH_SHORT).show();
            }
        };

        FirebaseDatabase.getInstance().getReference()
                .child(Constants.NODE_ORDERS)
                .orderByChild("sellerId")
                .equalTo(session.getUid())
                .addValueEventListener(ordersListener);
    }

    // Demo Orders for Testing
    private void addDemoOrders() {
        if (orderList.isEmpty()) {
            DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();

            dbRef.child(Constants.NODE_ORDERS)
                    .orderByChild("sellerId")
                    .equalTo(session.getUid())
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot snapshot) {
                            if (snapshot.exists()) return;

                            Order demo1 = new Order();
                            demo1.setId("demo_order_" + System.currentTimeMillis());
                            demo1.setBuyerId("demo_buyer");
                            demo1.setSellerId(session.getUid());
                            demo1.setProductName("Chicken Biryani");
                            demo1.setDetails("2kg biryani for family gathering");
                            demo1.setAddress("Lahore, Pakistan");
                            demo1.setAmount(1200);
                            demo1.setStatus(Constants.STATUS_PENDING);
                            demo1.setTimestamp(System.currentTimeMillis() - 3600000);
                            dbRef.child(Constants.NODE_ORDERS).child(demo1.getId()).setValue(demo1);

                            Order demo2 = new Order();
                            demo2.setId("demo_order_" + System.currentTimeMillis());
                            demo2.setBuyerId("demo_buyer");
                            demo2.setSellerId(session.getUid());
                            demo2.setProductName("Custom Jewelry");
                            demo2.setDetails("Pearl necklace for wedding");
                            demo2.setAddress("Islamabad, Pakistan");
                            demo2.setAmount(8500);
                            demo2.setStatus(Constants.STATUS_VERIFIED);
                            demo2.setTimestamp(System.currentTimeMillis() - 7200000);
                            dbRef.child(Constants.NODE_ORDERS).child(demo2.getId()).setValue(demo2);

                            DemoDataSeeder.clearDemoDataOnFirstRealUse(SellerDashboardActivity.this);
                        }

                        @Override
                        public void onCancelled(DatabaseError error) {}
                    });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Remove listeners to prevent memory leaks
        if (ordersListener != null) {
            FirebaseDatabase.getInstance().getReference()
                    .child(Constants.NODE_ORDERS)
                    .removeEventListener(ordersListener);
        }
        if (sellerStatusListener != null) {
            FirebaseDatabase.getInstance().getReference()
                    .child(Constants.NODE_SELLERS)
                    .child(session.getUid())
                    .removeEventListener(sellerStatusListener);
        }

        if (tts != null) tts.shutdown();
    }
}