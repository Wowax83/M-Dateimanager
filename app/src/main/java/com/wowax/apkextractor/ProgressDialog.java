package com.wowax.apkextractor;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.concurrent.atomic.AtomicBoolean;

/** Fortschrittsdialog mit Abbruch-Button für Batch-Operationen. Thread-sicher. */
public class ProgressDialog {

    private final Handler main = new Handler(Looper.getMainLooper());
    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    private final AlertDialog dialog;
    private final ProgressBar bar;
    private final TextView text;

    public ProgressDialog(Activity a, String title, int max) {
        bar = new ProgressBar(a, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(Math.max(1, max));
        text = new TextView(a);
        text.setTextSize(13);
        LinearLayout layout = new LinearLayout(a);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * a.getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad / 2, pad, 0);
        layout.addView(text);
        layout.addView(bar);
        dialog = new MaterialAlertDialogBuilder(a)
                .setTitle(title)
                .setView(layout)
                .setNegativeButton(a.getString(R.string.cancel), (d, w) -> cancelled.set(true))
                .setCancelable(false)
                .create();
    }

    public void show() {
        dialog.show();
    }

    public void update(int progress, String message) {
        main.post(() -> {
            bar.setProgress(progress);
            text.setText(message);
        });
    }

    public boolean isCancelled() {
        return cancelled.get();
    }

    public void dismiss() {
        main.post(dialog::dismiss);
    }
}
