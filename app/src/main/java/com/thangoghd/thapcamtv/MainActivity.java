package com.thangoghd.thapcamtv;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class MainActivity extends AppCompatActivity {
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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        backgroundImageView = findViewById(R.id.backgroundImageView);
        recyclerViewSports = findViewById(R.id.recyclerViewSports);
        recyclerViewMatches = findViewById(R.id.recyclerViewMatches);

        SportApi sportApi = ApiManager.getSportApi();
        sportRepository = new SportRepository(sportApi);

        setupSportsRecyclerView();
        // Load matches for the default sport (football)
        onSportSelected(currentSportIndex);
        setupPeriodicRefresh();
    }

    private void setupSportsRecyclerView() {
        recyclerViewSports.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
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
            runOnUiThread(() -> {
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

    private void loadMatches(String sportTypeKey) { // Get the key of the selected sport type
        sportRepository.getLiveMatches(new RepositoryCallback<List<Match>>() {
            @Override
            public void onSuccess(List<Match> result) {
                // Get matches by sport type
                Map<String, List<Match>> matchesBySportType = sportRepository.getMatchesBySportType(result);
                // Get the list of matches for the selected sport type
                matches = matchesBySportType.get(sportTypeKey);


                // Only update the sports list if it is the first load
                // or the sports list is currently empty
                if (isInitialLoad || availableSportTypes.length == 0) {
                    updateSportsAdapter(matchesBySportType);
                }

                // Update adapter for sports categories
                updateSportsAdapter(matchesBySportType);
                // Update RecyclerView with matches
                updateMatchesRecyclerView(); 
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(MainActivity.this, "Error loading matches", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateSportsAdapter(Map<String, List<Match>> matchesBySportType) {
        List<SportType> availableSports = new ArrayList<>();
        // Filter out the sports categories with matches
        for (SportType sportType : SportType.values()) {
            List<Match> sportMatches = matchesBySportType.get(sportType.getKey());
            if (sportMatches != null && !sportMatches.isEmpty()) {
                availableSports.add(sportType);
            }
        }

        // Update the list of available sports
        availableSportTypes = availableSports.toArray(new SportType[0]);


        sportsAdapter.updateSports(availableSportTypes);
    }

    private void updateMatchesRecyclerView() {
        if (matchesAdapter == null) {
            matchesAdapter = new MatchesAdapter(matches);
            // Set LayoutManager for recyclerViewMatches
            recyclerViewMatches.setLayoutManager(new GridLayoutManager(this, 3));
            recyclerViewMatches.addItemDecoration(new SpaceItemDecoration(0, 0, 0, 20));
            recyclerViewMatches.setAdapter(matchesAdapter);
        } else {
            // Update the adapter with new matches
            matchesAdapter.updateMatches(matches);
        }


        recyclerViewSports.smoothScrollToPosition(currentSportIndex);
        recyclerViewSports.post(() -> {
            RecyclerView.ViewHolder viewHolder = recyclerViewSports.findViewHolderForAdapterPosition(currentSportIndex);
            if (viewHolder != null) {
                viewHolder.itemView.requestFocus();
            }
        });
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
    protected void onResume() {
        super.onResume();
        refreshHandler.post(refreshRunnable);
    }

    @Override
    protected void onPause() {
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
                    runOnUiThread(() -> {
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
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Error refreshing matches", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }


    // Helper method to find match position by ID
    private int findMatchPositionById(String matchId) {
        for (int i = 0; i < matches.size(); i++) {
            if (matches.get(i).getId().equals(matchId)) {
                return i;
            }
        }
        return -1;
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

}