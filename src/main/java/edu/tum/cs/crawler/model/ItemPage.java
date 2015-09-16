package edu.tum.cs.crawler.model;

import java.util.List;

public class ItemPage extends Page {

    protected Item item;

    protected List<Page> pages;

    protected List<Link> links;

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public List<Page> getPages() {
        return pages;
    }

    public void setPages(List<Page> pages) {
        this.pages = pages;
    }

    public List<Link> getLinks() {
        return links;
    }

    public void setLinks(List<Link> links) {
        this.links = links;
    }

}
