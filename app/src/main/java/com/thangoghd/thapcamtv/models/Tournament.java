package com.thangoghd.thapcamtv.models;

public class Tournament {
    private String name;
    private String logo;
    private Integer priority;

    // Getters and setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLogo() {
        return logo;
    }

    public void setLogo(String logo) {
        this.logo = logo;
    }

    public Integer getPriority(){return priority;}
}
