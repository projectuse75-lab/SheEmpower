package com.sheempower.app;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.sheempower.app.activities.SellerRegistrationActivity;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.assertion.ViewAssertions.matches;

@RunWith(AndroidJUnit4.class)
public class SellerRegistrationActivityTest {

    @Rule
    public ActivityScenarioRule<SellerRegistrationActivity> activityRule =
            new ActivityScenarioRule<>(SellerRegistrationActivity.class);

    @Test
    public void sellerRegistrationFieldsDisplayed() {

        // Basic registration fields
        onView(withId(R.id.etSellerName))
                .check(matches(isDisplayed()));

        onView(withId(R.id.etSellerEmail))
                .check(matches(isDisplayed()));

        onView(withId(R.id.etSellerPassword))
                .check(matches(isDisplayed()));

        onView(withId(R.id.etBusinessName))
                .check(matches(isDisplayed()));

        onView(withId(R.id.etCity))
                .check(matches(isDisplayed()));

        onView(withId(R.id.etBio))
                .check(matches(isDisplayed()));

        // WhatsApp
        onView(withId(R.id.etWhatsApp))
                .perform(scrollTo())
                .check(matches(isDisplayed()));

        // Category
        onView(withId(R.id.spinnerCategory))
                .perform(scrollTo())
                .check(matches(isDisplayed()));

        // Capacity
        onView(withId(R.id.etCapacity))
                .perform(scrollTo())
                .check(matches(isDisplayed()));

        // Delivery
        onView(withId(R.id.spinnerCanDeliver))
                .perform(scrollTo())
                .check(matches(isDisplayed()));

        // Shop address
        onView(withId(R.id.etShopAddress))
                .perform(scrollTo())
                .check(matches(isDisplayed()));

        // CNIC upload
        onView(withId(R.id.btnPickCnic))
                .perform(scrollTo())
                .check(matches(isDisplayed()));

        // Final registration button
        onView(withId(R.id.btnSellerRegister))
                .perform(scrollTo())
                .check(matches(isDisplayed()));
    }

    @Test
    public void sellerRegistrationExtraControlsDisplayed() {

        // Password visibility button
        onView(withId(R.id.togglePasswordVisibility))
                .check(matches(isDisplayed()));

        // Password strength bar
        onView(withId(R.id.passwordStrengthBar))
                .check(matches(isDisplayed()));

        // Password strength text
        onView(withId(R.id.passwordStrengthText))
                .check(matches(isDisplayed()));

        // Voice recording button
        onView(withId(R.id.btnRecord))
                .perform(scrollTo())
                .check(matches(isDisplayed()));

        // Audio upload button
        onView(withId(R.id.btnPickAudio))
                .perform(scrollTo())
                .check(matches(isDisplayed()));
    }
}