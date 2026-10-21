package com.wowax.apkextractor;

import java.io.File;

public class FileItem {
    public final File file;
    public final String name;
    public final boolean isDirectory;
    public final long size;
    public final long lastModified;

    public FileItem(File f) {
        this.file = f;
        this.name = f.getName();
        this.isDirectory = f.isDirectory();
        this.size = f.length();
        this.lastModified = f.lastModified();
    }

    public boolean isApk() {
        return !isDirectory && name.toLowerCase().endsWith(".apk");
    }
}
