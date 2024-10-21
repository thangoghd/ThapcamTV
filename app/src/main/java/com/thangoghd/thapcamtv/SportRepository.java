package com.thangoghd.thapcamtv;
import android.util.Log;

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
        Map<String, List<Match>> groupedMatches = new LinkedHashMap<>(); // Use LinkedHashMap to maintain insertion order

        // First, group matches by sport type
        Map<String, List<Match>> tempGroupedMatches = new HashMap<>();
        for (Match match : matches) {
            if (!"finished".equalsIgnoreCase(match.getMatch_status())) { // Filter out finished matches
                tempGroupedMatches.computeIfAbsent(match.getSport_type(), k -> new ArrayList<>()).add(match);
            }
        }

        // Then, add sports in the priority order
        for (String sport : SPORT_PRIORITY) {
            if (tempGroupedMatches.containsKey(sport)) {
                List<Match> sportMatches = tempGroupedMatches.get(sport);
                // Sort matches by status: live first, then pending
                sportMatches.sort((m1, m2) -> {
                    if ("live".equalsIgnoreCase(m1.getMatch_status()) && !"live".equalsIgnoreCase(m2.getMatch_status())) {
                        return -1;
                    } else if (!"live".equalsIgnoreCase(m1.getMatch_status()) && "live".equalsIgnoreCase(m2.getMatch_status())) {
                        return 1;
                    } else {
                        return 0;
                    }
                });
                groupedMatches.put(sport, sportMatches);
            }
        }

        // Finally, add any remaining sports (which will include "Other")
        for (Map.Entry<String, List<Match>> entry : tempGroupedMatches.entrySet()) {
            if (!groupedMatches.containsKey(entry.getKey())) {
                List<Match> sportMatches = entry.getValue();
                // Sort matches by status: live first, then pending
                sportMatches.sort((m1, m2) -> {
                    if ("live".equalsIgnoreCase(m1.getMatch_status()) && !"live".equalsIgnoreCase(m2.getMatch_status())) {
                        return -1;
                    } else if (!"live".equalsIgnoreCase(m1.getMatch_status()) && "live".equalsIgnoreCase(m2.getMatch_status())) {
                        return 1;
                    } else {
                        return 0;
                    }
                });
                groupedMatches.put(entry.getKey(), sportMatches);
            }
        }

        return groupedMatches;
    }
}

interface RepositoryCallback<T> {
    void onSuccess(T result);
    void onError(Exception e);
}
