package com.thangoghd.thapcamtv.channels;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.tvprovider.media.tv.Channel;
import androidx.tvprovider.media.tv.ChannelLogoUtils;
import androidx.tvprovider.media.tv.TvContractCompat;

import com.thangoghd.thapcamtv.PlayerActivity;
import com.thangoghd.thapcamtv.R;
import com.thangoghd.thapcamtv.models.Commentator;
import com.thangoghd.thapcamtv.models.Match;
import com.thangoghd.thapcamtv.providers.ProgramImageProvider;
import com.thangoghd.thapcamtv.utils.MatchImageHelper;

import java.util.List;

public class LiveChannelHelper {
    private static final String CHANNEL_ID = "live_channel";
    private static final int CHANNEL_JOB_ID = 1001;
    private static final int CHANNEL_IMMEDIATE_JOB_ID = 1002;
    private static final long CHANNEL_UPDATE_INTERVAL = 600000; // 10 minutes

    // Program columns
    private static final String COLUMN_CHANNEL_ID = "channel_id";
    private static final String COLUMN_TITLE = "title";
    private static final String COLUMN_DESCRIPTION = "short_description";
    private static final String COLUMN_POSTER_ART_URI = "poster_art_uri";
    private static final String COLUMN_INTENT_URI = "intent_uri";
    private static final String COLUMN_INTERNAL_PROVIDER_ID = "internal_provider_id";
    private static final String COLUMN_SEARCHABLE = "searchable";
    private static final String COLUMN_TYPE = "type";
    private static final String COLUMN_LIVE = "live";
    private static final int TYPE_MOVIE = 0; // TvContractCompat.PreviewPrograms.TYPE_MOVIE

    public static boolean isChannelSupported(Context context) {
        boolean isVersionSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;
        boolean isTvDevice = context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_LEANBACK);
        boolean isSupported = isVersionSupported && isTvDevice;
        Log.d("LiveChannelHelper", "Channel support: " + isSupported + " (TV device: " + isTvDevice + ")");
        return isSupported;
    }

    public static long createOrGetChannel(Context context) {
        if (!isChannelSupported(context)) {
            Log.d("LiveChannelHelper", "Channel not supported on this device");
            return -1;
        }

        Log.d("LiveChannelHelper", "Creating or getting channel...");

        // Check if channel already exists
        String[] projection = {"_id", "internal_provider_id"};
        
        try (Cursor cursor = context.getContentResolver().query(
                TvContractCompat.Channels.CONTENT_URI,
                projection,
                null,
                null,
                null)) {
            
            if (cursor != null) {
                try {
                    int providerIdIndex = cursor.getColumnIndexOrThrow("internal_provider_id");
                    int idIndex = cursor.getColumnIndexOrThrow("_id");
                    
                    while (cursor.moveToNext()) {
                        String providerId = cursor.getString(providerIdIndex);
                        if (CHANNEL_ID.equals(providerId)) {
                            long existingChannelId = cursor.getLong(idIndex);
                            Log.d("LiveChannelHelper", "Found existing channel with ID: " + existingChannelId);
                            return existingChannelId;
                        }
                    }
                } catch (IllegalArgumentException e) {
                    Log.e("LiveChannelHelper", "Column not found", e);
                }
            }
        }

        // Create new channel if it doesn't exist
        Channel.Builder builder = new Channel.Builder();
        builder.setType(TvContractCompat.Channels.TYPE_PREVIEW)
                .setDisplayName("Trực tiếp")
                .setAppLinkIntentUri(Uri.parse("thapcamtv://live"))
                .setInternalProviderId(CHANNEL_ID);

        Uri channelUri = context.getContentResolver().insert(TvContractCompat.Channels.CONTENT_URI, builder.build().toContentValues());
        if (channelUri == null) {
            Log.e("LiveChannelHelper", "Failed to create channel");
            return -1;
        }
        
        long channelId = ContentUris.parseId(channelUri);
        Log.d("LiveChannelHelper", "Created new channel with ID: " + channelId);

        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.thapcam_channel_logo);
        ChannelLogoUtils.storeChannelLogo(context, channelId, bitmap);

        // Make channel browsable
        TvContractCompat.requestChannelBrowsable(context, channelId);
        Log.d("LiveChannelHelper", "Channel set as browsable");
        
        return channelId;
    }

    public static void scheduleChannelUpdate(Context context) {
        Log.d("LiveChannelHelper", "Scheduling channel updates...");
        ComponentName componentName = new ComponentName(context, LiveChannelUpdateService.class);
        JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (scheduler == null) {
            Log.e("LiveChannelHelper", "Failed to get JobScheduler");
            return;
        }

        // Schedule immediate job
        JobInfo.Builder immediateBuilder = new JobInfo.Builder(CHANNEL_IMMEDIATE_JOB_ID, componentName);
        immediateBuilder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                       .setMinimumLatency(1000); // Wait at least 1 second
        
        int immediateResult = scheduler.schedule(immediateBuilder.build());
        Log.d("LiveChannelHelper", "Immediate job schedule result: " + 
            (immediateResult == JobScheduler.RESULT_SUCCESS ? "SUCCESS" : "FAILURE"));

        // Schedule periodic job
        JobInfo.Builder periodicBuilder = new JobInfo.Builder(CHANNEL_JOB_ID, componentName);
        periodicBuilder.setPeriodic(CHANNEL_UPDATE_INTERVAL)
                      .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                      .setPersisted(true);
        
        int periodicResult = scheduler.schedule(periodicBuilder.build());
        Log.d("LiveChannelHelper", "Periodic job schedule result: " + 
            (periodicResult == JobScheduler.RESULT_SUCCESS ? "SUCCESS" : "FAILURE"));
    }

    public static void addProgramToChannel(Context context, long channelId, Match match) {
        if (match == null || match.getId() == null) return;

        ContentValues values = new ContentValues();
        values.put(COLUMN_CHANNEL_ID, channelId);

        List<Commentator> commentators = match.getCommentators();
        StringBuilder commentatorsNames = new StringBuilder("BLV: ");
        if (commentators != null && !commentators.isEmpty()) {
            if (commentators.size() > 1) {
                // Join commentator names with " - "
                for (int i = 0; i < commentators.size(); i++) {
                    commentatorsNames.append(commentators.get(i).getName());
                    if (i < commentators.size() - 1) {
                        commentatorsNames.append(" - ");
                    }
                }
            } else {
                // Single commentator
                commentatorsNames.append(commentators.get(0).getName());
            }
        } else {
            commentatorsNames.append("Nhà Đài");
        }
        // Get match title
        String matchTitle = match.getAway() == null ? match.getHome().getName() : match.getHome().getName() + " vs " + match.getAway().getName();

        values.put(COLUMN_TITLE, matchTitle);
        values.put(COLUMN_DESCRIPTION, match.getTournament().getName() + "\n" + commentatorsNames);
        values.put(COLUMN_TYPE, TYPE_MOVIE);

        // Try to find custom image from providers, fallback to tournament logo if not found
        String imageUrl = MatchImageHelper.findMatchImage(matchTitle, match.getTournament().getLogo());
        // Convert image URL to content URI that will serve the image in 16:9 ratio
        Uri imageUri = ProgramImageProvider.buildImageUri(imageUrl);
        values.put(COLUMN_POSTER_ART_URI, imageUri.toString());
        values.put(COLUMN_LIVE, 0);
        values.put(COLUMN_INTERNAL_PROVIDER_ID, match.getId());
        values.put(COLUMN_SEARCHABLE, 1);
        
        // Create an intent and convert it to URI string
        Intent intent = new Intent(context, PlayerActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("source_type", "live");
        intent.putExtra("is_loading", true);
        intent.putExtra("show_quality_spinner", true); 
        intent.putExtra("match_id", match.getId());
        intent.putExtra("match_slug", match.getSlug());
        intent.putExtra("sync_key", match.getSync() != null ? match.getSync() : match.getId());
        intent.putExtra("from", match.getFrom());
        
        // Convert intent to URI string with all flags preserved
        String intentUri = intent.toUri(Intent.URI_INTENT_SCHEME);
        values.put(COLUMN_INTENT_URI, intentUri);

        Uri programUri = context.getContentResolver().insert(TvContractCompat.PreviewPrograms.CONTENT_URI, values);
        if (programUri != null) {
            Log.d("LiveChannelHelper", "Added program: " + programUri);
        }
    }
}
