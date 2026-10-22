package com.wowax.apkextractor;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class DashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);
        InsetsHelper.applySystemBars(this);

        findViewById(R.id.card_extractor).setOnClickListener(v ->
                startActivity(new Intent(this, MainActivity.class)));

        findViewById(R.id.card_files).setOnClickListener(v ->
                startActivity(new Intent(this, FileBrowserActivity.class)));

        findViewById(R.id.card_network).setOnClickListener(v ->
                startActivity(new Intent(this, NetworkActivity.class)));

        loadStorageInfo();
    }

    private void loadStorageInfo() {
        ProgressBar bar = findViewById(R.id.storage_bar);
        TextView usedText = findViewById(R.id.storage_used);
        TextView freeText = findViewById(R.id.storage_free);

        try {
            StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
            long total = stat.getTotalBytes();
            long free = stat.getAvailableBytes();
            long used = total - free;
            int percent = total > 0 ? (int) (used * 100 / total) : 0;

            bar.setMax(100);
            bar.setProgress(percent);
            usedText.setText(percent + "% verwendet");
            freeText.setText(formatSize(free) + " frei von " + formatSize(total));
        } catch (Exception e) {
            usedText.setText("—");
            freeText.setText("Speicher nicht verfügbar");
        }
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        double kb = bytes / 1024.0;
        if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb);
        double mb = kb / 1024.0;
        if (mb < 1024) return String.format(Locale.US, "%.1f MB", mb);
        return String.format(Locale.US, "%.2f GB", mb / 1024.0);
    }
}
