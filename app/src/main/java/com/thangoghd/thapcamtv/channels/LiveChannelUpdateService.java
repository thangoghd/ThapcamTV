package com.thangoghd.thapcamtv.channels;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import androidx.tvprovider.media.tv.TvContractCompat;

import com.thangoghd.thapcamtv.api.ApiManager;
import com.thangoghd.thapcamtv.api.SportApi;
import com.thangoghd.thapcamtv.models.Match;
import com.thangoghd.thapcamtv.repositories.RepositoryCallback;
import com.thangoghd.thapcamtv.repositories.SportRepository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service to update live channel programs.
 */
public class LiveChannelUpdateService extends JobService {
    private UpdateTask updateTask;

    @Override
    public boolean onStartJob(JobParameters params) {
        updateTask = new UpdateTask(this);
        updateTask.execute(params);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        if (updateTask != null) {
            updateTask.cancel(true);
        }
        return true;
    }

    private static class UpdateTask extends AsyncTask<JobParameters, Void, Void> {
        private final JobService jobService;

        UpdateTask(JobService jobService) {
            this.jobService = jobService;
        }

        @Override
        protected Void doInBackground(JobParameters... params) {
            final JobParameters jobParams = params[0];

            // Get channel ID
            long channelId = LiveChannelHelper.createOrGetChannel(jobService);
            if (channelId == -1) {
                jobService.jobFinished(jobParams, false);
                return null;
            }

            // Delete all existing programs
            deleteAllPrograms(channelId);

            // Create repositories for both APIs
            SportApi veboApi = ApiManager.getSportApi(true);
            SportApi thapcamApi = ApiManager.getSportApi(false);
            SportRepository veboRepository = new SportRepository(veboApi);
            SportRepository thapcamRepository = new SportRepository(thapcamApi);

            final List<Match> allMatches = new ArrayList<>();
            final AtomicInteger completedCalls = new AtomicInteger(0);
            final Object lock = new Object();

            Log.d("LiveChannelUpdateService", "Starting to fetch matches from both sources");

            // Load matches from vebo.xyz
            veboRepository.getLiveMatches(new RepositoryCallback<List<Match>>() {
                @Override
                public void onSuccess(List<Match> result) {
                    Log.d("LiveChannelUpdateService", "Vebo matches loaded: " + (result != null ? result.size() : 0));
                    // Add vebo matches first (priority)
                    synchronized (allMatches) {
                        if (result != null) {
                            allMatches.addAll(result);
                            Log.d("LiveChannelUpdateService", "Total matches after vebo: " + allMatches.size());
                        }
                    }

                    // Check if both APIs have completed
                    if (completedCalls.incrementAndGet() == 2) {
                        synchronized (lock) {
                            lock.notify();
                        }
                    }
                }

                @Override
                public void onError(Exception e) {
                    Log.e("LiveChannelUpdateService", "Error loading vebo matches", e);
                    if (completedCalls.incrementAndGet() == 2) {
                        synchronized (lock) {
                            lock.notify();
                        }
                    }
                }
            });

            // Load matches from thapcam.xyz
            thapcamRepository.getLiveMatches(new RepositoryCallback<List<Match>>() {
                @Override
                public void onSuccess(List<Match> result) {
                    Log.d("LiveChannelUpdateService", "Thapcam matches loaded: " + (result != null ? result.size() : 0));
                    // Add thapcam matches after vebo matches
                    synchronized (allMatches) {
                        if (result != null) {
                            allMatches.addAll(result);
                            Log.d("LiveChannelUpdateService", "Total matches after thapcam: " + allMatches.size());
                        }
                    }

                    // Check if both APIs have completed
                    if (completedCalls.incrementAndGet() == 2) {
                        synchronized (lock) {
                            lock.notify();
                        }
                    }
                }

                @Override
                public void onError(Exception e) {
                    Log.e("LiveChannelUpdateService", "Error loading thapcam matches", e);
                    if (completedCalls.incrementAndGet() == 2) {
                        synchronized (lock) {
                            lock.notify();
                        }
                    }
                }
            });

            // Wait for both API calls to complete
            try {
                synchronized (lock) {
                    if (completedCalls.get() < 2) {
                        Log.d("LiveChannelUpdateService", "Waiting for API calls to complete...");
                        lock.wait(30000); // Wait up to 30 seconds
                    }
                }
            } catch (InterruptedException e) {
                Log.e("LiveChannelUpdateService", "Interrupted while waiting for API calls", e);
            }

            // Process matches
            Log.d("LiveChannelUpdateService", "All API calls completed. Total matches: " + allMatches.size());
            
            // Use SportRepository to process matches
            Map<String, List<Match>> matchesBySportType = new SportRepository(null).getMatchesBySportType(allMatches);
            
            // Combine all live matches from all sport types
            List<Match> liveMatches = new ArrayList<>();
            for (List<Match> sportMatches : matchesBySportType.values()) {
                for (Match match : sportMatches) {
                    if (match.getLive() && 
                        !"finished".equalsIgnoreCase(match.getMatch_status()) && 
                        !"canceled".equalsIgnoreCase(match.getMatch_status())) {
                        liveMatches.add(match);
                    }
                }
            }

            // Sort matches by sport type priority and tournament priority
            liveMatches.sort((m1, m2) -> {
                // First compare by sport type priority
                List<String> SPORT_PRIORITY = Arrays.asList("live", "football", "basketball", "esports", "tennis", "volleyball", "badminton", "race", "pool", "wwe", "event", "other");
                int sport1Index = SPORT_PRIORITY.indexOf(m1.getSport_type());
                int sport2Index = SPORT_PRIORITY.indexOf(m2.getSport_type());
                if (sport1Index != sport2Index) {
                    return Integer.compare(sport1Index, sport2Index);
                }
                
                // If same sport type, compare by tournament priority
                return Integer.compare(
                    m2.getTournament() != null ? m2.getTournament().getPriority() : 0,
                    m1.getTournament() != null ? m1.getTournament().getPriority() : 0
                );
            });

            // Use Set to avoid duplicates
            HashSet<String> addedMatchIds = new HashSet<>();
            
            int addedCount = 0;
            for (Match match : liveMatches) {
                try {
                    // Skip if this match ID has already been added
                    if (match.getId() == null || addedMatchIds.contains(match.getId())) {
                        continue;
                    }
                    
                    LiveChannelHelper.addProgramToChannel(jobService, channelId, match);
                    addedMatchIds.add(match.getId());
                    addedCount++;
                    
                    Log.d("LiveChannelUpdateService", "Added match to channel: " + match.getHome().getName() + " vs " + match.getAway() != null ? match.getAway().getName() : "Unknown" + " [" + match.getSport_type() + "]");
                } catch (Exception e) {
                    Log.e("LiveChannelUpdateService", "Error adding match to channel", e);
                }
            }

            Log.d("LiveChannelUpdateService", "Updated channel with " + addedCount + " programs out of " + liveMatches.size() + " matches");
            jobService.jobFinished(jobParams, false);
            return null;
        }

        private void deleteAllPrograms(long channelId) {
            try {
                // Get all programs
                Uri programsUri = TvContractCompat.PreviewPrograms.CONTENT_URI;
                String[] projection = {
                    TvContractCompat.PreviewPrograms._ID,
                    "channel_id"
                };
                
                try (Cursor cursor = jobService.getContentResolver().query(
                        programsUri,
                        projection,
                        null,
                        null,
                        null)) {
                    
                    if (cursor != null) {
                        int channelIdIndex = cursor.getColumnIndexOrThrow("channel_id");
                        int idIndex = cursor.getColumnIndexOrThrow(TvContractCompat.PreviewPrograms._ID);
                        
                        while (cursor.moveToNext()) {
                            long programChannelId = cursor.getLong(channelIdIndex);
                            if (programChannelId == channelId) {
                                long programId = cursor.getLong(idIndex);
                                Uri programUri = ContentUris.withAppendedId(programsUri, programId);
                                int deleted = jobService.getContentResolver().delete(programUri, null, null);
                                Log.d("LiveChannelUpdateService", "Deleted program: " + programId + ", result: " + deleted);
                            }
                        }
                    }
                }
                
                Log.d("LiveChannelUpdateService", "Finished deleting programs for channel: " + channelId);
            } catch (Exception e) {
                Log.e("LiveChannelUpdateService", "Error deleting programs", e);
            }
        }
    }
}
