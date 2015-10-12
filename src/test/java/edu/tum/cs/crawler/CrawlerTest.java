package edu.tum.cs.crawler;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.vocabulary.XMLSchema;
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
import java.time.Duration;
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
        configRepository.getConnection().close();
        dataRepository.getConnection().close();
        logRepository.getConnection().close();
        modelRepository.getConnection().close();
    }

    @Test
    public void testCourses() throws RepositoryException, RDFParseException, IOException, QueryEvaluationException,
            MalformedQueryException, InterruptedException, JMSException {
        seed(modelRepository, "courses");

        crawler().run();

        // Test faculty.
        String root = "https://campus.tum.de/";
        String section = root + "tumonline/wborg.display_virtuell?PORGNR=1&PORGTYP=28567";
        String faculty = root + "tumonline/organisationen.display?corg=14189";
        Assert.assertTrue(exists(dataRepository, section, root + "item/faculty", new URIImpl(faculty)));
        Assert.assertTrue(exists(dataRepository, faculty, root + "property/faculty/label", "Fakultät für Informatik"));

        // Test course.
        String course = root + "tumonline/lv.detail?clvnr=950209844";
        Assert.assertTrue(exists(dataRepository, faculty, root + "item/course", new URIImpl(course)));
        Assert.assertTrue(exists(dataRepository, course, root + "property/course/label", "Advanced Programming (IN1503)"));
        Assert.assertTrue(exists(dataRepository, course, root + "property/course/number", "240979466"));
        Assert.assertTrue(exists(dataRepository, course, root + "property/course/type", "Vorlesung"));

        // Test group.
        String group = root + "tumonline/wbTvw_List.lehrveranstaltung?pStpSpNr=950209844#653b00ceef259a276a8427bf394924b6f9545907";
        Assert.assertTrue(exists(dataRepository, course, root + "item/group", new URIImpl(group)));
        Assert.assertTrue(exists(dataRepository, group, root + "property/group/label", "Standardgruppe"));

        // Test appointment.
        String appointment = root + "tumonline/!wbTermin.wbEdit?pTerminNr=884751793";
        Assert.assertTrue(exists(dataRepository, group, root + "item/appointment", new URIImpl(appointment)));
        Assert.assertTrue(exists(dataRepository, appointment, root + "property/appointment/date", "2015-10-15", XMLSchema.DATE));
        Assert.assertTrue(exists(dataRepository, appointment, root + "property/appointment/start", "16:00:00.000", XMLSchema.TIME));
        Assert.assertTrue(exists(dataRepository, appointment, root + "property/appointment/end", "18:00:00.000", XMLSchema.TIME));

        // Test location.
        String location = root + "tumonline/ris.einzelRaum?raumKey=31250";
        Assert.assertTrue(exists(dataRepository, appointment, root + "item/location", new URIImpl(location)));
        String label = "CH 26411, Emil-Erlenmeyer-Hörsaal (5406.01.641A)";
        Assert.assertTrue(exists(dataRepository, location, root + "property/location/label", label));

        List<List<Value>> entries = entries(dataRepository);
    }

    @Test
    public void testDonations() throws RepositoryException, RDFParseException, IOException, InterruptedException,
            QueryEvaluationException, MalformedQueryException, JMSException {
        seed(modelRepository, "donations");

        crawler().run();

        // Test total number of entries (161 donations).
        Assert.assertEquals(805, count(dataRepository));

        // Test donation (06.10.2015).
        String root = "http://www.bundestag.de/";
        String section = root + "bundestag/parteienfinanzierung/fundstellen50000";
        String donation = section + "#98c8eb88cde45de9a2db558c9c28a6d641c76310";
        Assert.assertTrue(exists(dataRepository, section, root + "item/donation", new URIImpl(donation)));
        Assert.assertTrue(exists(dataRepository, donation, root + "property/donation/party", "CDU"));
        Assert.assertTrue(exists(dataRepository, donation, root + "property/donation/amount", new BigDecimal("90000.00")));
        String donor = "Evonik Industries AG\nRellinghauser Straße 1 - 11\n45128 Essen";
        Assert.assertTrue(exists(dataRepository, donation, root + "property/donation/donor", donor));
        Assert.assertTrue(exists(dataRepository, donation, root + "property/donation/date", "06.10.2015"));

        // Test donation with seven-figure amount (20.10.2010).
        donation = section + "/2010#af804c6bec22d6acb3fd60ccf6075e94924c3a11";
        Assert.assertTrue(exists(dataRepository, donation, root + "property/donation/amount", new BigDecimal("1030898.97")));

        // Test donation with annotated amount (26.02.2010).
        donation = section + "/2010#14e56f693bf204fe3811f7635b50d36df548deb4";
        Assert.assertTrue(exists(dataRepository, donation, root + "property/donation/amount", new BigDecimal("55886.41")));

        List<List<Value>> entries = entries(dataRepository);
    }

    @Test
    public void testEvents() throws RepositoryException, RDFParseException, IOException, QueryEvaluationException,
            MalformedQueryException, InterruptedException, JMSException {
        seed(modelRepository, "events");

        crawler().run();

        // Test event.
        String root = "http://www.muenchen.de/";
        String section = root + "veranstaltungen/events/comedy.html";
        String event = root + "veranstaltungen/event/4917.html";
        Assert.assertTrue(exists(dataRepository, section, root + "item/event", new URIImpl(event)));
        Assert.assertTrue(exists(dataRepository, event, root + "property/event/label", "Alfons: Wiedersehen macht Freunde"));

        // Test appointment.
        String appointment = event + "#1edae5f8f996352f12231362c35d70cb7fd780ee";
        Assert.assertTrue(exists(dataRepository, event, root + "item/appointment", new URIImpl(appointment)));
        String start = "2015-12-04T20:00:00.000+01:00";
        Assert.assertTrue(exists(dataRepository, appointment, root + "property/appointment/start", start, XMLSchema.DATETIME));

        // Tests location.
        String location = root + "veranstaltungen/orte/139986.html";
        Assert.assertTrue(exists(dataRepository, appointment, root + "item/location", new URIImpl(location)));
        Assert.assertTrue(exists(dataRepository, location, root + "property/location/label", "Münchner Volkstheater"));
        Assert.assertTrue(exists(dataRepository, location, root + "property/location/address", "Brienner Str. 50\n80333 München"));

        List<List<Value>> entries = entries(dataRepository);
    }

    @Test
    public void testMovies() throws RepositoryException, RDFParseException, IOException, QueryEvaluationException,
            MalformedQueryException, InterruptedException, JMSException {
        seed(modelRepository, "movies");

        crawler().run();

        // Test movie.
        String root = "http://www.imdb.com/";
        String section = root + "chart/top";
        String movie = root + "title/tt0111161/";
        Assert.assertTrue(exists(dataRepository, section, root + "item/movie", new URIImpl(movie)));
        Assert.assertTrue(exists(dataRepository, movie, root + "property/movie/label", "Die Verurteilten"));
        Assert.assertTrue(exists(dataRepository, movie, root + "property/movie/year", 1994));
        Assert.assertTrue(exists(dataRepository, movie, root + "property/movie/duration", Duration.parse("PT2H22M")));
        Assert.assertTrue(exists(dataRepository, movie, root + "property/movie/rating", new BigDecimal("9.3")));
        String description = "Two imprisoned men bond over a number of years, finding solace and eventual redemption through acts of common decency.";
        Assert.assertTrue(exists(dataRepository, movie, root + "property/movie/description", description));
        String poster = "http://ia.media-imdb.com/images/M/MV5BODU4MjU4NjIwNl5BMl5BanBnXkFtZTgwMDU2MjEyMDE@._V1_SX214_AL_.jpg";
        Assert.assertTrue(exists(dataRepository, movie, root + "property/movie/poster", new URIImpl(poster)));

        // Test director.
        String director = root + "name/nm0001104/";
        Assert.assertTrue(exists(dataRepository, movie, root + "item/director", new URIImpl(director)));
        Assert.assertTrue(exists(dataRepository, director, root + "property/director/label", "Frank Darabont"));

        // Test actors.
        String actor = root + "name/nm0000209/";
        Assert.assertTrue(exists(dataRepository, movie, root + "item/actor", new URIImpl(actor)));
        Assert.assertTrue(exists(dataRepository, actor, root + "property/actor/label", "Tim Robbins"));
        actor = root + "name/nm0000151/";
        Assert.assertTrue(exists(dataRepository, movie, root + "item/actor", new URIImpl(actor)));
        Assert.assertTrue(exists(dataRepository, actor, root + "property/actor/label", "Morgan Freeman"));
        actor = root + "name/nm0348409/";
        Assert.assertTrue(exists(dataRepository, movie, root + "item/actor", new URIImpl(actor)));
        Assert.assertTrue(exists(dataRepository, actor, root + "property/actor/label", "Bob Gunton"));

        List<List<Value>> entries = entries(dataRepository);
    }

    @Test
    public void testRecipes() throws RepositoryException, RDFParseException, IOException, QueryEvaluationException,
            MalformedQueryException, InterruptedException, JMSException {
        seed(modelRepository, "recipes");

        crawler().run();

        // Test recipe.
        String root = "http://www.chefkoch.de/";
        String section = root + "rs/s0/Donauwelle/Rezepte.html";
        String recipe = root + "rezepte/372611122991708/Donauwelle-Konditorenart.html";
        Assert.assertTrue(exists(dataRepository, section, root + "item/recipe", new URIImpl(recipe)));
        Assert.assertTrue(exists(dataRepository, recipe, root + "property/recipe/label", "Donauwelle Konditorenart"));
        Assert.assertTrue(exists(dataRepository, recipe, root + "property/recipe/workingTime", Duration.parse("PT1H")));
        Assert.assertTrue(exists(dataRepository, recipe, root + "property/recipe/difficulty", "normal"));
        Assert.assertTrue(exists(dataRepository, recipe, root + "property/recipe/portions", 1));
        Assert.assertTrue(exists(dataRepository, recipe, root + "property/recipe/rating", new BigDecimal("4.5")));

        // Test ingredients.
        String ingredient = recipe + "#82a6d13ff6c312236ce44b82f552f2190c7d5d77";
        Assert.assertTrue(exists(dataRepository, recipe, root + "item/ingredient", new URIImpl(ingredient)));
        Assert.assertTrue(exists(dataRepository, ingredient, root + "property/ingredient/label", "Butter"));
        Assert.assertTrue(exists(dataRepository, ingredient, root + "property/ingredient/amount", "350 g"));
        ingredient = recipe + "#b72f92496518feed5bcde739da8348ad779a0afa";
        Assert.assertTrue(exists(dataRepository, recipe, root + "item/ingredient", new URIImpl(ingredient)));
        Assert.assertTrue(exists(dataRepository, ingredient, root + "property/ingredient/label", "Zucker"));
        Assert.assertTrue(exists(dataRepository, ingredient, root + "property/ingredient/amount", "250 g"));
        ingredient = recipe + "#38a16de06bd9ac86fe8b226d0c493a7bf4a59ef1";
        Assert.assertTrue(exists(dataRepository, recipe, root + "item/ingredient", new URIImpl(ingredient)));
        Assert.assertTrue(exists(dataRepository, ingredient, root + "property/ingredient/label", "Ei(er)"));
        Assert.assertTrue(exists(dataRepository, ingredient, root + "property/ingredient/amount", "7 "));

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
        return exists(repository, subject, predicate, object, XMLSchema.DECIMAL);
    }

    /**
     * Checks whether repository entry exists.
     */
    protected boolean exists(Repository repository, String subject, String predicate, Duration object)
            throws RepositoryException, MalformedQueryException, QueryEvaluationException {
        return exists(repository, subject, predicate, object, XMLSchema.DURATION);
    }

    /**
     * Checks whether repository entry exists.
     */
    protected boolean exists(Repository repository, String subject, String predicate, int object)
            throws RepositoryException, MalformedQueryException, QueryEvaluationException {
        return exists(repository, subject, predicate, object, XMLSchema.INT);
    }

    /**
     * Checks whether repository entry exists.
     */
    protected boolean exists(Repository repository, String subject, String predicate, String object)
            throws RepositoryException, MalformedQueryException, QueryEvaluationException {
        return exists(repository, subject, predicate, object, XMLSchema.STRING);
    }

    /**
     * Checks whether repository entry exists.
     */
    protected boolean exists(Repository repository, String subject, String predicate, Object object, URI type)
            throws RepositoryException, MalformedQueryException, QueryEvaluationException {
        return repository(repository).ask(new URIImpl(subject), new URIImpl(predicate), object, type);
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
