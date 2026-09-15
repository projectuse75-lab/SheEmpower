package com.sheempower.app;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.sheempower.app.activities.MyOrdersActivity;
import com.sheempower.app.utils.Constants;
import com.sheempower.app.utils.SessionManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

@RunWith(AndroidJUnit4.class)
public class MyOrdersActivityTest {

    @Before
    public void setUp() {

        SessionManager sessionManager =
                new SessionManager(
                        ApplicationProvider
                                .getApplicationContext()
                );

        try {
            Tasks.await(FirebaseAuth.getInstance().signInWithEmailAndPassword(Constants.ADMIN_EMAIL, Constants.ADMIN_PASSWORD));
        } catch (Exception e) {
            try {
                Tasks.await(FirebaseAuth.getInstance().createUserWithEmailAndPassword(Constants.ADMIN_EMAIL, Constants.ADMIN_PASSWORD));
            } catch (Exception ex) {
                // Ignore
            }
        }

        sessionManager.saveUser(
                "test_buyer_123",
                "buyer",
                "Test Buyer"
        );
    }

    @Test
    public void myOrdersScreenDisplayed() {

        try (ActivityScenario<MyOrdersActivity> scenario =
                     ActivityScenario.launch(MyOrdersActivity.class)) {

            onView(withId(R.id.rvOrders))
                    .check(matches(isDisplayed()));

            onView(withId(R.id.bottomNav))
                    .check(matches(isDisplayed()));
        }
    }
}