package com.thangoghd.thapcamtv.providers;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.webkit.URLUtil;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProgramImageProvider extends ContentProvider {
    private static final String TAG = "ProgramImageProvider";
    private static final String AUTHORITY = "com.thangoghd.thapcamtv.programimage";
    private ExecutorService executorService;
    
    @Override
    public boolean onCreate() {
        executorService = Executors.newSingleThreadExecutor();
        return true;
    }
    
    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) {
        try {
            String imageUrl = uri.getQueryParameter("url");
            if (imageUrl == null || !URLUtil.isValidUrl(imageUrl)) {
                return null;
            }
            
            // Create cache directory if it doesn't exist
            File cacheDir = new File(getContext().getCacheDir(), "program_images");
            if (!cacheDir.exists()) {
                cacheDir.mkdirs();
            }
            
            // Create a unique filename based on the URL
            String filename = String.valueOf(imageUrl.hashCode()) + ".webp";
            File outputFile = new File(cacheDir, filename);
            
            // If file doesn't exist or is older than 1 hour, download and process it
            if (!outputFile.exists() || (System.currentTimeMillis() - outputFile.lastModified() > 3600000)) {
                // Download and process image
                Bitmap bitmap = downloadAndProcessImage(imageUrl);
                if (bitmap != null) {
                    // Save processed image
                    FileOutputStream fos = new FileOutputStream(outputFile);
                    bitmap.compress(Bitmap.CompressFormat.WEBP, 90, fos);
                    fos.close();
                    bitmap.recycle();
                }
            }
            
            // Return the processed image file
            return ParcelFileDescriptor.open(outputFile, ParcelFileDescriptor.MODE_READ_ONLY);
        } catch (Exception e) {
            Log.e(TAG, "Error opening file", e);
            return null;
        }
    }
    
    private Bitmap downloadAndProcessImage(String imageUrl) {
        try {
            // Download image
            URL url = new URL(imageUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            Bitmap originalBitmap = BitmapFactory.decodeStream(input);
            
            if (originalBitmap == null) {
                return null;
            }
            
            // Calculate dimensions for 16:9 ratio
            int targetWidth = 672;
            int targetHeight = (targetWidth * 9) / 16;
            
            // Create new bitmap with 16:9 ratio
            Bitmap processedBitmap = Bitmap.createScaledBitmap(originalBitmap, targetWidth, targetHeight, true);
            originalBitmap.recycle();
            
            return processedBitmap;
        } catch (IOException e) {
            Log.e(TAG, "Error downloading image", e);
            return null;
        }
    }
    
    // Required ContentProvider methods
    @Override
    public String getType(Uri uri) {
        return "image/webp";
    }
    
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("Not supported");
    }
    
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        throw new UnsupportedOperationException("Not supported");
    }
    
    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Not supported");
    }
    
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Not supported");
    }
    
    public static Uri buildImageUri(String imageUrl) {
        return new Uri.Builder()
            .scheme("content")
            .authority(AUTHORITY)
            .path("image")
            .appendQueryParameter("url", imageUrl)
            .build();
    }
}
