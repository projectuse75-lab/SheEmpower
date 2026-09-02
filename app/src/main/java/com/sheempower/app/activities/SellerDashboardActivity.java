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
import com.sheempower.app.utils.*;
import java.util.ArrayList;
import java.util.List;

public class SellerDashboardActivity extends AppCompatActivity {

    private TextView tvWelcome, tvStatus, tvEmpty;
    private RecyclerView rvOrders;
    private Button btnLogout, btnAddProduct;
    private OrderAdapter adapter;
    private final List<Order> orderList = new ArrayList<>();
    private SessionManager session;
    private TTSHelper tts;
    private ValueEventListener ordersListener;
    private ValueEventListener sellerStatusListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seller_dashboard);

        session = new SessionManager(this);
        tts = TTSHelper.getInstance(this);

        tvWelcome = findViewById(R.id.tvWelcome);
        tvStatus = findViewById(R.id.tvStatus);
        tvEmpty = findViewById(R.id.tvEmpty);
        rvOrders = findViewById(R.id.rvOrders);
        btnLogout = findViewById(R.id.btnLogout);
        btnAddProduct = findViewById(R.id.btnAddProduct);

        tvWelcome.setText("👋 " + session.getName());

        findViewById(R.id.btnSpeaker).setOnClickListener(v -> {
            tts.speak("Seller Dashboard. View and manage your orders.");
        });

        rvOrders.setLayoutManager(new LinearLayoutManager(this));
        adapter = new OrderAdapter(orderList, true);
        adapter.setOnStatusChangeListener((order, newStatus) -> {
            FirebaseDatabase.getInstance().getReference()
                    .child(Constants.NODE_ORDERS)
                    .child(order.getId())
                    .child("status")
                    .setValue(newStatus)
                    .addOnSuccessListener(v -> {
                        Toast.makeText(this, "Status updated to: " + newStatus, Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to update status", Toast.LENGTH_SHORT).show();
                    });
        });
        rvOrders.setAdapter(adapter);

        btnLogout.setOnClickListener(v -> {
            session.logout();
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });

        btnAddProduct.setOnClickListener(v -> {
            startActivity(new Intent(this, AddProductActivity.class));
        });

        loadSellerStatus();
        loadOrders();

        tts.speak("Seller Dashboard loaded.");

        // ✅ BOTTOM NAVIGATION (SELLER)
        BottomNavHelper.setupBottomNav(this, session);
        BottomNavHelper.setSelectedTab(this, 2);
    }

    private void loadSellerStatus() {
        sellerStatusListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Seller s = snapshot.getValue(Seller.class);
                if (s == null) return;

                if ("approved".equals(s.getVerificationStatus())) {
                    tvStatus.setText("✅ Verified Seller");
                    tvStatus.setTextColor(0xFF4CAF50);
                    btnAddProduct.setEnabled(true);
                } else if ("pending".equals(s.getVerificationStatus())) {
                    tvStatus.setText("⏳ Pending approval");
                    tvStatus.setTextColor(0xFFFFA000);
                    btnAddProduct.setEnabled(false);
                } else {
                    tvStatus.setText("❌ Rejected");
                    tvStatus.setTextColor(0xFFF44336);
                    btnAddProduct.setEnabled(false);
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
                orderList.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
                adapter.notifyDataSetChanged();
                tvEmpty.setVisibility(orderList.isEmpty() ? View.VISIBLE : View.GONE);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
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