package com.wowax.apkextractor;

import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Erstellt ZIP-Archive aus Dateien und Ordnern (rekursiv). */
public class ArchiveCreator {

    public interface Callback {
        void onSuccess(String path);
        void onError(String message);
    }

    public static void zip(List<File> sources, File destZip, Callback cb) {
        Handler main = new Handler(Looper.getMainLooper());
        new Thread(() -> {
            try {
                try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(destZip))) {
                    for (File src : sources) {
                        if (src.isDirectory()) {
                            addDir(zos, src, src.getName() + "/");
                        } else {
                            addFile(zos, src, src.getName());
                        }
                    }
                }
                final String p = destZip.getAbsolutePath();
                main.post(() -> cb.onSuccess(p));
            } catch (Exception e) {
                final String m = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
                main.post(() -> cb.onError(m));
            }
        }).start();
    }

    private static void addDir(ZipOutputStream zos, File dir, String prefix) throws IOException {
        zos.putNextEntry(new ZipEntry(prefix));
        zos.closeEntry();
        File[] children = dir.listFiles();
        if (children == null) return;
        for (File c : children) {
            String path = prefix + c.getName();
            if (c.isDirectory()) {
                addDir(zos, c, path + "/");
            } else {
                addFile(zos, c, path);
            }
        }
    }

    private static void addFile(ZipOutputStream zos, File f, String entryName) throws IOException {
        zos.putNextEntry(new ZipEntry(entryName));
        try (InputStream in = new FileInputStream(f)) {
            byte[] buf = new byte[64 * 1024];
            int n;
            while ((n = in.read(buf)) > 0) zos.write(buf, 0, n);
        }
        zos.closeEntry();
    }
}
