package com.thangoghd.thapcamtv;

import android.app.Application;

import com.thangoghd.thapcamtv.channels.HighlightChannelHelper;

public class ThapcamTVApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        initializeChannels();
    }

    private void initializeChannels() {
        if (HighlightChannelHelper.isChannelSupported(this)) {
            long channelId = HighlightChannelHelper.createOrGetChannel(this);
            if (channelId != -1) {
                HighlightChannelHelper.scheduleChannelUpdate(this);
            }
        }
    }
}
