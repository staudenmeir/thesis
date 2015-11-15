package edu.tum.cs.crawler.model;

import edu.tum.cs.crawler.TestCase;
import org.junit.Assert;
import org.junit.Test;

public class ItemTest extends TestCase {

    @Test
    public void testHasIdWithFalseResult() {
        Item item = new Item();

        boolean result = item.hasId();

        Assert.assertFalse(result);
    }

    @Test
    public void testHasIdWithTrueResult() {
        Item item = new Item();
        item.setId("");

        boolean result = item.hasId();

        Assert.assertTrue(result);
    }

}
