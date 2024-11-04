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

        qualityMap = (HashMap<String, String>) getIntent().getSerializableExtra("stream_url");
        if (qualityMap == null || qualityMap.isEmpty()) {
            Toast.makeText(this, "Không có bất cứ luồng phát sóng nào.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, new ArrayList<>(qualityMap.keySet()));
        qualitySpinner.setAdapter(adapter);

        String initialQuality = qualityMap.containsKey("FullHD") ? "FullHD" : qualitySpinner.getItemAtPosition(0).toString();
        int initialPosition = new ArrayList<>(qualityMap.keySet()).indexOf(initialQuality);
        qualitySpinner.setSelection(initialPosition);
        playStream(qualityMap.get(initialQuality));


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

        DefaultHttpDataSource.Factory dataSourceFactory = new DefaultHttpDataSource.Factory();
        MediaItem mediaItem = MediaItem.fromUri("https://hl.thapcam.link/hls/2light/bro/paok-021124.mp4/playlist.m3u8");
        HlsMediaSource hlsMediaSource = new HlsMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem);

        player.setMediaSource(hlsMediaSource);
        player.prepare();
        player.setPlayWhenReady(true);

        player.addListener(new Player.Listener() {
            @Override
            public void onPlayerError(PlaybackException error) {
                Log.e("PlayerActivity", "Playback error: " + error.getMessage() + " | URL: " + url);
                Toast.makeText(PlayerActivity.this, "Playback error", Toast.LENGTH_SHORT).show();
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
}
