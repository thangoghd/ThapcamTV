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
import com.thangoghd.thapcamtv.models.Replay;

public class HighlightChannelHelper {
    private static final String CHANNEL_ID = "highlight_channel";
    private static final int CHANNEL_JOB_ID = 1000;
    private static final long CHANNEL_UPDATE_INTERVAL = 3600000;
    private static final String COLUMN_CHANNEL_ID = "channel_id";
    private static final String COLUMN_TITLE = "title";
    private static final String COLUMN_TYPE = "type";
    private static final String COLUMN_POSTER_ART_URI = "poster_art_uri";
    private static final String COLUMN_INTENT_URI = "intent_uri";
    private static final String COLUMN_INTERNAL_PROVIDER_ID = "internal_provider_id";
    private static final String COLUMN_SEARCHABLE = "searchable";
    private static final String TYPE_PREVIEW = "TYPE_PREVIEW";
    private static final String TYPE_MOVIE = "TYPE_MOVIE";

    public static boolean isChannelSupported(Context context) {
        boolean isVersionSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;
        boolean isTvDevice = context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_LEANBACK);
        boolean isSupported = isVersionSupported && isTvDevice;
        Log.d("HighlightChannel", "Channel support: " + isSupported + " (TV device: " + isTvDevice + ")");
        return isSupported;
    }

    public static long createOrGetChannel(Context context) {
        if (!isChannelSupported(context)) {
            Log.d("HighlightChannel", "Channel not supported on this device");
            return -1;
        }

        // Check if channel already exists
        String[] projection = {TvContractCompat.Channels._ID, "internal_provider_id"};
        
        try (Cursor cursor = context.getContentResolver().query(
                TvContractCompat.Channels.CONTENT_URI,
                projection,
                null,
                null,
                null)) {
            
            if (cursor != null) {
                try {
                    int providerIdIndex = cursor.getColumnIndexOrThrow("internal_provider_id");
                    int idIndex = cursor.getColumnIndexOrThrow(TvContractCompat.Channels._ID);
                    
                    while (cursor.moveToNext()) {
                        String providerId = cursor.getString(providerIdIndex);
                        if (CHANNEL_ID.equals(providerId)) {
                            long existingChannelId = cursor.getLong(idIndex);
                            Log.d("HighlightChannel", "Found existing channel with ID: " + existingChannelId);
                            return existingChannelId;
                        }
                    }
                } catch (IllegalArgumentException e) {
                    Log.e("HighlightChannel", "Column not found", e);
                }
            }
        }

        // Create new channel if it doesn't exist
        Channel.Builder builder = new Channel.Builder();
        builder.setType(TvContractCompat.Channels.TYPE_PREVIEW)
                .setDisplayName("Highlights")
                .setAppLinkIntentUri(Uri.parse("thapcamtv://highlight"))
                .setInternalProviderId(CHANNEL_ID);

        Uri channelUri = context.getContentResolver().insert(TvContractCompat.Channels.CONTENT_URI, builder.build().toContentValues());
        if (channelUri == null) {
            Log.e("HighlightChannel", "Failed to create channel");
            return -1;
        }
        
        long channelId = ContentUris.parseId(channelUri);

        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher);
        ChannelLogoUtils.storeChannelLogo(context, channelId, bitmap);
        
        // Make channel browsable
        TvContractCompat.requestChannelBrowsable(context, channelId);
        
        return channelId;
    }

    public static void addProgramToChannel(Context context, long channelId, Replay replay) {
        if (!isChannelSupported(context) || channelId == -1) {
            return;
        }

        ContentValues values = new ContentValues();
        values.put(COLUMN_CHANNEL_ID, channelId);
        values.put(COLUMN_TITLE, replay.getName());
        values.put(COLUMN_POSTER_ART_URI, replay.getFeatureImage());
        values.put(COLUMN_TYPE, TYPE_MOVIE);
        values.put(COLUMN_INTERNAL_PROVIDER_ID, replay.getId());
        values.put(COLUMN_SEARCHABLE, 1);
        
        // Create intent URI for Android TV
        values.put(COLUMN_INTENT_URI, "thapcamtv://highlight/" + replay.getId());

        Uri programUri = context.getContentResolver().insert(
            TvContractCompat.PreviewPrograms.CONTENT_URI, 
            values
        );
    }

    private static Intent createProgramIntent(String replayId) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("thapcamtv://highlight/" + replayId));
        return intent;
    }

    public static void scheduleChannelUpdate(Context context) {
        if (!isChannelSupported(context)) {
            return;
        }

        JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        ComponentName componentName = new ComponentName(context, HighlightChannelUpdateService.class);

        JobInfo.Builder builder = new JobInfo.Builder(CHANNEL_JOB_ID, componentName)
                .setPeriodic(CHANNEL_UPDATE_INTERVAL)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPersisted(true);

        scheduler.schedule(builder.build());
    }
}
