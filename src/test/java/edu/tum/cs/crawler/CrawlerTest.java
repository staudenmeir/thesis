package edu.tum.cs.crawler;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.query.BindingSet;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;
import org.openrdf.sail.memory.MemoryStore;

import javax.jms.JMSException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class CrawlerTest extends TestCase {

    protected Repository configRepository;

    protected Repository dataRepository;

    protected Repository logRepository;

    protected Repository modelRepository;

    protected ActiveMQConnectionFactory connectionFactory;

    @Before
    public void setUp() throws RepositoryException {
        configRepository = new SailRepository(new MemoryStore());
        dataRepository = new SailRepository(new MemoryStore());
        logRepository = new SailRepository(new MemoryStore());
        modelRepository = new SailRepository(new MemoryStore());
        configRepository.initialize();
        dataRepository.initialize();
        logRepository.initialize();
        modelRepository.initialize();
        connectionFactory = new ActiveMQConnectionFactory("vm://localhost");
    }

    @After
    public void tearDown() throws RepositoryException {
        configRepository.shutDown();
        dataRepository.shutDown();
        logRepository.shutDown();
        modelRepository.shutDown();
    }

    @Test
    public void testCourses() throws RepositoryException, RDFParseException, IOException, QueryEvaluationException,
            MalformedQueryException, InterruptedException, JMSException {
        seed(modelRepository, "courses/model");

        crawler().run();

        // Test total number of entries (... courses).
        // TODO: Assert.assertEquals(, count(dataRepository));

        List<List<Value>> entries = entries(dataRepository);
    }

    @Test
    public void testDonations() throws RepositoryException, RDFParseException, IOException, InterruptedException,
            QueryEvaluationException, MalformedQueryException, JMSException {
        seed(modelRepository, "donations/model");

        crawler().run();

        // Test total number of entries (158 donations).
        Assert.assertEquals(790, count(dataRepository));

        // Test latest donation (01.07.2015).
        String root = "http://www.bundestag.de/";
        String section = root + "bundestag/parteienfinanzierung/fundstellen50000";
        String item = section + "#744b9335814163f1478ec4be4f4b3cb2bb6ce4e2";
        Assert.assertTrue(exists(dataRepository, section, root + "item/donation", new URIImpl(item)));
        Assert.assertTrue(exists(dataRepository, item, root + "property/donation/party", "CDU"));
        Assert.assertTrue(exists(dataRepository, item, root + "property/donation/amount", new BigDecimal("70000.00")));
        String donor = "Herr Dr. Georg Kofler\nHagrainer Straße 15\n83700 Rottach-Egern";
        Assert.assertTrue(exists(dataRepository, item, root + "property/donation/donor", donor));
        Assert.assertTrue(exists(dataRepository, item, root + "property/donation/date", "01.07.2015"));

        // Test donation with seven-figure amount (20.10.2010).
        item = section + "/2010#af804c6bec22d6acb3fd60ccf6075e94924c3a11";
        Assert.assertTrue(exists(dataRepository, item, root + "property/donation/amount", new BigDecimal("1030898.97")));

        // Test donation with annotated amount (26.02.2010).
        item = section + "/2010#14e56f693bf204fe3811f7635b50d36df548deb4";
        Assert.assertTrue(exists(dataRepository, item, root + "property/donation/amount", new BigDecimal("55886.41")));
    }

    @Test
    public void testEvents() throws RepositoryException, RDFParseException, IOException, QueryEvaluationException,
            MalformedQueryException, InterruptedException, JMSException {
        seed(modelRepository, "events/model");

        crawler().run();

        // Test total number of entries (... events).
        // TODO: Assert.assertEquals(, count(dataRepository));

        List<List<Value>> entries = entries(dataRepository);
    }

    @Test
    public void testMovies() throws RepositoryException, RDFParseException, IOException, QueryEvaluationException,
            MalformedQueryException, InterruptedException, JMSException {
        seed(modelRepository, "movies/model");

        crawler().run();

        // Test total number of entries (... movies).
        // TODO: Assert.assertEquals(, count(dataRepository));

        List<List<Value>> entries = entries(dataRepository);
    }

    @Test
    public void testRecipes() throws RepositoryException, RDFParseException, IOException, QueryEvaluationException,
            MalformedQueryException, InterruptedException, JMSException {
        seed(modelRepository, "recipes/model");

        crawler().run();

        // Test total number of entries (... recipes).
        // TODO: Assert.assertEquals(, count(dataRepository));

        List<List<Value>> entries = entries(dataRepository);
    }

    /**
     * Counts repository entries.
     */
    protected int count(Repository repository) throws RepositoryException, MalformedQueryException, QueryEvaluationException {
        TupleQueryResult result = repository(repository).select(
                "SELECT (COUNT( *) as ?count)\n" +
                "WHERE {\n" +
                    "?s ?p ?o\n" +
                "}");
        return Integer.parseInt(result.next().getBinding("count").getValue().stringValue());
    }

    /**
     * Creates crawler.
     */
    protected Crawler crawler() {
        return new Crawler(configRepository, dataRepository, logRepository, modelRepository, connectionFactory);
    }

    /**
     * Checks whether repository entry exists.
     */
    protected boolean exists(Repository repository, String subject, String predicate, BigDecimal object)
            throws RepositoryException, MalformedQueryException, QueryEvaluationException {
        return repository(repository).ask(new URIImpl(subject), new URIImpl(predicate), object);
    }

    /**
     * Checks whether repository entry exists.
     */
    protected boolean exists(Repository repository, String subject, String predicate, String object)
            throws RepositoryException, MalformedQueryException, QueryEvaluationException {
        return repository(repository).ask(new URIImpl(subject), new URIImpl(predicate), object);
    }

    /**
     * Checks whether repository entry exists.
     */
    protected boolean exists(Repository repository, String subject, String predicate, URI object)
            throws RepositoryException, MalformedQueryException, QueryEvaluationException {
        return repository(repository).ask(new URIImpl(subject), new URIImpl(predicate), object);
    }

    /**
     * Selects all entries from repository.
     */
    protected List<List<Value>> entries(Repository repository) throws RepositoryException, MalformedQueryException,
            QueryEvaluationException {
        List<List<Value>> entries = new ArrayList<List<Value>>();
        TupleQueryResult result = repository(repository).select(
                "SELECT ?s ?p ?o\n" +
                "WHERE {\n" +
                    "?s ?p ?o\n" +
                "}");
        while(result.hasNext()) {
            BindingSet bindings = result.next();
            List<Value> entry = new ArrayList<Value>();
            entry.add(bindings.getValue("s"));
            entry.add(bindings.getValue("p"));
            entry.add(bindings.getValue("o"));
            entries.add(entry);
        }
        result.close();
        return entries;
    }

    /**
     * Seeds turtle file to repository.
     */
    protected void seed(Repository repository, String name) throws RepositoryException, IOException, RDFParseException {
        InputStream input = this.getClass().getResourceAsStream("/" + name + ".ttl");
        repository.getConnection().add(input, "", RDFFormat.TURTLE);
    }

}
