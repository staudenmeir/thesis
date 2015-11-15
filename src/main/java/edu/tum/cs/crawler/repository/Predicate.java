package edu.tum.cs.crawler.repository;

public enum Predicate {

    ATTRIBUTE("attribute"), FORMAT("format"), ID("id"), ITEM("item"), LINK("link"), NEW("new"), NEXT("next"),
    OLD("old"), OPTIONAL("optional"), ORDER("order"), PAGE("page"), PARAM("param"), PATH("path"), PATTERN("pattern"),
    PROPERTY("property"), REPLACE("replace"), SCROLL("scroll"), SECTION("section"), SUB("sub"), TARGET("target"),
    TYPE("type"), WAIT("wait");

    private String text;

    private static final String prefix = "predicate://";

    Predicate(String text){
        this.text = text;
    }

    public String toString(){
        return prefix + text;
    }

}