package com.thangoghd.thapcamtv;

import java.util.List;

public class Match {
    private String id;
    private String key_sync;
    private String name;
    private String slug;
    private String date;
    private long timestamp;
    private Team home;
    private Team away;
    private Tournament tournament;
    private Scores scores;
    private String sport_type;
    private String match_status;
    private List<Commentator> commentators;
    private boolean is_live;
    private String time_str;


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSync() {
        return key_sync;
    }

    public void setSync(String key_sync) {
        this.key_sync = key_sync;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getMatch_status() {
        return match_status;
    }

    public void setMatch_status(String match_status) {
        this.match_status = match_status;
    }

    public Team getHome() {
        return home;
    }

    public void setHome(Team home) {
        this.home = home;
    }

    public Team getAway() {
        return away;
    }

    public void setAway(Team away) {
        this.away = away;
    }

    public Tournament getTournament() {
        return tournament;
    }

    public void setTournament(Tournament tournament) {
        this.tournament = tournament;
    }

    public Scores getScores() {
        return scores;
    }

    public void setScores(Scores scores) {
        this.scores = scores;
    }

    public String getSport_type() {
        return sport_type;
    }

    public void setSport_type(String sport_type) {
        this.sport_type = sport_type;
    }

    public List<Commentator> getCommentators() {
        return commentators;
    }

    public void setCommentators(List<Commentator> commentators) {
        this.commentators = commentators;
    }

    public boolean getLive(){return is_live;}

    public String getTimeInMatch(){return time_str;}

    public void setTimeInMatch(String time_str){this.time_str = time_str;}



}

class Team {
    private String name;
    private String short_name;
    private String logo;

    // Getters and setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getShort_name() {
        return short_name;
    }

    public void setShort_name(String short_name) {
        this.short_name = short_name;
    }

    public String getLogo() {
        return logo;
    }

    public void setLogo(String logo) {
        this.logo = logo;
    }
}

class Tournament {
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

class Scores {
    private int home;
    private int away;

    // Getters and setters
    public int getHome() {
        return home;
    }

    public void setHome(int home) {
        this.home = home;
    }

    public int getAway() {
        return away;
    }

    public void setAway(int away) {
        this.away = away;
    }
}

class Commentator {
    private String name;
    private String avatar;
    private String url;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}

