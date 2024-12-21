package com.thangoghd.thapcamtv.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class GitHubRelease {
    public static final String APK_FILENAME = "ThapcamTV.apk";

    @SerializedName("tag_name")
    private String tagName;
    
    @SerializedName("name")
    private String name;
    
    @SerializedName("body")
    private String body;
    
    @SerializedName("assets")
    private List<Asset> assets;
    
    public static class Asset {
        @SerializedName("browser_download_url")
        private String downloadUrl;
        
        @SerializedName("name")
        private String name;
        
        @SerializedName("size")
        private long size;

        public String getDownloadUrl() {
            return downloadUrl;
        }

        public String getName() {
            return name;
        }

        public long getSize() {
            return size;
        }
    }

    public String getTagName() {
        return tagName;
    }

    public String getName() {
        return name;
    }

    public String getBody() {
        return body;
    }

    public String getDownloadUrl() {
        if (assets != null) {
            for (Asset asset : assets) {
                if (APK_FILENAME.equals(asset.name)) {
                    return asset.downloadUrl;
                }
            }
        }
        return null;
    }
}
