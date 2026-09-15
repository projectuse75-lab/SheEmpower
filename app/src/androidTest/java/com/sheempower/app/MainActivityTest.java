package com.sheempower.app;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.sheempower.app.activities.MainActivity;

import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

@RunWith(AndroidJUnit4.class)
public class MainActivityTest {

    @Test
    public void mainScreenImportantViewsDisplayed() {

        try (ActivityScenario<MainActivity> scenario =
                     ActivityScenario.launch(MainActivity.class)) {

            onView(withId(R.id.etSearch))
                    .check(matches(isDisplayed()));

            onView(withId(R.id.btnJoin))
                    .check(matches(isDisplayed()));

            onView(withId(R.id.layoutCategories))
                    .check(matches(isDisplayed()));

            onView(withId(R.id.rvTopSellers))
                    .perform(scrollTo())
                    .check(matches(isDisplayed()));

            onView(withId(R.id.rvLatestProducts))
                    .perform(scrollTo())
                    .check(matches(isDisplayed()));

            onView(withId(R.id.tvProductsTab))
                    .perform(scrollTo())
                    .check(matches(isDisplayed()));

            onView(withId(R.id.tvSellersTab))
                    .perform(scrollTo())
                    .check(matches(isDisplayed()));
        }
    }

    @Test
    public void mainScreenCategoryLayoutDisplayed() {

        try (ActivityScenario<MainActivity> scenario =
                     ActivityScenario.launch(MainActivity.class)) {

            onView(withId(R.id.layoutCategories))
                    .check(matches(isDisplayed()));
        }
    }
}