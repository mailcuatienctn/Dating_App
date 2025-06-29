package com.example.datingapp.model;

public class Match {
    private String uidMatcher;
    private String nameMatcher;
    private String urlAvartar;

    public Match() {
    }

    public Match(String uidMatcher, String nameMatcher, String urlAvartar) {
        this.uidMatcher = uidMatcher;
        this.nameMatcher = nameMatcher;
        this.urlAvartar = urlAvartar;
    }

    public String getUidMatcher() {
        return uidMatcher;
    }

    public void setUidMatcher(String uidMatcher) {
        this.uidMatcher = uidMatcher;
    }

    public String getNameMatcher() {
        return nameMatcher;
    }

    public void setNameMatcher(String nameMatcher) {
        this.nameMatcher = nameMatcher;
    }

    public String getUrlAvartar() {
        return urlAvartar;
    }

    public void setUrlAvartar(String urlAvartar) {
        this.urlAvartar = urlAvartar;
    }

    @Override
    public String toString() {
        return "Match{" +
                "uidMatcher='" + uidMatcher + '\'' +
                ", nameMatcher='" + nameMatcher + '\'' +
                ", urlAvartar='" + urlAvartar + '\'' +
                '}';
    }
}
