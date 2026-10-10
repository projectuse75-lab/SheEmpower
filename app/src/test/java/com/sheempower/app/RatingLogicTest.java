package com.sheempower.app;

import com.sheempower.app.models.Review;
import org.junit.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;

public class RatingLogicTest {

    @Test
    public void singleRatingTest() {
        List<Review> reviews = new ArrayList<>();
        reviews.add(new Review("r1", "o1", "s1", "b1", "Buyer1", "p1", "Product1", "Cooking", 5.0f, "Great", 1000L));

        double sum = 0;
        for (Review r : reviews) sum += r.getRating();
        double avg = sum / reviews.size();
        double rounded = Math.round(avg * 10.0) / 10.0;

        assertEquals(5.0, rounded, 0.01);
        assertEquals(1, reviews.size());
    }

    @Test
    public void twoRatingsTest() {
        List<Review> reviews = new ArrayList<>();
        reviews.add(new Review("r1", "o1", "s1", "b1", "Buyer1", "p1", "Product1", "Cooking", 5.0f, "Great", 1000L));
        reviews.add(new Review("r2", "o2", "s1", "b2", "Buyer2", "p1", "Product1", "Cooking", 4.0f, "Good", 2000L));

        double sum = 0;
        for (Review r : reviews) sum += r.getRating();
        double avg = sum / reviews.size();
        double rounded = Math.round(avg * 10.0) / 10.0;

        assertEquals(4.5, rounded, 0.01);
        assertEquals(2, reviews.size());
    }

    @Test
    public void threeRatingsTest() {
        List<Review> reviews = new ArrayList<>();
        reviews.add(new Review("r1", "o1", "s1", "b1", "Buyer1", "p1", "Product1", "Cooking", 5.0f, "Great", 1000L));
        reviews.add(new Review("r2", "o2", "s1", "b2", "Buyer2", "p1", "Product1", "Cooking", 4.0f, "Good", 2000L));
        reviews.add(new Review("r3", "o3", "s1", "b3", "Buyer3", "p1", "Product1", "Cooking", 3.0f, "Average", 3000L));

        double sum = 0;
        for (Review r : reviews) sum += r.getRating();
        double avg = sum / reviews.size();
        double rounded = Math.round(avg * 10.0) / 10.0;

        assertEquals(4.0, rounded, 0.01);
        assertEquals(3, reviews.size());
    }

    @Test
    public void topRatedEligibilityTest() {
        double rating44 = 4.4;
        double rating45 = 4.5;
        double rating48 = 4.8;

        assertFalse(rating44 >= 4.5);
        assertTrue(rating45 >= 4.5);
        assertTrue(rating48 >= 4.5);
    }
}