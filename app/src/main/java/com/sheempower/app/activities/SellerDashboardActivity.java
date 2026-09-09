package com.sheempower.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.*;
import com.sheempower.app.R;
import com.sheempower.app.models.Order;
import com.sheempower.app.models.Seller;
import com.sheempower.app.utils.*;

public class SellerDashboardActivity extends AppCompatActivity {

    private TextView tvWelcome, tvStatus, tvTotalOrders, tvPendingOrders, tvCompletedOrders, tvEarnings;
    private Button btnAddProduct;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seller_dashboard);

        session = new SessionManager(this);

        tvWelcome         = findViewById(R.id.tvWelcome);
        tvStatus          = findViewById(R.id.tvStatus);
        tvTotalOrders     = findViewById(R.id.tvTotalOrders);
        tvPendingOrders   = findViewById(R.id.tvPendingOrders);
        tvCompletedOrders = findViewById(R.id.tvCompletedOrders);
        tvEarnings        = findViewById(R.id.tvEarnings);
        btnAddProduct     = findViewById(R.id.btnAddProduct);

        tvWelcome.setText("👋 " + session.getName());

        btnAddProduct.setOnClickListener(v -> startActivity(new Intent(this, AddProductActivity.class)));

        loadSellerStatus();
        loadStats();

        BottomNavHelper.setupForSeller(this, findViewById(R.id.bottomNav), 1);

        findViewById(R.id.btnSpeaker).setOnClickListener(v ->
                VoiceGuide.announceScreen(this, "خوش آمدید! یہ آپ کا ڈیش بورڈ ہے۔ یہاں آپ اپنے آرڈرز، کمائی اور نئی اشیاء شامل کر سکتی ہیں۔"));
    }

    @Override protected void onResume() { super.onResume(); loadSellerStatus(); loadStats(); }

    private void loadSellerStatus() {
        FirebaseDatabase.getInstance().getReference().child(Constants.NODE_SELLERS).child(session.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(DataSnapshot snapshot) {
                        Seller s = snapshot.getValue(Seller.class);
                        if (s == null) return;
                        if ("verified".equals(s.getVerificationStatus())) {
                            tvStatus.setText("✅ تصدیق شدہ — آپ کی دکان خریداروں کو نظر آ رہی ہے");
                            tvStatus.setTextColor(0xFF4CAF50);
                            btnAddProduct.setEnabled(true);
                        } else {
                            tvStatus.setText("⏳ ایڈمن کی منظوری کا انتظار");
                            tvStatus.setTextColor(0xFFFFA000);
                            btnAddProduct.setEnabled(false);
                        }
                    }
                    @Override public void onCancelled(DatabaseError error) {}
                });
    }

    private void loadStats() {
        FirebaseDatabase.getInstance().getReference().child(Constants.NODE_ORDERS)
                .orderByChild("sellerId").equalTo(session.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(DataSnapshot snapshot) {
                        int total = 0, pending = 0, completed = 0;
                        double earnings = 0;
                        for (DataSnapshot child : snapshot.getChildren()) {
                            Order o = child.getValue(Order.class);
                            if (o == null) continue;
                            total++;
                            if (Constants.STATUS_PENDING.equals(o.getStatus())) pending++;
                            if (Constants.STATUS_COMPLETED.equals(o.getStatus())) { completed++; earnings += o.getAmount(); }
                        }
                        tvTotalOrders.setText(String.valueOf(total));
                        tvPendingOrders.setText(String.valueOf(pending));
                        tvCompletedOrders.setText(String.valueOf(completed));
                        tvEarnings.setText("PKR " + (long) earnings);
                    }
                    @Override public void onCancelled(DatabaseError error) {}
                });
    }
}