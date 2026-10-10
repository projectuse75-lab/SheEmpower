package com.sheempower.app;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.sheempower.app.activities.ProfileActivity;
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
public class ProfileActivityTest {

    @Before
    public void setUp() {
        SessionManager sessionManager =
                new SessionManager(
                        androidx.test.core.app.ApplicationProvider
                                .getApplicationContext()
                );

        sessionManager.saveUser(
                "test_user_123",
                "buyer",
                "Test User"
        );
    }

    @Test
    public void profileScreenDisplayed() {

        try (ActivityScenario<ProfileActivity> scenario =
                     ActivityScenario.launch(ProfileActivity.class)) {

            onView(withId(R.id.ivProfilePic))
                    .check(matches(isDisplayed()));

            onView(withId(R.id.btnChangePic))
                    .check(matches(isDisplayed()));

            onView(withId(R.id.etFullName))
                    .check(matches(isDisplayed()));

            onView(withId(R.id.etPhone))
                    .check(matches(isDisplayed()));

            onView(withId(R.id.btnSave))
                    .perform(scrollTo())
                    .check(matches(isDisplayed()));

            onView(withId(R.id.btnMyOrders))
                    .perform(scrollTo())
                    .check(matches(isDisplayed()));

            onView(withId(R.id.btnMyReviews))
                    .perform(scrollTo())
                    .check(matches(isDisplayed()));

            onView(withId(R.id.btnLogout))
                    .perform(scrollTo())
                    .check(matches(isDisplayed()));

            // bottom navigation is not inside ScrollView
            onView(withId(R.id.bottomNav))
                    .check(matches(isDisplayed()));
        }
    }
}