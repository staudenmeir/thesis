package edu.tum.cs.crawler.model;

import edu.tum.cs.crawler.TestCase;
import org.junit.Assert;
import org.junit.Test;

public class PropertyTest extends TestCase {

    @Test
    public void testHasAttributeWithFalseResult() {
        Property property = new Property();

        boolean result = property.hasAttribute();

        Assert.assertFalse(result);
    }

    @Test
    public void testHasScrollWithTrueResult() {
        Property property = new Property();
        property.setAttribute("");

        boolean result = property.hasAttribute();

        Assert.assertTrue(result);
    }

    @Test
    public void testHasPatternWithFalseResult() {
        Property property = new Property();

        boolean result = property.hasPattern();

        Assert.assertFalse(result);
    }

    @Test
    public void testHasPatternWithTrueResult() {
        Property property = new Property();
        property.setPattern("");

        boolean result = property.hasPattern();

        Assert.assertTrue(result);
    }

}
