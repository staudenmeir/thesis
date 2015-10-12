package edu.tum.cs.crawler.repository;

import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;

public enum Type {

    ITEM("item"), ITEMS("items"), LIST("list");

    private String text;

    private static final String prefix = "crawler://type/";

    Type(String text){
        this.text = text;
    }

    public URI getUri(){
        return new URIImpl(toString());
    }

    public String toString(){
        return prefix + text;
    }

}