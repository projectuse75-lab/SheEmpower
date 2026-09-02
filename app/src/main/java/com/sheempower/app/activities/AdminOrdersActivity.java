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
import com.sheempower.app.utils.BottomNavHelper;
import com.sheempower.app.utils.Constants;
import com.sheempower.app.utils.SessionManager;
import com.sheempower.app.utils.TTSHelper;
import com.sheempower.app.utils.VoiceGuide;
import java.util.ArrayList;
import java.util.List;

public class AdminOrdersActivity extends AppCompatActivity {

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
        setContentView(R.layout.activity_admin_orders);

        session = new SessionManager(this);
        tts = TTSHelper.getInstance(this);

        rvOrders = findViewById(R.id.rvOrders);
        tvEmpty = findViewById(R.id.tvEmpty);

        // Speaker Icon
        findViewById(R.id.btnSpeaker).setOnClickListener(v -> {
            VoiceGuide.announceScreen(this,
                    "ایڈمن آرڈرز کی سکرین۔ یہاں تمام آرڈرز دکھائی دیں گے۔");
        });

        VoiceGuide.announceScreen(this,
                "ایڈمن آرڈرز کی سکرین۔");

        rvOrders.setLayoutManager(new LinearLayoutManager(this));
        adapter = new OrderAdapter(orderList, false); // false = read-only for admin
        rvOrders.setAdapter(adapter);

        // ✅ Bottom Navigation (using our simplified helper)
        BottomNavHelper.setupBottomNav(this, session);
        BottomNavHelper.setSelectedTab(this, 2); // Highlight center tab

        loadAllOrders();

        tts.speak("ایڈمن آرڈرز لوڈ ہو رہے ہیں");
    }

    private void loadAllOrders() {
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
                if (orderList.isEmpty()) {
                    tvEmpty.setText("📭 کوئی آرڈر نہیں\n\nNo orders yet");
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(AdminOrdersActivity.this,
                        "Error loading orders: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };

        FirebaseDatabase.getInstance().getReference()
                .child(Constants.NODE_ORDERS)
                .addValueEventListener(ordersListener); // Listen to all orders
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ordersListener != null) {
            FirebaseDatabase.getInstance().getReference()
                    .child(Constants.NODE_ORDERS)
                    .removeEventListener(ordersListener);
        }
        // TTSHelper is singleton — don't shut it down here
    }
}