package edu.tum.cs.crawler.repository;

public enum Predicate {

    ATTRIBUTE("attribute"), FORMAT("format"), ID("id"), IGNORE("ignore"), ITEM("item"), LINK("link"), NEW("new"),
    NEXT("next"), OLD("old"), OPTIONAL("optional"), ORDER("order"), PAGE("page"), PARAM("param"), PATH("path"),
    PROPERTY("property"), REGEX("regex"), REPLACE("replace"), SCROLL("scroll"), SECTION("section"), SUB("sub"),
    TARGET("target"), TYPE("type"), WAIT("wait");

    private String text;

    private static final String prefix = "crawler://predicate/";

    Predicate(String text){
        this.text = text;
    }

    public String toString(){
        return prefix + text;
    }

}