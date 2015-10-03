package edu.tum.cs.crawler.model;

public class ItemsPage extends ItemPage implements ListPage {

    protected String next;

    protected Integer wait;

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

}
