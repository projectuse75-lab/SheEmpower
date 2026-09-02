package com.sheempower.app.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.database.*;
import com.sheempower.app.R;
import com.sheempower.app.adapters.OrderAdapter;
import com.sheempower.app.models.Order;
import com.sheempower.app.utils.*;
import java.util.ArrayList;
import java.util.List;

public class MyOrdersActivity extends AppCompatActivity {

    private RecyclerView rvOrders;
    private TextView tvEmpty;
    private OrderAdapter adapter;
    private final List<Order> orderList = new ArrayList<>();
    private SessionManager session;
    private TTSHelper tts;
    private ValueEventListener ordersListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_order);

        session = new SessionManager(this);
        tts = TTSHelper.getInstance(this);

        // Find Views
        rvOrders = findViewById(R.id.rvOrders);
        tvEmpty = findViewById(R.id.tvEmpty);

        // ✅ Speaker Icon for TTS
        findViewById(R.id.btnSpeaker).setOnClickListener(v -> {
            VoiceGuide.announceScreen(this,
                    "میرے آرڈرز کی سکرین۔ یہاں آپ کے تمام آرڈرز دکھائی دیں گے۔");
        });

        // ✅ Auto Voice on Load
        VoiceGuide.announceScreen(this,
                "میرے آرڈرز کی سکرین۔ یہاں آپ کے تمام آرڈرز دکھائی دیں گے۔");

        // Setup RecyclerView
        rvOrders.setLayoutManager(new LinearLayoutManager(this));
        adapter = new OrderAdapter(orderList, false); // false = buyer mode
        rvOrders.setAdapter(adapter);

        // Load Orders
        loadOrders();

        // ✅ Bottom Navigation
        BottomNavHelper.setupBottomNav(this, session);
        BottomNavHelper.setSelectedTab(this, 2); // Highlight Center Tab
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

                // Show empty state
                tvEmpty.setVisibility(orderList.isEmpty() ? View.VISIBLE : View.GONE);
                if (orderList.isEmpty()) {
                    tvEmpty.setText("📭 ابھی تک کوئی آرڈر نہیں\n\nNo orders yet");
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(MyOrdersActivity.this,
                        "Error loading orders: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };

        FirebaseDatabase.getInstance().getReference()
                .child(Constants.NODE_ORDERS)
                .orderByChild("buyerId")
                .equalTo(session.getUid())
                .addValueEventListener(ordersListener);
    }

    // ============================================================
    // ON DESTROY
    // ============================================================
    @Override
    protected void onDestroy() {
        super.onDestroy();

        // ✅ Remove listener to prevent memory leaks
        if (ordersListener != null) {
            FirebaseDatabase.getInstance().getReference()
                    .child(Constants.NODE_ORDERS)
                    .removeEventListener(ordersListener);
        }

        // TTSHelper is a singleton — don't shut it down here
    }
}