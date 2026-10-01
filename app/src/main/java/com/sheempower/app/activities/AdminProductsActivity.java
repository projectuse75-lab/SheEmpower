package com.sheempower.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.database.*;
import com.sheempower.app.R;
import com.sheempower.app.adapters.ProductAdapter;
import com.sheempower.app.models.Product;
import com.sheempower.app.utils.Constants;
import com.sheempower.app.utils.VoiceGuide;
import com.sheempower.app.utils.PreferencesManager;
import java.util.ArrayList;
import java.util.List;

public class AdminProductsActivity extends AppCompatActivity {

    private RecyclerView rvProducts;
    private TextView tvEmpty;
    private ProductAdapter adapter;
    private final List<Product> productList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_products);

        ImageButton btnSpeaker = findViewById(R.id.btnSpeaker);
        VoiceGuide.setupSpeakerButton(btnSpeaker, this, "یہ نظام کی تمام اشیاء کی فہرست ہے۔");

        rvProducts = findViewById(R.id.rvProducts);
        tvEmpty    = findViewById(R.id.tvEmpty);

        rvProducts.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new ProductAdapter(productList,
                product -> {
                    Intent i = new Intent(this, ProductDetailActivity.class);
                    i.putExtra(ProductDetailActivity.EXTRA_PRODUCT_ID, product.getId());
                    startActivity(i);
                },
                product -> Toast.makeText(this, "ایڈمن آرڈر نہیں دے سکتا", Toast.LENGTH_SHORT).show(),
                product -> Toast.makeText(this, "ایڈمن چیٹ نہیں کر سکتا", Toast.LENGTH_SHORT).show()
        );
        rvProducts.setAdapter(adapter);

        loadProducts();
    }

    @Override
    protected void onResume() {
        super.onResume();
        VoiceGuide.announceScreen(this, "یہ نظام کی تمام اشیاء کی فہرست ہے۔");
        loadProducts();
    }

    private void loadProducts() {
        FirebaseDatabase.getInstance().getReference().child(Constants.NODE_PRODUCTS)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(DataSnapshot snapshot) {
                        productList.clear();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            Product p = child.getValue(Product.class);
                            if (p != null) productList.add(p);
                        }
                        adapter.notifyDataSetChanged();
                        tvEmpty.setVisibility(productList.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                    @Override public void onCancelled(DatabaseError error) {}
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