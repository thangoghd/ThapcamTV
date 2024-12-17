package com.thangoghd.thapcamtv;

import android.util.LruCache;

import com.thangoghd.thapcamtv.response.ReplayLinkResponse;
import com.thangoghd.thapcamtv.response.ReplayResponse;

import java.util.concurrent.TimeUnit;

public class ReplayCache {
    private static final long LIST_CACHE_DURATION = TimeUnit.HOURS.toMillis(1);
    private static final long DETAIL_CACHE_DURATION = TimeUnit.HOURS.toMillis(2);
    private static final int CACHE_SIZE = 4 * 1024 * 1024; // 4MiB

    private static final LruCache<String, CacheEntry> memoryCache = new LruCache<>(CACHE_SIZE);

    private static class CacheEntry {
        final Object data;
        final long timestamp;

        CacheEntry(Object data) {
            this.data = data;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired(long duration) {
            return System.currentTimeMillis() - timestamp > duration;
        }
    }

    // Cache for list (both highlight and full match)
    public static ReplayResponse getList(String type, int page) {
        String key = "list_" + type + "_" + page;  // type is "highlight" or "xemlai"
        CacheEntry entry = memoryCache.get(key);
        if (entry != null && !entry.isExpired(LIST_CACHE_DURATION)) {
            return (ReplayResponse) entry.data;
        }
        return null;
    }

    public static void putList(String type, int page, ReplayResponse data) {
        String key = "list_" + type + "_" + page;
        memoryCache.put(key, new CacheEntry(data));
    }

    // Cache for detail videos
    public static ReplayLinkResponse getDetail(String id) {
        String key = "detail_" + id;
        CacheEntry entry = memoryCache.get(key);
        if (entry != null && !entry.isExpired(DETAIL_CACHE_DURATION)) {
            return (ReplayLinkResponse) entry.data;
        }
        return null;
    }

    public static void putDetail(String id, ReplayLinkResponse data) {
        String key = "detail_" + id;
        memoryCache.put(key, new CacheEntry(data));
    }
}