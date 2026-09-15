package com.sheempower.app;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.sheempower.app.activities.AddProductActivity;
import com.sheempower.app.utils.SessionManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

@RunWith(AndroidJUnit4.class)
public class AddProductActivityTest {

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
    public void addProductScreenDisplayed() {

        try (ActivityScenario<AddProductActivity> scenario =
                     ActivityScenario.launch(AddProductActivity.class)) {

            onView(withId(R.id.etProductName))
                    .check(matches(isDisplayed()));

            onView(withId(R.id.etDescription))
                    .check(matches(isDisplayed()));

            onView(withId(R.id.etPrice))
                    .check(matches(isDisplayed()));

            onView(withId(R.id.btnPickImage))
                    .perform(scrollTo())
                    .check(matches(isDisplayed()));

            onView(withId(R.id.tvImageStatus))
                    .perform(scrollTo())
                    .check(matches(isDisplayed()));

            onView(withId(R.id.btnSubmit))
                    .perform(scrollTo())
                    .check(matches(isDisplayed()));
        }
    }
}