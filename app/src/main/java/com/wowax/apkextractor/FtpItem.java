package com.wowax.apkextractor;

public class FtpItem {
    public final String name;
    public final boolean isDirectory;
    public final boolean isParent;
    public final long size;
    public final String path; // absoluter Remote-Pfad (null für "..")

    public FtpItem(String name, boolean isDirectory, boolean isParent, long size, String path) {
        this.name = name;
        this.isDirectory = isDirectory;
        this.isParent = isParent;
        this.size = size;
        this.path = path;
    }
}
