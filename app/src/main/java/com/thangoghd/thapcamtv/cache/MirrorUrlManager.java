package com.thangoghd.thapcamtv.cache;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class MirrorUrlManager {
    private static final String TAG = "MirrorUrlManager";
    private static final String PREF_NAME = "MirrorUrlPrefs";
    private static final String KEY_MIRROR_URL = "mirror_url";
    private static final String KEY_LAST_UPDATE = "last_update";
    private static final long CACHE_DURATION = 24 * 60 * 60 * 1000; // 24 hours

    private static MirrorUrlManager instance;
    private final Context context;
    private String cachedMirrorUrl;

    private MirrorUrlManager(Context context) {
        this.context = context.getApplicationContext();
        loadFromCache();
    }

    public static synchronized MirrorUrlManager getInstance(Context context) {
        if (instance == null) {
            instance = new MirrorUrlManager(context);
        }
        return instance;
    }

    private void loadFromCache() {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        cachedMirrorUrl = prefs.getString(KEY_MIRROR_URL, null);
        Log.d(TAG, "Loaded from cache: " + cachedMirrorUrl);
    }

    public String getMirrorUrl() {
        if (shouldUpdateCache()) {
            return null;
        }
        return cachedMirrorUrl;
    }

    public void updateMirrorUrl(String newUrl) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_MIRROR_URL, newUrl);
        editor.putLong(KEY_LAST_UPDATE, System.currentTimeMillis());
        editor.apply();
        
        cachedMirrorUrl = newUrl;
        Log.d(TAG, "Updated mirror URL: " + newUrl);
    }

    private boolean shouldUpdateCache() {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        long lastUpdate = prefs.getLong(KEY_LAST_UPDATE, 0);
        long currentTime = System.currentTimeMillis();
        
        return cachedMirrorUrl == null || (currentTime - lastUpdate) > CACHE_DURATION;
    }
}
