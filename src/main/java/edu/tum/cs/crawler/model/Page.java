package edu.tum.cs.crawler.model;

public abstract class Page extends Model {

    protected Integer scroll;

    public Integer getScroll() {
        return scroll;
    }

    public void setScroll(Integer scroll) {
        this.scroll = scroll;
    }

    public boolean hasScroll() {
        return (scroll != null);
    }

}
