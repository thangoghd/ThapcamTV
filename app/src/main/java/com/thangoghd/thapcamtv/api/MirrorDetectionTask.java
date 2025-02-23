package com.thangoghd.thapcamtv.api;

import android.os.AsyncTask;
import android.util.Log;

import com.thangoghd.thapcamtv.cache.MirrorUrlManager;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MirrorDetectionTask extends AsyncTask<String, Void, String> {
    private static final String TAG = "MirrorDetectionTask";
    private final MirrorUrlManager mirrorManager;

    public MirrorDetectionTask(MirrorUrlManager mirrorManager) {
        this.mirrorManager = mirrorManager;
    }

    @Override
    protected String doInBackground(String... urls) {
        String url = urls[0];
        try {
            Log.d(TAG, "Found new detecting mirror");
            return detectNewMirror(url);
        } catch (Exception e) {
            Log.e(TAG, "Error detecting mirror", e);
            return null;
        }
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        if (result != null) {
            mirrorManager.updateMirrorUrl(result);
//            MainActivity.mirrorLink = result;
        }
    }

    private String detectNewMirror(String url) throws IOException {
        OkHttpClient client = new OkHttpClient.Builder()
                .followRedirects(false)
                .addInterceptor(chain -> {
                    Request request = chain.request().newBuilder()
                            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36")
                            .header("Accept", "text/html")
                            .build();
                    return chain.proceed(request);
                })
                .build();

        okhttp3.Response response = client.newCall(new Request.Builder().url(url).build()).execute();

        // Check redirect header first
        if (response.code() >= 300 && response.code() < 400) {
            String newUrl = response.header("Location");
            if (newUrl != null) {
                return newUrl;
            }
        }

        // Parse HTML content
        String html = response.body().string();
        Pattern pattern = Pattern.compile("(https://m\\.thapcam[0-9]+\\.live/?)");
        Matcher matcher = pattern.matcher(html);

        if (matcher.find()) {
            return matcher.group(1);
        } else {
            throw new IOException("Không tìm thấy mirror mới trong HTML");
        }
    }
}
