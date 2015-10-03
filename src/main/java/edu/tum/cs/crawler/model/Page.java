package edu.tum.cs.crawler.model;

import org.openrdf.model.URI;

import java.util.List;

public abstract class Page extends Model {

    protected Integer scroll;

    protected List<URI> ignores;

    public Integer getScroll() {
        return scroll;
    }

    public void setScroll(Integer scroll) {
        this.scroll = scroll;
    }

    public boolean hasScroll() {
        return (scroll != null);
    }

    public void setIgnores(List<URI> ignores) {
        this.ignores = ignores;
    }

    public boolean ignored(URI uri) {
        for(URI ignore : ignores) {
            if(ignore.equals(uri)) return true;
        }
        return false;
    }

}
