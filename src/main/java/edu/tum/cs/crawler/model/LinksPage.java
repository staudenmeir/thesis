package edu.tum.cs.crawler.model;

public class LinksPage extends Page implements ListPage {

    protected String path;

    protected String menu;

    protected String next;

    protected Integer wait;

    protected Page target;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getMenu() {
        return menu;
    }

    public void setMenu(String menu) {
        this.menu = menu;
    }

    public boolean hasMenu() {
        return (menu != null);
    }

    public String getNext() {
        return next;
    }

    public void setNext(String next) {
        this.next = next;
    }

    public boolean hasNext() {
        return (next != null);
    }

    public Integer getWait() {
        return wait;
    }

    public void setWait(Integer wait) {
        this.wait = wait;
    }

    public boolean hasWait() {
        return (wait != null);
    }

    public Page getTarget() {
        return target;
    }

    public void setTarget(Page target) {
        this.target = target;
    }

}
