package com.thangoghd.thapcamtv;

import android.content.Context;
import android.util.Log;

import com.thangoghd.thapcamtv.channels.HighlightChannelHelper;
import com.thangoghd.thapcamtv.channels.LiveChannelHelper;

public class Application extends android.app.Application {
    private static final String TAG = "ThapcamTV";
    private static Context context;
    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();

        Log.d(TAG, "Application onCreate started");

        // Initialize Highlight Channel
        if (HighlightChannelHelper.isChannelSupported(this)) {
            long channelId = HighlightChannelHelper.createOrGetChannel(this);
            if (channelId != -1) {
                HighlightChannelHelper.scheduleChannelUpdate(this);
                Log.d(TAG, "Highlight Channel initialized with ID: " + channelId);
            } else {
                Log.e(TAG, "Failed to create Highlight Channel");
            }
        } else {
            Log.d(TAG, "Highlight Channel not supported on this device");
        }

        // Initialize Live Channel
        if (LiveChannelHelper.isChannelSupported(this)) {
            long channelId = LiveChannelHelper.createOrGetChannel(this);
            if (channelId != -1) {
                LiveChannelHelper.scheduleChannelUpdate(this);
                Log.d(TAG, "Live Channel initialized with ID: " + channelId);
            } else {
                Log.e(TAG, "Failed to create Live Channel");
            }
        } else {
            Log.d(TAG, "Live Channel not supported on this device");
        }

        Log.d(TAG, "Application onCreate completed");
    }

    public static Context getContext() {
        return context;
    }
}
