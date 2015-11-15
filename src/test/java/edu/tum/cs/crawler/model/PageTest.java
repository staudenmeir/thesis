package edu.tum.cs.crawler.model;

import edu.tum.cs.crawler.TestCase;
import org.junit.Assert;
import org.junit.Test;

public class PageTest extends TestCase {

    @Test
    public void testHasScrollWithFalseResult() {
        Page page = new ObjectPage();

        boolean result = page.hasScroll();

        Assert.assertFalse(result);
    }

    @Test
    public void testHasScrollWithTrueResult() {
        Page page = new ObjectPage();
        page.setScroll(0);

        boolean result = page.hasScroll();

        Assert.assertTrue(result);
    }

}
