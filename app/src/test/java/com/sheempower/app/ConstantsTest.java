package com.sheempower.app;

import com.sheempower.app.utils.Constants;

import org.junit.Test;

import static org.junit.Assert.*;

public class ConstantsTest {

    @Test
    public void rolesTest() {
        assertEquals("buyer", Constants.ROLE_BUYER);
        assertEquals("seller", Constants.ROLE_SELLER);
        assertEquals("admin", Constants.ROLE_ADMIN);
    }

    @Test
    public void categoriesTest() {
        assertEquals(5, Constants.CATEGORIES.length);
        assertEquals("Cooking", Constants.CATEGORIES[0]);
        assertEquals("Stitching", Constants.CATEGORIES[1]);
        assertEquals("Mehndi", Constants.CATEGORIES[2]);
        assertEquals("Makeup", Constants.CATEGORIES[3]);
        assertEquals("Embroidery", Constants.CATEGORIES[4]);
    }

    @Test
    public void urduLabelTest() {
        assertEquals("کھانا پکانا",
                Constants.getUrduLabel("Cooking"));

        assertEquals("سلائی",
                Constants.getUrduLabel("Stitching"));

        assertEquals("مہندی",
                Constants.getUrduLabel("Mehndi"));

        assertEquals("میک اپ",
                Constants.getUrduLabel("Makeup"));

        assertEquals("کڑھائی",
                Constants.getUrduLabel("Embroidery"));
    }

    @Test
    public void unknownCategoryTest() {
        assertEquals("Unknown",
                Constants.getUrduLabel("Unknown"));
    }

    @Test
    public void travelTypeTest() {
        assertTrue(Constants.isTravelType("Mehndi"));
        assertTrue(Constants.isTravelType("Makeup"));

        assertFalse(Constants.isTravelType("Cooking"));
        assertFalse(Constants.isTravelType("Stitching"));
        assertFalse(Constants.isTravelType("Embroidery"));
    }

    @Test
    public void approvalTest() {
        assertTrue(Constants.isApproved(null));
        assertTrue(Constants.isApproved(""));
        assertTrue(Constants.isApproved("verified"));
        assertTrue(Constants.isApproved("approved"));
        assertTrue(Constants.isApproved("active"));

        assertFalse(Constants.isApproved("pending"));
        assertFalse(Constants.isApproved("rejected"));
    }

    @Test
    public void statusLabelTest() {
        assertEquals(
                "زیر التواء / Pending",
                Constants.getStatusLabel(
                        Constants.STATUS_PENDING,
                        Constants.DELIVERY_METHOD_DELIVERY
                )
        );

        assertEquals(
                "کام جاری ہے / In Progress",
                Constants.getStatusLabel(
                        Constants.STATUS_IN_PROGRESS,
                        Constants.DELIVERY_METHOD_DELIVERY
                )
        );
    }

    @Test
    public void completedDeliveryStatusTest() {
        assertEquals(
                "مکمل ہو گیا / Completed",
                Constants.getStatusLabel(
                        Constants.STATUS_COMPLETED,
                        Constants.DELIVERY_METHOD_DELIVERY
                )
        );
    }

    @Test
    public void completedPickupStatusTest() {
        assertEquals(
                "مکمل ہو گیا / Completed",
                Constants.getStatusLabel(
                        Constants.STATUS_COMPLETED,
                        Constants.DELIVERY_METHOD_PICKUP
                )
        );
    }
}
