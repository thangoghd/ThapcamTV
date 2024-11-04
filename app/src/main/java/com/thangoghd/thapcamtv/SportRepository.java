package com.thangoghd.thapcamtv;
import android.util.Log;

import androidx.annotation.NonNull;

import com.thangoghd.thapcamtv.models.Match;

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
import java.util.Set;
import java.util.stream.Collectors;

public class SportRepository {
    private final SportApi api;
    private static final List<String> SPORT_PRIORITY = Arrays.asList(
        "football", "basketball", "esports", "tennis", "volleyball", "badminton", "race", "pool", "wwe", "event"
    );

    public SportRepository(SportApi api) {
        this.api = api;
    }

    public void getLiveMatches(final RepositoryCallback<List<Match>> callback) {
        Log.d("SportRepository", "Fetching live matches...");
        api.getLiveMatches().enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse> call, @NonNull Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d("SportRepository", "Received " + response.body().getData().size() + " matches");
                    callback.onSuccess(response.body().getData());
                } else {
                    Log.e("SportRepository", "API call failed: " + response.code());
                    callback.onError(new Exception("API call failed"));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {
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
            if (!"finished".equalsIgnoreCase(match.getMatch_status()) && !"canceled".equalsIgnoreCase(match.getMatch_status())) {
                tempGroupedMatches.computeIfAbsent(match.getSport_type(), k -> new ArrayList<>()).add(match);
            }
        }

        calculateTopTwoPriorities(matches);

        // Sort matches by criteria
        for (List<Match> sportMatches : tempGroupedMatches.values()) {
            sportMatches.sort((m1, m2) -> {
                int priority1 = m1.getTournament().getPriority();
                int priority2 = m2.getTournament().getPriority();

                // Compare by top 2 priority
                if (isTopTwoPriority(priority1) != isTopTwoPriority(priority2)) {
                    return isTopTwoPriority(priority2) ? 1 : -1;
                }

                // Compare by broadcast status
                if (m1.getLive() != m2.getLive()) {
                    return m2.getLive() ? 1 : -1;
                }

                // Compare by match status (live)
                if ("live".equalsIgnoreCase(m1.getMatch_status()) != "live".equalsIgnoreCase(m2.getMatch_status())) {
                    return "live".equalsIgnoreCase(m2.getMatch_status()) ? 1 : -1;
                }

                // Compare by "pending" status
                if ("pending".equalsIgnoreCase(m1.getMatch_status()) != "pending".equalsIgnoreCase(m2.getMatch_status())) {
                    return "pending".equalsIgnoreCase(m2.getMatch_status()) ? 1 : -1;
                }

                // Compare by priority
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

    // This code is used to calculate and check the top two priorities in a list of matches
    private Set<Integer> topTwoPriorities;

    private void calculateTopTwoPriorities(List<Match> matches) {
        topTwoPriorities = matches.stream()
                .map(match -> match.getTournament().getPriority())
                .distinct()
                .sorted(Collections.reverseOrder())
                .limit(2)
                .collect(Collectors.toSet());
    }

    private boolean isTopTwoPriority(int priority) {
        return topTwoPriorities.contains(priority);
    }
}

interface RepositoryCallback<T> {
    void onSuccess(T result);
    void onError(Exception e);
}
