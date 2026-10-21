package com.wowax.apkextractor;

import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ArchiveExtractor {

    public interface Callback {
        void onSuccess(String outDir);
        void onError(String message);
    }

    public static void extract(File archive, Callback callback) {
        Handler main = new Handler(Looper.getMainLooper());
        new Thread(() -> {
            try {
                String base = archive.getName();
                int dot = base.lastIndexOf('.');
                String stem = dot > 0 ? base.substring(0, dot) : base;
                File destDir = new File(archive.getParentFile(), stem);
                if (!destDir.exists() && !destDir.mkdirs()) {
                    throw new Exception("Zielordner konnte nicht erstellt werden");
                }
                String destCanonical = destDir.getCanonicalPath();

                try (ZipFile zip = new ZipFile(archive)) {
                    Enumeration<? extends ZipEntry> entries = zip.entries();
                    while (entries.hasMoreElements()) {
                        ZipEntry e = entries.nextElement();
                        File out = new File(destDir, e.getName());
                        String outCanonical = out.getCanonicalPath();
                        if (!outCanonical.equals(destCanonical)
                                && !outCanonical.startsWith(destCanonical + File.separator)) {
                            continue; // zip-slip guard
                        }
                        if (e.isDirectory()) {
                            out.mkdirs();
                            continue;
                        }
                        File parent = out.getParentFile();
                        if (parent != null && !parent.exists()) parent.mkdirs();
                        try (InputStream in = zip.getInputStream(e);
                             OutputStream os = new FileOutputStream(out)) {
                            byte[] buf = new byte[64 * 1024];
                            int n;
                            while ((n = in.read(buf)) > 0) os.write(buf, 0, n);
                        }
                    }
                }
                final String result = destDir.getAbsolutePath();
                main.post(() -> callback.onSuccess(result));
            } catch (Exception ex) {
                String m = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
                main.post(() -> callback.onError(m));
            }
        }).start();
    }
}
