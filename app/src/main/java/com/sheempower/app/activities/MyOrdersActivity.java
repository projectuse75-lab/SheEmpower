package com.sheempower.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
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

public class MyOrdersActivity extends AppCompatActivity {

    private RecyclerView rvOrders;
    private TextView tvEmpty;
    private OrderAdapter adapter;
    private final List<Order> orderList = new ArrayList<>();
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_orders);

        session  = new SessionManager(this);
        rvOrders = findViewById(R.id.rvOrders);
        tvEmpty  = findViewById(R.id.tvEmpty);

        VoiceGuide.announceScreen(this,
                "یہ آپ کے تمام آرڈرز کی فہرست ہے۔ ہر آرڈر کے ساتھ اس کی موجودہ حالت لکھی ہے، جیسے کام شروع نہیں ہوا، کام جاری ہے، یا مکمل ہو گیا۔");

        rvOrders.setLayoutManager(new LinearLayoutManager(this));
        adapter = new OrderAdapter(orderList, false);
        rvOrders.setAdapter(adapter);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, "سیشن ختم ہو گیا، دوبارہ لاگ ان کریں", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        BottomNavHelper.setupForBuyerOrVisitor(this, findViewById(R.id.bottomNav), 2);

        loadOrders();
    }

    @Override protected void onResume() { super.onResume(); loadOrders(); }

    private void loadOrders() {
        FirebaseDatabase.getInstance().getReference().child(Constants.NODE_ORDERS)
                .orderByChild("buyerId").equalTo(session.getUid())
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