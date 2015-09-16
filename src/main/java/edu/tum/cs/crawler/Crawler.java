package edu.tum.cs.crawler;

import edu.tum.cs.crawler.model.*;
import edu.tum.cs.crawler.queue.Element;
import edu.tum.cs.crawler.repository.Repository;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.commons.codec.digest.DigestUtils;
import org.joda.time.format.DateTimeFormat;
import org.json.JSONObject;
import org.openqa.selenium.*;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.RepositoryException;

import javax.jms.*;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Crawler {

    protected ActiveMQConnectionFactory connectionFactory;

    protected Connection connection;

    protected Session session;

    protected MessageProducer producer;

    protected MessageConsumer consumer;

    protected Repository configRepository;

    protected Repository dataRepository;

    protected Repository logRepository;

    protected Repository modelRepository;

    protected WebDriver driver;

    public Crawler(org.openrdf.repository.Repository configRepository, org.openrdf.repository.Repository dataRepository,
                   org.openrdf.repository.Repository logRepository, org.openrdf.repository.Repository modelRepository,
                   ActiveMQConnectionFactory connectionFactory) {
        this.configRepository = new Repository(configRepository);
        this.dataRepository = new Repository(dataRepository);
        this.logRepository = new Repository(logRepository);
        this.modelRepository = new Repository(modelRepository);
        this.connectionFactory = connectionFactory;
    }

    /**
     * Runs crawler.
     */
    public void run() throws QueryEvaluationException, RepositoryException, MalformedQueryException, InterruptedException,
            JMSException {
        // Open queue.
        connection = connectionFactory.createConnection();
        connection.start();
        session = connection.createSession(true, Session.SESSION_TRANSACTED);
        Queue queue = session.createQueue("crawler");
        producer = session.createProducer(queue);
        consumer = session.createConsumer(queue);

        // Send sections to queue if it is empty.
        ObjectMessage message = (ObjectMessage) consumer.receive(10000);
        if(message == null) {
            for(Section section : modelRepository.selectSections()) {
                send(section.getUri(), section.getPage(), section.getUri());
            }
            session.commit();
        }

        // Process queue.
        driver = new FirefoxDriver();
        if(message != null) processMessage(message);
        while(true) {
            message = (ObjectMessage) consumer.receive(10000);
            if(message == null) break;
            processMessage(message);
        }
        driver.close();

        // Close queue.
        session.close();
        connection.close();
    }

    /**
     * Inserts item data into data repository.
     */
    protected void insert(URI uri, Item item, Map<URI, Object> data, URI parent) throws RepositoryException {
        dataRepository.insert(parent, item.getUri(), uri);
        for(Map.Entry<URI, Object> entry : data.entrySet()) {
            Object object = entry.getValue();
            if(object instanceof BigDecimal) dataRepository.insert(uri, entry.getKey(), (BigDecimal) object);
            if(object instanceof Date) dataRepository.insert(uri, entry.getKey(), (Date) object);
            if(object instanceof Duration) dataRepository.insert(uri, entry.getKey(), (Duration) object);
            if(object instanceof Integer) dataRepository.insert(uri, entry.getKey(), (Integer) object);
            if(object instanceof String) dataRepository.insert(uri, entry.getKey(), (String) object);
            if(object instanceof URI) dataRepository.insert(uri, entry.getKey(), (URI) object);
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
     * Processes item page.
     */
    protected void processItemPage(URI uri, ItemPage itemPage, URI parent, SearchContext root)
            throws RepositoryException, InterruptedException, JMSException {
        Item item = itemPage.getItem();
        List<WebElement> itemRoots = root.findElements(By.xpath(item.getPath()));
        for(WebElement itemRoot : itemRoots) {
            // Collect data.
            Map<URI, Object> data = new HashMap<URI, Object>();
            for(Property property : item.getProperties()) {
                WebElement element;
                try {
                    element = itemRoot.findElement(By.xpath(property.getPath()));
                } catch (NoSuchElementException e) {
                    if(property.isOptional()) continue;
                    throw e;
                }
                data.put(property.getUri(), value(property, element));
            }

            // Find or create item uri if necessary.
            URI itemUri = uri;
            if(itemPage instanceof ItemsPage) {
                if(item.hasId()) {
                    WebElement id = itemRoot.findElement(By.xpath(item.getId()));
                    itemUri = new URIImpl(id.getAttribute("href"));
                } else {
                    JSONObject jsonObject = new JSONObject();
                    for(Map.Entry<URI, Object> entry : data.entrySet()) {
                        jsonObject.accumulate(entry.getKey().toString(), entry.getValue().toString());
                    }
                    String hash = DigestUtils.sha1Hex(jsonObject.toString());
                    itemUri = new URIImpl(uri + "#" + hash);
                }
            }

            // Insert data into repository.
            insert(itemUri, item, data, parent);

            // Process pages.
            for(Page page : itemPage.getPages()) {
                processUri(uri, page, itemUri, itemRoot);
            }

            // Send links to queue.
            for(Link link : itemPage.getLinks()) {
                WebElement linkElement = root.findElement(By.xpath(link.getPath()));
                URI linkUri = new URIImpl(linkElement.getAttribute("href"));
                send(linkUri, link.getTarget(), parent);
            }
        }
    }

    /**
     * Processes links page.
     */
    protected void processLinksPage(LinksPage linksPage, URI parent, SearchContext root) throws JMSException {
        List<WebElement> links = root.findElements(By.xpath(linksPage.getPath()));
        for(WebElement link : links) {
            send(link.getAttribute("href"), linksPage.getTarget(), parent);
        }
    }

    /**
     * Processes list page.
     */
    protected void processListPage(URI uri, ListPage listPage, URI parent, SearchContext root)
            throws InterruptedException, RepositoryException, JMSException {
        // Get menu links, if required.
        if(listPage.hasMenu()) {
            List<WebElement> links = root.findElements(By.xpath(listPage.getMenu()));
            listPage.setMenu(null);
            if(listPage.hasWait()) {
                for(WebElement link : links) {
                    link.click();
                    Thread.sleep(listPage.getWait());
                    processUri(uri, (Page) listPage, parent, root);
                }
                driver.navigate().to(uri.toString());
            } else {
                for(WebElement link : links) {
                    send(link.getAttribute("href"), (Page) listPage, parent);
                }
            }
        }

        // Click next button until it disappears, if required.
        if(listPage.hasNext()) {
            String next = listPage.getNext();
            listPage.setNext(null);
            while(true) {
                List<WebElement> nextElement = root.findElements(By.xpath(next));
                if(nextElement.isEmpty()) break;
                nextElement.get(0).click();
                if(listPage.hasWait()) Thread.sleep(listPage.getWait());
                processUri(uri, (Page) listPage, parent, root);
            }
            listPage.setNext(next);
            driver.navigate().to(uri.toString());
        }
    }

    /**
     * Processes queue message.
     */
    protected void processMessage(ObjectMessage message) throws JMSException, InterruptedException, RepositoryException {
        Element element = (Element) message.getObject();
        driver.navigate().to(element.getUri().toString());
        processUri(element.getUri(), element.getPage(), element.getParent(), driver);
        session.commit();
    }

    /**
     * Processes uri.
     */
    protected void processUri(URI uri, Page page, URI parent, SearchContext root) throws RepositoryException,
            InterruptedException, JMSException {
        prepareUri(page);
        if(page instanceof ListPage) processListPage(uri, (ListPage) page, parent, root);
        if(page instanceof ItemPage) processItemPage(uri, (ItemPage) page, parent, root);
        if(page instanceof LinksPage) processLinksPage((LinksPage) page, parent, root);
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
    protected void send(String uri, Page page, URI parent) throws JMSException {
        send(new URIImpl(uri), page, parent);
    }

    /**
     * Sends uri to queue.
     */
    protected void send(URI uri, Page page, URI parent) throws JMSException {
        Element element = new Element(uri, page, parent);
        ObjectMessage message = session.createObjectMessage(element);
        producer.send(message);
    }

    /**
     * Extracts and converts property value.
     */
    protected Object value(Property property, WebElement element) {
        // Get attribute value if required, otherwise the element's text.
        String value = (property.hasAttribute()) ? element.getAttribute(property.getAttribute()) : element.getText();

        // Run pattern matching if required.
        if(property.hasPattern()) {
            Matcher matcher = Pattern.compile(property.getPattern()).matcher(value);
            value = (matcher.find()) ? matcher.group() : "";
        }

        // Run replacements if required.
        if(property.hasReplace()) {
            for(List<String> replace : property.getReplace()) {
                value = value.replaceAll(replace.get(0), replace.get(1));
            }
        }

        // Convert value if required.
        URI type = property.getType();
        if(type.getLocalName().equals("anyURI")) return new URIImpl(value);
        if(type.getLocalName().equals("dateTime")) {
            return DateTimeFormat.forPattern(property.getFormat()).parseDateTime(value).toDate();
        }
        if(type.getLocalName().equals("decimal")) return new BigDecimal(value);
        if(type.getLocalName().equals("duration")) return Duration.parse(value);
        if(type.getLocalName().equals("int")) return new Integer(value);

        // Return string otherwise.
        return value;
    }

}