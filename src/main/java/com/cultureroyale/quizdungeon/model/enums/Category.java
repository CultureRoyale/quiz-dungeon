package com.cultureroyale.quizdungeon.model.enums;

public enum Category {
    MUSIQUE("musique"),
    CULTURE_GENERALE("culture_generale"),
    ART_LITTERATURE("art_litterature"),
    TV_CINEMA("tv_cinema"),
    ACTU_POLITIQUE("actu_politique"),
    SPORT("sport"),
    JEUX_VIDEOS("jeux_videos"),
    HISTOIRE("histoire"),
    GEOGRAPHIE("geographie"),
    SCIENCE("science"),
    GASTRONOMIE("gastronomie");

    private final String value;

    Category(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
