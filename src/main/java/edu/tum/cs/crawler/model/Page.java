package edu.tum.cs.crawler.model;

import java.util.List;

public abstract class Page extends Model {

    protected Integer scroll;

    protected List<String> params;

    public Integer getScroll() {
        return scroll;
    }

    public void setScroll(Integer scroll) {
        this.scroll = scroll;
    }

    public boolean hasScroll() {
        return (scroll != null);
    }

    public List<String> getParams() {
        return params;
    }

    public void setParams(List<String> params) {
        this.params = params;
    }

}
