package com.sheempower.app.utils;

import android.content.Context;

public class DemoDataSeeder {

    /** Aapne khud Firebase mein JSON import karke data daal diya hai,
     *  isliye yeh function ab kuch seed nahi karta — sirf yahan rakha hai
     *  taake SplashActivity ka call crash na kare. App seedha Firebase
     *  ke maujooda data (users/sellers/products) ko use karegi. */
    public static void seedIfNeeded(Context ctx) {
        // Intentionally empty
    }

    public static void clearDemoDataOnFirstRealUse(Context ctx) {
        // Intentionally empty — aapka data permanent rahega, kabhi delete nahi hoga
    }
}