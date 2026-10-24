package com.wowax.apkextractor;

import android.os.Handler;
import android.os.Looper;

import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Entpackt ZIP, 7z, TAR, TAR.GZ und GZ-Archive. */
public class ArchiveExtractor {

    public interface Callback {
        void onSuccess(String outDir);
        void onError(String message);
    }

    public static boolean isSupported(String name) {
        String l = name.toLowerCase();
        return l.endsWith(".zip") || l.endsWith(".7z") || l.endsWith(".tar")
                || l.endsWith(".tar.gz") || l.endsWith(".tgz") || l.endsWith(".gz");
    }

    public static void extract(File archive, Callback callback) {
        Handler main = new Handler(Looper.getMainLooper());
        new Thread(() -> {
            try {
                File destDir = destDirFor(archive);
                if (!destDir.exists() && !destDir.mkdirs()) {
                    throw new Exception("Zielordner konnte nicht erstellt werden");
                }
                String name = archive.getName().toLowerCase();
                if (name.endsWith(".zip")) extractZip(archive, destDir);
                else if (name.endsWith(".7z")) extract7z(archive, destDir);
                else if (name.endsWith(".tar.gz") || name.endsWith(".tgz")) extractTarGz(archive, destDir);
                else if (name.endsWith(".tar")) extractTar(archive, destDir);
                else if (name.endsWith(".gz")) extractGz(archive, destDir);
                else throw new Exception("Unbekanntes Archivformat");

                final String result = destDir.getAbsolutePath();
                main.post(() -> callback.onSuccess(result));
            } catch (Exception ex) {
                final String m = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
                main.post(() -> callback.onError(m));
            }
        }).start();
    }

    private static File destDirFor(File archive) {
        String n = archive.getName();
        String l = n.toLowerCase();
        String stem;
        if (l.endsWith(".tar.gz")) stem = n.substring(0, n.length() - 7);
        else if (l.endsWith(".tgz")) stem = n.substring(0, n.length() - 4);
        else {
            int dot = n.lastIndexOf('.');
            stem = dot > 0 ? n.substring(0, dot) : n;
        }
        return new File(archive.getParentFile(), stem);
    }

    private static void extractZip(File archive, File destDir) throws Exception {
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
                    pipe(in, os);
                }
            }
        }
    }

    private static void extractTar(File archive, File destDir) throws Exception {
        try (TarArchiveInputStream tin = new TarArchiveInputStream(new FileInputStream(archive))) {
            TarArchiveEntry e;
            while ((e = tin.getNextTarEntry()) != null) {
                File out = new File(destDir, e.getName());
                if (!isWithin(destDir, out)) continue;
                if (e.isDirectory()) {
                    out.mkdirs();
                    continue;
                }
                File parent = out.getParentFile();
                if (parent != null && !parent.exists()) parent.mkdirs();
                try (OutputStream os = new FileOutputStream(out)) {
                    pipe(tin, os);
                }
            }
        }
    }

    private static void extractTarGz(File archive, File destDir) throws Exception {
        try (InputStream fin = new FileInputStream(archive);
             GzipCompressorInputStream gin = new GzipCompressorInputStream(fin);
             TarArchiveInputStream tin = new TarArchiveInputStream(gin)) {
            TarArchiveEntry e;
            while ((e = tin.getNextTarEntry()) != null) {
                File out = new File(destDir, e.getName());
                if (!isWithin(destDir, out)) continue;
                if (e.isDirectory()) {
                    out.mkdirs();
                    continue;
                }
                File parent = out.getParentFile();
                if (parent != null && !parent.exists()) parent.mkdirs();
                try (OutputStream os = new FileOutputStream(out)) {
                    pipe(tin, os);
                }
            }
        }
    }

    private static void extractGz(File archive, File destDir) throws Exception {
        String n = archive.getName();
        String stem = n.toLowerCase().endsWith(".gz") ? n.substring(0, n.length() - 3) : n + ".out";
        File out = new File(destDir, stem);
        try (GzipCompressorInputStream gin = new GzipCompressorInputStream(new FileInputStream(archive));
             OutputStream os = new FileOutputStream(out)) {
            pipe(gin, os);
        }
    }

    private static void extract7z(File archive, File destDir) throws Exception {
        try (SevenZFile sz = new SevenZFile(archive)) {
            SevenZArchiveEntry e;
            while ((e = sz.getNextEntry()) != null) {
                String name = e.getName();
                if (name == null) continue;
                File out = new File(destDir, name);
                if (!isWithin(destDir, out)) continue;
                if (e.isDirectory()) {
                    out.mkdirs();
                    continue;
                }
                File parent = out.getParentFile();
                if (parent != null && !parent.exists()) parent.mkdirs();
                try (OutputStream os = new FileOutputStream(out)) {
                    byte[] buf = new byte[64 * 1024];
                    int n;
                    while ((n = sz.read(buf)) > 0) os.write(buf, 0, n);
                }
            }
        }
    }

    private static boolean isWithin(File base, File f) throws IOException {
        String bc = base.getCanonicalPath();
        String fc = f.getCanonicalPath();
        return fc.equals(bc) || fc.startsWith(bc + File.separator);
    }

    private static void pipe(InputStream in, OutputStream os) throws IOException {
        byte[] buf = new byte[64 * 1024];
        int n;
        while ((n = in.read(buf)) > 0) os.write(buf, 0, n);
    }
}
