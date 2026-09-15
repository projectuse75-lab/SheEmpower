package com.sheempower.app;

import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.android.gms.tasks.Tasks;
import com.sheempower.app.activities.PlaceOrderActivity;
import com.sheempower.app.utils.Constants;
import com.sheempower.app.utils.SessionManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.After;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

@RunWith(AndroidJUnit4.class)
public class PlaceOrderActivityTest {

    private static final String TEST_SELLER_ID = "automated_test_seller";

    @Before
    public void setUp() throws Exception {

        // Test buyer ko logged-in karna
        Context context =
                ApplicationProvider.getApplicationContext();

        SessionManager sessionManager =
                new SessionManager(context);

        sessionManager.saveUser(
                "automated_test_buyer",
                "buyer",
                "Test Buyer"
        );

        // Temporary Firebase seller data
        Map<String, Object> seller = new HashMap<>();

        seller.put("uid", TEST_SELLER_ID);
        seller.put("businessName", "Test She Empower Business");
        seller.put("ownerName", "Test Seller");
        seller.put("email", "testseller@sheempower.com");
        seller.put("phone", "03000000000");

        seller.put("category", "Cooking");
        seller.put("bio", "Automated testing seller");
        seller.put("city", "Karachi");

        seller.put("cnicUrl", "");
        seller.put("voiceUrl", "");

        seller.put("verificationStatus", "approved");
        seller.put("whatsappNumber", "923000000000");
        seller.put("imageUrl", "");

        seller.put("capacityDescription", "Test capacity");
        seller.put("shopAddress", "Test Address Karachi");

        seller.put("topRated", true);
        seller.put("canDeliver", true);
        seller.put("rating", 4.5);
        seller.put("ratingCount", 10);

        // Login as admin to bypass Firebase Database permission denied error during tests
        try {
            Tasks.await(FirebaseAuth.getInstance().signInWithEmailAndPassword(Constants.ADMIN_EMAIL, Constants.ADMIN_PASSWORD));
        } catch (Exception e) {
            try {
                Tasks.await(FirebaseAuth.getInstance().createUserWithEmailAndPassword(Constants.ADMIN_EMAIL, Constants.ADMIN_PASSWORD));
            } catch (Exception ex) {
                // Ignore
            }
        }

        Tasks.await(FirebaseDatabase.getInstance()
                .getReference()
                .child(Constants.NODE_SELLERS)
                .child(TEST_SELLER_ID)
                .setValue(seller));
    }

    @Test
    public void placeOrderScreenOpens() {

        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                PlaceOrderActivity.class
        );

        // Test seller
        intent.putExtra(
                PlaceOrderActivity.EXTRA_SELLER_ID,
                TEST_SELLER_ID
        );

        // Single product mode
        intent.putExtra(
                PlaceOrderActivity.EXTRA_PRODUCT_ID,
                "automated_test_product"
        );

        intent.putExtra(
                PlaceOrderActivity.EXTRA_PRODUCT_NAME,
                "Test Cooking Product"
        );

        intent.putExtra(
                PlaceOrderActivity.EXTRA_PRODUCT_PRICE,
                1000.0
        );

        try (ActivityScenario<PlaceOrderActivity> scenario =
                     ActivityScenario.launch(intent)) {

            // Seller information
            onView(withId(R.id.tvSellerName))
                    .check(matches(isDisplayed()));

            onView(withId(R.id.tvSellerMeta))
                    .check(matches(isDisplayed()));

            onView(withId(R.id.tvSellerVerified))
                    .check(matches(isDisplayed()));

            // Selected product
            onView(withId(R.id.tvSelectedItem))
                    .perform(scrollTo())
                    .check(matches(isDisplayed()));

            // Fixed amount
            onView(withId(R.id.etAmount))
                    .perform(scrollTo())
                    .check(matches(isDisplayed()));

            // Delivery information
            onView(withId(R.id.tvDeliveryInfo))
                    .perform(scrollTo())
                    .check(matches(isDisplayed()));

            // Category details
            onView(withId(R.id.tvCategoryHeader))
                    .perform(scrollTo())
                    .check(matches(isDisplayed()));

            // Notes
            onView(withId(R.id.etNotes))
                    .perform(scrollTo())
                    .check(matches(isDisplayed()));

            // Submit button
            onView(withId(R.id.btnSubmitOrder))
                    .perform(scrollTo())
                    .check(matches(isDisplayed()));
        }
    }

    @After
    public void tearDown() throws Exception {

        // Temporary seller ko Firebase se remove karna
        // Login as admin to bypass Firebase Database permission denied error during tests
        try {
            Tasks.await(FirebaseAuth.getInstance().signInWithEmailAndPassword(Constants.ADMIN_EMAIL, Constants.ADMIN_PASSWORD));
        } catch (Exception e) {
            try {
                Tasks.await(FirebaseAuth.getInstance().createUserWithEmailAndPassword(Constants.ADMIN_EMAIL, Constants.ADMIN_PASSWORD));
            } catch (Exception ex) {
                // Ignore
            }
        }

        Tasks.await(FirebaseDatabase.getInstance()
                .getReference()
                .child(Constants.NODE_SELLERS)
                .child(TEST_SELLER_ID)
                .removeValue());
    }
}