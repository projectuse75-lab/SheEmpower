package com.sheempower.app;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.sheempower.app.activities.AdminDashboardActivity;

import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

@RunWith(AndroidJUnit4.class)
public class AdminDashboardActivityTest {

    @Test
    public void adminDashboardScreenDisplayed() {

        try (ActivityScenario<AdminDashboardActivity> scenario =
                     ActivityScenario.launch(AdminDashboardActivity.class)) {

            // Main dashboard sections
            onView(withId(R.id.layoutProductsStat))
                    .check(matches(isDisplayed()));

            // Pending sellers section
            onView(withId(R.id.rvPending))
                    .perform(scrollTo())
                    .check(matches(isDisplayed()));

            // All sellers section
            onView(withId(R.id.rvAllSellers))
                    .perform(scrollTo())
                    .check(matches(isDisplayed()));

            // Bottom navigation
            onView(withId(R.id.bottomNav))
                    .check(matches(isDisplayed()));
        }
    }
}