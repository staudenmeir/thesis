package edu.tum.cs.crawler.repository;

import org.openrdf.model.IRI;
import org.openrdf.model.ValueFactory;

public enum Type {

    LIST("list"), OBJECT("object"), OBJECTS("objects");

    private String text;

    private static final String prefix = "type://";

    Type(String text){
        this.text = text;
    }

    public IRI getUri(ValueFactory factory){
        return factory.createIRI(toString());
    }

    public String toString(){
        return prefix + text;
    }

}