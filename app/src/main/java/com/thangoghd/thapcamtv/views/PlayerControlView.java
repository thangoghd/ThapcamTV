package com.thangoghd.thapcamtv.views;

import android.content.Context;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.exoplayer2.Player;
import com.thangoghd.thapcamtv.R;
import com.thangoghd.thapcamtv.adapters.SimpleMatchAdapter;
import com.thangoghd.thapcamtv.models.Match;

import java.util.ArrayList;
import java.util.List;

public class PlayerControlView extends FrameLayout {
    private static final int AUTO_HIDE_DELAY_MS = 5000;
    private static final String TAG = "PlayerControlView";
    private static final int GRID_COLUMN_COUNT = 4;

    // UI Components
    private ImageButton playPauseButton;
    private ImageButton rewindButton;
    private ImageButton forwardButton;
    private ImageButton chatToggleButton;
    private ImageButton qualityButton;
    private SeekBar progressBar;
    private TextView positionText;
    private TextView durationText;
    private View controlsRoot;
    private View controlButtonsRow;
    private View matchListContainer;
    private RecyclerView matchesRecyclerView;

    // Control variables
    private Player player;
    private final Handler hideHandler;
    private boolean isVisible = false; // Set initial state to hidden
    private final Runnable hideAction;
    private PlayerControlCallback playerCallback;
    private boolean isChatVisible = false;
    private boolean isAutoHideEnabled = true;
    private Handler autoHideHandler = new Handler(Looper.getMainLooper());
    private final Runnable autoHideRunnable = () -> {
        Log.d(TAG, "autoHideRunnable executed, isAutoHideEnabled=" + isAutoHideEnabled + ", isVisible=" + isVisible);
        if (isAutoHideEnabled && isVisible) {
            hide();
        }
    };

    private String[] qualities;
    private int currentQualityIndex = 0;
    private boolean isMatchListVisible = false;

    private SimpleMatchAdapter matchAdapter;
    private List<Match> matches = new ArrayList<>();

    private long position;
    private long duration;
    private boolean scrubbing = false; // Track if user is currently seeking
    private long scrubPosition = 0; // Position while seeking
    private boolean isPlaying;

    private boolean isPreviewMode = true; // Track if match list is in preview mode
    private int normalHeight; // Store normal height of match list
    private int previewHeight; // Store preview height of match list

    public interface PlayerControlCallback {
        void onVisibilityChanged(boolean isVisible);
        void onMatchSelected(Match match, String from);
        void onPlayPause(boolean isPlaying);
        void onRewind();
        void onForward();
        void onChatToggle();
        void onQualitySelected(int position);
        void onSeekTo(long position);
    }

    public PlayerControlView(Context context) {
        this(context, null);
    }

    public PlayerControlView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PlayerControlView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        hideHandler = new Handler(Looper.getMainLooper());
        hideAction = this::hide;
        autoHideHandler = new Handler(Looper.getMainLooper());

        initializeViews(context);
    }

    private void initializeViews(Context context) {
        LayoutInflater.from(context).inflate(R.layout.player_basic_controls, this, true);
        
        controlsRoot = findViewById(R.id.controls_root);
        controlButtonsRow = findViewById(R.id.control_buttons_row);
        matchListContainer = findViewById(R.id.match_list_container);
        matchesRecyclerView = findViewById(R.id.matches_recycler_view);
        playPauseButton = findViewById(R.id.play_pause_button);
        rewindButton = findViewById(R.id.rewind_button);
        forwardButton = findViewById(R.id.forward_button);
        chatToggleButton = findViewById(R.id.chat_toggle_button);
        qualityButton = findViewById(R.id.quality_button);
        progressBar = findViewById(R.id.progress_bar);
        positionText = findViewById(R.id.position_text);
        durationText = findViewById(R.id.duration_text);
        
        playPauseButton.requestFocus();

        setupProgressBar();

        playPauseButton.setOnClickListener(v -> {
            isPlaying = !isPlaying;
            updatePlayPauseButton();
            if (playerCallback != null) {
                playerCallback.onPlayPause(isPlaying);
            }
            resetAutoHideTimer();
        });

        rewindButton.setOnClickListener(v -> {
            Log.d(TAG, "rewindButton clicked");
            if (playerCallback != null) {
                playerCallback.onRewind();
            }
            resetAutoHideTimer();
        });

        forwardButton.setOnClickListener(v -> {
            Log.d(TAG, "forwardButton clicked");
            if (playerCallback != null) {
                playerCallback.onForward();
            }
            resetAutoHideTimer();
        });

        chatToggleButton.setOnClickListener(v -> {
            Log.d(TAG, "chatToggleButton clicked");
            if (playerCallback != null) {
                playerCallback.onChatToggle();
            }
            resetAutoHideTimer();
        });

        qualityButton.setOnClickListener(v -> {
            Log.d(TAG, "qualityButton clicked");
            showQualityDialog();
            resetAutoHideTimer();
        });

        // Setup match list
        matchAdapter = new SimpleMatchAdapter(context, (match, from) -> {
            if (playerCallback != null) {
                // Hide match list before callback to prevent multiple clicks
                hideMatchList();
                playerCallback.onMatchSelected(match, from);
            }
        });
        
        // Setup grid layout with columns
        GridLayoutManager layoutManager = new GridLayoutManager(context, GRID_COLUMN_COUNT);
        matchesRecyclerView.setLayoutManager(layoutManager);
        matchesRecyclerView.setAdapter(matchAdapter);

        // Add spacing between grid items
        int spacing = getResources().getDimensionPixelSize(R.dimen.grid_spacing);
        matchesRecyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
                                       @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                outRect.left = spacing;
                outRect.right = spacing;
                outRect.top = spacing;
                outRect.bottom = spacing;
            }
        });

        // Get dimensions
        normalHeight = getResources().getDimensionPixelSize(R.dimen.match_list_height);
        previewHeight = getResources().getDimensionPixelSize(R.dimen.match_list_preview_height);
        
        // Initially show in preview mode
        showMatchListPreview();

        // Initially hide controls
        controlsRoot.setVisibility(View.INVISIBLE);
        matchListContainer.setVisibility(View.GONE);
    }

    private void setupProgressBar() {
        progressBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    scrubPosition = (duration * progress) / seekBar.getMax();
                    updatePositionText(scrubPosition);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                scrubbing = true;
                removeCallbacks(hideAction);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                scrubbing = false;
                if (playerCallback != null) {
                    playerCallback.onSeekTo(scrubPosition);
                }
                resetAutoHideTimer();
            }
        });

        progressBar.setOnKeyListener((v, keyCode, event) -> {
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_LEFT:
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    if (event.getAction() == KeyEvent.ACTION_DOWN) {
                        if (!scrubbing) {
                            scrubbing = true;
                            scrubPosition = position;
                        }
                        // Calculate seek amount based on duration
                        long seekAmount = duration / 100; // 1% of duration
                        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                            scrubPosition = Math.max(0, scrubPosition - seekAmount);
                        } else {
                            scrubPosition = Math.min(duration, scrubPosition + seekAmount);
                        }
                        // Update progress bar and text
                        progressBar.setProgress((int) (progressBar.getMax() * scrubPosition / duration));
                        updatePositionText(scrubPosition);
                        // Reset auto hide timer while seeking
                        resetAutoHideTimer();
                        return true;
                    } else if (event.getAction() == KeyEvent.ACTION_UP) {
                        if (scrubbing) {
                            scrubbing = false;
                            if (playerCallback != null) {
                                playerCallback.onSeekTo(scrubPosition);
                            }
                            resetAutoHideTimer();
                        }
                        return true;
                    }
                    break;
            }
            return false;
        });
    }

    public void setPlayer(Player player) {
        this.player = player;
        if (player != null) {
            isPlaying = player.isPlaying();
            updatePlayPauseButton();
            updateProgress(player.getCurrentPosition(), player.getDuration());
        }
    }

    public void setCallback(PlayerControlCallback callback) {
        this.playerCallback = callback;
    }

    public void setQualityOptions(String[] qualities) {
        this.qualities = qualities;

        // Select FullHD by default if available
        for (int i = 0; i < qualities.length; i++) {
            if (qualities[i].equals("FullHD")) {
                currentQualityIndex = i;
                break;
            }
        }
    }

    public void updatePlayPauseButton() {
        if (playPauseButton != null) {
            playPauseButton.setImageResource(isPlaying ? 
                R.drawable.ic_pause : R.drawable.ic_play_arrow);
        }
    }

    public void setPlaying(boolean playing) {
        if (this.isPlaying != playing) {
            this.isPlaying = playing;
            updatePlayPauseButton();
        }
    }

    public void updateProgress(long position, long duration) {
        if (!scrubbing) {
            this.position = position;
            this.duration = duration;
            
            if (progressBar != null && duration > 0) {
                progressBar.setProgress((int) (progressBar.getMax() * position / duration));
            }
            
            updatePositionText(position);
            updateDurationText();
        }
    }

    private void updatePositionText(long position) {
        if (positionText != null) {
            positionText.setText(stringForTime(position));
        }
    }

    private void updateDurationText() {
        if (durationText != null) {
            durationText.setText(stringForTime(duration));
        }
    }

    private String stringForTime(long timeMs) {
        long totalSeconds = timeMs / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    public void show() {
        Log.d(TAG, "show() called, isVisible=" + isVisible);
        if (!isVisible) {
            playPauseButton.requestFocus();
            isVisible = true;
            controlsRoot.setVisibility(View.VISIBLE);
            showMatchListPreview(); // Show match list in preview mode
            resetAutoHideTimer();
            if (playerCallback != null) {
                playerCallback.onVisibilityChanged(true);
            }
        }
    }

    public void hide() {
        Log.d(TAG, "hide() called, isVisible=" + isVisible);
        if (isVisible) {
            isVisible = false;
            isMatchListVisible = false;
            controlsRoot.setVisibility(View.INVISIBLE);
            matchListContainer.setVisibility(View.GONE);
            autoHideHandler.removeCallbacks(autoHideRunnable);
            if (playerCallback != null) {
                playerCallback.onVisibilityChanged(false);
            }
        }
    }

    private void resetAutoHideTimer() {
        Log.d(TAG, "Resetting auto-hide timer");
        autoHideHandler.removeCallbacks(autoHideRunnable);
        if (isAutoHideEnabled) {
            autoHideHandler.postDelayed(autoHideRunnable, AUTO_HIDE_DELAY_MS);
        }
    }

    public void hideQualitySpinner() {
        if (qualityButton != null) {
            qualityButton.setVisibility(View.GONE);
            chatToggleButton.setVisibility(View.GONE);
        }
    }

    public void updateChatIcon(boolean isVisible) {
        isChatVisible = isVisible;
        if (chatToggleButton != null) {
            chatToggleButton.setImageResource(isVisible ? 
                R.drawable.option_chat_disable : R.drawable.option_chat_enable);
        }
    }

    public void setQualitySpinnerWidth(boolean isChatVisible) {
        if (qualityButton != null) {
            ViewGroup.LayoutParams params = qualityButton.getLayoutParams();
            params.width = isChatVisible ? 
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50, getResources().getDisplayMetrics()) 
                : ViewGroup.LayoutParams.MATCH_PARENT;
            qualityButton.setLayoutParams(params);
        }
    }

    private void showQualityDialog() {
        if (qualities == null || qualities.length == 0) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.AlertDialogTheme);
        builder.setTitle("Chất lượng");

        // Create list with radio buttons, highlight current quality
        builder.setSingleChoiceItems(qualities, currentQualityIndex, (dialog, which) -> {
            if (currentQualityIndex != which) {
                currentQualityIndex = which;
                if (playerCallback != null) {
                    playerCallback.onQualitySelected(which);
                }
            }
            dialog.dismiss();
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void setMatches(List<Match> newMatches) {
        Log.d(TAG, "setMatches: size=" + (newMatches != null ? newMatches.size() : 0));
        matches.clear();
        if (newMatches != null) {
            matches.addAll(newMatches);
        }
        matchAdapter.updateMatches(matches);
    }

    private void showMatchListPreview() {
        if (matchListContainer != null) {
            isPreviewMode = true;
            matchListContainer.setVisibility(View.VISIBLE);
            ViewGroup.LayoutParams params = matchListContainer.getLayoutParams();
            params.height = previewHeight;
            matchListContainer.setLayoutParams(params);
            matchListContainer.requestLayout();
        }
    }

    private void showMatchListFull() {
        if (matchListContainer != null) {
            isPreviewMode = false;
            isMatchListVisible = true;
            matchListContainer.setVisibility(View.VISIBLE);

            // Animate height change
            android.animation.ValueAnimator animator = android.animation.ValueAnimator.ofInt(previewHeight, normalHeight);
            animator.setDuration(250); // Duration in milliseconds
            animator.setInterpolator(new AccelerateDecelerateInterpolator());
            
            animator.addUpdateListener(animation -> {
                int value = (int) animation.getAnimatedValue();
                ViewGroup.LayoutParams params = matchListContainer.getLayoutParams();
                params.height = value;
                matchListContainer.setLayoutParams(params);
            });
            
            animator.addListener(new android.animation.AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(android.animation.Animator animation) {
                    matchesRecyclerView.requestFocus();
                }
            });
            
            animator.start();
            resetAutoHideTimer();
        }
    }

    private void hideMatchList() {
        if (matchListContainer != null && isMatchListVisible) {
            // Animate back to preview height
            android.animation.ValueAnimator animator = android.animation.ValueAnimator.ofInt(normalHeight, previewHeight);
            animator.setDuration(250);
            animator.setInterpolator(new AccelerateDecelerateInterpolator());
            
            animator.addUpdateListener(animation -> {
                int value = (int) animation.getAnimatedValue();
                ViewGroup.LayoutParams params = matchListContainer.getLayoutParams();
                params.height = value;
                matchListContainer.setLayoutParams(params);
            });
            
            animator.addListener(new android.animation.AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(android.animation.Animator animation) {
                    isMatchListVisible = false;
                    isPreviewMode = true;
                    playPauseButton.requestFocus();
                }
            });
            
            animator.start();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d(TAG, "onKeyDown: keyCode=" + keyCode + ", isVisible=" + isVisible + ", isMatchListVisible=" + isMatchListVisible);
        
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                if (!isVisible) {
                    show();
                    return true;
                }
                break;

            case KeyEvent.KEYCODE_DPAD_DOWN:
                if (isVisible && !isMatchListVisible) {
                    View focusedView = findFocus();
                    Log.d(TAG, "onKeyDown: focusedView=" + (focusedView != null ? focusedView.getId() : "null"));
                    if (focusedView != null && 
                        (focusedView == playPauseButton || 
                         focusedView == rewindButton || 
                         focusedView == forwardButton || 
                         focusedView == chatToggleButton || 
                         focusedView == qualityButton)) {
                        showMatchListFull();
                        return true;
                    }
                }
                break;

            case KeyEvent.KEYCODE_DPAD_UP:
                if (isVisible && isMatchListVisible) {
                    View focusedView = findFocus();
                    // Check if focus is in matches_recycler_view
                    if (focusedView != null && 
                        (focusedView.getParent() == matchesRecyclerView || focusedView == matchesRecyclerView)) {
                        // Get the position of the focused item
                        GridLayoutManager layoutManager = (GridLayoutManager) matchesRecyclerView.getLayoutManager();
                        if (layoutManager != null) {
                            int position = layoutManager.getPosition(focusedView);
                            if (position < GRID_COLUMN_COUNT) {
                                hideMatchList();
                                return true;
                            }
                        }
                    }
                }
                break;
        }

        resetAutoHideTimer();
        return super.onKeyDown(keyCode, event);
    }

    public void setAutoHideEnabled(boolean enabled) {
        isAutoHideEnabled = enabled;
        if (!enabled) {
            autoHideHandler.removeCallbacks(autoHideRunnable);
        }
    }

    public boolean isVisible() {
        return isVisible;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        Log.d(TAG, "onAttachedToWindow");
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        Log.d(TAG, "onDetachedFromWindow");
        hideHandler.removeCallbacks(hideAction);
        autoHideHandler.removeCallbacks(autoHideRunnable);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            Log.d(TAG, "onTouchEvent: ACTION_DOWN");
            resetAutoHideTimer();
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        Log.d(TAG, "onGenericMotionEvent");
        resetAutoHideTimer();
        return super.onGenericMotionEvent(event);
    }
}
