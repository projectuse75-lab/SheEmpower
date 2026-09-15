package com.sheempower.app;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.sheempower.app.activities.SellerProfileActivity;
import com.sheempower.app.utils.SessionManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

@RunWith(AndroidJUnit4.class)
public class SellerProfileActivityTest {

    @Before
    public void setUp() {
        SessionManager sessionManager =
                new SessionManager(
                        ApplicationProvider.getApplicationContext()
                );

        sessionManager.saveUser(
                "test_buyer_123",
                "buyer",
                "Test Buyer"
        );
    }

    @Test
    public void sellerProfileScreenDisplayed() {

        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                SellerProfileActivity.class
        );

        intent.putExtra(
                SellerProfileActivity.EXTRA_SELLER_ID,
                "test_seller_123"
        );

        try (ActivityScenario<SellerProfileActivity> scenario =
                     ActivityScenario.launch(intent)) {

            onView(withId(R.id.ivSellerAvatar))
                    .check(matches(isDisplayed()));

            onView(withId(R.id.btnPlaceOrder))
                    .check(matches(isDisplayed()));

            onView(withId(R.id.btnWhatsAppChat))
                    .check(matches(isDisplayed()));
        }
    }
}