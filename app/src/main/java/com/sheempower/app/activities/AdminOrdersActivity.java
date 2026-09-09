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
import java.util.ArrayList;
import java.util.List;

public class AdminOrdersActivity extends AppCompatActivity {

    private RecyclerView rvOrders;
    private TextView tvEmpty;
    private OrderAdapter adapter;
    private final List<Order> orderList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_orders);

        rvOrders = findViewById(R.id.rvOrders);
        tvEmpty  = findViewById(R.id.tvEmpty);

        rvOrders.setLayoutManager(new LinearLayoutManager(this));
        adapter = new OrderAdapter(orderList, false);
        rvOrders.setAdapter(adapter);

        BottomNavHelper.setupForAdmin(this, findViewById(R.id.bottomNav), 2);

        loadAllOrders();
    }

    @Override protected void onResume() { super.onResume(); loadAllOrders(); }

    private void loadAllOrders() {
        FirebaseDatabase.getInstance().getReference().child(Constants.NODE_ORDERS)
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
                    @Override public void onCancelled(DatabaseError error) {}
                });
    }
}