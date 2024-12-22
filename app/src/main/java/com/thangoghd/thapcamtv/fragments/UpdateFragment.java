package com.thangoghd.thapcamtv;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import com.thangoghd.thapcamtv.api.RetrofitClient;
import com.thangoghd.thapcamtv.models.GitHubRelease;
import com.thangoghd.thapcamtv.utils.UpdateManager;
import java.io.File;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UpdateFragment extends Fragment {
    private TextView tvVersion;
    private TextView tvChangelog;
    private Button btnLater;
    private Button btnUpdate;
    private View rootView;
    private GitHubRelease currentRelease;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_update, container, false);
        
        tvVersion = rootView.findViewById(R.id.tvVersion);
        tvChangelog = rootView.findViewById(R.id.tvChangelog);
        btnLater = rootView.findViewById(R.id.btnLater);
        btnUpdate = rootView.findViewById(R.id.btnUpdate);
        
        btnLater.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());
        btnUpdate.setOnClickListener(v -> downloadUpdate());
        
        checkForUpdate();
        
        return rootView;
    }
    
    private void checkForUpdate() {
        RetrofitClient.getGitHubApiService()
            .getLatestRelease("thangoghd", "ThapcamTV")
            .enqueue(new Callback<GitHubRelease>() {
                @Override
                public void onResponse(Call<GitHubRelease> call, Response<GitHubRelease> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        currentRelease = response.body();
                        String currentVersion = getCurrentVersion();
                        String latestVersion = currentRelease.getTagName().replace("v", "");
                        
                        if (isUpdateAvailable(currentVersion, latestVersion)) {
                            showUpdateInfo();
                        } else {
                            requireActivity().getSupportFragmentManager().popBackStack();
                        }
                    }
                }
                
                @Override
                public void onFailure(Call<GitHubRelease> call, Throwable t) {
                    requireActivity().getSupportFragmentManager().popBackStack();
                }
            });
    }
    
    private void showUpdateInfo() {
        tvVersion.setText("Phiên bản " + currentRelease.getTagName());
        tvChangelog.setText(currentRelease.getBody());
        btnUpdate.requestFocus();
    }
    
    private String getCurrentVersion() {
        try {
            return requireContext().getPackageManager()
                .getPackageInfo(requireContext().getPackageName(), 0)
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
    
    private void downloadUpdate() {
        try {
            String downloadUrl = currentRelease.getDownloadUrl();
            if (downloadUrl == null) return;
            
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(downloadUrl));
            request.setTitle("Đang tải bản cập nhật");
            request.setDescription("Đang tải ThapcamTV...");
            request.setDestinationInExternalFilesDir(requireContext(), 
                Environment.DIRECTORY_DOWNLOADS, GitHubRelease.APK_FILENAME);
            request.setNotificationVisibility(
                DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            
            DownloadManager downloadManager = (DownloadManager) 
                requireContext().getSystemService(Context.DOWNLOAD_SERVICE);
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
            
            requireContext().registerReceiver(onComplete, 
                new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void installUpdate() {
        try {
            File file = new File(requireContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), 
                GitHubRelease.APK_FILENAME);
            Uri apkUri = FileProvider.getUriForFile(requireContext(), 
                requireContext().getPackageName() + ".provider", file);
            
            Intent install = new Intent(Intent.ACTION_VIEW);
            install.setDataAndType(apkUri, "application/vnd.android.package-archive");
            install.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(install);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
