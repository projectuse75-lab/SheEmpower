package com.sheempower.app;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.sheempower.app.utils.SessionManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class SessionManagerTest {

    private SessionManager sessionManager;

    @Before
    public void setUp() {
        Context context =
                InstrumentationRegistry.getInstrumentation().getTargetContext();

        sessionManager = new SessionManager(context);
        sessionManager.logout();
    }

    @Test
    public void saveUserTest() {

        sessionManager.saveUser(
                "12345",
                "seller",
                "Ayesha"
        );

        assertEquals("12345", sessionManager.getUid());
        assertEquals("seller", sessionManager.getRole());
        assertEquals("Ayesha", sessionManager.getName());
    }

    @Test
    public void loginStatusTest() {

        sessionManager.saveUser(
                "12345",
                "seller",
                "Ayesha"
        );

        assertTrue(sessionManager.isLoggedIn());
    }

    @Test
    public void sellerRoleTest() {

        sessionManager.saveUser(
                "12345",
                "seller",
                "Ayesha"
        );

        assertTrue(sessionManager.isSeller());
        assertFalse(sessionManager.isBuyer());
        assertFalse(sessionManager.isAdmin());
    }

    @Test
    public void logoutTest() {

        sessionManager.saveUser(
                "12345",
                "seller",
                "Ayesha"
        );

        sessionManager.logout();

        assertFalse(sessionManager.isLoggedIn());
    }
}