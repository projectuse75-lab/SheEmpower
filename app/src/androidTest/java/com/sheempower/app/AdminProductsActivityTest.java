package com.sheempower.app;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.sheempower.app.activities.AdminProductsActivity;

import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

@RunWith(AndroidJUnit4.class)
public class AdminProductsActivityTest {

    @Test
    public void adminProductsScreenDisplayed() {

        try (ActivityScenario<AdminProductsActivity> scenario =
                     ActivityScenario.launch(AdminProductsActivity.class)) {

            onView(withId(R.id.rvProducts))
                    .check(matches(isDisplayed()));
        }
    }
}