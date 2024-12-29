package com.thangoghd.thapcamtv.repositories;
import android.util.Log;

import androidx.annotation.NonNull;

import com.thangoghd.thapcamtv.api.SportApi;
import com.thangoghd.thapcamtv.models.Match;
import com.thangoghd.thapcamtv.response.MatchResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class SportRepository {
    private final SportApi api;
    private static final List<String> SPORT_PRIORITY = Arrays.asList(
        "live", "football", "basketball", "esports", "tennis", "volleyball", "badminton", "race", "pool", "wwe", "event", "other"
    );


    public SportRepository(SportApi api) {
        this.api = api;
    }

    public void getMatches(final RepositoryCallback<List<Match>> callback) {
        api.getLiveMatches().enqueue(new Callback<MatchResponse>() {
            @Override
            public void onResponse(@NonNull Call<MatchResponse> call, @NonNull Response<MatchResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d("SportRepository", "Received " + response.body().getData().size() + " matches");
                    callback.onSuccess(response.body().getData());
                } else {
                    Log.e("SportRepository", "API call failed: " + response.code());
                    callback.onError(new Exception("API call failed"));
                }
            }

            @Override
            public void onFailure(@NonNull Call<MatchResponse> call, @NonNull Throwable t) {
                Log.e("SportRepository", "API call failed", t);
                callback.onError((Exception) t);
            }
        });
    }

    public Map<String, List<Match>> getMatchesBySportType(List<Match> matches) {
        Map<String, List<Match>> groupedMatches = new LinkedHashMap<>();
        Map<String, List<Match>> tempGroupedMatches = new HashMap<>();
    
        // First, handle live matches
        List<Match> liveMatches = new ArrayList<>();
        
        // Group matches by sport type
        for (Match match : matches) {
            if (!"finished".equalsIgnoreCase(match.getMatchStatus()) &&
                !"canceled".equalsIgnoreCase(match.getMatchStatus())) {
                
                // Add to live matches if it's live
                if (match.getLive()) {
                    liveMatches.add(match);
                }
                
                tempGroupedMatches.computeIfAbsent(match.getSportType(), k -> new ArrayList<>()).add(match);
            }
        }
        
        // Add live matches first if there are any
        if (!liveMatches.isEmpty()) {
            // Sort live matches by sport type priority first, then by tournament priority
            liveMatches.sort((m1, m2) -> {
                // First compare by sport type priority
                int sport1Index = SPORT_PRIORITY.indexOf(m1.getSportType());
                int sport2Index = SPORT_PRIORITY.indexOf(m2.getSportType());
                if (sport1Index != sport2Index) {
                    return Integer.compare(sport1Index, sport2Index);
                }
                
                // If same sport type, compare by tournament priority
                return Integer.compare(m2.getTournament().getPriority(), m1.getTournament().getPriority());
            });
            groupedMatches.put("live", liveMatches);
        }

        // Sort matches by criteria
        for (List<Match> sportMatches : tempGroupedMatches.values()) {
            sportMatches.sort((m1, m2) -> {
                // Compare by broadcast status
                if (m1.getLive() != m2.getLive()) {
                    return m2.getLive() ? 1 : -1;
                }

                // Compare by match status (live)
                if ("live".equalsIgnoreCase(m1.getMatchStatus()) != "live".equalsIgnoreCase(m2.getMatchStatus())) {
                    return "live".equalsIgnoreCase(m2.getMatchStatus()) ? 1 : -1;
                }

                // Compare by "pending" status
                if ("pending".equalsIgnoreCase(m1.getMatchStatus()) != "pending".equalsIgnoreCase(m2.getMatchStatus())) {
                    return "pending".equalsIgnoreCase(m2.getMatchStatus()) ? 1 : -1;
                }

                // Compare by priority
                return Integer.compare(m2.getTournament().getPriority(), m1.getTournament().getPriority());
            });
        }

        // Add sorted matches to the final map based on SPORT_PRIORITY
        for (String sport : SPORT_PRIORITY) {
            if (tempGroupedMatches.containsKey(sport) && !sport.equals("live")) {
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
}
