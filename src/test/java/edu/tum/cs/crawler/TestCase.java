package edu.tum.cs.crawler;

import edu.tum.cs.crawler.repository.Repository;

abstract public class TestCase {

    protected Repository repository(org.openrdf.repository.Repository repository) {
        return new Repository(repository);
    }

}
