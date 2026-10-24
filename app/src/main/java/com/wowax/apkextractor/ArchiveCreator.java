package com.wowax.apkextractor;

import android.os.Handler;
import android.os.Looper;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Erstellt ZIP-, TAR- und TAR.GZ-Archive aus Dateien und Ordnern (rekursiv). */
public class ArchiveCreator {

    public interface Callback {
        void onSuccess(String path);
        void onError(String message);
    }

    public static void create(List<File> sources, File dest, Callback cb) {
        String name = dest.getName().toLowerCase();
        if (name.endsWith(".tar.gz") || name.endsWith(".tgz")) {
            createTarGz(sources, dest, cb);
        } else if (name.endsWith(".tar")) {
            createTar(sources, dest, cb);
        } else {
            createZip(sources, dest, cb);
        }
    }

    private static void createZip(List<File> sources, File destZip, Callback cb) {
        Handler main = new Handler(Looper.getMainLooper());
        new Thread(() -> {
            try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(destZip))) {
                for (File src : sources) {
                    if (src.isDirectory()) {
                        addDirToZip(zos, src, src.getName() + "/");
                    } else {
                        addFileToZip(zos, src, src.getName());
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

    private static void createTar(List<File> sources, File dest, Callback cb) {
        Handler main = new Handler(Looper.getMainLooper());
        new Thread(() -> {
            try (TarArchiveOutputStream tout = new TarArchiveOutputStream(new FileOutputStream(dest))) {
                for (File src : sources) {
                    addToTar(tout, src, src.getName());
                }
                final String p = dest.getAbsolutePath();
                main.post(() -> cb.onSuccess(p));
            } catch (Exception e) {
                final String m = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
                main.post(() -> cb.onError(m));
            }
        }).start();
    }

    private static void createTarGz(List<File> sources, File dest, Callback cb) {
        Handler main = new Handler(Looper.getMainLooper());
        new Thread(() -> {
            try (FileOutputStream fos = new FileOutputStream(dest);
                 GzipCompressorOutputStream gos = new GzipCompressorOutputStream(fos);
                 TarArchiveOutputStream tout = new TarArchiveOutputStream(gos)) {
                for (File src : sources) {
                    addToTar(tout, src, src.getName());
                }
                final String p = dest.getAbsolutePath();
                main.post(() -> cb.onSuccess(p));
            } catch (Exception e) {
                final String m = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
                main.post(() -> cb.onError(m));
            }
        }).start();
    }

    private static void addDirToZip(ZipOutputStream zos, File dir, String prefix) throws IOException {
        zos.putNextEntry(new ZipEntry(prefix));
        zos.closeEntry();
        File[] children = dir.listFiles();
        if (children == null) return;
        for (File c : children) {
            String path = prefix + c.getName();
            if (c.isDirectory()) {
                addDirToZip(zos, c, path + "/");
            } else {
                addFileToZip(zos, c, path);
            }
        }
    }

    private static void addFileToZip(ZipOutputStream zos, File f, String entryName) throws IOException {
        zos.putNextEntry(new ZipEntry(entryName));
        try (InputStream in = new FileInputStream(f)) {
            pipe(in, zos);
        }
        zos.closeEntry();
    }

    private static void addToTar(TarArchiveOutputStream tout, File f, String prefix) throws IOException {
        if (f.isDirectory()) {
            TarArchiveEntry e = new TarArchiveEntry(f, prefix + "/");
            tout.putArchiveEntry(e);
            tout.closeArchiveEntry();
            File[] children = f.listFiles();
            if (children != null) {
                for (File c : children) {
                    addToTar(tout, c, prefix + "/" + c.getName());
                }
            }
        } else {
            TarArchiveEntry e = new TarArchiveEntry(f, prefix);
            tout.putArchiveEntry(e);
            try (InputStream in = new FileInputStream(f)) {
                pipe(in, tout);
            }
            tout.closeArchiveEntry();
        }
    }

    private static void pipe(InputStream in, OutputStream os) throws IOException {
        byte[] buf = new byte[64 * 1024];
        int n;
        while ((n = in.read(buf)) > 0) os.write(buf, 0, n);
    }
}
