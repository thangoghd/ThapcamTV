package com.thangoghd.thapcamtv;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.thangoghd.thapcamtv.models.Match;
import com.thangoghd.thapcamtv.adapters.MatchesAdapter;
import com.thangoghd.thapcamtv.adapters.SportsAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Response;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.net.ssl.HttpsURLConnection;
import java.security.cert.X509Certificate;

public class LiveFragment extends Fragment {
    private RecyclerView recyclerViewSports;
    private RecyclerView recyclerViewMatches;
    private SportRepository sportRepository;
    private List<Match> matches; // Store matches for the selected sport
    private SportsAdapter sportsAdapter; // Adapter for sports categories
    private MatchesAdapter matchesAdapter;
    private SportType[] availableSportTypes;
    private int currentSportIndex = 0;
    private boolean isInitialLoad = true; // Add a variable to track the first load
    private boolean isLoading = false; // Add this variable to track loading state
    private ImageView backgroundImageView;
    private final Handler refreshHandler = new Handler(Looper.getMainLooper());
    private final long REFRESH_INTERVAL = 30000; // 30 seconds
    private Runnable refreshRunnable;
    public int focusedPosition = RecyclerView.NO_POSITION;
    private Map<String, List<Match>> matchesCache = new HashMap<>(); // Add cache for matches
    private View loadingView; // Add loading view reference

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_live, container, false);

        allowAllSSL();

        backgroundImageView = view.findViewById(R.id.backgroundImageView);
        recyclerViewSports = view.findViewById(R.id.recyclerViewSports);
        recyclerViewMatches = view.findViewById(R.id.recyclerViewMatches);
        loadingView = view.findViewById(R.id.loadingView); // Initialize loading view

        SportApi sportApi = ApiManager.getSportApi(false);
        sportRepository = new SportRepository(sportApi);

        setupSportsRecyclerView();
        // Load matches for the default sport (football)
        onSportSelected(currentSportIndex);

        setupPeriodicRefresh();

        return view;
    }

    private void setupSportsRecyclerView() {
        recyclerViewSports.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false)
        );

        // Initialize with an empty array, will be updated after data is available
        availableSportTypes = SportType.values();
        sportsAdapter = new SportsAdapter(availableSportTypes, this::onSportSelected);
        recyclerViewSports.setAdapter(sportsAdapter);
    }

    private void onSportSelected(int index) {
        if (availableSportTypes != null && index >= 0 && index < availableSportTypes.length && !isLoading) {
            currentSportIndex = index;
            SportType selectedSport = availableSportTypes[index];

            // Mark the first load as completed
            isInitialLoad = false;

            // Load matches by the selected sport type
            loadMatches(selectedSport.getKey());
            changeBackground(selectedSport);

            // Update UI
            requireActivity().runOnUiThread(() -> {
                // If only one sport element needs to be changed, use notifyItemChanged
                sportsAdapter.notifyItemChanged(index);

                // Ensure focus on the selected sport
                recyclerViewSports.smoothScrollToPosition(index);
                recyclerViewSports.post(() -> {
                    RecyclerView.ViewHolder viewHolder = recyclerViewSports.findViewHolderForAdapterPosition(index);
                    if (viewHolder != null) {
                        viewHolder.itemView.requestFocus();
                    }
                });
            });
        }
    }

    private void loadMatches(String sportTypeKey) {
        // Check cache first
        if (matchesCache.containsKey(sportTypeKey)) {
            List<Match> cachedMatches = matchesCache.get(sportTypeKey);
            matches = cachedMatches;
            updateMatchesRecyclerView();
            // Load fresh data in background
            loadMatchesFromNetwork(sportTypeKey, false);
            return;
        }

        // No cache, load from network with loading indicator
        loadMatchesFromNetwork(sportTypeKey, true);
    }

    private void loadMatchesFromNetwork(String sportTypeKey, boolean showLoadingIndicator) {
        isLoading = true;
        if (showLoadingIndicator) {
            showLoading(true);
        }

        sportRepository.getLiveMatches(new RepositoryCallback<List<Match>>() {
            @Override
            public void onSuccess(List<Match> result) {
                if (!isAdded()) {
                    isLoading = false;
                    showLoading(false);
                    return;
                }

                Map<String, List<Match>> matchesBySportType = sportRepository.getMatchesBySportType(result);
                matches = matchesBySportType.get(sportTypeKey);
                
                // Update cache
                matchesCache.clear(); // Clear old cache
                matchesCache.putAll(matchesBySportType);

                if (isInitialLoad || availableSportTypes.length == 0) {
                    updateSportsAdapter(matchesBySportType);
                }

                updateSportsAdapter(matchesBySportType);
                updateMatchesRecyclerView();
                
                isLoading = false;
                showLoading(false);
            }

            @Override
            public void onError(Exception e) {
                isLoading = false;
                showLoading(false);
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Không thể tải dữ liệu của các trận đấu.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void updateSportsAdapter(Map<String, List<Match>> matchesBySportType) {
        List<SportType> availableSports = new ArrayList<>();
        // Filter sports categories with matches
        for (SportType sportType : SportType.values()) {
            List<Match> sportMatches = matchesBySportType.get(sportType.getKey());
            if (sportMatches != null && !sportMatches.isEmpty()) {
                availableSports.add(sportType);
            }
        }

        // Compare the new list with the current list
        SportType[] newSportTypes = availableSports.toArray(new SportType[0]);
        if (newSportTypes.length == availableSportTypes.length) {
            boolean isSame = true;
            for (int i = 0; i < newSportTypes.length; i++) {
                if (!newSportTypes[i].equals(availableSportTypes[i])) {
                    isSame = false;
                    break;
                }
            }

            // If the list does not change, no update is needed
            if (isSame) return;
        }

        // Get the index of the previous focus position
        int previousFocusPosition = currentSportIndex;

        availableSportTypes = newSportTypes;
        sportsAdapter.updateSports(availableSportTypes);

        // Ensure focus on the selected sport
        recyclerViewSports.post(() -> {
            if (previousFocusPosition >= 0 && previousFocusPosition < availableSportTypes.length) {
                recyclerViewSports.scrollToPosition(previousFocusPosition);
                RecyclerView.ViewHolder viewHolder = recyclerViewSports.findViewHolderForAdapterPosition(previousFocusPosition);
                if (viewHolder != null) {
                    viewHolder.itemView.requestFocus();
                }
            }
        });
    }

    private void updateMatchesRecyclerView() {
        if (matchesAdapter == null) {
            // Initialize matchesAdapter and pass listener to handle onClick event
            matchesAdapter = new MatchesAdapter(matches, this, matchId -> {
                // Call the fetchMatchStreamUrl function when the user clicks on a match
                fetchMatchStreamUrl(matchId);
            });
            // Set LayoutManager for recyclerViewMatches
            recyclerViewMatches.setLayoutManager(new GridLayoutManager(getContext(), 3));
            recyclerViewMatches.addItemDecoration(new SpaceItemDecoration(0, 0, 0, 20));
            recyclerViewMatches.setAdapter(matchesAdapter);
        } else {
            // Update the adapter with new matches
            matchesAdapter.updateMatches(matches);
        }

        for (int i = 0; i < matchesAdapter.getItemCount(); i++) {
            MatchesAdapter.MatchViewHolder holder = (MatchesAdapter.MatchViewHolder) recyclerViewMatches.findViewHolderForAdapterPosition(i);
            if (holder != null) {
                holder.bind(matches.get(i), this);
            }
        }
    }

    private void setupPeriodicRefresh() {
        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                if (currentSportIndex >= 0 && availableSportTypes != null &&
                        currentSportIndex < availableSportTypes.length) {
                    refreshMatches(availableSportTypes[currentSportIndex].getKey());
                }
                refreshHandler.postDelayed(this, REFRESH_INTERVAL);
            }
        };
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshHandler.post(refreshRunnable);
    }

    @Override
    public void onPause() {
        super.onPause();
        refreshHandler.removeCallbacks(refreshRunnable);
    }

    private void refreshMatches(String sportTypeKey) {
        sportRepository.getLiveMatches(new RepositoryCallback<List<Match>>() {
            @Override
            public void onSuccess(List<Match> result) {
                // Process the result list to update directly into `matches` without creating a new list
                if (matches != null) {
                    List<Integer> updatedIndices = new ArrayList<>();

                    for (Match newMatch : result) {
                        for (int i = 0; i < matches.size(); i++) {
                            Match currentMatch = matches.get(i);

                            // Check if the match needs to be updated
                            if (currentMatch.getId().equals(newMatch.getId())) {
                                // Only update necessary information
                                if (!currentMatch.getScores().equals(newMatch.getScores()) ||
                                        !currentMatch.getTimeInMatch().equals(newMatch.getTimeInMatch()) ||
                                        !currentMatch.getMatch_status().equals(newMatch.getMatch_status())) {

                                    currentMatch.setScores(newMatch.getScores());
                                    currentMatch.setTimeInMatch(newMatch.getTimeInMatch());
                                    currentMatch.setMatch_status(newMatch.getMatch_status());

                                    updatedIndices.add(i);
                                }
                                break;
                            }
                        }
                    }

                    // Update UI more efficiently
                    requireActivity().runOnUiThread(() -> {
                        for (int index : updatedIndices) {
                            matchesAdapter.notifyItemChanged(index);
                        }

                        // Restore focus if necessary
                        if (focusedPosition != RecyclerView.NO_POSITION) {
                            RecyclerView.ViewHolder viewHolder =
                                    recyclerViewMatches.findViewHolderForAdapterPosition(focusedPosition);
                            if (viewHolder != null) {
                                viewHolder.itemView.requestFocus();
                            }
                        }

                        // Clear updatedIndices to free up temporary memory
                        updatedIndices.clear();
                    });
                }
            }

            @Override
            public void onError(Exception e) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Không thể lấy dữ liệu của các trận đấu.", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void fetchMatchStreamUrl(String matchId) {
        SportApi sportApi = ApiManager.getSportApi(false);
        Call<JsonObject> call = sportApi.getMatchStreamUrl(matchId);

        call.enqueue(new retrofit2.Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                if (!isAdded()) return;

                if (response.isSuccessful() && response.body() != null) {
                    String jsonResponse = response.body().toString();
                    parseJsonAndStartPlayer(jsonResponse);
                } else {
                    String errorMessage = "Mã lỗi: " + response.code();
                    Log.e("API_ERROR", errorMessage);
                    requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Lỗi khi tải luồng phát sóng." + errorMessage, Toast.LENGTH_SHORT).show());
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Lỗi khi tải luồng phát sóng.", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void parseJsonAndStartPlayer(String jsonResponse) {
        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(jsonResponse, JsonObject.class);

        // Check if "play_urls" is null
        if (jsonObject.getAsJsonObject("data").get("play_urls").isJsonNull()) {
            requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Trận đấu chưa được phát sóng.", Toast.LENGTH_SHORT).show());
            return;
        }

        JsonArray playUrls = jsonObject.getAsJsonObject("data").getAsJsonArray("play_urls");

        HashMap<String, String> qualityMap = new HashMap<>();
        for (JsonElement element : playUrls) {
            JsonObject urlObject = element.getAsJsonObject();
            String name = urlObject.get("name").getAsString();
            String url = urlObject.get("url").getAsString();
            qualityMap.put(name, url);
        }

        if (!qualityMap.isEmpty()) {
            startVideoPlayer(qualityMap);
        } else {
            requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Trận đấu chưa được phát sóng.", Toast.LENGTH_SHORT).show());
        }
    }

    private void startVideoPlayer(HashMap<String, String> qualityMap) {
        requireActivity().runOnUiThread(() -> {
            Intent intent = new Intent(getContext(), PlayerActivity.class);
            intent.putExtra("stream_url", qualityMap);
            intent.putExtra("source_type", "live");
            startActivity(intent);
        });
    }

    private void allowAllSSL() {
        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, new TrustManager[] { new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) {}
                @Override
                public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
            } }, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void changeBackground(SportType sportType) {
        int newBackgroundResource = R.drawable.background_other;

        switch (sportType) {
            case ESPORTS:
                newBackgroundResource = R.drawable.background_esports;
                break;
            case BASKETBALL:
                newBackgroundResource = R.drawable.background_basketball;
                break;
            case FOOTBALL:
                newBackgroundResource = R.drawable.background_football;
                break;
            case RACE:
                newBackgroundResource = R.drawable.background_race;
                break;
            case BOXING:
                newBackgroundResource = R.drawable.background_wwe;
                break;
            case VOLLEYBALL:
                newBackgroundResource = R.drawable.background_volleyball;
                break;
            case TENNIS:
                newBackgroundResource = R.drawable.background_tennis;
                break;
            case BADMINTON:
                newBackgroundResource = R.drawable.background_badminton;
                break;
            case BILLIARD:
                newBackgroundResource = R.drawable.background_pool;
                break;
            default:
                break;
        }

        final int backgroundResourceToSet = newBackgroundResource;

        backgroundImageView.animate()
                .alpha(0f)
                .setDuration(300)
                .withEndAction(() -> {
                    backgroundImageView.setImageResource(backgroundResourceToSet);
                    backgroundImageView.animate()
                            .alpha(1f)
                            .setDuration(300)
                            .start();
                })
                .start();
    }

    private void showLoading(boolean show) {
        if (loadingView != null) {
            loadingView.setVisibility(show ? View.VISIBLE : View.GONE);
            recyclerViewMatches.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }
}