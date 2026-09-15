package com.sheempower.app;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.sheempower.app.activities.ProductDetailActivity;

import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

@RunWith(AndroidJUnit4.class)
public class ProductDetailActivityTest {

    @Test
    public void productDetailScreenDisplayed() {

        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                ProductDetailActivity.class
        );

        intent.putExtra(
                ProductDetailActivity.EXTRA_PRODUCT_ID,
                "test_product_123"
        );

        try (ActivityScenario<ProductDetailActivity> scenario =
                     ActivityScenario.launch(intent)) {

            onView(withId(R.id.ivProduct))
                    .check(matches(isDisplayed()));

            onView(withId(R.id.tvProductName))
                    .check(matches(isDisplayed()));

            onView(withId(R.id.tvProductPrice))
                    .check(matches(isDisplayed()));

            onView(withId(R.id.tvDescription))
                    .check(matches(isDisplayed()));

            onView(withId(R.id.btnViewSeller))
                    .perform(scrollTo())
                    .check(matches(isDisplayed()));

            onView(withId(R.id.btnPlaceOrder))
                    .perform(scrollTo())
                    .check(matches(isDisplayed()));

            onView(withId(R.id.btnWhatsApp))
                    .perform(scrollTo())
                    .check(matches(isDisplayed()));
        }
    }
}
