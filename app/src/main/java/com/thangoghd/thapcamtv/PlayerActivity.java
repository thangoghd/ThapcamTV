package com.thangoghd.thapcamtv;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
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

    private ExoPlayer player;
    private PlayerView playerView;
    private Map<String, String> qualityMap;
    private Spinner qualitySpinner;

    private Handler hideHandler = new Handler(Looper.getMainLooper());
    private Runnable hideRunnable = new Runnable() {
        @Override
        public void run() {
            qualitySpinner.setVisibility(View.INVISIBLE);
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);
    
        playerView = findViewById(R.id.player_view);
        qualitySpinner = findViewById(R.id.quality_spinner);

        String videoUrl = getIntent().getStringExtra("replay_url");
        boolean showQualitySpinner = getIntent().getBooleanExtra("show_quality_spinner", true);
        qualityMap = (HashMap<String, String>) getIntent().getSerializableExtra("stream_url");
        String sourceType = getIntent().getStringExtra("source_type");

        if (sourceType.equals("replay")) {
            // Handle replay
            if (videoUrl != null && !videoUrl.isEmpty()) {
                playStream(videoUrl);
            } else {
                Toast.makeText(this, "Không có luồng phát sóng nào.", Toast.LENGTH_SHORT).show();
                finish();
            }
        } else if (sourceType.equals("live")) {
            // Handle live stream
            if (qualityMap != null && !qualityMap.isEmpty()) {
                setupQualitySpinner();
            } else {
                Toast.makeText(this, "Không có luồng phát sóng nào.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }

        qualitySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                String selectedQuality = adapterView.getItemAtPosition(position).toString();
                String url = qualityMap.get(selectedQuality);
                playStream(url);
                resetHideTimer();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                // Nothing to do
            }
        });
        resetHideTimer();
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
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.release();
            player = null;
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
