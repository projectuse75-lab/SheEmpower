package com.sheempower.app.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.database.*;
import com.sheempower.app.R;
import com.sheempower.app.adapters.OrderAdapter;
import com.sheempower.app.models.Order;
import com.sheempower.app.utils.Constants;
import com.sheempower.app.utils.DemoDataSeeder;
import com.sheempower.app.utils.SessionManager;
import com.sheempower.app.utils.TTSHelper;
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

        rvOrders = findViewById(R.id.rvOrders);
        tvEmpty = findViewById(R.id.tvEmpty);

        // ✅ Speaker Icon for TTS
        findViewById(R.id.btnSpeaker).setOnClickListener(v -> {
            String instruction = "My Orders screen. Shows all your orders with real-time status updates.";
            tts.speak(instruction);
        });

        rvOrders.setLayoutManager(new LinearLayoutManager(this));
        adapter = new OrderAdapter(orderList, false);
        rvOrders.setAdapter(adapter);

        loadOrders();

        // ✅ Auto-speak
        tts.speak("My Orders screen loaded.");

        // ✅ If no orders, show demo orders (for testing)
        if (orderList.isEmpty()) {
            addDemoOrders();
        }
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
                // Sort by timestamp (newest first)
                orderList.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
                adapter.notifyDataSetChanged();
                tvEmpty.setVisibility(orderList.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onCancelled(DatabaseError error) {}
        };

        FirebaseDatabase.getInstance().getReference()
                .child(Constants.NODE_ORDERS)
                .orderByChild("buyerId")
                .equalTo(session.getUid())
                .addValueEventListener(ordersListener);
    }

    // ✅ Demo Orders for Testing (Only if user is logged in)
    private void addDemoOrders() {
        if (!session.isLoggedIn()) return;

        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();
        String buyerId = session.getUid();

        // Check if demo orders already exist
        dbRef.child(Constants.NODE_ORDERS)
                .orderByChild("buyerId")
                .equalTo(buyerId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        if (snapshot.exists()) return; // Already has orders

                        // Create 2 demo orders
                        Order order1 = new Order();
                        order1.setId("demo_order_1");
                        order1.setBuyerId(buyerId);
                        order1.setSellerId("demo_seller_1");
                        order1.setProductName("Chicken Biryani");
                        order1.setDetails("2kg biryani for family gathering");
                        order1.setAddress("Lahore, Pakistan");
                        order1.setAmount(1200);
                        order1.setStatus(Constants.STATUS_PENDING);
                        order1.setTimestamp(System.currentTimeMillis() - 86400000); // Yesterday
                        dbRef.child(Constants.NODE_ORDERS).child("demo_order_1").setValue(order1);

                        Order order2 = new Order();
                        order2.setId("demo_order_2");
                        order2.setBuyerId(buyerId);
                        order2.setSellerId("demo_seller_2");
                        order2.setProductName("Pearl Necklace");
                        order2.setDetails("Custom pearl necklace for wedding");
                        order2.setAddress("Islamabad, Pakistan");
                        order2.setAmount(8500);
                        order2.setStatus(Constants.STATUS_VERIFIED);
                        order2.setTimestamp(System.currentTimeMillis() - 172800000); // 2 days ago
                        dbRef.child(Constants.NODE_ORDERS).child("demo_order_2").setValue(order2);

                        // Clear demo data flag so real orders won't get mixed
                        DemoDataSeeder.clearDemoDataOnFirstRealUse(MyOrdersActivity.this);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {}
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ordersListener != null) {
            FirebaseDatabase.getInstance().getReference()
                    .child(Constants.NODE_ORDERS)
                    .removeEventListener(ordersListener);
        }
        if (tts != null) tts.shutdown();
    }
}