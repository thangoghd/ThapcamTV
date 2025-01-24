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
        // Simple similarity check - can be improved with more sophisticated algorithms
        if (title1.equals(title2)) return true;
        
        // Check if one title contains the other
        if (title1.contains(title2) || title2.contains(title1)) return true;
        
        // Split titles into words and check for common words
        String[] words1 = title1.split("\\s+");
        String[] words2 = title2.split("\\s+");
        
        int commonWords = 0;
        for (String word1 : words1) {
            for (String word2 : words2) {
                if (word1.equals(word2) && word1.length() > 2) { // ignore short words
                    commonWords++;
                }
            }
        }
        
        // If more than 50% of words match
        return commonWords >= Math.min(words1.length, words2.length) / 2;
    }
}
