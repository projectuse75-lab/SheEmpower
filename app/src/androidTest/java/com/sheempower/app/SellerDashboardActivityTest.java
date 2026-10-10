package com.sheempower.app;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.sheempower.app.activities.SellerDashboardActivity;
import com.sheempower.app.utils.SessionManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

@RunWith(AndroidJUnit4.class)
public class SellerDashboardActivityTest {

    @Before
    public void setUp() {
        SessionManager sessionManager =
                new SessionManager(
                        androidx.test.core.app.ApplicationProvider
                                .getApplicationContext()
                );

        sessionManager.saveUser(
                "test_seller_123",
                "seller",
                "Test Seller"
        );
    }

    @Test
    public void sellerDashboardScreenDisplayed() {

        try (ActivityScenario<SellerDashboardActivity> scenario =
                     ActivityScenario.launch(SellerDashboardActivity.class)) {

            // Basic dashboard views
            onView(withId(R.id.tvCategory))
                    .check(matches(isDisplayed()));

            onView(withId(R.id.tvWelcome))
                    .check(matches(isDisplayed()));

            // Add product button
            onView(withId(R.id.btnAddProduct))
                    .check(matches(isDisplayed()));

            // Reviews card
            onView(withId(R.id.cardSellerReviews))
                    .check(matches(isDisplayed()));

            // Bottom navigation
            onView(withId(R.id.bottomNav))
                    .check(matches(isDisplayed()));
        }
    }
}