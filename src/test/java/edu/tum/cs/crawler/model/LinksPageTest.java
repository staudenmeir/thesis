package edu.tum.cs.crawler.model;

import edu.tum.cs.crawler.TestCase;
import org.junit.Assert;
import org.junit.Test;

public class LinksPageTest extends TestCase {

    @Test
    public void testHasNextWithFalseResult() {
        LinksPage linksPage = new LinksPage();

        boolean result = linksPage.hasNext();

        Assert.assertFalse(result);
    }

    @Test
    public void testHasScrollWithTrueResult() {
        LinksPage linksPage = new LinksPage();
        linksPage.setNext("");

        boolean result = linksPage.hasNext();

        Assert.assertTrue(result);
    }

    @Test
    public void testHasWaitWithFalseResult() {
        LinksPage linksPage = new LinksPage();

        boolean result = linksPage.hasWait();

        Assert.assertFalse(result);
    }

    @Test
    public void testHasWaitWithTrueResult() {
        LinksPage linksPage = new LinksPage();
        linksPage.setWait(0);

        boolean result = linksPage.hasWait();

        Assert.assertTrue(result);
    }

}
