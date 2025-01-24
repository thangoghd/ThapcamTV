package com.thangoghd.thapcamtv.models;

import com.google.gson.annotations.SerializedName;

public class ProviderMatch {
    @SerializedName("name")
    private String name;
    
    @SerializedName("url")
    private String imageUrl;

    public String getName() {
        return name;
    }

    public String getImageUrl() {
        return imageUrl;
    }
}
