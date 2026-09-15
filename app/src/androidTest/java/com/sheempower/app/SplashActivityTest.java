package com.sheempower.app;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.sheempower.app.activities.SplashActivity;

import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

@RunWith(AndroidJUnit4.class)
public class SplashActivityTest {

    @Test
    public void splashScreenDisplayed() {

        try (ActivityScenario<SplashActivity> scenario =
                     ActivityScenario.launch(SplashActivity.class)) {

            onView(withId(R.id.splashScreen))
                    .check(matches(isDisplayed()));
        }
    }
}