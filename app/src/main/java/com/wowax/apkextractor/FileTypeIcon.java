package com.wowax.apkextractor;

import java.util.Locale;

public class FileTypeIcon {

    public static int iconFor(String name) {
        String n = name.toLowerCase(Locale.US);
        if (n.endsWith(".pdf")) return R.drawable.ic_type_pdf;
        if (n.endsWith(".apk")) return R.drawable.ic_type_apk;
        if (n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".png") || n.endsWith(".gif")
                || n.endsWith(".webp") || n.endsWith(".bmp") || n.endsWith(".svg") || n.endsWith(".heic")) {
            return R.drawable.ic_type_image;
        }
        if (n.endsWith(".mp4") || n.endsWith(".mkv") || n.endsWith(".avi") || n.endsWith(".mov")
                || n.endsWith(".webm") || n.endsWith(".flv") || n.endsWith(".m4v") || n.endsWith(".ts")) {
            return R.drawable.ic_type_video;
        }
        if (n.endsWith(".mp3") || n.endsWith(".wav") || n.endsWith(".flac") || n.endsWith(".aac")
                || n.endsWith(".ogg") || n.endsWith(".m4a") || n.endsWith(".opus")) {
            return R.drawable.ic_type_audio;
        }
        if (n.endsWith(".zip") || n.endsWith(".rar") || n.endsWith(".7z") || n.endsWith(".tar")
                || n.endsWith(".gz") || n.endsWith(".bz2") || n.endsWith(".xz")) {
            return R.drawable.ic_type_archive;
        }
        if (n.endsWith(".doc") || n.endsWith(".docx") || n.endsWith(".txt") || n.endsWith(".md")
                || n.endsWith(".rtf") || n.endsWith(".odt") || n.endsWith(".xls") || n.endsWith(".xlsx")
                || n.endsWith(".ppt") || n.endsWith(".pptx") || n.endsWith(".csv")) {
            return R.drawable.ic_type_doc;
        }
        return R.drawable.ic_type_file;
    }
}
