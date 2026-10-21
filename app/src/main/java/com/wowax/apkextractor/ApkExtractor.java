package com.wowax.apkextractor;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class ApkExtractor {

    public interface Callback {
        void onSuccess(ExtractResult result);
        void onError(String message);
    }

    private static final String APK_MIME = "application/vnd.android.package-archive";
    private static final String SUBDIR = "APK Extractor";

    private final Context context;
    private final AppInfo app;
    private final Callback callback;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public ApkExtractor(Context context, AppInfo app, Callback callback) {
        this.context = context;
        this.app = app;
        this.callback = callback;
    }

    public void start() {
        new Thread(() -> {
            try {
                ExtractResult result = run();
                mainHandler.post(() -> callback.onSuccess(result));
            } catch (Exception e) {
                String msg = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
                mainHandler.post(() -> callback.onError(msg));
            }
        }).start();
    }

    private ExtractResult run() throws Exception {
        boolean hasSplits = app.splitSourceDirs != null && app.splitSourceDirs.length > 0;
        String baseName = sanitize(app.appName) + "_" + sanitize(app.versionName == null ? "1.0" : app.versionName);

        ExtractResult result = new ExtractResult();
        result.isSplit = hasSplits;

        if (Build.VERSION.SDK_INT >= 29) {
            // Scoped Storage -> MediaStore Downloads
            String relPath = Environment.DIRECTORY_DOWNLOADS + "/" + SUBDIR;
            if (hasSplits) {
                relPath += "/" + baseName;
            }
            Uri baseUri = saveViaMediaStore(new File(app.sourceDir), baseName + ".apk", relPath);
            result.shareUri = baseUri;
            result.outputPath = relPath + "/" + baseName + ".apk";

            if (hasSplits) {
                for (String split : app.splitSourceDirs) {
                    File sf = new File(split);
                    if (!sf.exists()) continue;
                    saveViaMediaStore(sf, sf.getName(), relPath);
                }
            }
            result.displayName = baseName + ".apk";
        } else {
            // Legacy: direkter Dateizugriff auf /Download/
            File baseDir = new File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    SUBDIR);
            if (hasSplits) {
                baseDir = new File(baseDir, baseName);
            }
            if (!baseDir.exists() && !baseDir.mkdirs()) {
                throw new Exception("Ordner konnte nicht erstellt werden");
            }

            File outFile = new File(baseDir, baseName + ".apk");
            copy(new File(app.sourceDir), outFile);
            result.shareUri = FileProvider.getUriForFile(context,
                    context.getPackageName() + ".fileprovider", outFile);
            result.outputPath = outFile.getAbsolutePath();
            result.displayName = baseName + ".apk";

            if (hasSplits) {
                for (String split : app.splitSourceDirs) {
                    File sf = new File(split);
                    if (!sf.exists()) continue;
                    copy(sf, new File(baseDir, sf.getName()));
                }
            }
        }
        return result;
    }

    private Uri saveViaMediaStore(File src, String displayName, String relPath) throws Exception {
        ContentResolver resolver = context.getContentResolver();
        ContentValues values = new ContentValues();
        values.put(MediaStore.Downloads.DISPLAY_NAME, displayName);
        values.put(MediaStore.Downloads.MIME_TYPE, APK_MIME);
        values.put(MediaStore.Downloads.RELATIVE_PATH, relPath);

        Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
        if (uri == null) throw new Exception("MediaStore Einfuegung fehlgeschlagen");

        try (OutputStream os = resolver.openOutputStream(uri);
             InputStream is = new FileInputStream(src)) {
            if (os == null) throw new Exception("Output-Stream konnte nicht geoeffnet werden");
            pipe(is, os);
        }
        return uri;
    }

    private void copy(File src, File dst) throws Exception {
        try (InputStream is = new FileInputStream(src);
             OutputStream os = new FileOutputStream(dst)) {
            pipe(is, os);
        }
    }

    private void pipe(InputStream is, OutputStream os) throws Exception {
        byte[] buf = new byte[64 * 1024];
        int n;
        while ((n = is.read(buf)) > 0) {
            os.write(buf, 0, n);
        }
        os.flush();
    }

    private String sanitize(String s) {
        if (s == null) return "";
        return s.replaceAll("[^a-zA-Z0-9._-]", "_").replaceAll("_+", "_");
    }
}
