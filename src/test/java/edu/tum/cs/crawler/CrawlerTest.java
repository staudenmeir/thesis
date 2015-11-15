package edu.tum.cs.crawler;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openrdf.model.IRI;
import org.openrdf.model.Value;
import org.openrdf.repository.Repository;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.sail.memory.MemoryStore;
import org.openrdf.sail.memory.model.MemIRI;
import org.openrdf.sail.memory.model.MemLiteral;

import javax.jms.JMSException;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class CrawlerTest extends TestCase {

    protected Repository dataRepository;

    protected Repository diffRepository;

    protected Repository modelRepository;

    protected ActiveMQConnectionFactory connectionFactory;

    @Before
    public void setUp() {
        dataRepository = new SailRepository(new MemoryStore());
        dataRepository.initialize();
        diffRepository = new SailRepository(new MemoryStore());
        diffRepository.initialize();
        modelRepository = new SailRepository(new MemoryStore());
        modelRepository.initialize();
        connectionFactory = new ActiveMQConnectionFactory("vm://localhost");
    }

    @After
    public void tearDown() {
        dataRepository.getConnection().close();
        diffRepository.getConnection().close();
        modelRepository.getConnection().close();
    }

    /**
     * Tests bundestag.de.
     */
    @Test
    public void testWebsite1() throws JMSException, InterruptedException, IOException {
        seed(modelRepository, "bundestag.de/model");

        Crawler crawler = crawler();
        IRI context = crawler.run();

        // question 1
        testExtracts(crawler, "bundestag.de/1/extract");

        // question 2
        testResult(dataRepository, context, "bundestag.de/1/data");

        // question 3
        testResult(diffRepository, context, "bundestag.de/1/diff");
        testResultNot(diffRepository, context, "bundestag.de/1/diff.not");

        seed(dataRepository, "bundestag.de/2/data", context);

        context = crawler.run();

        testResult(diffRepository, context, "bundestag.de/2/diff");
        testResultNot(diffRepository, context, "bundestag.de/2/diff.not");

    }

    /**
     * Tests campus.tum.de.
     */
    @Test
    public void testWebsite2() throws JMSException, InterruptedException, IOException {
        seed(modelRepository, "campus.tum.de/model");

        Crawler crawler = crawler();
        IRI context = crawler.run();

        // question 1
        testExtracts(crawler, "campus.tum.de/1/extract");

        // question 2
        testResult(dataRepository, context, "campus.tum.de/1/data");

        // question 3
        testResult(diffRepository, context, "campus.tum.de/1/diff");
        testResultNot(diffRepository, context, "campus.tum.de/1/diff.not");

        IRI subject = iri("https://campus.tum.de/tumonline/!wbTermin.wbEdit?pTerminNr=884835648");
        repository(dataRepository).delete(subject, iri("property://appointment/start"), null, context);
        seed(dataRepository, "campus.tum.de/2/data", context);

        context = crawler.run();

        testResult(diffRepository, context, "campus.tum.de/2/diff");
        testResultNot(diffRepository, context, "campus.tum.de/2/diff.not");
    }

    /**
     * Tests chefkoch.de.
     */
    @Test
    public void testWebsite3() throws JMSException, InterruptedException, IOException {
        seed(modelRepository, "chefkoch.de/model");

        Crawler crawler = crawler();
        IRI context = crawler.run();

        // question 1
        testExtracts(crawler, "chefkoch.de/1/extract");

        // question 2
        testResult(dataRepository, context, "chefkoch.de/1/data");

        // question 3
        testResult(diffRepository, context, "chefkoch.de/1/diff");
        testResultNot(diffRepository, context, "chefkoch.de/1/diff.not");

        IRI subject = iri("http://www.chefkoch.de/rezepte/372611122991708/Donauwelle-Konditorenart.html");
        repository(dataRepository).delete(subject, iri("property://recipe/rating"), null, context);
        seed(dataRepository, "chefkoch.de/2/data", context);

        context = crawler.run();

        testResult(diffRepository, context, "chefkoch.de/2/diff");
        testResultNot(diffRepository, context, "chefkoch.de/2/diff.not");
    }

    /**
     * Tests imdb.com.
     */
    @Test
    public void testWebsite4() throws JMSException, InterruptedException, IOException {
        seed(modelRepository, "imdb.com/model");

        Crawler crawler = crawler();
        IRI context = crawler.run();

        // question 1
        testExtracts(crawler, "imdb.com/1/extract");

        // question 2
        testResult(dataRepository, context, "imdb.com/1/data");

        // question 3
        testResult(diffRepository, context, "imdb.com/1/diff");
        testResultNot(diffRepository, context, "imdb.com/1/diff.not");

        IRI subject = iri("http://www.imdb.com/title/tt0903747/");
        repository(dataRepository).delete(subject, iri("property://series/rating"), null, context);
        seed(dataRepository, "imdb.com/2/data", context);

        context = crawler.run();

        testResult(diffRepository, context, "imdb.com/2/diff");
        testResultNot(diffRepository, context, "imdb.com/2/diff.not");
    }

    /**
     * Tests muenchen.de.
     */
    @Test
    public void testWebsite5() throws JMSException, InterruptedException, IOException {
        seed(modelRepository, "muenchen.de/model");

        Crawler crawler = crawler();
        IRI context = crawler.run();

        // question 1
        testExtracts(crawler, "muenchen.de/1/extract");

        // question 2
        testResult(dataRepository, context, "muenchen.de/1/data");

        // question 3
        testResult(diffRepository, context, "muenchen.de/1/diff");
        testResultNot(diffRepository, context, "muenchen.de/1/diff.not");

        IRI subject = iri("http://www.muenchen.de/veranstaltungen/orte/140242.html");
        repository(dataRepository).delete(subject, iri("property://location/address"), null, context);
        seed(dataRepository, "muenchen.de/2/data", context);

        context = crawler.run();

        testResult(diffRepository, context, "muenchen.de/2/diff");
        testResultNot(diffRepository, context, "muenchen.de/2/diff.not");
    }

    /**
     * Creates crawler instance.
     */
    protected Crawler crawler() {
        return new Crawler(dataRepository, diffRepository, modelRepository, connectionFactory);
    }

    /**
     * Creates iri.
     */
    protected IRI iri(String iri) {
        return dataRepository.getValueFactory().createIRI(iri);
    }

    /**
     * Seeds N3 file to repository.
     */
    protected void seed(Repository repository, String name) throws IOException {
        seed(repository, name, null);
    }

    /**
     * Seeds N3 file to repository.
     */
    protected void seed(Repository repository, String name, IRI context) throws IOException {
        InputStream input = this.getClass().getResourceAsStream("/" + name + ".n3");
        repository.getConnection().add(input, "", RDFFormat.N3, context);
    }

    /**
     * Tests whether extracts contains entries.
     */
    protected void testExtracts(Crawler crawler, String name) throws IOException {
        InputStream input = this.getClass().getResourceAsStream("/" + name + ".json");
        JSONTokener jsonTokener = new JSONTokener(input);
        JSONObject entries = (JSONObject) jsonTokener.nextValue();
        for(Iterator<String> iterator = entries.keys(); iterator.hasNext();) {
            String uri = iterator.next();
            Map<String, String> actual = crawler.extracts.get(uri);
            Map<String, String> expected = new HashMap<String, String>();
            JSONObject data = entries.getJSONObject(uri);
            for(Iterator<String> iterator2 = data.keys(); iterator2.hasNext();) {
                String key = iterator2.next();
                expected.put(key, data.getString(key));
            }
            boolean exists = ((actual != null) && actual.equals(expected));
            Assert.assertTrue(uri, exists);
        }
    }

    /**
     * Tests whether repository graph contains entries.
     */
    protected void testResult(Repository repository, IRI context, String seed) throws IOException {
        testResult(repository, context, seed, false);
    }

    /**
     * Tests whether repository graph contains entries.
     */
    protected void testResult(Repository repository, IRI context, String seed, boolean not) throws IOException {
        Repository expected = new SailRepository(new MemoryStore());
        expected.initialize();
        seed(expected, seed);
        edu.tum.cs.crawler.repository.Repository actual = repository(repository);
        List<List<Value>> entries = repository(expected).selectEntries();
        for(List<Value> entry : entries) {
            boolean exists;
            IRI subject = (IRI) entry.get(0);
            IRI predicate = (IRI) entry.get(1);
            if(entry.get(2) instanceof MemIRI) {
                exists = actual.ask(subject, predicate, (IRI) entry.get(2), context);
            } else {
                String object = ((MemLiteral) entry.get(2)).getLabel();
                IRI type = ((MemLiteral) entry.get(2)).getDatatype();
                exists = actual.ask(subject, predicate, object, type, context);
            }
            Assert.assertTrue(entry.toString(), exists == !not);
        }
        expected.getConnection().close();
    }

    /**
     * Tests whether repository graph doesn't contain entries.
     */
    protected void testResultNot(Repository repository, IRI context, String seed) throws IOException {
        testResult(repository, context, seed, true);
    }

}