package edu.tum.cs.crawler.repository;

public enum Predicate {

    ATTRIBUTE("attribute"), FORMAT("format"), ID("id"), ITEM("item"), LINK("link"), MENU("menu"), NEXT("next"),
    OPTIONAL("optional"), PAGE("page"), PATH("path"), PATTERN("pattern"), PROPERTY("property"), REPLACE("replace"),
    SCROLL("scroll"), SECTION("section"), TARGET("target"), TYPE("type"), WAIT("wait");

    private String text;

    private static final String prefix = "http://crawler/predicate/";

    Predicate(String text){
        this.text = text;
    }

    public String toString(){
        return prefix + text;
    }

}