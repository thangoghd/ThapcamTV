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

public class LiveFragment extends Fragment {
    private RecyclerView recyclerViewSports;
    private RecyclerView recyclerViewMatches;
    private List<Match> matches; // Store matches for the selected sport
    private SportsAdapter sportsAdapter; // Adapter for sports categories
    private MatchesAdapter matchesAdapter;
    private SportType[] availableSportTypes;
    private int currentSportIndex = 0;
    private boolean isInitialLoad = true; // Add a variable to track the first load
    private ImageView backgroundImageView;
    private final Handler refreshHandler = new Handler(Looper.getMainLooper());
    private final long REFRESH_INTERVAL = 30000;
    private Runnable refreshRunnable;
    public int focusedPosition = RecyclerView.NO_POSITION;
    private Map<String, List<Match>> matchesCache = new HashMap<>(); // Add cache for matches
    private View loadingView; // Add loading view reference
    private List<Match> allMatches; // Store all matches from both APIs

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_live, container, false);

        backgroundImageView = view.findViewById(R.id.backgroundImageView);
        recyclerViewSports = view.findViewById(R.id.recyclerViewSports);
        recyclerViewMatches = view.findViewById(R.id.recyclerViewMatches);
        loadingView = view.findViewById(R.id.loadingView); // Initialize loading view

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

        allMatches = new ArrayList<>();
        final AtomicInteger completedCalls = new AtomicInteger(0);

        // Load matches from vebo.xyz
        veboRepository.getMatches(new RepositoryCallback<List<Match>>() {
            @Override
            public void onSuccess(List<Match> result) {
                if (!isAdded()) {
                    showLoading(false);
                    return;
                }

                // Add vebo matches first (priority)
                synchronized (allMatches) {
                    for (Match match : result) {
                        match.setFrom("vebo");
                    }
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
        thapcamRepository.getMatches(new RepositoryCallback<List<Match>>() {
            @Override
            public void onSuccess(List<Match> result) {
                if (!isAdded()) {
                    showLoading(false);
                    return;
                }

                // Add thapcam matches after vebo matches
                synchronized (allMatches) {
                    for (Match match : result) {
                        match.setFrom("thapcam");
                    }
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
        
        Map<String, List<Match>> matchesBySportType = new SportRepository(null).getMatchesBySportType(allMatches);
        matches = matchesBySportType.get(sportTypeKey);
        matchesCache.putAll(matchesBySportType);

        if (isInitialLoad || availableSportTypes.length == 0) {
            updateSportsAdapter(matchesBySportType);
        }

        updateSportsAdapter(matchesBySportType);
        updateMatchesRecyclerView();
        showLoading(false);

        if (allMatches.isEmpty() && getContext() != null) {
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
            matchesAdapter = new MatchesAdapter(matches, this, match -> {
                PlayerActivity.fetchMatchStreamUrl(match, new RepositoryCallback<JsonObject>() {
                    @Override
                    public void onSuccess(JsonObject result) {
                        // Handle success - có thể start PlayerActivity với data
                        Intent intent = new Intent(getContext(), PlayerActivity.class);
                        intent.putExtra("source_type", "live");
                        intent.putExtra("is_loading", true);
                        intent.putExtra("match_id", match.getId());
                        intent.putExtra("sport_type", match.getSportType());
                        intent.putExtra("sync_key", match.getSync() != null ? match.getSync() : match.getId());
                        intent.putExtra("from", match.getFrom());
                        intent.putExtra("show_quality_spinner", true);
                        startActivity(intent);
                    }

                    @Override
                    public void onError(Exception e) {
                        // Handle error - hiển thị thông báo lỗi
                        Toast.makeText(getContext(), "Có lỗi xảy ra khi tải dữ liệu", Toast.LENGTH_SHORT).show();
                    }
                });
            });
            // Set LayoutManager for recyclerViewMatches
            recyclerViewMatches.setLayoutManager(new GridLayoutManager(getContext(), 3));
            recyclerViewMatches.addItemDecoration(new SpaceItemDecoration(0, 0, 0, 35));
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
                refreshMatches();
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

    private void refreshMatches() {
        // Create repositories for both sources
        SportApi veboApi = ApiManager.getSportApi(true);
        SportApi thapcamApi = ApiManager.getSportApi(false);
        SportRepository veboRepository = new SportRepository(veboApi);
        SportRepository thapcamRepository = new SportRepository(thapcamApi);

        final List<Match> combinedMatches = new ArrayList<>();
        final AtomicInteger completedCalls = new AtomicInteger(0);

        // Load matches from vebo.xyz
        veboRepository.getMatches(new RepositoryCallback<List<Match>>() {
            @Override
            public void onSuccess(List<Match> result) {
                if (!isAdded()) return;

                synchronized (combinedMatches) {
                    for (Match match : result) {
                        if (match.getLive()) {
                            combinedMatches.add(match);
                        }
                    }
                }

                if (completedCalls.incrementAndGet() == 2) {
                    updateMatchesUI(combinedMatches);
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e("LiveFragment", "Error refreshing vebo matches", e);
                if (completedCalls.incrementAndGet() == 2) {
                    updateMatchesUI(combinedMatches);
                }
            }
        });

        // Load matches from thapcam.xyz
        thapcamRepository.getMatches(new RepositoryCallback<List<Match>>() {
            @Override
            public void onSuccess(List<Match> result) {
                if (!isAdded()) return;

                synchronized (combinedMatches) {
                    for (Match match : result) {
                        if (match.getLive()) {
                            combinedMatches.add(match);
                        }
                    }
                }

                if (completedCalls.incrementAndGet() == 2) {
                    updateMatchesUI(combinedMatches);
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e("LiveFragment", "Error refreshing thapcam matches", e);
                if (completedCalls.incrementAndGet() == 2) {
                    updateMatchesUI(combinedMatches);
                }
            }
        });
    }

    private void updateMatchesUI(List<Match> newMatches) {
        if (matches != null && availableSportTypes != null && 
            currentSportIndex >= 0 && currentSportIndex < availableSportTypes.length) {
            
            String currentSportType = availableSportTypes[currentSportIndex].getKey();
            List<Integer> updatedIndices = new ArrayList<>();

            // Filter matches by current sport type
            List<Match> filteredMatches = new ArrayList<>();
            for (Match match : newMatches) {
                // If currentSportType is "live", add all live matches
                if ("live".equals(currentSportType)) {
                    if (match.getLive()) {
                        filteredMatches.add(match);
                    }
                } 
                // Otherwise, only take matches of the current sportType
                else if (currentSportType.equals(match.getSportType())) {
                    filteredMatches.add(match);
                }
            }

            // Update UI with filtered matches
            for (Match newMatch : filteredMatches) {
                boolean found = false;
                for (int i = 0; i < matches.size(); i++) {
                    Match currentMatch = matches.get(i);

                    if (currentMatch.getId().equals(newMatch.getId())) {
                        found = true;
                        // Only update if there are changes
                        if (!currentMatch.getScores().equals(newMatch.getScores()) ||
                                !currentMatch.getTimeInMatch().equals(newMatch.getTimeInMatch()) ||
                                !currentMatch.getMatchStatus().equals(newMatch.getMatchStatus())) {

                            currentMatch.setScores(newMatch.getScores());
                            currentMatch.setTimeInMatch(newMatch.getTimeInMatch());
                            currentMatch.setMatch_status(newMatch.getMatchStatus());
                            updatedIndices.add(i);
                        }
                        break;
                    }
                }
                
                // If it's a new match and belongs to the current sport type, add it to the list
                if (!found) {
                    matches.add(newMatch);
                    updatedIndices.add(matches.size() - 1);
                }
            }

            // Remove matches that no longer match the current sport type
            for (int i = matches.size() - 1; i >= 0; i--) {
                Match currentMatch = matches.get(i);
                boolean shouldKeep = false;
                
                if ("live".equals(currentSportType)) {
                    shouldKeep = currentMatch.getLive();
                } else {
                    shouldKeep = currentSportType.equals(currentMatch.getSportType());
                }
                
                if (!shouldKeep) {
                    matches.remove(i);
                    matchesAdapter.notifyItemRemoved(i);
                }
            }

            // Update UI
            requireActivity().runOnUiThread(() -> {
                // Update items that have changed
                for (int index : updatedIndices) {
                    if (index < matches.size()) {  // Check for IndexOutOfBoundsException
                        matchesAdapter.notifyItemChanged(index);
                    }
                }

                // Reset focus if needed
                if (focusedPosition != RecyclerView.NO_POSITION && focusedPosition < matches.size()) {
                    RecyclerView.ViewHolder holder = recyclerViewMatches.findViewHolderForAdapterPosition(focusedPosition);
                    if (holder != null) {
                        holder.itemView.requestFocus();
                    }
                }
            });
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