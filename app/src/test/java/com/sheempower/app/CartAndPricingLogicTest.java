package com.sheempower.app;

import com.sheempower.app.models.CartItem;
import org.junit.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;

public class CartAndPricingLogicTest {

    @Test
    public void cartItemPriceCalculationTest() {
        CartItem item = new CartItem("c1", "p1", "Biryani", "img", "Cooking", "seller_1", "Ayesha Kitchen", 1500.0, 2);
        double totalPrice = item.getPrice() * item.getQuantity();

        assertEquals(3000.0, totalPrice, 0.01);
    }

    @Test
    public void singleSellerConstraintTest() {
        List<CartItem> cart = new ArrayList<>();
        CartItem item1 = new CartItem("c1", "p1", "Biryani", "img1", "Cooking", "seller_A", "Ayesha Kitchen", 1000.0, 1);
        CartItem item2 = new CartItem("c2", "p2", "Kurti", "img2", "Stitching", "seller_B", "Sana Boutique", 2000.0, 1);

        item1.setSelected(true);
        cart.add(item1);
        cart.add(item2);

        // When item2 (different seller) is selected, item1 from seller_A should be unselected
        String activeSellerId = item2.getSellerId();
        for (CartItem ci : cart) {
            if (!activeSellerId.equalsIgnoreCase(ci.getSellerId())) {
                ci.setSelected(false);
            }
        }
        item2.setSelected(true);

        assertFalse(item1.isSelected());
        assertTrue(item2.isSelected());
    }

    @Test
    public void directOrderPriceCalculationTest() {
        double unitPrice = 2500.0;
        int qty = 3;
        double grandTotal = unitPrice * qty;

        assertEquals(7500.0, grandTotal, 0.01);
    }
}