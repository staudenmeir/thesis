package edu.tum.cs.crawler.model;

import java.util.List;

public class ItemPage extends Page {

    protected Item item;

    protected List<Page> subPages;

    protected List<Link> links;

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public List<Page> getSubPages() {
        return subPages;
    }

    public void setSubPages(List<Page> subPages) {
        this.subPages = subPages;
    }

    public List<Link> getLinks() {
        return links;
    }

    public void setLinks(List<Link> links) {
        this.links = links;
    }

}
