package com.sheempower.app.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
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
import com.sheempower.app.utils.PreferencesManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SellerOrdersActivity extends AppCompatActivity {

    private RecyclerView rvOrders;
    private TextView tvEmpty;
    private OrderAdapter adapter;
    private final List<Order> fullOrderList = new ArrayList<>();
    private final List<Order> displayedOrderList = new ArrayList<>();
    private SessionManager session;

    private Button btnFilterAll, btnFilterPending, btnFilterInProgress, btnFilterCompleted;
    private String currentSelectedFilter = "ALL";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seller_orders);

        session  = new SessionManager(this);
        rvOrders = findViewById(R.id.rvOrders);
        tvEmpty  = findViewById(R.id.tvEmpty);

        btnFilterAll         = findViewById(R.id.btnFilterAll);
        btnFilterPending     = findViewById(R.id.btnFilterPending);
        btnFilterInProgress  = findViewById(R.id.btnFilterInProgress);
        btnFilterCompleted   = findViewById(R.id.btnFilterCompleted);

        setupFilterButtons();

        ImageButton btnSpeaker = findViewById(R.id.btnSpeaker);
        VoiceGuide.setupSpeakerButton(btnSpeaker, this,
                "یہاں آپ کو ملنے والے آرڈرز کی فہرست ہے۔ ہر آرڈر پر نیچے بٹن دبا کر اس کی حالت آگے بڑھائیں — پہلے کام شروع کریں دبائیں، پھر جب کام مکمل ہو جائے تو مکمل کا بٹن دبائیں۔");

        rvOrders.setLayoutManager(new LinearLayoutManager(this));
        adapter = new OrderAdapter(displayedOrderList, true);
        adapter.setOnStatusChangeListener((order, newStatus) -> {
            FirebaseDatabase.getInstance().getReference().child(Constants.NODE_ORDERS)
                    .child(order.getId()).child("status").setValue(newStatus)
                    .addOnSuccessListener(v -> loadOrders());
        });
        rvOrders.setAdapter(adapter);

        BottomNavHelper.setupForSeller(this, findViewById(R.id.bottomNav), 2);
        BottomNavHelper.setupBackButton(this);

        loadOrders();
    }

    @Override
    protected void onResume() {
        super.onResume();
        ImageButton btnSpeaker = findViewById(R.id.btnSpeaker);
        if (btnSpeaker != null) {
            VoiceGuide.updateSpeakerIcon(btnSpeaker, VoiceGuide.isMuted(this));
        }
        VoiceGuide.announceScreen(this,
                "یہاں آپ کو ملنے والے آرڈرز کی فہرست ہے۔ ہر آرڈر پر نیچے بٹن دبا کر اس کی حالت آگے بڑھائیں — پہلے کام شروع کریں دبائیں، پھر جب کام مکمل ہو جائے تو مکمل کا بٹن دبائیں۔");
        loadOrders();
    }

    @Override
    protected void onPause() {
        super.onPause();
        VoiceGuide.stop(this);
    }

    private void setupFilterButtons() {
        if (btnFilterAll != null) btnFilterAll.setOnClickListener(v -> applyFilter("ALL"));
        if (btnFilterPending != null) btnFilterPending.setOnClickListener(v -> applyFilter(Constants.STATUS_PENDING));
        if (btnFilterInProgress != null) btnFilterInProgress.setOnClickListener(v -> applyFilter(Constants.STATUS_IN_PROGRESS));
        if (btnFilterCompleted != null) btnFilterCompleted.setOnClickListener(v -> applyFilter(Constants.STATUS_COMPLETED));
    }

    private void applyFilter(String status) {
        currentSelectedFilter = status;

        updateFilterButtonStyles(btnFilterAll, "ALL".equals(status));
        updateFilterButtonStyles(btnFilterPending, Constants.STATUS_PENDING.equals(status));
        updateFilterButtonStyles(btnFilterInProgress, Constants.STATUS_IN_PROGRESS.equals(status));
        updateFilterButtonStyles(btnFilterCompleted, Constants.STATUS_COMPLETED.equals(status));

        displayedOrderList.clear();
        for (Order o : fullOrderList) {
            if (o == null) continue;
            if ("ALL".equalsIgnoreCase(status) || status.equalsIgnoreCase(o.getStatus())) {
                displayedOrderList.add(o);
            }
        }

        if (adapter != null) adapter.notifyDataSetChanged();
        if (tvEmpty != null) {
            tvEmpty.setVisibility(displayedOrderList.isEmpty() ? View.VISIBLE : View.GONE);
            if (displayedOrderList.isEmpty() && !"ALL".equalsIgnoreCase(status)) {
                tvEmpty.setText("اس حالت کا کوئی آرڈر موجود نہیں / No orders in this status");
            } else if (displayedOrderList.isEmpty()) {
                tvEmpty.setText("ابھی تک کوئی آرڈر نہیں");
            }
        }
    }

    private void updateFilterButtonStyles(Button btn, boolean isSelected) {
        if (btn == null) return;
        boolean isDark = isDarkMode();
        if (isSelected) {
            btn.setBackgroundResource(R.drawable.bg_skeu_button_primary);
            btn.setTextColor(0xFFFFFFFF);
        } else {
            btn.setBackgroundResource(R.drawable.bg_skeu_button_cream);
            btn.setTextColor(isDark ? 0xFFFFFFFF : 0xFF192A56);
        }
    }

    private boolean isDarkMode() {
        PreferencesManager prefs = new PreferencesManager(this);
        return prefs.isDarkThemeEnabled();
    }

    private void loadOrders() {
        FirebaseDatabase.getInstance().getReference().child(Constants.NODE_ORDERS)
                .orderByChild("sellerId").equalTo(session.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(DataSnapshot snapshot) {
                        fullOrderList.clear();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            Order o = child.getValue(Order.class);
                            if (o != null) fullOrderList.add(o);
                        }
                        // Sort newest/latest orders at the top!
                        Collections.sort(fullOrderList, (a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
                        applyFilter(currentSelectedFilter);
                    }
                    @Override public void onCancelled(DatabaseError error) {
                        tvEmpty.setVisibility(View.VISIBLE);
                        tvEmpty.setText("خرابی: " + error.getMessage());
                    }
                });
    }

    // ✅ DARK THEME - ADD AT END OF CLASS
    private void applyTheme() {
        PreferencesManager prefs = new PreferencesManager(this);
        if (prefs.isDarkThemeEnabled()) {
            setTheme(R.style.Theme_SheEmpower_Dark);
        } else {
            setTheme(R.style.Theme_SheEmpower);
        }
    }
}