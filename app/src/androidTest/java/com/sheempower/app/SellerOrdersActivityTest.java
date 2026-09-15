package com.sheempower.app;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.sheempower.app.activities.SellerOrdersActivity;
import com.sheempower.app.utils.SessionManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

@RunWith(AndroidJUnit4.class)
public class SellerOrdersActivityTest {

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
    public void sellerOrdersScreenDisplayed() {

        try (ActivityScenario<SellerOrdersActivity> scenario =
                     ActivityScenario.launch(SellerOrdersActivity.class)) {

            onView(withId(R.id.rvOrders))
                    .check(matches(isDisplayed()));

            onView(withId(R.id.bottomNav))
                    .check(matches(isDisplayed()));
        }
    }
}
