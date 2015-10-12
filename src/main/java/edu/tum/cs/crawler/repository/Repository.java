package edu.tum.cs.crawler.repository;

import edu.tum.cs.crawler.model.*;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.XMLSchema;
import org.openrdf.query.*;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.sail.memory.model.BooleanMemLiteral;
import org.openrdf.sail.memory.model.IntegerMemLiteral;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;

public class Repository {

    protected org.openrdf.repository.Repository repository;

    public Repository(org.openrdf.repository.Repository repository) {
        this.repository = repository;
    }

    public boolean ask(URI subject, URI predicate, Object object, URI type) throws RepositoryException,
            MalformedQueryException, QueryEvaluationException {
        return connection().prepareBooleanQuery(QueryLanguage.SPARQL,
                "ASK { <" + subject + "> <" + predicate + "> \"\"\"" + object + "\"\"\"^^<" + type + "> }").evaluate();
    }

    public boolean ask(URI subject, URI predicate, URI object) throws RepositoryException,
            MalformedQueryException, QueryEvaluationException {
        return connection().prepareBooleanQuery(QueryLanguage.SPARQL,
                "ASK { <" + subject + "> <" + predicate + "> <" + object + "> }").evaluate();
    }

    public void insert(Resource subject, URI predicate, BigDecimal object) throws RepositoryException {
        insert(subject, predicate, object, XMLSchema.DECIMAL);
    }

    public void insert(Resource subject, URI predicate, Date object) throws RepositoryException {
        connection().add(subject, predicate, factory().createLiteral(object));
    }

    public void insert(Resource subject, URI predicate, Duration object) throws RepositoryException {
        insert(subject, predicate, object, XMLSchema.DURATION);
    }

    public void insert(Resource subject, URI predicate, Integer object) throws RepositoryException {
        insert(subject, predicate, object, XMLSchema.INT);
    }

    public void insert(Resource subject, URI predicate, LocalDate object) throws RepositoryException {
        insert(subject, predicate, object, XMLSchema.DATE);
    }

    public void insert(Resource subject, URI predicate, LocalTime object) throws RepositoryException {
        insert(subject, predicate, object, XMLSchema.TIME);
    }

    public void insert(Resource subject, URI predicate, String object) throws RepositoryException {
        insert(subject, predicate, object, XMLSchema.STRING);
    }

    public void insert(Resource subject, URI predicate, Object object, URI type) throws RepositoryException {
        connection().add(subject, predicate, factory().createLiteral(object.toString(), type));
    }

    public void insert(Resource subject, URI predicate, URI object) throws RepositoryException {
        connection().add(subject, predicate, object);
    }

    public TupleQueryResult select(String query) throws RepositoryException, MalformedQueryException, QueryEvaluationException {
        return connection().prepareTupleQuery(QueryLanguage.SPARQL, query).evaluate();
    }

    public List<URI> selectIgnores(Page page) throws QueryEvaluationException, RepositoryException,
            MalformedQueryException {
        List<URI> ignores = new ArrayList<URI>();
        TupleQueryResult result = select(
                "SELECT ?ignore\n" +
                "WHERE {\n" +
                    "<" + page.getUri() + "> <" + Predicate.IGNORE + "> ?ignore\n" +
                "}");
        while(result.hasNext()) {
            BindingSet bindings = result.next();
            ignores.add((URI) bindings.getValue("ignore"));
        }
        result.close();
        return ignores;
    }

    public Item selectItem(Page page) throws QueryEvaluationException, RepositoryException, MalformedQueryException {
        TupleQueryResult result = select(
                "SELECT ?uri ?path ?id\n" +
                "WHERE {\n" +
                    "<" + page.getUri() + "> <" + Predicate.ITEM + "> ?uri .\n" +
                    "?uri <" + Predicate.PATH + "> ?path\n" +
                    "OPTIONAL { ?uri <" + Predicate.ID + "> ?id }\n" +
                "}");
        BindingSet bindings = result.next();
        Item item = new Item();
        item.setUri((URI) bindings.getValue("uri"));
        if(bindings.hasBinding("path")) item.setPath(bindings.getValue("path").stringValue());
        if(bindings.hasBinding("id")) item.setId(bindings.getValue("id").stringValue());
        item.setProperties(selectProperties(item));
        result.close();
        return item;
    }

    public List<Link> selectLinks(Page page) throws QueryEvaluationException, RepositoryException,
            MalformedQueryException {
        List<Link> links = new ArrayList<Link>();
        TupleQueryResult result = select(
                "SELECT ?uri ?path\n" +
                "WHERE {\n" +
                    "<" + page.getUri() + "> <" + Predicate.LINK + "> ?uri .\n" +
                    "?uri <" + Predicate.PATH + "> ?path\n" +
                "}");
        while(result.hasNext()) {
            BindingSet bindings = result.next();
            Link link = new Link();
            link.setUri((URI) bindings.getValue("uri"));
            link.setPath(bindings.getValue("path").stringValue());
            link.setTarget(selectPage(link, Predicate.TARGET));
            links.add(link);
        }
        result.close();
        return links;
    }

    public Page selectPage(Model model, Predicate predicate) throws QueryEvaluationException, RepositoryException,
            MalformedQueryException {
        return selectPages(model, predicate).get(0);
    }

    public List<Page> selectPages(Model model, Predicate predicate) throws QueryEvaluationException, RepositoryException,
            MalformedQueryException {
        List<Page> pages = new ArrayList<Page>();
        TupleQueryResult result = select(
                "SELECT ?uri ?type ?path ?next ?wait ?scroll\n" +
                "WHERE {\n" +
                    "<" + model.getUri() + "> <" + predicate + "> ?uri .\n" +
                    "?uri <" + Predicate.TYPE + "> ?type\n" +
                    "OPTIONAL { ?uri <" + Predicate.PATH + "> ?path }\n" +
                    "OPTIONAL { ?uri <" + Predicate.NEXT + "> ?next }\n" +
                    "OPTIONAL { ?uri <" + Predicate.WAIT + "> ?wait }\n" +
                    "OPTIONAL { ?uri <" + Predicate.SCROLL + "> ?scroll }\n" +
                "}");
        while(result.hasNext()) {
            BindingSet bindings = result.next();
            Page page;
            URI type = (URI) bindings.getValue("type");
            if(type.equals(Type.ITEM.getUri())) {
                page = new ItemPage();
            } else {
                page = (type.equals(Type.ITEMS.getUri())) ? new ItemsPage() : new LinksPage();
            }
            page.setUri((URI) bindings.getValue("uri"));
            if(bindings.hasBinding("scroll")) page.setScroll(((IntegerMemLiteral) bindings.getValue("scroll")).intValue());
            if(page instanceof ItemPage) {
                ItemPage itemPage = (ItemPage) page;
                itemPage.setItem(selectItem(page));
                itemPage.setSubPages(selectPages(page, Predicate.SUB));
                itemPage.setLinks(selectLinks(page));
            }
            if(page instanceof LinksPage) {
                LinksPage linksPage = (LinksPage) page;
                if(bindings.hasBinding("path")) linksPage.setPath(bindings.getValue("path").stringValue());
                linksPage.setTarget(selectPage(page, Predicate.TARGET));
            }
            if(page instanceof ListPage) {
                ListPage listPage = (ListPage) page;
                if(bindings.hasBinding("next")) listPage.setNext(bindings.getValue("next").stringValue());
                if(bindings.hasBinding("wait")) listPage.setWait(((IntegerMemLiteral) bindings.getValue("wait")).intValue());
            }
            page.setIgnores(selectIgnores(page));
            page.setParams(selectParams(page));
            pages.add(page);
        }
        result.close();
        return pages;
    }

    public List<String> selectParams(Page page) throws QueryEvaluationException, RepositoryException,
            MalformedQueryException {
        List<String> params = new ArrayList<String>();
        TupleQueryResult result = select(
                "SELECT ?param\n" +
                "WHERE {\n" +
                    "<" + page.getUri() + "> <" + Predicate.PARAM + "> ?param\n" +
                "}");
        while(result.hasNext()) {
            BindingSet bindings = result.next();
            String param = bindings.getValue("param").stringValue();
            params.add(param);
        }
        result.close();
        return params;
    }

    public List<Property> selectProperties(Item item) throws RepositoryException, QueryEvaluationException,
            MalformedQueryException {
        List<Property> properties = new ArrayList<Property>();
        TupleQueryResult result = select(
                "SELECT ?uri ?path ?optional ?attribute ?regex ?type ?format\n" +
                "WHERE {\n" +
                    "<" + item.getUri() + "> <" + Predicate.PROPERTY + "> ?uri .\n" +
                    "?uri <" + Predicate.PATH + "> ?path\n" +
                    "OPTIONAL { ?uri <" + Predicate.OPTIONAL + "> ?optional }\n" +
                    "OPTIONAL { ?uri <" + Predicate.ATTRIBUTE + "> ?attribute }\n" +
                    "OPTIONAL { ?uri <" + Predicate.REGEX + "> ?regex }\n" +
                    "OPTIONAL { ?uri <" + Predicate.TYPE + "> ?type }\n" +
                    "OPTIONAL { ?uri <" + Predicate.FORMAT + "> ?format }\n" +
                "}");
        while(result.hasNext()) {
            BindingSet bindings = result.next();
            Property property = new Property();
            property.setUri((URI) bindings.getValue("uri"));
            property.setPath(bindings.getValue("path").stringValue());
            boolean optional = bindings.hasBinding("optional") &&
                    ((BooleanMemLiteral) bindings.getValue("optional")).booleanValue();
            property.setOptional(optional);
            if(bindings.hasBinding("attribute")) property.setAttribute(bindings.getValue("attribute").stringValue());
            if(bindings.hasBinding("regex")) property.setRegex(bindings.getValue("regex").stringValue());
            property.setReplaces(selectReplaces(property));
            property.setType((bindings.hasBinding("type")) ? (URI) bindings.getValue("type") : XMLSchema.STRING);
            if(bindings.hasBinding("format")) property.setFormat(bindings.getValue("format").stringValue());
            properties.add(property);
        }
        result.close();
        return properties;
    }

    public Map<String, String> selectReplaces(Property property) throws QueryEvaluationException, RepositoryException,
            MalformedQueryException {
        Map<String, String> replaces = new LinkedHashMap<String, String>();
        TupleQueryResult result = select(
                "SELECT ?old ?new\n" +
                "WHERE {\n" +
                    "<" + property.getUri() + "> <" + Predicate.REPLACE + "> ?uri .\n" +
                    "?uri <" + Predicate.OLD + "> ?old .\n" +
                    "?uri <" + Predicate.NEW + "> ?new\n" +
                    "OPTIONAL { ?uri <" + Predicate.ORDER + "> ?order }\n" +
                "}\n" +
                "ORDER BY ?order");
        while(result.hasNext()) {
            BindingSet bindings = result.next();
            replaces.put(bindings.getValue("old").stringValue(), bindings.getValue("new").stringValue());
        }
        result.close();
        return replaces;
    }

    public List<Section> selectSections() throws QueryEvaluationException, RepositoryException, MalformedQueryException {
        List<Section> sections = new ArrayList<Section>();
        TupleQueryResult result = select(
                "SELECT ?uri\n" +
                "WHERE {\n" +
                    "?s <" + Predicate.SECTION + "> ?uri\n" +
                "}");
        while(result.hasNext()) {
            BindingSet bindings = result.next();
            Section section = new Section();
            section.setUri((URI) bindings.getValue("uri"));
            section.setPage(selectPage(section, Predicate.PAGE));
            sections.add(section);
        }
        result.close();
        return sections;
    }

    protected RepositoryConnection connection() throws RepositoryException {
        return repository.getConnection();
    }

    protected ValueFactory factory() throws RepositoryException {
        return connection().getValueFactory();
    }

}
