package com.sheempower.app;

import com.sheempower.app.models.OrderField;
import com.sheempower.app.utils.CategoryFormConfig;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class CategoryFormConfigTest {

    @Test
    public void cookingCategoryTest() {

        List<OrderField> fields =
                CategoryFormConfig.getFieldsForCategory("Cooking");

        assertNotNull(fields);
        assertEquals(6, fields.size());
    }

    @Test
    public void stitchingCategoryTest() {

        List<OrderField> fields =
                CategoryFormConfig.getFieldsForCategory("Stitching");

        assertNotNull(fields);
        assertEquals(8, fields.size());
    }

    @Test
    public void mehndiCategoryTest() {

        List<OrderField> fields =
                CategoryFormConfig.getFieldsForCategory("Mehndi");

        assertNotNull(fields);
        assertEquals(5, fields.size());
    }

    @Test
    public void makeupCategoryTest() {

        List<OrderField> fields =
                CategoryFormConfig.getFieldsForCategory("Makeup");

        assertNotNull(fields);
        assertEquals(6, fields.size());
    }

    @Test
    public void embroideryCategoryTest() {

        List<OrderField> fields =
                CategoryFormConfig.getFieldsForCategory("Embroidery");

        assertNotNull(fields);
        assertEquals(5, fields.size());
    }

    @Test
    public void nullCategoryTest() {

        List<OrderField> fields =
                CategoryFormConfig.getFieldsForCategory(null);

        assertNotNull(fields);
        assertTrue(fields.isEmpty());
    }

    @Test
    public void invalidCategoryTest() {

        List<OrderField> fields =
                CategoryFormConfig.getFieldsForCategory("InvalidCategory");

        assertNotNull(fields);
        assertTrue(fields.isEmpty());
    }
}