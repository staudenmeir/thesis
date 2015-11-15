package edu.tum.cs.crawler.model;

import org.openrdf.model.IRI;

import java.util.Map;

public class Property extends Model {

    protected String path;

    protected boolean optional;

    protected String attribute;

    protected String pattern;

    protected Map<String, String> replaces;

    protected IRI type;

    protected String format;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public boolean isOptional() {
        return optional;
    }

    public void setOptional(boolean optional) {
        this.optional = optional;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public boolean hasPattern() {
        return (pattern != null);
    }

    public String getAttribute() {
        return attribute;
    }

    public void setAttribute(String attribute) {
        this.attribute = attribute;
    }

    public boolean hasAttribute() {
        return (attribute != null);
    }

    public Map<String, String> getReplaces() {
        return replaces;
    }

    public void setReplaces(Map<String, String> replaces) {
        this.replaces = replaces;
    }

    public IRI getType() {
        return type;
    }

    public void setType(IRI type) {
        this.type = type;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

}
