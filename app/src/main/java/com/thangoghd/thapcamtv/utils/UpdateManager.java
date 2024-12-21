package com.thangoghd.thapcamtv.utils;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import com.thangoghd.thapcamtv.api.RetrofitClient;
import com.thangoghd.thapcamtv.models.GitHubRelease;
import java.io.File;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UpdateManager {
    private static final String TAG = "UpdateManager";
    private static final String GITHUB_OWNER = "thangoghd";
    private static final String GITHUB_REPO = "ThapcamTV";
    
    private static final String PREF_NAME = "update_prefs";
    private static final String LAST_CHECK_TIME = "last_check_time";
    private static final long CHECK_INTERVAL = 24 * 60 * 60 * 1000;
    
    private Context context;
    private String currentVersion;
    
    public UpdateManager(Context context) {
        this.context = context;
        try {
            currentVersion = context.getPackageManager()
                .getPackageInfo(context.getPackageName(), 0)
                .versionName;
        } catch (Exception e) {
            Log.e(TAG, "Error getting current version", e);
        }
    }

    private boolean shouldCheckUpdate() {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        long lastCheckTime = prefs.getLong(LAST_CHECK_TIME, 0);
        long currentTime = System.currentTimeMillis();
        
        if (currentTime - lastCheckTime >= CHECK_INTERVAL) {
            prefs.edit().putLong(LAST_CHECK_TIME, currentTime).apply();
            return true;
        }
        return false;
    }
    
    public void checkForUpdate() {
        if (!shouldCheckUpdate()) {
            return;
        }

        RetrofitClient.getGitHubApiService()
            .getLatestRelease(GITHUB_OWNER, GITHUB_REPO)
            .enqueue(new Callback<GitHubRelease>() {
                @Override
                public void onResponse(Call<GitHubRelease> call, Response<GitHubRelease> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        GitHubRelease release = response.body();
                        String latestVersion = release.getTagName().replace("v", "");
                        
                        if (isUpdateAvailable(currentVersion, latestVersion)) {
                            showUpdateDialog(release);
                        }
                    }
                }
                
                @Override
                public void onFailure(Call<GitHubRelease> call, Throwable t) {
                    Log.e(TAG, "Error checking for updates", t);
                }
            });
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
            Log.e(TAG, "Error comparing versions", e);
            return false;
        }
    }
    
    private void showUpdateDialog(GitHubRelease release) {
        new AlertDialog.Builder(context)
            .setTitle("Có phiên bản mới")
            .setMessage("Phiên bản " + release.getTagName() + " đã sẵn sàng.\n\n" + 
                       "Thay đổi:\n" + release.getBody())
            .setPositiveButton("Cập nhật ngay", (dialog, which) -> {
                String downloadUrl = release.getDownloadUrl();
                if (downloadUrl != null) {
                    downloadUpdate(downloadUrl);
                }
            })
            .setNegativeButton("Để sau", null)
            .show();
    }
    
    private void downloadUpdate(String downloadUrl) {
        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(downloadUrl));
            request.setTitle("Đang tải bản cập nhật");
            request.setDescription("Đang tải ThapcamTV...");
            request.setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, 
                GitHubRelease.APK_FILENAME);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            
            DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            long downloadId = downloadManager.enqueue(request);
            
            BroadcastReceiver onComplete = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction())) {
                        long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                        if (id == downloadId) {
                            installUpdate();
                            context.unregisterReceiver(this);
                        }
                    }
                }
            };
            
            context.registerReceiver(onComplete, 
                new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
            
        } catch (Exception e) {
            Log.e(TAG, "Error downloading update", e);
        }
    }
    
    private void installUpdate() {
        try {
            File file = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), 
                GitHubRelease.APK_FILENAME);
            Uri apkUri = FileProvider.getUriForFile(context, 
                context.getPackageName() + ".provider", file);
            
            Intent install = new Intent(Intent.ACTION_VIEW);
            install.setDataAndType(apkUri, "application/vnd.android.package-archive");
            install.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(install);
            
        } catch (Exception e) {
            Log.e(TAG, "Error installing update", e);
        }
    }
}
