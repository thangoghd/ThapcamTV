package com.thangoghd.thapcamtv;

import android.util.Log;

import com.thangoghd.thapcamtv.channels.HighlightChannelHelper;
import com.thangoghd.thapcamtv.channels.LiveChannelHelper;

public class Application extends android.app.Application {
    @Override
    public void onCreate() {
        super.onCreate();

        Log.d("ThapcamTV", "Application onCreate started");

        // Initialize Highlight Channel
        if (HighlightChannelHelper.isChannelSupported(this)) {
            long channelId = HighlightChannelHelper.createOrGetChannel(this);
            if (channelId != -1) {
                HighlightChannelHelper.scheduleChannelUpdate(this);
                Log.d("ThapcamTV", "Highlight Channel initialized with ID: " + channelId);
            } else {
                Log.e("ThapcamTV", "Failed to create Highlight Channel");
            }
        } else {
            Log.d("ThapcamTV", "Highlight Channel not supported on this device");
        }

        // Initialize Live Channel
        if (LiveChannelHelper.isChannelSupported(this)) {
            long channelId = LiveChannelHelper.createOrGetChannel(this);
            if (channelId != -1) {
                LiveChannelHelper.scheduleChannelUpdate(this);
                Log.d("ThapcamTV", "Live Channel initialized with ID: " + channelId);
            } else {
                Log.e("ThapcamTV", "Failed to create Live Channel");
            }
        } else {
            Log.d("ThapcamTV", "Live Channel not supported on this device");
        }

        Log.d("ThapcamTV", "Application onCreate completed");
    }
}
