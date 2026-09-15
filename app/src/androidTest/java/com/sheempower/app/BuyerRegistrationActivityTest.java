package com.sheempower.app;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.sheempower.app.activities.BuyerRegisterActivity;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.assertion.ViewAssertions.matches;

@RunWith(AndroidJUnit4.class)
public class BuyerRegistrationActivityTest {

    @Rule
    public ActivityScenarioRule<BuyerRegisterActivity> activityRule =
            new ActivityScenarioRule<>(BuyerRegisterActivity.class);

    @Test
    public void buyerRegistrationFieldsDisplayed() {

        onView(withId(R.id.etName))
                .check(matches(isDisplayed()));

        onView(withId(R.id.etEmail))
                .check(matches(isDisplayed()));

        onView(withId(R.id.etPassword))
                .check(matches(isDisplayed()));

        onView(withId(R.id.etPhone))
                .check(matches(isDisplayed()));

        onView(withId(R.id.btnRegister))
                .check(matches(isDisplayed()));
    }

    @Test
    public void passwordControlsDisplayed() {

        onView(withId(R.id.togglePasswordVisibility))
                .check(matches(isDisplayed()));

        onView(withId(R.id.passwordStrengthBar))
                .check(matches(isDisplayed()));

        onView(withId(R.id.passwordStrengthText))
                .check(matches(isDisplayed()));
    }
}