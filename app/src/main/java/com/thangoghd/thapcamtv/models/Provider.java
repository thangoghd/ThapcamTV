package com.thangoghd.thapcamtv.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Provider {
    @SerializedName("id")
    private String id;
    
    @SerializedName("name")
    private String name;
    
    @SerializedName("groups")
    private List<Group> groups;

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<Group> getGroups() {
        return groups;
    }
}

