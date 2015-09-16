package edu.tum.cs.crawler.model;

import org.openrdf.model.URI;

import java.io.Serializable;

public abstract class Model implements Serializable {

    protected URI uri;

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

}
