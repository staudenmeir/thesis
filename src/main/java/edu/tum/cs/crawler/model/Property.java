package edu.tum.cs.crawler.model;

import org.openrdf.model.URI;

import java.util.Map;

public class Property extends Model {

    protected String path;

    protected boolean optional;

    protected String attribute;

    protected String regex;

    protected Map<String, String> replaces;

    protected URI type;

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

    public String getRegex() {
        return regex;
    }

    public void setRegex(String regex) {
        this.regex = regex;
    }

    public boolean hasRegex() {
        return (regex != null);
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

    public URI getType() {
        return type;
    }

    public void setType(URI type) {
        this.type = type;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

}
