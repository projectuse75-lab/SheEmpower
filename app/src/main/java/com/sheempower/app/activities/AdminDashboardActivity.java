package com.sheempower.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.database.*;
import com.sheempower.app.R;
import com.sheempower.app.adapters.PendingSellerAdapter;
import com.sheempower.app.models.Seller;
import com.sheempower.app.utils.BottomNavHelper;
import com.sheempower.app.utils.Constants;
import com.sheempower.app.utils.VoiceGuide;
import java.util.ArrayList;
import java.util.List;

public class AdminDashboardActivity extends AppCompatActivity {

    private TextView tvTotalSellers, tvTotalProducts, tvTotalBuyers, tvPendingCount;
    private RecyclerView rvPending, rvAllSellers;
    private TextView tvEmptyPending;

    private PendingSellerAdapter pendingAdapter;
    private final List<Seller> pendingList = new ArrayList<>();

    private AllSellerManageAdapter allSellerAdapter;
    private final List<Seller> allSellerList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        VoiceGuide.announceScreen(this, "یہ ایڈمن ڈیش بورڈ ہے۔ نئی بیچنے والیاں منظور کریں اور تمام ڈیٹا کا جائزہ لیں۔");

        tvTotalSellers  = findViewById(R.id.tvTotalSellers);
        tvTotalProducts = findViewById(R.id.tvTotalProducts);
        tvTotalBuyers   = findViewById(R.id.tvTotalBuyers);
        tvPendingCount  = findViewById(R.id.tvPendingCount);
        rvPending       = findViewById(R.id.rvPending);
        rvAllSellers    = findViewById(R.id.rvAllSellers);
        tvEmptyPending  = findViewById(R.id.tvEmptyPending);

        // Products stat card click → poori list dikhati hai
        findViewById(R.id.layoutProductsStat).setOnClickListener(v ->
                startActivity(new Intent(this, AdminProductsActivity.class)));

        rvPending.setLayoutManager(new LinearLayoutManager(this));
        pendingAdapter = new PendingSellerAdapter(pendingList,
                seller -> updateStatus(seller, "verified"),
                seller -> updateStatus(seller, "rejected"),
                this::openSellerProfile // row click → profile
        );
        rvPending.setAdapter(pendingAdapter);

        rvAllSellers.setLayoutManager(new LinearLayoutManager(this));
        allSellerAdapter = new AllSellerManageAdapter(allSellerList, this::toggleSellerBlock, this::openSellerProfile);
        rvAllSellers.setAdapter(allSellerAdapter);

        BottomNavHelper.setupForAdmin(this, findViewById(R.id.bottomNav), 1);

        loadStats();
        loadPendingSellers();
        loadAllSellers();
    }

    @Override protected void onResume() {
        super.onResume();
        loadStats();
        loadPendingSellers();
        loadAllSellers();
    }

    private void openSellerProfile(Seller seller) {
        Intent i = new Intent(this, SellerProfileActivity.class);
        i.putExtra(SellerProfileActivity.EXTRA_SELLER_ID, seller.getUid());
        startActivity(i);
    }

    private void loadStats() {
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();
        dbRef.child(Constants.NODE_SELLERS).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(DataSnapshot snapshot) { tvTotalSellers.setText(String.valueOf(snapshot.getChildrenCount())); }
            @Override public void onCancelled(DatabaseError error) {}
        });
        dbRef.child(Constants.NODE_PRODUCTS).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(DataSnapshot snapshot) { tvTotalProducts.setText(String.valueOf(snapshot.getChildrenCount())); }
            @Override public void onCancelled(DatabaseError error) {}
        });
        dbRef.child(Constants.NODE_USERS).orderByChild("role").equalTo("buyer")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(DataSnapshot snapshot) { tvTotalBuyers.setText(String.valueOf(snapshot.getChildrenCount())); }
                    @Override public void onCancelled(DatabaseError error) { tvTotalBuyers.setText("—"); }
                });
    }

    private void loadPendingSellers() {
        FirebaseDatabase.getInstance().getReference().child(Constants.NODE_SELLERS)
                .orderByChild("verificationStatus").equalTo("pending")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(DataSnapshot snapshot) {
                        pendingList.clear();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            Seller s = child.getValue(Seller.class);
                            if (s != null) pendingList.add(s);
                        }
                        pendingAdapter.notifyDataSetChanged();
                        tvPendingCount.setText(pendingList.size() + " منظوری کے منتظر");
                        tvEmptyPending.setVisibility(pendingList.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
                    }
                    @Override public void onCancelled(DatabaseError error) {
                        tvEmptyPending.setVisibility(android.view.View.VISIBLE);
                        tvEmptyPending.setText("خرابی: " + error.getMessage());
                    }
                });
    }

    private void loadAllSellers() {
        FirebaseDatabase.getInstance().getReference().child(Constants.NODE_SELLERS)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(DataSnapshot snapshot) {
                        allSellerList.clear();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            Seller s = child.getValue(Seller.class);
                            if (s != null) allSellerList.add(s);
                        }
                        allSellerAdapter.notifyDataSetChanged();
                    }
                    @Override public void onCancelled(DatabaseError error) {}
                });
    }

    private void updateStatus(Seller seller, String status) {
        FirebaseDatabase.getInstance().getReference().child(Constants.NODE_SELLERS)
                .child(seller.getUid()).child("verificationStatus").setValue(status)
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, seller.getBusinessName() + " → " + status, Toast.LENGTH_SHORT).show();
                    loadPendingSellers();
                    loadAllSellers();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "خرابی: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void toggleSellerBlock(Seller seller) {
        String newStatus = Constants.isApproved(seller.getVerificationStatus()) ? "blocked" : "verified";
        updateStatus(seller, newStatus);
    }

    static class AllSellerManageAdapter extends RecyclerView.Adapter<AllSellerManageAdapter.VH> {
        interface OnToggle { void onToggle(Seller seller); }
        interface OnRowClick { void onClick(Seller seller); }
        private final List<Seller> list;
        private final OnToggle onToggle;
        private final OnRowClick onRowClick;

        AllSellerManageAdapter(List<Seller> list, OnToggle onToggle, OnRowClick onRowClick) {
            this.list = list; this.onToggle = onToggle; this.onRowClick = onRowClick;
        }

        @Override public VH onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            android.view.View v = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_admin_seller_manage, parent, false);
            return new VH(v);
        }

        @Override public void onBindViewHolder(VH holder, int position) {
            Seller s = list.get(position);
            holder.tvName.setText(s.getBusinessName() + " (" + s.getCategory() + ")");
            boolean approved = Constants.isApproved(s.getVerificationStatus());
            holder.tvStatus.setText(approved ? "✅ فعال" : "🚫 " + s.getVerificationStatus());
            holder.tvStatus.setTextColor(approved ? 0xFF4CAF50 : 0xFFF44336);
            holder.btnToggle.setText(approved ? "بند کریں" : "فعال کریں");
            holder.btnToggle.setOnClickListener(v -> onToggle.onToggle(s));
            holder.itemView.setOnClickListener(v -> onRowClick.onClick(s));
        }

        @Override public int getItemCount() { return list.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvStatus;
            Button btnToggle;
            VH(android.view.View itemView) {
                super(itemView);
                tvName    = itemView.findViewById(R.id.tvAdminSellerName);
                tvStatus  = itemView.findViewById(R.id.tvAdminSellerStatus);
                btnToggle = itemView.findViewById(R.id.btnAdminToggle);
            }
        }
    }
}