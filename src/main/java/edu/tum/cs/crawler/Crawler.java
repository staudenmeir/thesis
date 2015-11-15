package edu.tum.cs.crawler;

import edu.tum.cs.crawler.model.*;
import edu.tum.cs.crawler.queue.Element;
import edu.tum.cs.crawler.repository.Repository;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.commons.codec.digest.DigestUtils;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.joda.time.format.DateTimeFormat;
import org.json.JSONArray;
import org.json.JSONObject;
import org.openqa.selenium.*;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.firefox.internal.ProfilesIni;
import org.openrdf.model.IRI;
import org.openrdf.model.vocabulary.XMLSchema;

import javax.jms.*;
import javax.jms.Queue;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Crawler {

    public Map<String, Map<String, String>> extracts;

    protected ActiveMQConnectionFactory connectionFactory;

    protected Connection connection;

    protected Session session;

    protected MessageProducer producer;

    protected MessageConsumer consumer;

    protected Repository dataRepository;

    protected Repository diffRepository;

    protected Repository modelRepository;

    protected WebDriver driver;

    public Crawler(org.openrdf.repository.Repository dataRepository, org.openrdf.repository.Repository diffRepository,
                   org.openrdf.repository.Repository modelRepository, ActiveMQConnectionFactory connectionFactory) {
        this.dataRepository = new Repository(dataRepository);
        this.diffRepository = new Repository(diffRepository);
        this.modelRepository = new Repository(modelRepository);
        this.connectionFactory = connectionFactory;
    }

    /**
     * Runs crawler.
     */
    public IRI run() throws InterruptedException, JMSException {
        // Open queue.
        connection = connectionFactory.createConnection();
        connection.start();
        session = connection.createSession(true, Session.SESSION_TRANSACTED);
        Queue queue = session.createQueue("crawler");
        producer = session.createProducer(queue);
        consumer = session.createConsumer(queue);

        // Check whether queue is empty (= last run was completed).
        IRI context;
        ObjectMessage message = (ObjectMessage) consumer.receive(10000);
        if(message == null) {
            // Send sections to queue (= fresh start).
            long timestamp = System.currentTimeMillis() / 1000L;
            context = dataRepository.createIRI("run://" + timestamp);
            for(Section section : modelRepository.selectSections()) {
                send(section.getUri(), section.getPage(), section.getUri(), context);
            }
            session.commit();
        } else {
            context = ((Element) message.getObject()).getContext();
        }

        // Open browser.
        FirefoxProfile profile = (new ProfilesIni()).getProfile("crawler");
        driver = new FirefoxDriver(profile);

        // Process queue.
        extracts = new HashMap<String, Map<String, String>>();
        if(message != null) processMessage(message);
        while(true) {
            message = (ObjectMessage) consumer.receive(10000);
            if(message == null) break;
            processMessage(message);
        }

        // Close browser.
        driver.close();

        // Close queue.
        session.close();
        connection.close();

        // Detect differences to previous run and log them.
        dataRepository.diff(diffRepository, context);

        return context;
    }

    /**
     * Cleans uri by removing query parameters and the anchor.
     */
    protected IRI clean(IRI uri, Page page) {
        String string = uri.toString();
        for(String param : page.getParams()) {
            string = string.replaceAll("[&?]" + Pattern.quote(param) + ".*?(?=&|\\?|$)", "");
        }
        if(string.contains("&") && !string.contains("?")) string = string.replaceFirst("&", "?");
        return dataRepository.createIRI(string);
    }

    /**
     * Inserts object data into data repository.
     */
    protected void insert(IRI uri, Item item, Map<IRI, Object> data, IRI parent, IRI context) {
        dataRepository.insert(parent, item.getUri(), uri, context);
        for(Map.Entry<IRI, Object> entry : data.entrySet()) {
            Object object = entry.getValue();
            if(object instanceof BigDecimal) dataRepository.insert(uri, entry.getKey(), (BigDecimal) object, context);
            if(object instanceof Date) dataRepository.insert(uri, entry.getKey(), (Date) object, context);
            if(object instanceof Duration) dataRepository.insert(uri, entry.getKey(), (Duration) object, context);
            if(object instanceof Integer) dataRepository.insert(uri, entry.getKey(), (Integer) object, context);
            if(object instanceof LocalDate) dataRepository.insert(uri, entry.getKey(), (LocalDate) object, context);
            if(object instanceof LocalTime) dataRepository.insert(uri, entry.getKey(), (LocalTime) object, context);
            if(object instanceof String) dataRepository.insert(uri, entry.getKey(), (String) object, context);
            if(object instanceof IRI) dataRepository.insert(uri, entry.getKey(), (IRI) object, context);
        }
    }

    /**
     * Executes JavaScript code.
     */
    protected Object javaScript(String code) {
        return ((JavascriptExecutor) driver).executeScript(code);
    }

    /**
     * Prepares uri for processing.
     */
    protected void prepareUri(Page page) throws InterruptedException {
        // Scroll page until it's fully loaded, if required.
        if(page.hasScroll()) {
            scrollToEnd();
            Thread.sleep(page.getScroll());
            while(!scrolledToEnd()) {
                scrollToEnd();
                Thread.sleep(page.getScroll());
            }
        }
    }

    /**
     * Processes links page.
     */
    protected void processLinksPage(LinksPage linksPage, IRI parent, IRI context, SearchContext root)
            throws JMSException {
        List<WebElement> links = root.findElements(By.xpath(linksPage.getPath()));
        for(WebElement link : links) {
            send(link.getAttribute("href"), linksPage.getTarget(), parent, context);
        }
    }

    /**
     * Processes list page.
     */
    protected void processListPage(IRI uri, ListPage listPage, IRI parent, IRI context, SearchContext root)
            throws InterruptedException, JMSException {
        // Click next button until it disappears, if required.
        if(listPage.hasNext()) {
            String next = listPage.getNext();
            List<WebElement> nextElement;
            if(listPage.hasWait()) {
                listPage.setNext(null);
                while(true) {
                    nextElement = root.findElements(By.xpath(next));
                    if(nextElement.isEmpty()) break;
                    nextElement.get(0).click();
                    if(listPage.hasWait()) Thread.sleep(listPage.getWait());
                    processUri(uri, (Page) listPage, parent, context, root);
                }
                listPage.setNext(next);
            } else {
                nextElement = root.findElements(By.xpath(next));
                if(!nextElement.isEmpty()) send(nextElement.get(0).getAttribute("href"), (Page) listPage, parent, context);
            }
        }
    }

    /**
     * Processes queue message.
     */
    protected void processMessage(ObjectMessage message) throws InterruptedException, JMSException {
        Element element = (Element) message.getObject();
        IRI uri = element.getUri();
        Page page = element.getPage();
        driver.navigate().to(uri.toString());
        processUri(uri, page, element.getParent(), element.getContext(), driver);
        session.commit();
    }

    /**
     * Processes object page.
     */
    protected void processObjectPage(IRI uri, ObjectPage objectPage, IRI parent, IRI context, SearchContext root)
            throws InterruptedException, JMSException {
        Item item = objectPage.getItem();
        List<WebElement> objectRoots = root.findElements(By.xpath(item.getPath()));
        for(WebElement objectRoot : objectRoots) {
            // Extract and process data.
            Map<String, String> rawData = new LinkedHashMap<String, String>();
            Map<IRI, Object> processedData = new HashMap<IRI, Object>();
            for(Property property : item.getProperties()) {
                WebElement element;
                try {
                    element = objectRoot.findElement(By.xpath(property.getPath()));
                } catch (NoSuchElementException e) {
                    if(property.isOptional()) continue;
                    throw e;
                }
                String value = (property.hasAttribute()) ? element.getAttribute(property.getAttribute()) : element.getText();
                rawData.put(property.getUri().toString(), value);
                processedData.put(property.getUri(), processValue(property, value));
            }

            // Find or create object uri if necessary.
            IRI objectUri = uri;
            if(objectPage instanceof ObjectsPage) {
                if(item.hasId()) {
                    WebElement id = objectRoot.findElement(By.xpath(item.getId()));
                    objectUri = dataRepository.createIRI(id.getAttribute("href"));
                    objectUri = clean(objectUri, objectPage);
                } else {
                    JSONArray data = new JSONArray();
                    for(Map.Entry<String, String> entry : rawData.entrySet()) {
                        JSONObject property = new JSONObject();
                        property.put(entry.getKey(), entry.getValue());
                        data.put(property);
                    }
                    String hash = DigestUtils.sha1Hex(data.toString());
                    objectUri = dataRepository.createIRI(uri + "#" + hash);
                }
            }

            // Add raw data to extracts.
            extracts.put(objectUri.toString(), rawData);

            // Insert data into repository.
            insert(objectUri, item, processedData, parent, context);

            // Process sub pages.
            for(Page page : objectPage.getSubPages()) {
                processUri(uri, page, objectUri, context, objectRoot);
            }

            // Send links to queue.
            for(Link link : objectPage.getLinks()) {
                WebElement linkElement = root.findElement(By.xpath(link.getPath()));
                IRI linkUri = dataRepository.createIRI(linkElement.getAttribute("href"));
                send(linkUri, link.getTarget(), uri, context);
            }
        }
    }

    /**
     * Processes uri.
     */
    protected void processUri(IRI uri, Page page, IRI parent, IRI context, SearchContext root)
            throws InterruptedException, JMSException {
        prepareUri(page);
        if(page instanceof ObjectPage) processObjectPage(uri, (ObjectPage) page, parent, context, root);
        if(page instanceof LinksPage) processLinksPage((LinksPage) page, parent, context, root);
        if(page instanceof ListPage) processListPage(uri, (ListPage) page, parent, context, root);
    }

    /**
     * Processes value.
     */
    protected Object processValue(Property property, String value) {
        // Run pattern matching if required.
        if(property.hasPattern()) {
            Matcher matcher = Pattern.compile(property.getPattern()).matcher(value);
            value = (matcher.find()) ? matcher.group() : "";
        }

        // Run replacements if required.
        for(Map.Entry<String, String> replace : property.getReplaces().entrySet()) {
            value = value.replaceAll(replace.getKey(), replace.getValue());
        }

        // Convert value if required.
        IRI type = property.getType();
        if(type.equals(XMLSchema.ANYURI)) return dataRepository.createIRI(value);
        if(type.equals(XMLSchema.DATE) || type.equals(XMLSchema.DATETIME) || type.equals(XMLSchema.TIME)) {
            DateTime dateTime = DateTimeFormat.forPattern(property.getFormat()).parseDateTime(value);
            if(type.equals(XMLSchema.DATE)) return dateTime.toLocalDate();
            if(type.equals(XMLSchema.DATETIME)) return dateTime.toDate();
            if(type.equals(XMLSchema.TIME)) return dateTime.toLocalTime();
        }
        if(type.equals(XMLSchema.DECIMAL)) return new BigDecimal(value);
        if(type.equals(XMLSchema.DURATION)) return Duration.parse(value);
        if(type.equals(XMLSchema.INT)) return new Integer(value);

        // Return string otherwise.
        return value;
    }

    /**
     * Checks whether page is scrolled to the end.
     */
    protected boolean scrolledToEnd() {
        return (Boolean) javaScript("return ((window.innerHeight + window.scrollY) >= document.body.scrollHeight);");
    }

    /**
     * Scrolls page to the end.
     */
    protected void scrollToEnd() {
        javaScript("window.scrollTo(0, document.body.scrollHeight);");
    }

    /**
     * Sends uri to queue.
     */
    protected void send(String uri, Page page, IRI parent, IRI context) throws JMSException {
        send(dataRepository.createIRI(uri), page, parent, context);
    }

    /**
     * Sends uri to queue.
     */
    protected void send(IRI uri, Page page, IRI parent, IRI context) throws JMSException {
        Element element = new Element(clean(uri, page), page, parent, context);
        ObjectMessage message = session.createObjectMessage(element);
        producer.send(message);
    }

}