package edu.tum.cs.crawler.repository;

import edu.tum.cs.crawler.model.*;
import edu.tum.cs.crawler.model.Model;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.openrdf.model.*;
import org.openrdf.model.vocabulary.XMLSchema;
import org.openrdf.query.*;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.sail.memory.model.MemLiteral;
import org.openrdf.sail.memory.model.MemIRI;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;

public class Repository {

    protected org.openrdf.repository.Repository repository;

    public Repository(org.openrdf.repository.Repository repository) {
        this.repository = repository;
    }

    public boolean ask(IRI subject, IRI predicate, String object, IRI type, IRI context) {
        String query = "ASK { GRAPH <" + context + "> { <" + subject + "> <" + predicate + "> ?object } }";
        BooleanQuery booleanQuery = getConnection().prepareBooleanQuery(QueryLanguage.SPARQL, query);
        booleanQuery.setBinding("object", factory().createLiteral(object, type));
        return booleanQuery.evaluate();
    }

    public boolean ask(IRI subject, IRI predicate, IRI object, IRI context) {
        String query = "ASK { GRAPH <" + context + "> { ";
        query += (subject != null) ? "<" + subject + ">" : "?s";
        query += " ";
        query += (predicate != null) ? "<" + predicate + ">" : "?p";
        query += " ";
        query += (object != null) ? "<" + object + ">" : "?o";
        query += " } }";
        return getConnection().prepareBooleanQuery(QueryLanguage.SPARQL, query).evaluate();
    }

    public IRI createIRI(String iri) {
        return factory().createIRI(iri);
    }

    public void delete(IRI subject, IRI predicate, IRI object, IRI context) {
        String query = "DELETE WHERE { GRAPH <" + context + "> { <" + subject + "> <" + predicate + "> ";
        query += (object != null) ? "<" + object + ">" : "?o";
        query += " } }";
        getConnection().prepareUpdate(QueryLanguage.SPARQL, query).execute();
    }

    public void diff(Repository diffRepository, IRI context) {
        // Find previous context.
        IRI previousContext = findPreviousContext(context);

        // Detect added and changed entries.
        IRI added = factory().createIRI("predicate://added");
        IRI updated = factory().createIRI("predicate://changed");
        List<List<Value>> entries = selectEntries(context);
        for(List<Value> entry : entries) {
            // Differentiate between item and property entry.
            IRI subject = (IRI) entry.get(0);
            IRI predicate = (IRI) entry.get(1);
            String scheme = predicate.toString().split(":")[0];
            if(scheme.equals("item")) {
                // Detect added item entry.
                IRI object = (IRI) entry.get(2);
                boolean exists = ask(subject, predicate, object, previousContext);
                if(!exists) diffRepository.insert(subject, added, object, context);
            } else {
                // Detect added or changed property entry.
                // Differentiate between uri and literal object.
                boolean exists = ask(subject, predicate, null, previousContext);
                if(exists) {
                    boolean changed;
                    if(entry.get(2) instanceof MemIRI) {
                        changed = !ask(subject, predicate, (IRI) entry.get(2), previousContext);
                    } else {
                        String object = ((MemLiteral) entry.get(2)).getLabel();
                        IRI type = ((MemLiteral) entry.get(2)).getDatatype();
                        changed = !ask(subject, predicate, object, type, previousContext);
                    }
                    if(changed) diffRepository.insert(subject, updated, predicate, context);
                } else {
                    // Log added entry only if its object wasn't added (optional property).
                    boolean objectWasAdded = !ask(null, null, subject, previousContext);
                    if(!objectWasAdded) diffRepository.insert(subject, added, predicate, context);
                }
            }
        }

        // Detect removed entries.
        IRI removed = factory().createIRI("predicate://removed");
        List<List<Value>> previousEntries = selectEntries(previousContext);
        for(List<Value> entry : previousEntries) {
            // Differentiate between item and property entry.
            IRI subject = (IRI) entry.get(0);
            IRI predicate = (IRI) entry.get(1);
            String scheme = predicate.toString().split(":")[0];
            if(scheme.equals("item")) {
                // Detect removed item entry.
                IRI object = (IRI) entry.get(2);
                boolean exists = ask(subject, predicate, object, context);
                if(!exists) diffRepository.insert(subject, removed, object, context);
            } else {
                // Detect removed property entry.
                boolean exists = ask(subject, predicate, null, context);
                if(!exists) {
                    // Log removed entry only if its object wasn't removed (optional property).
                    boolean objectWasRemoved = !ask(null, null, subject, context);
                    if(!objectWasRemoved) diffRepository.insert(subject, removed, predicate, context);
                }
            }
        }

        // Delete item diff entries that don't belong to the topmost objects.
        List<List<Value>> toBeDeleted = new ArrayList<List<Value>>();
        List<List<Value>> logEntries = diffRepository.selectEntries(context);
        for(List<Value> entry : logEntries) {
            String scheme = entry.get(2).toString().split(":")[0];
            if(!scheme.equals("property") && diffRepository.ask(null, (IRI) entry.get(1), (IRI) entry.get(0), context)) {
                toBeDeleted.add(entry);
            }
        }
        for(List<Value> entry : toBeDeleted) {
            diffRepository.delete((IRI) entry.get(0), (IRI) entry.get(1), (IRI) entry.get(2), context);
        }
    }

    public IRI findPreviousContext(IRI context) {
        long contextTimestamp = Long.parseLong(context.stringValue().split("//")[1]);
        long previousTimestamp = 0;
        RepositoryResult<Resource> contextIDs = getConnection().getContextIDs();
        while(contextIDs.hasNext()) {
            Resource contextID = contextIDs.next();
            long timestamp = Long.parseLong(contextID.stringValue().split("//")[1]);
            if((timestamp < contextTimestamp) && (timestamp > previousTimestamp)) previousTimestamp = timestamp;
        }
        return factory().createIRI("run://" + previousTimestamp);
    }

    public RepositoryConnection getConnection() {
        return repository.getConnection();
    }

    public void insert(Resource subject, IRI predicate, BigDecimal object, IRI context) {
        insert(subject, predicate, object.toString(), XMLSchema.DECIMAL, context);
    }

    public void insert(Resource subject, IRI predicate, Date object, IRI context) {
        getConnection().add(subject, predicate, factory().createLiteral(object), context);
    }

    public void insert(Resource subject, IRI predicate, Duration object, IRI context) {
        insert(subject, predicate, object.toString(), XMLSchema.DURATION, context);
    }

    public void insert(Resource subject, IRI predicate, Integer object, IRI context) {
        insert(subject, predicate, object.toString(), XMLSchema.INT, context);
    }

    public void insert(Resource subject, IRI predicate, LocalDate object, IRI context) {
        insert(subject, predicate, object.toString(), XMLSchema.DATE, context);
    }

    public void insert(Resource subject, IRI predicate, LocalTime object, IRI context) {
        insert(subject, predicate, object.toString(), XMLSchema.TIME, context);
    }

    public void insert(Resource subject, IRI predicate, String object, IRI context) {
        insert(subject, predicate, object, XMLSchema.STRING, context);
    }

    public void insert(Resource subject, IRI predicate, IRI object, IRI context) {
        getConnection().add(subject, predicate, object, context);
    }

    public TupleQueryResult select(String query) {
        return getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query).evaluate();
    }

    public List<List<Value>> selectEntries() {
        return selectEntries(null);
    }

    public List<List<Value>> selectEntries(IRI context) {
        List<List<Value>> entries = new ArrayList<List<Value>>();
        String graph = (context != null) ? "GRAPH <" + context + ">" : "";
        TupleQueryResult result = select(
                "SELECT ?s ?p ?o ?g\n" +
                "WHERE {\n" +
                        graph + "{ ?s ?p ?o }\n" +
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

    public Item selectItem(Page page) {
        TupleQueryResult result = select(
                "SELECT ?uri ?path ?id\n" +
                "WHERE {\n" +
                    "<" + page.getUri() + "> <" + Predicate.ITEM + "> ?uri .\n" +
                    "?uri <" + Predicate.PATH + "> ?path\n" +
                    "OPTIONAL { ?uri <" + Predicate.ID + "> ?id }\n" +
                "}");
        BindingSet bindings = result.next();
        Item item = new Item();
        item.setUri((IRI) bindings.getValue("uri"));
        if(bindings.hasBinding("path")) item.setPath(bindings.getValue("path").stringValue());
        if(bindings.hasBinding("id")) item.setId(bindings.getValue("id").stringValue());
        item.setProperties(selectProperties(item));
        result.close();
        return item;
    }

    public List<Link> selectLinks(Page page) {
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
            link.setUri((IRI) bindings.getValue("uri"));
            link.setPath(bindings.getValue("path").stringValue());
            link.setTarget(selectPage(link, Predicate.TARGET));
            links.add(link);
        }
        result.close();
        return links;
    }

    public Page selectPage(Model model, Predicate predicate) {
        return selectPages(model, predicate).get(0);
    }

    public List<Page> selectPages(Model model, Predicate predicate) {
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
            IRI type = (IRI) bindings.getValue("type");
            if(type.equals(Type.OBJECT.getUri(factory()))) {
                page = new ObjectPage();
            } else {
                page = (type.equals(Type.OBJECTS.getUri(factory()))) ? new ObjectsPage() : new LinksPage();
            }
            page.setUri((IRI) bindings.getValue("uri"));
            if(bindings.hasBinding("scroll")) page.setScroll(((Literal) bindings.getValue("scroll")).intValue());
            if(page instanceof ObjectPage) {
                ObjectPage objectPage = (ObjectPage) page;
                objectPage.setItem(selectItem(page));
                objectPage.setSubPages(selectPages(page, Predicate.SUB));
                objectPage.setLinks(selectLinks(page));
            }
            if(page instanceof LinksPage) {
                LinksPage linksPage = (LinksPage) page;
                if(bindings.hasBinding("path")) linksPage.setPath(bindings.getValue("path").stringValue());
                linksPage.setTarget(selectPage(page, Predicate.TARGET));
            }
            if(page instanceof ListPage) {
                ListPage listPage = (ListPage) page;
                if(bindings.hasBinding("next")) listPage.setNext(bindings.getValue("next").stringValue());
                if(bindings.hasBinding("wait")) listPage.setWait(((Literal) bindings.getValue("wait")).intValue());
            }
            page.setParams(selectParams(page));
            pages.add(page);
        }
        result.close();
        return pages;
    }

    public List<String> selectParams(Page page) {
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

    public List<Property> selectProperties(Item item) {
        List<Property> properties = new ArrayList<Property>();
        TupleQueryResult result = select(
                "SELECT ?uri ?path ?optional ?attribute ?pattern ?type ?format\n" +
                "WHERE {\n" +
                    "<" + item.getUri() + "> <" + Predicate.PROPERTY + "> ?uri .\n" +
                    "?uri <" + Predicate.PATH + "> ?path\n" +
                    "OPTIONAL { ?uri <" + Predicate.OPTIONAL + "> ?optional }\n" +
                    "OPTIONAL { ?uri <" + Predicate.ATTRIBUTE + "> ?attribute }\n" +
                    "OPTIONAL { ?uri <" + Predicate.PATTERN + "> ?pattern }\n" +
                    "OPTIONAL { ?uri <" + Predicate.TYPE + "> ?type }\n" +
                    "OPTIONAL { ?uri <" + Predicate.FORMAT + "> ?format }\n" +
                "}\n" +
                "ORDER BY ?uri");
        while(result.hasNext()) {
            BindingSet bindings = result.next();
            Property property = new Property();
            property.setUri((IRI) bindings.getValue("uri"));
            property.setPath(bindings.getValue("path").stringValue());
            boolean optional = bindings.hasBinding("optional") && ((Literal) bindings.getValue("optional")).booleanValue();
            property.setOptional(optional);
            if(bindings.hasBinding("attribute")) property.setAttribute(bindings.getValue("attribute").stringValue());
            if(bindings.hasBinding("pattern")) property.setPattern(bindings.getValue("pattern").stringValue());
            property.setReplaces(selectReplaces(property));
            property.setType((bindings.hasBinding("type")) ? (IRI) bindings.getValue("type") : XMLSchema.STRING);
            if(bindings.hasBinding("format")) property.setFormat(bindings.getValue("format").stringValue());
            properties.add(property);
        }
        result.close();
        return properties;
    }

    public Map<String, String> selectReplaces(Property property) {
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

    public List<Section> selectSections() {
        List<Section> sections = new ArrayList<Section>();
        TupleQueryResult result = select(
                "SELECT ?uri\n" +
                "WHERE {\n" +
                    "<model:> <" + Predicate.SECTION + "> ?uri\n" +
                "}");
        while(result.hasNext()) {
            BindingSet bindings = result.next();
            Section section = new Section();
            section.setUri((IRI) bindings.getValue("uri"));
            section.setPage(selectPage(section, Predicate.PAGE));
            sections.add(section);
        }
        result.close();
        return sections;
    }

    protected ValueFactory factory() {
        return getConnection().getValueFactory();
    }

    protected void insert(Resource subject, IRI predicate, String object, IRI type, IRI context) {
        getConnection().add(subject, predicate, factory().createLiteral(object, type), context);
    }

}
