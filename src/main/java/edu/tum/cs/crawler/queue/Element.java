package edu.tum.cs.crawler.queue;

import edu.tum.cs.crawler.model.Page;
import org.openrdf.model.URI;

import java.io.Serializable;

public class Element implements Serializable {

    protected URI uri;

    protected Page page;

    protected URI parent;

    public Element(URI uri, Page page, URI parent) {
        this.uri = uri;
        this.page = page;
        this.parent = parent;
    }

    public URI getUri() {
        return uri;
    }

    public Page getPage() {
        return page;
    }

    public URI getParent() {
        return parent;
    }

}
