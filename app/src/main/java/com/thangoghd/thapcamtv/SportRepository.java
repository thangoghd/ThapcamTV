package com.thangoghd.thapcamtv;
import android.util.Log;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SportRepository {
    private SportApi api;
    private static final List<String> SPORT_PRIORITY = Arrays.asList(
        "football", "basketball", "esports", "tennis", "volleyball", "badminton"
    );

    public SportRepository(SportApi api) {
        this.api = api;
    }

    public void getLiveMatches(final RepositoryCallback<List<Match>> callback) {
        Log.d("SportRepository", "Fetching live matches...");
        api.getLiveMatches().enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d("SportRepository", "Received " + response.body().getData().size() + " matches");
                    callback.onSuccess(response.body().getData());
                } else {
                    Log.e("SportRepository", "API call failed: " + response.code());
                    callback.onError(new Exception("API call failed"));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                Log.e("SportRepository", "API call failed", t);
                callback.onError((Exception) t);
            }
        });
    }

    public Map<String, List<Match>> getMatchesBySportType(List<Match> matches) {
        Map<String, List<Match>> groupedMatches = new LinkedHashMap<>();
        Map<String, List<Match>> tempGroupedMatches = new HashMap<>();
    
        // Group matches by sport type
        for (Match match : matches) {
            if (!"finished".equalsIgnoreCase(match.getMatch_status())) {
                tempGroupedMatches.computeIfAbsent(match.getSport_type(), k -> new ArrayList<>()).add(match);
            }
        }
    
        // Sort matches within each sport type
        for (List<Match> sportMatches : tempGroupedMatches.values()) {
            sportMatches.sort((m1, m2) -> {
                int priority1 = m1.getTournament().getPriority();
                int priority2 = m2.getTournament().getPriority();
                
                // Compare by priority groups
                if (isTopTwoPriority(priority1, matches) != isTopTwoPriority(priority2, matches)) {
                    return isTopTwoPriority(priority2, matches) ? 1 : -1;
                }
                
                // Compare by live broadcast
                if (m1.getLive() != m2.getLive()) {
                    return m2.getLive() ? 1 : -1;
                }
                
                // Compare by match status
                if ("live".equalsIgnoreCase(m1.getMatch_status()) != "live".equalsIgnoreCase(m2.getMatch_status())) {
                    return "live".equalsIgnoreCase(m2.getMatch_status()) ? 1 : -1;
                }
                
                // If all above are equal, sort by priority
                return Integer.compare(priority2, priority1);
            });
        }
    
        // Add sorted matches to the final map
        for (String sport : SPORT_PRIORITY) {
            if (tempGroupedMatches.containsKey(sport)) {
                groupedMatches.put(sport, tempGroupedMatches.get(sport));
            }
        }
    
        // Add remaining sports
        for (Map.Entry<String, List<Match>> entry : tempGroupedMatches.entrySet()) {
            if (!groupedMatches.containsKey(entry.getKey())) {
                groupedMatches.put(entry.getKey(), entry.getValue());
            }
        }
    
        return groupedMatches;
    }

    private boolean isTopTwoPriority(int priority, List<Match> matches) {
        List<Integer> priorities = new ArrayList<>();
        for (Match match : matches) {
            int matchPriority = match.getTournament().getPriority();
            if (!priorities.contains(matchPriority)) {
                priorities.add(matchPriority);
            }
        }
        priorities.sort(Collections.reverseOrder());
        return priorities.indexOf(priority) < 2;
    }
}

interface RepositoryCallback<T> {
    void onSuccess(T result);
    void onError(Exception e);
}
