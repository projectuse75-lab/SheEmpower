package com.sheempower.app;

import com.sheempower.app.models.Review;
import org.junit.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;

public class ReviewFilterTest {

    @Test
    public void productSpecificFilterTest() {
        List<Review> allReviews = new ArrayList<>();
        allReviews.add(new Review("r1", "o1", "s1", "b1", "Buyer1", "prod_biryani", "Biryani", "Cooking", 5.0f, "Yummy", 1000L));
        allReviews.add(new Review("r2", "o2", "s1", "b2", "Buyer2", "prod_biryani", "Biryani", "Cooking", 4.0f, "Tasty", 2000L));
        allReviews.add(new Review("r3", "o3", "s1", "b3", "Buyer3", "prod_kurti", "Kurti", "Stitching", 5.0f, "Nice", 3000L));

        List<Review> biryaniReviews = new ArrayList<>();
        List<Review> kurtiReviews = new ArrayList<>();

        for (Review r : allReviews) {
            if ("prod_biryani".equalsIgnoreCase(r.getProductId())) {
                biryaniReviews.add(r);
            }
            if ("prod_kurti".equalsIgnoreCase(r.getProductId())) {
                kurtiReviews.add(r);
            }
        }

        assertEquals(2, biryaniReviews.size());
        assertEquals(1, kurtiReviews.size());
        assertEquals("prod_biryani", biryaniReviews.get(0).getProductId());
        assertEquals("prod_kurti", kurtiReviews.get(0).getProductId());
    }

    @Test
    public void sellerSpecificFilterTest() {
        List<Review> allReviews = new ArrayList<>();
        allReviews.add(new Review("r1", "o1", "seller_A", "b1", "Buyer1", "p1", "Biryani", "Cooking", 5.0f, "Great", 1000L));
        allReviews.add(new Review("r2", "o2", "seller_A", "b2", "Buyer2", "p2", "Kurti", "Stitching", 4.0f, "Good", 2000L));
        allReviews.add(new Review("r3", "o3", "seller_B", "b3", "Buyer3", "p3", "Mehndi", "Mehndi", 5.0f, "Beautiful", 3000L));

        List<Review> sellerAReviews = new ArrayList<>();
        List<Review> sellerBReviews = new ArrayList<>();

        for (Review r : allReviews) {
            if ("seller_A".equalsIgnoreCase(r.getSellerId())) sellerAReviews.add(r);
            if ("seller_B".equalsIgnoreCase(r.getSellerId())) sellerBReviews.add(r);
        }

        assertEquals(2, sellerAReviews.size());
        assertEquals(1, sellerBReviews.size());
    }

    @Test
    public void buyerMyReviewsFilterTest() {
        List<Review> allReviews = new ArrayList<>();
        allReviews.add(new Review("r1", "o1", "s1", "buyer_ayesha", "Ayesha", "p1", "Biryani", "Cooking", 5.0f, "Great", 1000L));
        allReviews.add(new Review("r2", "o2", "s2", "buyer_ayesha", "Ayesha", "p2", "Kurti", "Stitching", 4.0f, "Good", 2000L));
        allReviews.add(new Review("r3", "o3", "s1", "buyer_fatima", "Fatima", "p1", "Biryani", "Cooking", 5.0f, "Delicious", 3000L));

        List<Review> ayeshaReviews = new ArrayList<>();
        for (Review r : allReviews) {
            if ("buyer_ayesha".equalsIgnoreCase(r.getBuyerId())) ayeshaReviews.add(r);
        }

        assertEquals(2, ayeshaReviews.size());
        assertEquals("buyer_ayesha", ayeshaReviews.get(0).getBuyerId());
        assertEquals("buyer_ayesha", ayeshaReviews.get(1).getBuyerId());
    }

    @Test
    public void categoryFilterTest() {
        List<Review> sellerReviews = new ArrayList<>();
        sellerReviews.add(new Review("r1", "o1", "s1", "b1", "Buyer1", "p1", "Biryani", "Cooking", 5.0f, "Tasty", 1000L));
        sellerReviews.add(new Review("r2", "o2", "s1", "b2", "Buyer2", "p2", "Kurti", "Stitching", 4.0f, "Nice", 2000L));
        sellerReviews.add(new Review("r3", "o3", "s1", "b3", "Buyer3", "p3", "Mehndi", "Mehndi", 5.0f, "Lovely", 3000L));

        List<Review> cookingList = new ArrayList<>();
        List<Review> stitchingList = new ArrayList<>();

        for (Review r : sellerReviews) {
            if ("Cooking".equalsIgnoreCase(r.getCategory())) cookingList.add(r);
            if ("Stitching".equalsIgnoreCase(r.getCategory())) stitchingList.add(r);
        }

        assertEquals(1, cookingList.size());
        assertEquals(1, stitchingList.size());
        assertEquals("Cooking", cookingList.get(0).getCategory());
        assertEquals("Stitching", stitchingList.get(0).getCategory());
    }
}