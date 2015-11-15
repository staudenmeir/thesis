package edu.tum.cs.crawler.queue;

import edu.tum.cs.crawler.model.Page;
import org.openrdf.model.IRI;

import java.io.Serializable;

public class Element implements Serializable {

    protected IRI uri;

    protected Page page;

    protected IRI parent;

    protected IRI context;

    public Element(IRI uri, Page page, IRI parent, IRI context) {
        this.uri = uri;
        this.page = page;
        this.parent = parent;
        this.context = context;
    }

    public IRI getUri() {
        return uri;
    }

    public Page getPage() {
        return page;
    }

    public IRI getParent() {
        return parent;
    }

    public IRI getContext() {
        return context;
    }

}
