package edu.tum.cs.crawler.model;

import org.json.JSONArray;
import org.openrdf.model.URI;

import java.util.ArrayList;
import java.util.List;

public class Property extends Model {

    protected String path;

    protected boolean optional;

    protected String attribute;

    protected String pattern;

    protected List<List<String>> replace;

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

    public List<List<String>> getReplace() {
        return replace;
    }

    public void setReplace(String replace) {
        List<List<String>> list = new ArrayList<List<String>>();
        JSONArray array = new JSONArray(replace);
        for(int i = 0; i < array.length(); i++) {
            List<String> entry = (new ArrayList<String>());
            JSONArray array2 = (JSONArray) array.get(i);
            for(int j = 0; j < array2.length(); j++) {
                entry.add(array2.get(j).toString());
            }
            list.add(entry);
        }
        this.replace = list;
    }

    public boolean hasReplace() {
        return (replace != null);
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
