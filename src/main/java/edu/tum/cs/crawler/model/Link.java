package edu.tum.cs.crawler.model;

public class Link extends Model {

    protected String path;

    protected Page target;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Page getTarget() {
        return target;
    }

    public void setTarget(Page target) {
        this.target = target;
    }

}
