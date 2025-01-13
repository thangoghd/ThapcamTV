package com.thangoghd.thapcamtv;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.thangoghd.thapcamtv.api.ApiManager;
import com.thangoghd.thapcamtv.api.SportApi;
import com.thangoghd.thapcamtv.response.ReplayLinkResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class PlayerActivity extends AppCompatActivity {

    private static PlayerActivity instance;
    private ExoPlayer player;
    private PlayerView playerView;
    private Map<String, String> qualityMap;
    private Spinner qualitySpinner;
    private ProgressBar loadingProgressBar;
    private WebView commentsWebView;
    private ImageButton chatToggleButton;
    private boolean isChatVisible = false;

    private final Handler hideHandler = new Handler(Looper.getMainLooper());
    private final Runnable hideRunnable = new Runnable() {
        @Override
        public void run() {
            qualitySpinner.setVisibility(View.INVISIBLE);
            chatToggleButton.setVisibility(View.INVISIBLE);
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

        // Add fullscreen flags
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        
        // Hide system UI
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_FULLSCREEN
        );

        allowAllSSL();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    
        playerView = findViewById(R.id.player_view);
        qualitySpinner = findViewById(R.id.quality_spinner);
        loadingProgressBar = findViewById(R.id.loading_progress);
        commentsWebView = findViewById(R.id.comments_webview);
        chatToggleButton = findViewById(R.id.chat_toggle_button);

        String videoUrl = getIntent().getStringExtra("replay_url");
        String sourceType = getIntent().getStringExtra("source_type");
        String matchId = getIntent().getStringExtra("match_id");
        String sportType = getIntent().getStringExtra("sport_type");
        boolean isLoading = getIntent().getBooleanExtra("is_loading", false);
        boolean showQualitySpinner = getIntent().getBooleanExtra("show_quality_spinner", false);

        // Handle intent from Android TV channel
        if (getIntent().getData() != null) {
            Uri data = getIntent().getData();
            if ("highlight".equals(data.getHost())) {
                String highlightId = data.getLastPathSegment();
                if (highlightId != null) {
                    showLoading(true);
                    fetchHighlightVideoFromChannel(highlightId);
                    return;
                }
            }
        }

        Log.d("PlayerActivity", "onCreate - sourceType: " + sourceType + 
              ", matchId: " + matchId + ", sportType: " + sportType + 
              ", isLoading: " + isLoading + ", from: " + getIntent().getStringExtra("from"));

        if (isLoading && matchId != null) {
            showLoading(true);
            fetchMatchStreamUrl(matchId, sportType);
        } else {
            handleVideoSource(sourceType, videoUrl);
        }

        if (!showQualitySpinner) {
            qualitySpinner.setVisibility(View.GONE);
            chatToggleButton.setVisibility(View.GONE);
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
//        resetHideTimer();
    }

    public void onStreamUrlReceived(HashMap<String, String> streamUrls) {
        runOnUiThread(() -> {
            showLoading(false);
            if (streamUrls != null && !streamUrls.isEmpty()) {
                this.qualityMap = streamUrls;
                setupChatToggle();
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
        chatToggleButton.setVisibility(View.VISIBLE);
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
        headers.put("Origins", "https://i.fdcdn.xyz/");

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
            public void onPlayerError(@NonNull PlaybackException error) {
                Log.e("PlayerActivity", "Không thể phát video: " + error.getMessage() + " | URL: " + url);
                String errorMessage = error.getCause() instanceof com.google.android.exoplayer2.upstream.HttpDataSource.InvalidResponseCodeException ? 
                    "Không thể phát video! (Mã lỗi: " + ((com.google.android.exoplayer2.upstream.HttpDataSource.InvalidResponseCodeException) error.getCause()).responseCode + ")" :
                    "Không thể phát video!";
                Toast.makeText(PlayerActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
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
        if (commentsWebView != null) {
            commentsWebView.stopLoading();
            commentsWebView.clearHistory();
            commentsWebView.clearCache(true);
            commentsWebView.destroy();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (player != null) {
            player.setPlayWhenReady(false);
        }
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (player != null) {
            player.setPlayWhenReady(true);
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (player != null) {
            player.setPlayWhenReady(false);
        }
    }

    private void fetchMatchStreamUrl(String matchId, String sportType) {
        Log.d("PlayerActivity", "Fetching stream URL for matchId: " + matchId + ", sportType: " + sportType);
        
        SportApi api;
        Call<JsonObject> call;
        
        String from = getIntent().getStringExtra("from");
        if ("vebo".equals(from)) {
            api = ApiManager.getSportApi(true); // vebo.xyz
            call = api.getVeboStreamUrl(matchId);
        } else {
            // Default to thapcam.xyz API
            api = ApiManager.getSportApi(false); // thapcam.xyz
            call = api.getThapcamStreamUrl(matchId);
        }

        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                Log.d("PlayerActivity", "Stream URL API response received");
                if (response.isSuccessful() && response.body() != null) {
                    String jsonResponse = response.body().toString();
                    try {
                        parseJsonAndStartPlayer(jsonResponse);
                    } catch (Exception e) {
                        Log.e("PlayerActivity", "Error parsing stream URL response", e);
                        showError("Có lỗi xảy ra khi tải dữ liệu");
                    }
                } else {
                    Log.e("PlayerActivity", "Stream URL API error: " + response.code());
                    showError("Không thể tải dữ liệu trận đấu.");
                }
            }
            
            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                Log.e("PlayerActivity", "Stream URL API call failed", t);
                showError("Không thể kết nối đến máy chủ.");
            }
        });
    }

    private void parseJsonAndStartPlayer(String jsonResponse) {
        try {
            Gson gson = new Gson();
            JsonObject jsonObject = gson.fromJson(jsonResponse, JsonObject.class);
            JsonObject data = jsonObject.getAsJsonObject("data");

            // Check if data is null or play_urls is null/empty
            if (data == null || data.get("play_urls") == null || data.get("play_urls").isJsonNull()) {
                Log.w("PlayerActivity", "No play URLs found in response");
                showError("Trận đấu chưa được phát sóng.");
                return;
            }

            JsonArray playUrls = data.getAsJsonArray("play_urls");
            if (playUrls == null || playUrls.isEmpty()) {
                Log.w("PlayerActivity", "Empty play URLs array");
                showError("Trận đấu chưa được phát sóng.");
                return;
            }

            HashMap<String, String> qualityMap = new HashMap<>();
            for (JsonElement element : playUrls) {
                if (!element.isJsonObject()) continue;
                
                JsonObject urlObject = element.getAsJsonObject();
                if (!urlObject.has("name") || !urlObject.has("url")) continue;

                String name = urlObject.get("name").getAsString();
                String url = urlObject.get("url").getAsString();
                qualityMap.put(name, url);
                Log.d("PlayerActivity", "Found quality option: " + name);
            }

            if (!qualityMap.isEmpty()) {
                onStreamUrlReceived(qualityMap);
            } else {
                Log.w("PlayerActivity", "No valid stream URLs found");
                showError("Trận đấu chưa được phát sóng.");
            }
        } catch (Exception e) {
            Log.e("PlayerActivity", "Error parsing JSON response", e);
            showError("Có lỗi xảy ra khi tải dữ liệu.");
        }
    }

    private void fetchHighlightVideoFromChannel(String id) {
        Log.d("PlayerActivity", "Fetching highlight video for id: " + id);
        
        ApiManager.getSportApi(true).getReplayDetails(id).enqueue(new Callback<ReplayLinkResponse>() {
            @Override
            public void onResponse(@NonNull Call<ReplayLinkResponse> call, @NonNull Response<ReplayLinkResponse> response) {
                Log.d("PlayerActivity", "Highlight video API response received");
                if (response.isSuccessful() && response.body() != null) {
                    String videoUrl = response.body().getData().getVideoUrl();
                    runOnUiThread(() -> {
                        showLoading(false);
                        handleVideoSource("replay", videoUrl);
                        qualitySpinner.setVisibility(View.GONE);
                        chatToggleButton.setVisibility(View.GONE);
                    });
                } else {
                    Log.e("PlayerActivity", "Highlight video API error: " + response.code());
                    showError("Không thể lấy được luồng video");
                }
            }

            @Override
            public void onFailure(@NonNull Call<ReplayLinkResponse> call, @NonNull Throwable t) {
                Log.e("PlayerActivity", "Highlight video API call failed", t);
                showError("Error: " + t.getMessage());
            }
        });
    }

    private void setupChatToggle() {
        ViewGroup.LayoutParams spinnerParams = qualitySpinner.getLayoutParams();
        
        chatToggleButton.setOnClickListener(v -> {
            isChatVisible = !isChatVisible;
            chatToggleButton.setImageResource(isChatVisible ? 
                R.drawable.option_chat_disable : R.drawable.option_chat_enable);
            spinnerParams.width = isChatVisible ? (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 50, getResources().getDisplayMetrics()) : ViewGroup.LayoutParams.MATCH_PARENT;

            if (isChatVisible) {
                // Get sync key from intent
                String syncKey = getIntent().getStringExtra("sync_key");
                String chatUrl = String.format("https://chat.vebotv.me/?room=%s",
                        syncKey);

                // Setup WebView only when becoming visible
                commentsWebView.getSettings().setJavaScriptEnabled(true);
                commentsWebView.setWebViewClient(new WebViewClient());
                commentsWebView.loadUrl(chatUrl);
                commentsWebView.setVisibility(View.VISIBLE);
                Log.d("PlayerActivity", "Loading chat URL: " + chatUrl);
                
            } else {
                // Clean up WebView when hidden
                commentsWebView.stopLoading();
                commentsWebView.loadUrl("about:blank");
                commentsWebView.clearHistory();
                commentsWebView.clearCache(true);
                commentsWebView.setVisibility(View.GONE);
            }
            
            // Adjust player container weight
            View playerContainer = findViewById(R.id.player_container);
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) playerContainer.getLayoutParams();
            params.weight = isChatVisible ? 0.9f : 1.0f;
            playerContainer.setLayoutParams(params);
        });
    }

    private void allowAllSSL() {
        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, new TrustManager[] { new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) {}
                @Override
                public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
            } }, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
