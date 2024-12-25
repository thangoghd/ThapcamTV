package com.thangoghd.thapcamtv.fragments;

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

import com.thangoghd.thapcamtv.R;
import com.thangoghd.thapcamtv.api.RetrofitClient;
import com.thangoghd.thapcamtv.models.GitHubRelease;

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
    private BroadcastReceiver downloadReceiver;
    private static final int REQUEST_INSTALL_PERMISSION = 1001;
    private String pendingDownloadUrl = null;

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
    
    private String formatFileSize(long size) {
        if (size <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return String.format("%.1f %s", size / Math.pow(1024, digitGroups), units[digitGroups]);
    }

    private String formatSpeed(long bytesPerSecond) {
        return formatFileSize(bytesPerSecond) + "/s";
    }

    private void checkInstallPermissionAndDownload() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            android.content.pm.PackageManager pm = requireContext().getPackageManager();
            boolean hasPermission = pm.canRequestPackageInstalls();
            if (!hasPermission) {
                // Save URL to download after getting permission
                pendingDownloadUrl = currentRelease.getDownloadUrl();
                
                // Show dialog to explain
                new android.app.AlertDialog.Builder(requireContext())
                    .setTitle("Cần cấp quyền")
                    .setMessage("Ứng dụng cần quyền cài đặt từ nguồn không xác định để có thể tự động cập nhật. Nhấn \"Đồng ý\" để mở cài đặt và cấp quyền.")
                    .setPositiveButton("Đồng ý", (dialog, which) -> {
                        try {
                            Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
                            intent.setData(Uri.parse("package:" + requireContext().getPackageName()));
                            startActivityForResult(intent, REQUEST_INSTALL_PERMISSION);
                        } catch (Exception e) {
                            e.printStackTrace();
                            android.widget.Toast.makeText(requireContext(), "Không thể mở cài đặt. Vui lòng cấp quyền thủ công.", android.widget.Toast.LENGTH_LONG).show();
                        }
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
                return;
            }
        }
        // If permission is already granted or Android version < O, start downloading
        startDownload(currentRelease.getDownloadUrl());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_INSTALL_PERMISSION) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                android.content.pm.PackageManager pm = requireContext().getPackageManager();
                if (pm.canRequestPackageInstalls() && pendingDownloadUrl != null) {
                    // Start download
                    startDownload(pendingDownloadUrl);
                    pendingDownloadUrl = null;
                }
            }
        }
    }

    private void downloadUpdate() {
        try {
            String downloadUrl = currentRelease.getDownloadUrl();
            if (downloadUrl == null) return;
            
            // Check permission before downloading
            checkInstallPermissionAndDownload();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startDownload(String downloadUrl) {
        try {
            // Show progress dialog
            android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(requireContext());
            progressDialog.setTitle("Đang tải bản cập nhật");
            progressDialog.setMessage("Đang chuẩn bị...");
            progressDialog.setProgressStyle(android.app.ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.setProgress(0);
            progressDialog.setCancelable(false);
            progressDialog.show();
            
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
            
            // Create a thread to track download progress
            new Thread(() -> {
                boolean downloading = true;
                long lastBytes = 0;
                long lastTime = System.currentTimeMillis();
                
                while (downloading) {
                    DownloadManager.Query q = new DownloadManager.Query();
                    q.setFilterById(downloadId);
                    android.database.Cursor cursor = downloadManager.query(q);
                    if (cursor.moveToFirst()) {
                        int bytesDownloadedColumn = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR);
                        int bytesTotalColumn = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES);
                        int statusColumn = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                        
                        // Check if columns exist
                        if (bytesDownloadedColumn < 0 || bytesTotalColumn < 0 || statusColumn < 0) {
                            continue;
                        }
                        
                        int bytesDownloaded = cursor.getInt(bytesDownloadedColumn);
                        int bytesTotal = cursor.getInt(bytesTotalColumn);
                        
                        // Calculate speed
                        long currentTime = System.currentTimeMillis();
                        long timeDiff = currentTime - lastTime;
                        long bytesDiff = bytesDownloaded - lastBytes;
                        long speed = (timeDiff > 0) ? (bytesDiff * 1000 / timeDiff) : 0;
                        
                        // Update last values
                        lastBytes = bytesDownloaded;
                        lastTime = currentTime;
                        
                        if (bytesTotal > 0) {
                            final int progress = (bytesDownloaded * 100) / bytesTotal;
                            final String status = String.format("%s / %s • %s", 
                                formatFileSize(bytesDownloaded),
                                formatFileSize(bytesTotal),
                                formatSpeed(speed));
                            
                            requireActivity().runOnUiThread(() -> {
                                progressDialog.setProgress(progress);
                                progressDialog.setMessage(status);
                            });
                        }
                        
                        if (cursor.getInt(statusColumn) == DownloadManager.STATUS_SUCCESSFUL) {
                            downloading = false;
                        }
                    }
                    cursor.close();
                    try {
                        Thread.sleep(200);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                requireActivity().runOnUiThread(() -> {
                    progressDialog.dismiss();
                });
            }).start();
            
            downloadReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction())) {
                        long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                        if (id == downloadId) {
                            installUpdate();
                            if (context != null) {
                                try {
                                    context.unregisterReceiver(this);
                                    downloadReceiver = null;
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                }
            };
            
            requireContext().registerReceiver(downloadReceiver, 
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (downloadReceiver != null) {
            try {
                requireContext().unregisterReceiver(downloadReceiver);
                downloadReceiver = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
