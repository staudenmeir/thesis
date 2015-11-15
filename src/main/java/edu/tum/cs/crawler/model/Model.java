package edu.tum.cs.crawler.model;

import org.openrdf.model.IRI;

import java.io.Serializable;

public abstract class Model implements Serializable {

    protected IRI uri;

    public IRI getUri() {
        return uri;
    }

    public void setUri(IRI uri) {
        this.uri = uri;
    }

}
