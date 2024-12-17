package com.thangoghd.thapcamtv;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class PlayerActivity extends AppCompatActivity {

    public static final String ACTION_STREAM_READY = "com.thangoghd.thapcamtv.STREAM_READY";
    private static PlayerActivity instance;
    private ExoPlayer player;
    private PlayerView playerView;
    private Map<String, String> qualityMap;
    private Spinner qualitySpinner;
    private ProgressBar loadingProgressBar;

    private Handler hideHandler = new Handler(Looper.getMainLooper());
    private Runnable hideRunnable = new Runnable() {
        @Override
        public void run() {
            qualitySpinner.setVisibility(View.INVISIBLE);
        }
    };

    public static PlayerActivity getInstance() {
        return instance;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;
        setContentView(R.layout.activity_player);
    
        playerView = findViewById(R.id.player_view);
        qualitySpinner = findViewById(R.id.quality_spinner);
        loadingProgressBar = findViewById(R.id.loading_progress);

        String videoUrl = getIntent().getStringExtra("replay_url");
        String sourceType = getIntent().getStringExtra("source_type");
        boolean isLoading = getIntent().getBooleanExtra("is_loading", false);
        if (isLoading) {
            showLoading(true);
        } else {
            handleVideoSource(sourceType, videoUrl);
        }

        qualitySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String quality = parent.getItemAtPosition(position).toString();
                String streamUrl = qualityMap.get(quality);
                if (streamUrl != null) {
                    playStream(streamUrl);
                }
                resetHideTimer();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Nothing to do
            }
        });
        resetHideTimer();
    }

    public void onStreamUrlReceived(HashMap<String, String> streamUrls) {
        runOnUiThread(() -> {
            showLoading(false);
            if (streamUrls != null && !streamUrls.isEmpty()) {
                this.qualityMap = streamUrls;
                setupQualitySpinner();
            } else {
                showError("Không có luồng phát sóng.");
            }
        });
    }

    private void handleVideoSource(String sourceType, String videoUrl) {
        if ("replay".equals(sourceType)) {
            if (videoUrl != null && !videoUrl.isEmpty()) {
                playStream(videoUrl);
            } else {
                showError("Không có luồng phát sóng.");
            }
        } else if ("live".equals(sourceType)) {
            if (qualityMap != null && !qualityMap.isEmpty()) {
                setupQualitySpinner();
            } else {
                showError("Không có luồng phát sóng.");
            }
        }
    }

    private void setupQualitySpinner() {
        qualitySpinner.setVisibility(View.VISIBLE);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, new ArrayList<>(qualityMap.keySet()));
        qualitySpinner.setAdapter(adapter);
    
        String initialQuality = qualityMap.containsKey("FullHD") ? "FullHD" : qualitySpinner.getItemAtPosition(0).toString();
        int initialPosition = new ArrayList<>(qualityMap.keySet()).indexOf(initialQuality);
        qualitySpinner.setSelection(initialPosition);
    }
    
    private void resetHideTimer() {
        qualitySpinner.setVisibility(View.VISIBLE);
        hideHandler.removeCallbacks(hideRunnable);
        hideHandler.postDelayed(hideRunnable, 5000);
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        resetHideTimer();
    }

    private void playStream(String url) {
        if (player != null) {
            player.release();
        }

        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);

        // Configure headers based on thapcam app
        Map<String, String> headers = new HashMap<>();
        headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/110.0.0.0 Safari/537.36");
        headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8");
        headers.put("Accept-Language", "vi-VN,vi;q=0.8,en-US;q=0.5,en;q=0.3");
        headers.put("Connection", "keep-alive");
        headers.put("Referer", "https://i.fdcdn.xyz/");

        // Create DataSource Factory with headers
        DefaultHttpDataSource.Factory dataSourceFactory = new DefaultHttpDataSource.Factory()
            .setDefaultRequestProperties(headers)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(15000)
            .setAllowCrossProtocolRedirects(true);

        // Create MediaSource
        MediaItem mediaItem = MediaItem.fromUri(url);
        HlsMediaSource hlsMediaSource = new HlsMediaSource.Factory(dataSourceFactory)
            .setAllowChunklessPreparation(true)
            .createMediaSource(mediaItem);

        player.setMediaSource(hlsMediaSource);
        player.prepare();
        player.setPlayWhenReady(true);

        player.addListener(new Player.Listener() {
            @Override
            public void onPlayerError(PlaybackException error) {
                Log.e("PlayerActivity", "Không thể phát video: " + error.getMessage() + " | URL: " + url);
                Toast.makeText(PlayerActivity.this, "Không thể phát video!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showLoading(boolean show) {
        if (loadingProgressBar != null) {
            loadingProgressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (playerView != null) {
            playerView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.release();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (player != null) {
            player.setPlayWhenReady(false);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (player != null) {
            player.setPlayWhenReady(true);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (player != null) {
            player.setPlayWhenReady(false);
        }
    }
}
