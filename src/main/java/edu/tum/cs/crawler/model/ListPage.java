package edu.tum.cs.crawler.model;

public interface ListPage {

    String getNext();

    void setNext(String next);

    boolean hasNext();

    Integer getWait();

    void setWait(Integer wait);

    boolean hasWait();

}
