package edu.tum.cs.crawler.repository;

import edu.tum.cs.crawler.TestCase;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openrdf.model.Literal;
import org.openrdf.model.IRI;
import org.openrdf.model.Value;
import org.openrdf.model.vocabulary.XMLSchema;
import org.openrdf.repository.*;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.memory.MemoryStore;

import java.math.BigDecimal;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;
import java.util.List;

public class RepositoryTest extends TestCase {

    protected Repository repository;
    
    protected RepositoryConnection connection;

    protected IRI subject;

    protected IRI predicate;

    protected IRI object;

    protected IRI context;

    protected IRI x;

    protected IRI y;

    @Before
    public void setUp() {
        org.openrdf.repository.Repository repository = new SailRepository(new MemoryStore());
        repository.initialize();
        connection = repository.getConnection();
        this.repository = repository(repository);
        subject = createIRI("s:");
        predicate = createIRI("p:");
        object = createIRI("o:");
        context = createIRI("c:");
        x = createIRI("x:");
        y = createIRI("y:");
    }

    @After
    public void tearDown() {
        connection.close();
    }

    @Test
    public void testAskWithStringObjectAndFalseResult() {
        Literal object = connection.getValueFactory().createLiteral("o", XMLSchema.STRING);
        connection.add(subject, predicate, object);
        connection.add(subject, predicate, object, x);

        boolean result = repository.ask(subject, predicate, "o", XMLSchema.STRING, context);

        Assert.assertFalse(result);
    }

    @Test
    public void testAskWithStringObjectAndTrueResult() {
        Literal object = connection.getValueFactory().createLiteral("o", XMLSchema.STRING);
        connection.add(subject, predicate, object, context);

        boolean result = repository.ask(subject, predicate, "o", XMLSchema.STRING, context);

        Assert.assertTrue(result);
    }

    @Test
    public void testAskWithUriObjectAndFalseResult() {
        connection.add(subject, predicate, object);
        connection.add(subject, predicate, object, x);

        boolean result1 = repository.ask(subject, predicate, object, context);
        boolean result2 = repository.ask(null, predicate, object, context);
        boolean result3 = repository.ask(subject, null, object, context);
        boolean result4 = repository.ask(subject, predicate, null, context);

        Assert.assertFalse(result1 || result2 || result3 || result4);
    }

    @Test
    public void testAskWithUriObjectAndTrueResult() {
        connection.add(subject, predicate, object, context);

        boolean result1 = repository.ask(subject, predicate, object, context);
        boolean result2 = repository.ask(null, predicate, object, context);
        boolean result3 = repository.ask(subject, null, object, context);
        boolean result4 = repository.ask(subject, predicate, null, context);

        Assert.assertTrue(result1 && result2 && result3 && result4);
    }

    @Test
    public void testDeleteWithoutObject() {
        connection.add(subject, predicate, object);
        connection.add(subject, predicate, object, x);
        connection.add(subject, predicate, y, x);

        repository.delete(subject, predicate, null, x);

        Assert.assertEquals(1, connection.size());
        Assert.assertEquals(0, connection.size(x));
    }

    @Test
    public void testDeleteWithObject() {
        connection.add(subject, predicate, object);
        connection.add(subject, predicate, object, x);
        connection.add(subject, predicate, y, x);

        repository.delete(subject, predicate, object, x);

        Assert.assertEquals(2, connection.size());
        Assert.assertEquals(1, connection.size(x));
    }

    @Test
    public void testFindPreviousContextWithoutContexts() {
        IRI context = createIRI("run://12");
        IRI result = repository.findPreviousContext(context);

        Assert.assertEquals("run://0", result.stringValue());
    }

    @Test
    public void testFindPreviousContextWithContexts() {
        connection.add(subject, predicate, object, createIRI("run://12"));
        connection.add(subject, predicate, object, createIRI("run://34"));
        connection.add(subject, predicate, object, createIRI("run://78"));

        IRI context = createIRI("run://56");
        IRI result = repository.findPreviousContext(context);

        Assert.assertEquals("run://34", result.stringValue());
    }

    @Test
    public void testInsertWithDecimal() {
        BigDecimal object = new BigDecimal("1.2");
        repository.insert(subject, predicate, object, context);

        boolean result = repository.ask(subject, predicate, object.toString(), XMLSchema.DECIMAL, context);
        Assert.assertTrue(result);
    }

    @Test
    public void testInsertWithDate() {
        Date date = new Date();
        repository.insert(subject, predicate, date, context);

        Format format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        String object = format.format(date).replaceAll("(\\+\\d{2})", "$1:");
        boolean result = repository.ask(subject, predicate, object, XMLSchema.DATETIME, context);
        Assert.assertTrue(result);
    }

    @Test
    public void testInsertWithDuration() {
        Duration object = Duration.parse("PT1H");
        repository.insert(subject, predicate, object, context);

        boolean result = repository.ask(subject, predicate, object.toString(), XMLSchema.DURATION, context);
        Assert.assertTrue(result);
    }

    @Test
    public void testInsertWithInteger() {
        Integer object = 123;
        repository.insert(subject, predicate, object, context);

        boolean result = repository.ask(subject, predicate, object.toString(), XMLSchema.INT, context);
        Assert.assertTrue(result);
    }

    @Test
    public void testInsertWithLocalDate() {
        LocalDate object = new LocalDate();
        repository.insert(subject, predicate, object, context);

        boolean result = repository.ask(subject, predicate, object.toString(), XMLSchema.DATE, context);
        Assert.assertTrue(result);
    }

    @Test
    public void testInsertWithLocalTime() {
        LocalTime object = new LocalTime();
        repository.insert(subject, predicate, object, context);

        boolean result = repository.ask(subject, predicate, object.toString(), XMLSchema.TIME, context);
        Assert.assertTrue(result);
    }

    @Test
    public void testInsertWithString() {
        String object = "abc";
        repository.insert(subject, predicate, object, context);

        boolean result = repository.ask(subject, predicate, object, XMLSchema.STRING, context);
        Assert.assertTrue(result);
    }

    @Test
    public void testInsertWithUri() {
        repository.insert(subject, predicate, object, context);

        boolean result = repository.ask(subject, predicate, object, context);
        Assert.assertTrue(result);
    }

    @Test
    public void testSelectEntries() {
        connection.add(subject, predicate, object, context);
        connection.add(subject, predicate, object, x);

        List<List<Value>> result = repository.selectEntries(context);

        Assert.assertEquals(1, result.size());
        Assert.assertEquals(subject, result.get(0).get(0));
        Assert.assertEquals(predicate, result.get(0).get(1));
        Assert.assertEquals(object, result.get(0).get(2));
    }

    protected IRI createIRI(String iri) {
        return connection.getValueFactory().createIRI(iri);
    }

}
