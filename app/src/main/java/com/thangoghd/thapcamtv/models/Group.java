package com.thangoghd.thapcamtv.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Group {
    @SerializedName("id")
    private String id;
    
    @SerializedName("name")
    private String name;
    
    @SerializedName("channels")
    private List<Channel> channels;

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<Channel> getChannels() {
        return channels;
    }
}
