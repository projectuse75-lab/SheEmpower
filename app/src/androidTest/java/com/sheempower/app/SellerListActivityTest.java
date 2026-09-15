package com.sheempower.app;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.sheempower.app.activities.SellerListActivity;

import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

@RunWith(AndroidJUnit4.class)
public class SellerListActivityTest {

    @Test
    public void sellerListScreenDisplayed() {

        Intent intent = new Intent(
                androidx.test.core.app.ApplicationProvider.getApplicationContext(),
                SellerListActivity.class
        );

        intent.putExtra(SellerListActivity.EXTRA_CATEGORY, "Cooking");

        try (ActivityScenario<SellerListActivity> scenario =
                     ActivityScenario.launch(intent)) {

            onView(withId(R.id.tvCategoryTitle))
                    .check(matches(isDisplayed()));

            onView(withId(R.id.tvSellerCount))
                    .check(matches(isDisplayed()));

            onView(withId(R.id.rvSellers))
                    .check(matches(isDisplayed()));

            onView(withId(R.id.rvProducts))
                    .perform(scrollTo())
                    .check(matches(isDisplayed()));
        }
    }

    @Test
    public void sellerListCategoryTitleDisplayed() {

        Intent intent = new Intent(
                androidx.test.core.app.ApplicationProvider.getApplicationContext(),
                SellerListActivity.class
        );

        intent.putExtra(SellerListActivity.EXTRA_CATEGORY, "Cooking");

        try (ActivityScenario<SellerListActivity> scenario =
                     ActivityScenario.launch(intent)) {

            onView(withId(R.id.tvCategoryTitle))
                    .check(matches(isDisplayed()));
        }
    }
}
