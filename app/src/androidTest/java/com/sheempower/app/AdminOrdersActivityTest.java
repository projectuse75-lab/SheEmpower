package com.sheempower.app;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.sheempower.app.activities.AdminOrdersActivity;

import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

@RunWith(AndroidJUnit4.class)
public class AdminOrdersActivityTest {

    @Test
    public void adminOrdersScreenDisplayed() {

        try (ActivityScenario<AdminOrdersActivity> scenario =
                     ActivityScenario.launch(AdminOrdersActivity.class)) {

            onView(withId(R.id.rvOrders))
                    .check(matches(isDisplayed()));

            onView(withId(R.id.bottomNav))
                    .check(matches(isDisplayed()));
        }
    }
}