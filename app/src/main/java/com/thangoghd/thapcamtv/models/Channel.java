package com.thangoghd.thapcamtv.models;

import com.google.gson.annotations.SerializedName;

public class Channel {
    @SerializedName("id")
    private String id;
    
    @SerializedName("name")
    private String name;
    
    @SerializedName("image")
    private Image image;

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Image getImage() {
        return image;
    }
}
