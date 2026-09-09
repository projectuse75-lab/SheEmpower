package com.sheempower.app.activities;

import android.os.Bundle;
import android.widget.TextView;
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
import com.sheempower.app.utils.VoiceGuide;
import java.util.ArrayList;
import java.util.List;

public class SellerOrdersActivity extends AppCompatActivity {

    private RecyclerView rvOrders;
    private TextView tvEmpty;
    private OrderAdapter adapter;
    private final List<Order> orderList = new ArrayList<>();
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seller_orders);

        session  = new SessionManager(this);
        rvOrders = findViewById(R.id.rvOrders);
        tvEmpty  = findViewById(R.id.tvEmpty);

        rvOrders.setLayoutManager(new LinearLayoutManager(this));
        adapter = new OrderAdapter(orderList, true);
        adapter.setOnStatusChangeListener((order, newStatus) -> {
            FirebaseDatabase.getInstance().getReference().child(Constants.NODE_ORDERS)
                    .child(order.getId()).child("status").setValue(newStatus)
                    .addOnSuccessListener(v -> loadOrders());
        });
        rvOrders.setAdapter(adapter);

        BottomNavHelper.setupForSeller(this, findViewById(R.id.bottomNav), 2);

        findViewById(R.id.btnSpeaker).setOnClickListener(v ->
                VoiceGuide.announceScreen(this, "یہ آپ کے آرڈرز کی فہرست ہے۔ یہاں آپ دیکھ سکتی ہیں کہ کون سا کام نیا ہے اور کون سا مکمل ہو چکا ہے۔"));

        loadOrders();
    }

    @Override protected void onResume() { super.onResume(); loadOrders(); }

    private void loadOrders() {
        FirebaseDatabase.getInstance().getReference().child(Constants.NODE_ORDERS)
                .orderByChild("sellerId").equalTo(session.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(DataSnapshot snapshot) {
                        orderList.clear();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            Order o = child.getValue(Order.class);
                            if (o != null) orderList.add(o);
                        }
                        adapter.notifyDataSetChanged();
                        tvEmpty.setVisibility(orderList.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
                    }
                    @Override public void onCancelled(DatabaseError error) {
                        tvEmpty.setVisibility(android.view.View.VISIBLE);
                        tvEmpty.setText("خرابی: " + error.getMessage());
                    }
                });
    }
}