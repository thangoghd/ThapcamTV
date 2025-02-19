package com.thangoghd.thapcamtv;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.thangoghd.thapcamtv.api.ApiManager;
import com.thangoghd.thapcamtv.api.RetrofitClient;
import com.thangoghd.thapcamtv.api.SportApi;
import com.thangoghd.thapcamtv.api.StreamUrlFetcher;
import com.thangoghd.thapcamtv.models.Match;
import com.thangoghd.thapcamtv.repositories.RepositoryCallback;
import com.thangoghd.thapcamtv.repositories.SportRepository;
import com.thangoghd.thapcamtv.response.ReplayLinkResponse;
import com.thangoghd.thapcamtv.views.PlayerControlView;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import android.view.LayoutInflater;
import android.view.Gravity;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class PlayerActivity extends AppCompatActivity {

    private static PlayerActivity instance;
    private ExoPlayer player;
    private PlayerView playerView;
    private PlayerControlView playerControlView;
    private Map<String, String> qualityMap;
    private ProgressBar loadingProgressBar;
    private WebView commentsWebView;
    private boolean isChatVisible = false;
    private String syncKey;
    private final Handler progressHandler = new Handler(Looper.getMainLooper());
    private final Runnable updateProgressAction = new Runnable() {
        @Override
        public void run() {
            if (player != null) {
                playerControlView.updateProgress(player.getCurrentPosition(), player.getDuration());
            }
            progressHandler.postDelayed(this, 1000);
        }
    };

    private final Runnable hideRunnable = new Runnable() {
        @Override
        public void run() {
            playerControlView.hide();
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
        playerControlView = findViewById(R.id.player_controls);
        loadingProgressBar = findViewById(R.id.loading_progress);
        commentsWebView = findViewById(R.id.comments_webview);

        setupPlayerControlView();

        String videoUrl = getIntent().getStringExtra("replay_url");
        String sourceType = getIntent().getStringExtra("source_type");
        String matchId = getIntent().getStringExtra("match_id");
        String matchFrom = getIntent().getStringExtra("from");
        syncKey = getIntent().getStringExtra("sync_key");
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
              ", matchId: " + matchId +
              ", isLoading: " + isLoading + ", from: " + matchFrom +
              ", syncKey: " + syncKey);

        if (isLoading && matchId != null) {
            showLoading(true);
            Match match = new Match();
            match.setId(matchId);
            match.setFrom(matchFrom);
            fetchMatchStreamUrl(match, new RepositoryCallback<JsonObject>() {
                @Override
                public void onSuccess(JsonObject result) {
                    parseJsonAndStartPlayer(result.toString());
                }

                @Override
                public void onError(Exception e) {
                    showErrorDialog("Có lỗi xảy ra khi tải dữ liệu");
                }
            });
        } else {
            handleVideoSource(sourceType, videoUrl);
        }

        if (!showQualitySpinner) {
            playerControlView.hideQualitySpinner();
        }
    }

    private void setupPlayerControlView() {
        boolean showQualitySpinner = getIntent().getBooleanExtra("show_quality_spinner", false);
        playerControlView.setShowQualitySpinner(showQualitySpinner);
        
        playerControlView.setCallback(new PlayerControlView.PlayerControlCallback() {
            @Override
            public void onVisibilityChanged(boolean isVisible) {
                // Load matches when controls become visible
                if (isVisible) {
                    loadLiveMatches();
                }
            }

            @Override
            public void onMatchSelected(Match match, String from, String syncKey) {
                if (match != null) {
                    String matchId = match.getId();
                    // Update syncKey
                    if (syncKey != null) {
                        updateSyncKey(syncKey);
                    }
                    fetchMatchStreamUrl(match, new RepositoryCallback<JsonObject>() {
                        @Override
                        public void onSuccess(JsonObject result) {
                            parseJsonAndStartPlayer(result.toString());
                        }

                        @Override
                        public void onError(Exception e) {
                            showErrorDialog("Có lỗi xảy ra khi tải dữ liệu");
                        }
                    });
                }
            }

            @Override
            public void onPlayPause(boolean isPlaying) {
                if (player != null) {
                    if (isPlaying) {
                        player.play();
                    } else {
                        player.pause();
                    }
                }
            }

            @Override
            public void onRewind() {
                if (player != null) {
                    player.seekTo(Math.max(0, player.getCurrentPosition() - 10000));
                }
            }

            @Override
            public void onForward() {
                if (player != null) {
                    player.seekTo(Math.min(player.getDuration(), player.getCurrentPosition() + 10000));
                }
            }

            @Override
            public void onChatToggle() {
                toggleChat();
            }

            @Override
            public void onQualitySelected(int position) {
                if (qualityMap != null && position < qualityMap.size()) {
                    String quality = new ArrayList<>(qualityMap.keySet()).get(position);
                    String streamUrl = qualityMap.get(quality);
                    if (streamUrl != null) {
                        playStream(streamUrl);
                    }
                }
            }

            @Override
            public void onSeekTo(long position) {
                if (player != null) {
                    player.seekTo(position);
                }
            }
        });
    }

    public void onStreamUrlReceived(HashMap<String, String> streamUrls) {
        runOnUiThread(() -> {
            showLoading(false);
            if (streamUrls != null && !streamUrls.isEmpty()) {
                this.qualityMap = streamUrls;
                setupQualitySpinner();
            } else {
                showErrorDialog("Không có luồng phát sóng.");
            }
        });
    }

    private void handleVideoSource(String sourceType, String url) {
        if ("replay".equals(sourceType)) {
            if (url != null && !url.isEmpty()) {
                playStream(url);
            } else {
                showErrorDialog("Không có luồng phát sóng.");
            }
        } else if ("live".equals(sourceType)) {
            if (qualityMap != null && !qualityMap.isEmpty()) {
                setupQualitySpinner();
            } else {
                showErrorDialog("Không có luồng phát sóng.");
            }
        }
    }

    private void setupQualitySpinner() {
        if (qualityMap != null && !qualityMap.isEmpty()) {
            String[] qualities = qualityMap.keySet().toArray(new String[0]);
            playerControlView.setQualityOptions(qualities);

            // Select FullHD by default if available
            String initialQuality = qualityMap.containsKey("FullHD") ? "FullHD" : qualities[0];
            String streamUrl = qualityMap.get(initialQuality);
            if (streamUrl != null) {
                playStream(streamUrl);
            }
        }
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
    }

    private void playStream(String url) {
        initializePlayer();

        // Configure headers based on thapcam app
        Map<String, String> headers = new HashMap<>();
        String refererUrl = RetrofitClient.getRefererUrl();
        headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/110.0.0.0 Safari/537.36");
        headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8");
        headers.put("Accept-Language", "vi-VN,vi;q=0.8,en-US;q=0.5,en;q=0.3");
        headers.put("Connection", "keep-alive");
        boolean showQualitySpinner = getIntent().getBooleanExtra("show_quality_spinner", false);
        if (showQualitySpinner) {
            headers.put("Referer", refererUrl);
            headers.put("Origins", refererUrl);
        }

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

        // Start progress updates
        progressHandler.removeCallbacks(updateProgressAction);
        progressHandler.post(updateProgressAction);

        player.addListener(new Player.Listener() {
            @Override
            public void onPlayerError(@NonNull PlaybackException error) {
                Log.e("PlayerActivity", "Không thể phát video: " + error.getMessage() + " | URL: " + url);
                String errorMessage = "Không thể phát video!";
                if (error.getCause() instanceof HttpDataSource.InvalidResponseCodeException) {
                    int responseCode = ((HttpDataSource.InvalidResponseCodeException) error.getCause()).responseCode;
                    errorMessage = "Không thể phát video! (Mã lỗi: " + responseCode + ")";
                    if (responseCode == 403) {
                        RetrofitClient.fetchConfig();
                    }
                }
                showErrorDialog(errorMessage);
            }
        });
    }

    private void initializePlayer() {
        if (player == null) {
            player = new ExoPlayer.Builder(this).build();
            player.setPlayWhenReady(true); // Auto play when ready
            player.addListener(new Player.Listener() {
                @Override
                public void onPlaybackStateChanged(int state) {
                    if (state == Player.STATE_READY) {
                        showLoading(false);
                        playerControlView.setPlaying(player.isPlaying());
                    }
                }

                @Override
                public void onIsPlayingChanged(boolean isPlaying) {
                    playerControlView.setPlaying(isPlaying);
                }
            });
            playerView.setPlayer(player);
        }
    }

    private void showLoading(boolean show) {
        if (loadingProgressBar != null) {
            loadingProgressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (playerView != null) {
            playerView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    private void showErrorDialog(String message) {
        View errorDialog = LayoutInflater.from(this).inflate(R.layout.dialog_error, null);
        TextView errorMessage = errorDialog.findViewById(R.id.error_message);
        errorMessage.setText(message);

        FrameLayout playerContainer = findViewById(R.id.player_container);
        if (playerContainer.indexOfChild(errorDialog) == -1) {
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
            );
            params.gravity = Gravity.CENTER;
            playerContainer.addView(errorDialog, params);
        }

        errorDialog.setVisibility(View.VISIBLE);
        new Handler().postDelayed(() -> {
            if (errorDialog != null) {
                playerContainer.removeView(errorDialog);
            }
        }, 3000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        progressHandler.removeCallbacks(updateProgressAction);
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
        playerControlView.setAutoHideEnabled(false);
        if (player != null) {
            player.setPlayWhenReady(false);
        }
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onResume() {
        super.onResume();
        playerControlView.setAutoHideEnabled(true);
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

    private static void handleStreamApiResponse(Response<JsonObject> response, RepositoryCallback<JsonObject> callback) {
        if (response.isSuccessful() && response.body() != null) {
            callback.onSuccess(response.body());
        } else {
            String errorMsg = "Failed to fetch stream URL. Code: " + response.code();
            if (response.errorBody() != null) {
                try {
                    errorMsg += ". Error: " + response.errorBody().string();
                } catch (IOException e) {
                    Log.e("PlayerActivity", "Error reading error body", e);
                }
            }
            callback.onError(new Exception(errorMsg));
        }
    }

    private static void handleStreamApiFailure(Throwable t, RepositoryCallback<JsonObject> callback) {
        Log.e("PlayerActivity", "API call failed", t);
        callback.onError(t instanceof Exception ? (Exception) t : new Exception(t));
    }

    public static void fetchMatchStreamUrl(Match match, RepositoryCallback<JsonObject> callback) {
        if (match == null) {
            callback.onError(new IllegalArgumentException("Match cannot be null"));
            return;
        }

        if ("vebo".equals(match.getFrom())) {
            // For vebo.xyz
            try {
                SportApi api = ApiManager.getSportApi(true);
                api.getVeboStreamUrl(match.getId())
                   .enqueue(new Callback<JsonObject>() {
                        @Override
                        public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                            handleStreamApiResponse(response, callback);
                        }
                        
                        @Override
                        public void onFailure(Call<JsonObject> call, Throwable t) {
                            handleStreamApiFailure(t, callback);
                        }
                    });
            } catch (Exception e) {
                Log.e("PlayerActivity", "Error creating Vebo API call", e);
                callback.onError(e);
            }
        } else {
            // For thapcam.xyz
            StreamUrlFetcher.fetchStreamMetadata(match, new StreamUrlFetcher.Callback<StreamUrlFetcher.StreamMetadata>() {
                @Override
                public void onSuccess(StreamUrlFetcher.StreamMetadata metadata) {
                    try {
                        SportApi api = ApiManager.getSportApi(false);
                        api.getThapcamStreamUrl(metadata.encryptedId, "no", metadata.token)
                           .enqueue(new Callback<JsonObject>() {
                                @Override
                                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                                    handleStreamApiResponse(response, callback);
                                }
                                
                                @Override
                                public void onFailure(Call<JsonObject> call, Throwable t) {
                                    handleStreamApiFailure(t, callback);
                                }
                            });
                    } catch (Exception e) {
                        Log.e("PlayerActivity", "Error creating Thapcam API call", e);
                        callback.onError(e);
                    }
                }
                
                @Override
                public void onError(Exception e) {
                    Log.e("PlayerActivity", "Error fetching stream metadata", e);
                    callback.onError(e);
                }
            });
        }
    }

    private void parseJsonAndStartPlayer(String jsonResponse) {
        try {
            Gson gson = new Gson();
            JsonObject jsonObject = gson.fromJson(jsonResponse, JsonObject.class);
            JsonObject data = jsonObject.getAsJsonObject("data");

            // Check if data is null or play_urls is null/empty
            if (data == null || data.get("play_urls") == null || data.get("play_urls").isJsonNull()) {
                Log.w("PlayerActivity", "No play URLs found in response");
                showErrorDialog("Trận đấu chưa được phát sóng.");
                return;
            }

            JsonArray playUrls = data.getAsJsonArray("play_urls");
            if (playUrls == null || playUrls.isEmpty()) {
                Log.w("PlayerActivity", "Empty play URLs array");
                showErrorDialog("Trận đấu chưa được phát sóng.");
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
                showErrorDialog("Trận đấu chưa được phát sóng.");
            }
        } catch (Exception e) {
            Log.e("PlayerActivity", "Error parsing JSON response", e);
            showErrorDialog("Có lỗi xảy ra khi tải dữ liệu.");
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
                        playerControlView.hideQualitySpinner();
                    });
                } else {
                    Log.e("PlayerActivity", "Highlight video API error: " + response.code());
                    showErrorDialog("Không thể lấy được luồng video");
                }
            }

            @Override
            public void onFailure(@NonNull Call<ReplayLinkResponse> call, @NonNull Throwable t) {
                Log.e("PlayerActivity", "Highlight video API call failed", t);
                showErrorDialog("Error: " + t.getMessage());
            }
        });
    }

    private void toggleChat() {
        isChatVisible = !isChatVisible;
        
        // Update PlayerControlView
        playerControlView.updateChatIcon(isChatVisible);
        playerControlView.setQualitySpinnerWidth(isChatVisible);

        if (isChatVisible) {
            // Get sync key from intent
            String chatUrl = String.format("https://chat.vebotv.me/?room=%s", syncKey);

            // Setup WebView
            commentsWebView.getSettings().setJavaScriptEnabled(true);
            commentsWebView.setWebViewClient(new WebViewClient());
            commentsWebView.loadUrl(chatUrl);
            commentsWebView.setVisibility(View.VISIBLE);
            Log.d("PlayerActivity", "Loading chat URL: " + chatUrl);
        } else {
            // Clean up WebView
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
    }

    private void updateSyncKey(String newSyncKey) {
        this.syncKey = newSyncKey;
        // Update WebView URL with new syncKey if chat is visible
        if (commentsWebView != null && commentsWebView.getVisibility() == View.VISIBLE) {
            String chatUrl = String.format("https://chat.vebotv.me/?room=%s", syncKey);
            commentsWebView.loadUrl(chatUrl);
        }
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

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // If controls are not visible, show them first for any key press
        if (!playerControlView.isVisible()) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_CENTER:
                case KeyEvent.KEYCODE_ENTER:
                case KeyEvent.KEYCODE_DPAD_LEFT:
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                case KeyEvent.KEYCODE_DPAD_UP:
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    playerControlView.show();
                    return true;
            }
        }
        
        // Let PlayerControlView handle all UI-related keys first
        if (playerControlView.onKeyDown(keyCode, event)) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        // Show controls on any touch event if they're hidden
        if (!playerControlView.isVisible()) {
            playerControlView.show();
            return true;
        }
        return super.dispatchTouchEvent(ev);
    }

    private void loadLiveMatches() {
        // Create repositories for both APIs
        SportApi veboApi = ApiManager.getSportApi(true);
        SportApi thapcamApi = ApiManager.getSportApi(false);
        SportRepository veboRepository = new SportRepository(veboApi);
        SportRepository thapcamRepository = new SportRepository(thapcamApi);

        final List<Match> allMatches = new ArrayList<>();
        final AtomicInteger completedCalls = new AtomicInteger(0);

        // Load matches from vebo.xyz
        veboRepository.getMatches(new RepositoryCallback<List<Match>>() {
            @Override
            public void onSuccess(List<Match> result) {
                synchronized (allMatches) {
                    if (result != null) {
                        for (Match match : result) {
                            if (match.getLive()) {
                                match.setFrom("vebo");
                                allMatches.add(match);
                            }
                        }
                    }
                }
                checkAndUpdateMatches(completedCalls, allMatches);
            }

            @Override
            public void onError(Exception e) {
                Log.e("PlayerActivity", "Error loading vebo matches", e);
                checkAndUpdateMatches(completedCalls, allMatches);
            }
        });

        // Load matches from thapcam.xyz
        thapcamRepository.getMatches(new RepositoryCallback<List<Match>>() {
            @Override
            public void onSuccess(List<Match> result) {
                synchronized (allMatches) {
                    if (result != null) {
                        for (Match match : result) {
                            if (match.getLive() && 
                                !"finished".equalsIgnoreCase(match.getMatchStatus()) &&
                                !"canceled".equalsIgnoreCase(match.getMatchStatus())) {
                                match.setFrom("thapcam");
                                allMatches.add(match);
                            }
                        }
                    }
                }
                checkAndUpdateMatches(completedCalls, allMatches);
            }

            @Override
            public void onError(Exception e) {
                Log.e("PlayerActivity", "Error loading thapcam matches", e);
                checkAndUpdateMatches(completedCalls, allMatches);
            }
        });
    }

    private void checkAndUpdateMatches(AtomicInteger completedCalls, List<Match> allMatches) {
        if (completedCalls.incrementAndGet() == 2) {
            // Sort matches by sport type priority and tournament priority
            List<String> SPORT_PRIORITY = Arrays.asList("live", "football", "basketball", "esports",
                "tennis", "volleyball", "badminton", "race", "pool", "wwe", "event", "other");
            
            allMatches.sort((m1, m2) -> {
                // First compare by sport type priority
                int sport1Index = SPORT_PRIORITY.indexOf(m1.getSportType());
                int sport2Index = SPORT_PRIORITY.indexOf(m2.getSportType());
                if (sport1Index != sport2Index) {
                    return Integer.compare(sport1Index, sport2Index);
                }
                
                // If same sport type, compare by tournament priority
                return Integer.compare(
                    m2.getTournament() != null ? m2.getTournament().getPriority() : 0,
                    m1.getTournament() != null ? m1.getTournament().getPriority() : 0
                );
            });

            // Debug sorted matches
            Log.d("PlayerActivity", "Sorted matches:");
            for (Match match : allMatches) {
                Log.d("PlayerActivity", "Match - id: " + match.getId() + 
                      ", sync: " + match.getSync() + 
                      ", from: " + match.getFrom() +
                      ", sport: " + match.getSportType());
            }

            runOnUiThread(() -> {
                playerControlView.setMatches(allMatches);
            });
        }
    }
}
