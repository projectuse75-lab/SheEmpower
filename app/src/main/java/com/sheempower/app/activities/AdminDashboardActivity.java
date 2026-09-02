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
import com.sheempower.app.adapters.PendingSellerAdapter;
import com.sheempower.app.models.Seller;
import com.sheempower.app.utils.*;
import java.util.ArrayList;
import java.util.List;

public class AdminDashboardActivity extends AppCompatActivity {

    private RecyclerView rvPending;
    private TextView tvEmpty, tvCount;
    private Button btnLogout;
    private PendingSellerAdapter adapter;
    private final List<Seller> pendingList = new ArrayList<>();
    private SessionManager session;
    private TTSHelper tts;
    private ValueEventListener pendingListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        session = new SessionManager(this);
        tts = TTSHelper.getInstance(this);

        if (!session.isLoggedIn() || !session.isAdmin()) {
            Toast.makeText(this, "Admin access only!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        rvPending = findViewById(R.id.rvPending);
        tvEmpty = findViewById(R.id.tvEmpty);
        tvCount = findViewById(R.id.tvCount);
        btnLogout = findViewById(R.id.btnLogout);

        findViewById(R.id.btnSpeaker).setOnClickListener(v -> {
            tts.speak("Admin Dashboard. Manage pending seller approvals.");
        });

        rvPending.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PendingSellerAdapter(pendingList, new PendingSellerAdapter.OnSellerAction() {
            @Override
            public void onApprove(Seller seller) {
                updateStatus(seller, "approved");
            }

            @Override
            public void onReject(Seller seller) {
                updateStatus(seller, "rejected");
            }

            @Override
            public void onToggleTopRated(Seller seller) {
                toggleTopRated(seller);
            }
        });
        rvPending.setAdapter(adapter);

        btnLogout.setOnClickListener(v -> {
            session.logout();
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });

        loadPendingSellers();

        tts.speak("Admin Dashboard loaded.");

        // ✅ BOTTOM NAVIGATION (ADMIN)
        BottomNavHelper.setupBottomNav(this, session);
        BottomNavHelper.setSelectedTab(this, 2);
    }

    private void loadPendingSellers() {
        pendingListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                pendingList.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Seller s = child.getValue(Seller.class);
                    if (s != null) pendingList.add(s);
                }
                adapter.notifyDataSetChanged();
                tvCount.setText(pendingList.size() + " pending approval(s)");
                tvEmpty.setVisibility(pendingList.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(AdminDashboardActivity.this, "Error loading pending sellers", Toast.LENGTH_SHORT).show();
            }
        };

        FirebaseDatabase.getInstance().getReference()
                .child(Constants.NODE_SELLERS)
                .orderByChild("verificationStatus")
                .equalTo("pending")
                .addValueEventListener(pendingListener);
    }

    private void updateStatus(Seller seller, String status) {
        if (!session.isAdmin()) {
            Toast.makeText(this, "Unauthorized!", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseDatabase.getInstance().getReference()
                .child(Constants.NODE_SELLERS)
                .child(seller.getUid())
                .child("verificationStatus")
                .setValue(status)
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, seller.getBusinessName() + " → " + status, Toast.LENGTH_SHORT).show();
                    tts.speak(seller.getBusinessName() + " " + status);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void toggleTopRated(Seller seller) {
        if (!session.isAdmin()) {
            Toast.makeText(this, "Unauthorized!", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean newStatus = !seller.isTopRated();
        FirebaseDatabase.getInstance().getReference()
                .child(Constants.NODE_SELLERS)
                .child(seller.getUid())
                .child("isTopRated")
                .setValue(newStatus)
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, seller.getBusinessName() + " → " +
                            (newStatus ? "⭐ Top Rated (ON)" : "⭐ Top Rated (OFF)"), Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (pendingListener != null) {
            FirebaseDatabase.getInstance().getReference()
                    .child(Constants.NODE_SELLERS)
                    .removeEventListener(pendingListener);
        }
        if (tts != null) tts.shutdown();
    }
}