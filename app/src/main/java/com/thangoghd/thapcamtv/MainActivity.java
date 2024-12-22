package com.thangoghd.thapcamtv;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.leanback.widget.BrowseFrameLayout;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import com.thangoghd.thapcamtv.api.ApiManager;
import com.thangoghd.thapcamtv.api.RetrofitClient;
import com.thangoghd.thapcamtv.response.ReplayLinkResponse;
import com.thangoghd.thapcamtv.utils.UpdateManager;
import com.thangoghd.thapcamtv.models.GitHubRelease;

public class MainActivity extends FragmentActivity implements View.OnKeyListener{
    private BrowseFrameLayout navBar;

    private LinearLayout btnLive;
    private LinearLayout btnHighlight;
    private LinearLayout btnReplay;
    private LinearLayout btnCheckUpdate;
    private LinearLayout lastSelectedMenu;

    private boolean SIDE_MENU = false;
    private UpdateManager updateManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        navBar = findViewById(R.id.blfNavBar);
        btnLive = findViewById(R.id.navLive);
        btnHighlight = findViewById(R.id.navHighlight);
        btnReplay = findViewById(R.id.navFullMatch);
        btnCheckUpdate = findViewById(R.id.navCheckUpdate);
        
        // Ẩn nút Check Update mặc định
        btnCheckUpdate.setVisibility(View.GONE);

        navBar.setOnKeyListener(this);
        btnLive.setOnKeyListener(this);
        btnHighlight.setOnKeyListener(this);
        btnReplay.setOnKeyListener(this);
        btnCheckUpdate.setOnKeyListener(this);

        lastSelectedMenu = btnLive;
        lastSelectedMenu.requestFocus();

        changeFragment(new LiveFragment());
        
        // Khởi tạo và check update
        updateManager = new UpdateManager(this);
        checkForUpdate();

        // Handle deep link from channel
        if (getIntent() != null && getIntent().getData() != null) {
            Uri data = getIntent().getData();
            if (data.getHost().equals("highlight")) {
                String highlightId = data.getLastPathSegment();
                if (highlightId != null) {
                    openHighlightVideo(highlightId);
                }
            }
        }
    }

    @Override
    public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER){
            if (view.getId() == R.id.navLive) {
                changeFragment(new LiveFragment());
                animateMenu(btnLive);
            } else if (view.getId() == R.id.navHighlight) {
                changeFragment(new HighlightFragment());
                animateMenu(btnHighlight);
            } else if (view.getId() == R.id.navFullMatch) {
                changeFragment(new FullMatchFragment());
                animateMenu(btnReplay);
            } else if (view.getId() == R.id.navCheckUpdate) {
                changeFragment(new UpdateFragment());
                animateMenu(btnCheckUpdate);
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            if (!SIDE_MENU) {
                openMenu();
                SIDE_MENU = true;
            }
        }
        return false;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT && SIDE_MENU) {
            closeMenu();
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            if (btnLive.hasFocus()) { 
                closeMenu();
            }
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            if (btnCheckUpdate.hasFocus()) {
                closeMenu();
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        if (SIDE_MENU) {
            SIDE_MENU = false;
            closeMenu();
        } else {
            super.onBackPressed();
        }
    }

    private void openMenu() {
        lastSelectedMenu.requestFocus();
        navBar.requestLayout();
        animateMenuWidth(navBar, getWidthInPercent(this, 5), getWidthInPercent(this, 16));
        SIDE_MENU = true;
    }

    private void closeMenu() {
        if(!SIDE_MENU) return;
        navBar.requestLayout();
        animateMenuWidth(navBar, getWidthInPercent(this, 16), getWidthInPercent(this, 5));
        SIDE_MENU = false;
    }

    private int getWidthInPercent(Context context, int percent) {
        int width = context.getResources().getDisplayMetrics().widthPixels;
        return (width * percent) / 100;
    }

    private void animateMenuWidth(View view, int startWidth, int endWidth) {
        ValueAnimator animator = ValueAnimator.ofInt(startWidth, endWidth);
        animator.addUpdateListener(animation -> {
            view.getLayoutParams().width = (int) animation.getAnimatedValue();
            view.requestLayout();
        });
        animator.setDuration(300);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.start();
    }

    private void animateMenu(LinearLayout menu) {
        lastSelectedMenu = menu;
        lastSelectedMenu.requestFocus();
    }

    private void changeFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.sideMenu, fragment)
                .commit();
        closeMenu();
    }

    private void checkForUpdate() {
        RetrofitClient.getGitHubApiService()
            .getLatestRelease("thangoghd", "ThapcamTV")
            .enqueue(new Callback<GitHubRelease>() {
                @Override
                public void onResponse(Call<GitHubRelease> call, Response<GitHubRelease> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        GitHubRelease release = response.body();
                        String currentVersion = getCurrentVersion();
                        String latestVersion = release.getTagName().replace("v", "");
                        
                        if (isUpdateAvailable(currentVersion, latestVersion)) {
                            // Hiện nút Check Update khi có bản mới
                            btnCheckUpdate.setVisibility(View.VISIBLE);
                        } else {
                            // Ẩn nút Check Update khi đang là bản mới nhất
                            btnCheckUpdate.setVisibility(View.GONE);
                        }
                    }
                }
                
                @Override
                public void onFailure(Call<GitHubRelease> call, Throwable t) {
                    // Ẩn nút Check Update khi có lỗi
                    btnCheckUpdate.setVisibility(View.GONE);
                }
            });
    }
    
    private String getCurrentVersion() {
        try {
            return getPackageManager()
                .getPackageInfo(getPackageName(), 0)
                .versionName;
        } catch (Exception e) {
            return "0.0.0";
        }
    }
    
    private boolean isUpdateAvailable(String currentVersion, String latestVersion) {
        try {
            String[] current = currentVersion.split("\\.");
            String[] latest = latestVersion.split("\\.");
            
            for (int i = 0; i < Math.min(current.length, latest.length); i++) {
                int currentPart = Integer.parseInt(current[i]);
                int latestPart = Integer.parseInt(latest[i]);
                
                if (latestPart > currentPart) {
                    return true;
                } else if (latestPart < currentPart) {
                    return false;
                }
            }
            
            return latest.length > current.length;
        } catch (Exception e) {
            return false;
        }
    }

    private void openHighlightVideo(String id) {
        ApiManager.getSportApi(true).getReplayDetails(id).enqueue(new Callback<ReplayLinkResponse>() {
            @Override
            public void onResponse(Call<ReplayLinkResponse> call, Response<ReplayLinkResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String videoUrl = response.body().getData().getVideoUrl();
                    Intent intent = new Intent(MainActivity.this, PlayerActivity.class);
                    intent.putExtra("replay_url", videoUrl);
                    intent.putExtra("show_quality_spinner", false);
                    intent.putExtra("source_type", "replay");
                    startActivity(intent);
                } else {
                    Toast.makeText(MainActivity.this, "Không thể lấy được luồng video", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ReplayLinkResponse> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}