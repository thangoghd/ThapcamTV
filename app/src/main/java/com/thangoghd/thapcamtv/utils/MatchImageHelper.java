package com.thangoghd.thapcamtv.utils;

import android.text.TextUtils;
import android.util.Log;
import com.thangoghd.thapcamtv.api.RetrofitClient;
import com.thangoghd.thapcamtv.api.SportApi;
import com.thangoghd.thapcamtv.models.Provider;
import com.thangoghd.thapcamtv.models.Group;
import com.thangoghd.thapcamtv.models.Channel;
import retrofit2.Response;

public class MatchImageHelper {
    private static final String TAG = "MatchImageHelper";
    
    public static String findMatchImage(String matchTitle, String defaultImage) {
        try {
            SportApi sportApi = RetrofitClient.getSportClient().create(SportApi.class);
            Response<Provider> response = sportApi.getProviders().execute();
            
            if (response.isSuccessful() && response.body() != null) {
                Provider provider = response.body();
                if (provider.getGroups() != null) {
                    for (Group group : provider.getGroups()) {
                        if ("match_live".equals(group.getId()) && group.getChannels() != null) {
                            // Normalize match title for comparison
                            String normalizedTitle = normalizeTitle(matchTitle);
                            
                            for (Channel channel : group.getChannels()) {
                                String normalizedChannelName = normalizeTitle(channel.getName());
                                
                                // Check if titles are similar
                                if (isSimilarTitles(normalizedTitle, normalizedChannelName)) {
                                    if (channel.getImage() != null && !TextUtils.isEmpty(channel.getImage().getUrl())) {
                                        return channel.getImage().getUrl();
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error finding match image", e);
        }
        
        return defaultImage;
    }
    
    private static String normalizeTitle(String title) {
        if (title == null) return "";
        // Remove special characters and convert to lowercase
        return title.toLowerCase()
                   .replaceAll("[^a-z0-9\\s]", "")
                   .replaceAll("\\s+", " ")
                   .trim();
    }
    
    private static boolean isSimilarTitles(String title1, String title2) {
        if (title1 == null || title2 == null) return false;
        
        // Normalize titles
        title1 = title1.toLowerCase().trim();
        title2 = title2.toLowerCase().trim();
        
        // Check if both titles contain "vs"
        boolean hasVs1 = title1.contains("vs");
        boolean hasVs2 = title2.contains("vs");
        
        // If neither has "vs", just compare the titles directly
        if (!hasVs1 && !hasVs2) {
            return title1.equals(title2);
        }
        
        // If only one has "vs", titles are different
        if (hasVs1 != hasVs2) {
            return false;
        }
        
        // Both have "vs", split and compare team names
        String[] teams1 = title1.split("vs");
        String[] teams2 = title2.split("vs");
        
        if (teams1.length != 2 || teams2.length != 2) {
            return false;
        }
        
        String home1 = teams1[0].trim();
        String away1 = teams1[1].trim();
        String home2 = teams2[0].trim();
        String away2 = teams2[1].trim();
        
        return home1.equals(home2) || away1.equals(away2) || home1.equals(away2) || home2.equals(away1);
    }
}
