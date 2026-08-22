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
import com.sheempower.app.utils.Constants;
import com.sheempower.app.utils.DemoDataSeeder;
import com.sheempower.app.utils.SessionManager;
import com.sheempower.app.utils.TTSHelper;
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

    // ✅ Store listener reference for cleanup
    private ValueEventListener pendingListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        session = new SessionManager(this);
        tts = TTSHelper.getInstance(this);

        // ============================================================
        // ✅ CRITICAL SECURITY CHECK: Only Admin can access this
        // ============================================================
        if (!session.isLoggedIn()) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        if (!session.isAdmin()) {
            Toast.makeText(this, "🚫 Admin access only! Unauthorized access detected.", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        // ✅ Find Views
        rvPending = findViewById(R.id.rvPending);
        tvEmpty = findViewById(R.id.tvEmpty);
        tvCount = findViewById(R.id.tvCount);
        btnLogout = findViewById(R.id.btnLogout);

        // ✅ Speaker Icon for TTS Voice Guidance
        findViewById(R.id.btnSpeaker).setOnClickListener(v -> {
            String instruction = "Admin Dashboard. Manage pending seller approvals. " +
                    "Tap Approve to verify a seller, or Reject to decline. " +
                    "You can also view CNIC and play voice samples for verification.";
            tts.speak(instruction);
        });

        // ✅ Setup Pending Seller Adapter (with 3 callbacks: Approve, Reject, ToggleTopRated)
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

        // ✅ Logout
        btnLogout.setOnClickListener(v -> {
            tts.speak("Logging out as Admin.");
            session.logout();
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });

        loadPendingSellers();

        // ✅ Auto-speak screen instruction
        tts.speak("Admin Dashboard loaded.");
    }

    private void loadPendingSellers() {
        // ✅ Use real-time listener for live updates
        pendingListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                pendingList.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Seller s = child.getValue(Seller.class);
                    if (s != null) {
                        pendingList.add(s);
                    }
                }
                adapter.notifyDataSetChanged();
                tvCount.setText(pendingList.size() + " pending approval(s)");

                // ✅ Show empty state
                if (pendingList.isEmpty()) {
                    tvEmpty.setVisibility(View.VISIBLE);
                    tvEmpty.setText("🎉 No pending sellers\n\nAll sellers are approved!");
                } else {
                    tvEmpty.setVisibility(View.GONE);
                }
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

    // ============================================================
    // ✅ UPDATE SELLER STATUS (Approve / Reject)
    // ============================================================
    private void updateStatus(Seller seller, String status) {
        // ✅ Security Check: Verify again before updating
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
                    String message = seller.getBusinessName() + " → " + status;
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                    tts.speak(message);

                    // ✅ If approved, clear demo data on first real approval
                    if ("approved".equals(status)) {
                        DemoDataSeeder.clearDemoDataOnFirstRealUse(this);
                    }

                    // ✅ Listener will auto-refresh due to real-time updates
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // ============================================================
    // ✅ TOGGLE TOP RATED STATUS
    // ============================================================
    private void toggleTopRated(Seller seller) {
        // ✅ Security Check
        if (!session.isAdmin()) {
            Toast.makeText(this, "Unauthorized!", Toast.LENGTH_SHORT).show();
            return;
        }

        // ✅ Only toggle if seller is approved
        if (!"approved".equals(seller.getVerificationStatus())) {
            Toast.makeText(this, "Only approved sellers can be Top Rated", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean newStatus = !seller.isTopRated();
        FirebaseDatabase.getInstance().getReference()
                .child(Constants.NODE_SELLERS)
                .child(seller.getUid())
                .child("isTopRated")
                .setValue(newStatus)
                .addOnSuccessListener(v -> {
                    String message = seller.getBusinessName() + " → " +
                            (newStatus ? "⭐ Top Rated (ON)" : "⭐ Top Rated (OFF)");
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                    tts.speak(message);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // ✅ Remove listeners to prevent memory leaks
        if (pendingListener != null) {
            FirebaseDatabase.getInstance().getReference()
                    .child(Constants.NODE_SELLERS)
                    .removeEventListener(pendingListener);
        }

        if (tts != null) tts.shutdown();
    }
}