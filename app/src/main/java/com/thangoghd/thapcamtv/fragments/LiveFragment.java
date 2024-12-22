package com.thangoghd.thapcamtv.fragments;

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
import com.thangoghd.thapcamtv.PlayerActivity;
import com.thangoghd.thapcamtv.R;
import com.thangoghd.thapcamtv.SpaceItemDecoration;
import com.thangoghd.thapcamtv.SportType;
import com.thangoghd.thapcamtv.adapters.MatchesAdapter;
import com.thangoghd.thapcamtv.adapters.SportsAdapter;
import com.thangoghd.thapcamtv.api.ApiManager;
import com.thangoghd.thapcamtv.api.SportApi;
import com.thangoghd.thapcamtv.models.Match;
import com.thangoghd.thapcamtv.repositories.RepositoryCallback;
import com.thangoghd.thapcamtv.repositories.SportRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

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

        // First try to load from vebo.xyz
        SportApi sportApi = ApiManager.getSportApi(true);
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
        if (availableSportTypes != null && index >= 0 && index < availableSportTypes.length) {
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
        loadMatchesFromNetwork(sportTypeKey, true);
    }

    private void loadMatchesFromNetwork(String sportTypeKey, boolean showLoadingIndicator) {
        if (showLoadingIndicator) {
            showLoading(true);
        }

        // Create repositories for both APIs
        SportApi veboApi = ApiManager.getSportApi(true);
        SportApi thapcamApi = ApiManager.getSportApi(false);
        SportRepository veboRepository = new SportRepository(veboApi);
        SportRepository thapcamRepository = new SportRepository(thapcamApi);

        final List<Match> allMatches = new ArrayList<>();
        final AtomicInteger completedCalls = new AtomicInteger(0);

        // Load matches from vebo.xyz
        veboRepository.getLiveMatches(new RepositoryCallback<List<Match>>() {
            @Override
            public void onSuccess(List<Match> result) {
                if (!isAdded()) {
                    showLoading(false);
                    return;
                }

                // Add vebo matches first (priority)
                synchronized (allMatches) {
                    allMatches.addAll(result);
                }

                // Check if both APIs have completed
                if (completedCalls.incrementAndGet() == 2) {
                    processAllMatches(allMatches, sportTypeKey);
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e("LiveFragment", "Error loading vebo matches", e);
                if (completedCalls.incrementAndGet() == 2) {
                    processAllMatches(allMatches, sportTypeKey);
                }
            }
        });

        // Load matches from thapcam.xyz
        thapcamRepository.getLiveMatches(new RepositoryCallback<List<Match>>() {
            @Override
            public void onSuccess(List<Match> result) {
                if (!isAdded()) {
                    showLoading(false);
                    return;
                }

                // Add thapcam matches after vebo matches
                synchronized (allMatches) {
                    allMatches.addAll(result);
                }

                // Check if both APIs have completed
                if (completedCalls.incrementAndGet() == 2) {
                    processAllMatches(allMatches, sportTypeKey);
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e("LiveFragment", "Error loading thapcam matches", e);
                if (completedCalls.incrementAndGet() == 2) {
                    processAllMatches(allMatches, sportTypeKey);
                }
            }
        });
    }

    private void processAllMatches(List<Match> allMatches, String sportTypeKey) {
        if (!isAdded()) {
            showLoading(false);
            return;
        }

        // Create a map to store matches by key_sync
        Map<String, Match> matchMap = new HashMap<>();

        // Iterate over the list in the correct order (vebo first, thapcam last)
        for (Match match : allMatches) {
            String keySync = match.getSync();
            if (keySync != null) {
                // If it's football and already exists in the map, keep the vebo data (vebo priority)
                if ("football".equals(match.getSport_type()) && matchMap.containsKey(keySync)) {
                    continue;
                }
                // Otherwise, add the match to the map
                matchMap.put(keySync, match);
            }
        }

        // Convert the map values to a list
        List<Match> uniqueMatches = new ArrayList<>(matchMap.values());

        Map<String, List<Match>> matchesBySportType = new SportRepository(null).getMatchesBySportType(uniqueMatches);
        matches = matchesBySportType.get(sportTypeKey);
        matchesCache.putAll(matchesBySportType);

        if (isInitialLoad || availableSportTypes.length == 0) {
            updateSportsAdapter(matchesBySportType);
        }

        updateSportsAdapter(matchesBySportType);
        updateMatchesRecyclerView();
        showLoading(false);

        if (uniqueMatches.isEmpty() && getContext() != null) {
            Toast.makeText(getContext(), "Không thể tải dữ liệu của các trận đấu.", Toast.LENGTH_SHORT).show();
        }
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
        // Start PlayerActivity immediately with loading state
        Intent intent = new Intent(getContext(), PlayerActivity.class);
        intent.putExtra("source_type", "live");
        intent.putExtra("is_loading", true);
        intent.putExtra("match_id", matchId);
        startActivity(intent);

        SportApi api;
        Call<JsonObject> call;

        // Find the match to determine its sport type
        Match currentMatch = null;
        if (matches != null) {
            for (Match match : matches) {
                if (match.getId().equals(matchId)) {
                    currentMatch = match;
                    break;
                }
            }
        }

        // If it's football, use vebo.xyz API
        if (currentMatch != null && "football".equals(currentMatch.getSport_type())) {
            api = ApiManager.getSportApi(true); // vebo.xyz
            call = api.getVeboStreamUrl(matchId);
        } else {
            // For other sports, use thapcam.xyz API
            api = ApiManager.getSportApi(false); // thapcam.xyz
            // Add "tc" prefix if not already present
            String tcMatchId = matchId.startsWith("tc") ? matchId.substring(2) : matchId;
            call = api.getThapcamStreamUrl(tcMatchId);
        }

        call.enqueue(new retrofit2.Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                if (!isAdded()) return;

                if (response.isSuccessful() && response.body() != null) {
                    String jsonResponse = response.body().toString();
                    parseJsonAndStartPlayer(jsonResponse, true); // true indicates this is a background load
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                // No need to show toast as PlayerActivity will handle the error
            }
        });
    }

    private void parseJsonAndStartPlayer(String jsonResponse, boolean isBackgroundLoad) {
        try {
            Gson gson = new Gson();
            JsonObject jsonObject = gson.fromJson(jsonResponse, JsonObject.class);
            JsonObject data = jsonObject.getAsJsonObject("data");

            // Check if data is null or play_urls is null/empty
            if (data == null || data.get("play_urls") == null || data.get("play_urls").isJsonNull()) {
                if (!isBackgroundLoad) {
                    requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Trận đấu chưa được phát sóng.", Toast.LENGTH_SHORT).show());
                }
                return;
            }

            JsonArray playUrls = data.getAsJsonArray("play_urls");
            if (playUrls == null || playUrls.size() == 0) {
                if (!isBackgroundLoad) {
                    requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Trận đấu chưa được phát sóng.", Toast.LENGTH_SHORT).show());
                }
                return;
            }

            HashMap<String, String> qualityMap = new HashMap<>();
            for (JsonElement element : playUrls) {
                if (!element.isJsonObject()) continue;
                
                JsonObject urlObject = element.getAsJsonObject();
                if (!urlObject.has("name") || !urlObject.has("url")) continue;

                String name = urlObject.get("name").getAsString();
                String url = urlObject.get("url").getAsString();
                qualityMap.put(name, url);
            }

            if (!qualityMap.isEmpty()) {
                if (isBackgroundLoad) {
                    sendStreamUrlToPlayer(qualityMap);
                } else {
                    startVideoPlayer(qualityMap);
                }
            } else {
                if (!isBackgroundLoad) {
                    requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Trận đấu chưa được phát sóng.", Toast.LENGTH_SHORT).show());
                }
            }
        } catch (Exception e) {
            Log.e("LiveFragment", "Error parsing JSON response", e);
            if (!isBackgroundLoad) {
                requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Có lỗi xảy ra khi tải dữ liệu.", Toast.LENGTH_SHORT).show());
            }
        }
    }

    private void sendStreamUrlToPlayer(HashMap<String, String> qualityMap) {
        PlayerActivity playerActivity = PlayerActivity.getInstance();
        if (playerActivity != null) {
            playerActivity.onStreamUrlReceived(qualityMap);
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