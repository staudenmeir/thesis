package edu.tum.cs.crawler.model;

import edu.tum.cs.crawler.TestCase;
import org.junit.Assert;
import org.junit.Test;

public class ObjectsPageTest extends TestCase {

    @Test
    public void testHasNextWithFalseResult() {
        ObjectsPage objectsPage = new ObjectsPage();

        boolean result = objectsPage.hasNext();

        Assert.assertFalse(result);
    }

    @Test
    public void testHasScrollWithTrueResult() {
        ObjectsPage objectsPage = new ObjectsPage();
        objectsPage.setNext("");

        boolean result = objectsPage.hasNext();

        Assert.assertTrue(result);
    }

    @Test
    public void testHasWaitWithFalseResult() {
        ObjectsPage objectsPage = new ObjectsPage();

        boolean result = objectsPage.hasWait();

        Assert.assertFalse(result);
    }

    @Test
    public void testHasWaitWithTrueResult() {
        ObjectsPage objectsPage = new ObjectsPage();
        objectsPage.setWait(0);

        boolean result = objectsPage.hasWait();

        Assert.assertTrue(result);
    }

}
