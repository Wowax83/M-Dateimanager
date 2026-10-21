package com.wowax.apkextractor;

import jcifs.smb.SmbFile;

public class SmbItem {
    public final String name;
    public final boolean isDirectory;
    public final boolean isParent;
    public final long size;
    public final SmbFile file; // null für den ".."-Pseudo-Eintrag

    public SmbItem(String name, boolean isDirectory, boolean isParent, long size, SmbFile file) {
        this.name = name;
        this.isDirectory = isDirectory;
        this.isParent = isParent;
        this.size = size;
        this.file = file;
    }
}
