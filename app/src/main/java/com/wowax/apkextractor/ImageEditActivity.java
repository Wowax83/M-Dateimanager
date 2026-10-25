package com.wowax.apkextractor;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/** Bildbearbeitung: Rotation + Zuschneiden, speichert das Ergebnis in-place. */
public class ImageEditActivity extends BaseActivity {

    private String path;
    private Bitmap bitmap;
    private ImageView imageView;
    private CropOverlayView cropView;
    private boolean cropMode = false;
    private TextView btnRotateLeft, btnRotateRight, btnCrop, btnSave;
    private ImageButton backButton;
    private FrameLayout.LayoutParams backLp;
    private TextView titleView;
    private FrameLayout.LayoutParams titleLp;
    private LinearLayout bottomBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        path = getIntent().getStringExtra("path");
        if (path == null) {
            finish();
            return;
        }
        bitmap = decodeSampled(path, 3000);
        if (bitmap == null) {
            finish();
            return;
        }

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        imageView = new ImageView(this);
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        root.addView(imageView, matchParent());

        cropView = new CropOverlayView(this);
        cropView.setVisibility(View.GONE);
        root.addView(cropView, matchParent());

        buildTopBar(root);
        buildBottomBar(root);
        applyInsets(root);

        setContentView(root);
        refreshImage();
    }

    private FrameLayout.LayoutParams matchParent() {
        return new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }

    private void buildTopBar(FrameLayout root) {
        backButton = new ImageButton(this);
        backButton.setImageResource(R.drawable.ic_back);
        backButton.setBackgroundResource(R.drawable.bg_back_button);
        backButton.setScaleType(ImageView.ScaleType.CENTER);
        backButton.setContentDescription(getString(R.string.back));
        backButton.setOnClickListener(v -> finish());
        backLp = new FrameLayout.LayoutParams(dp(40), dp(40));
        backLp.gravity = Gravity.TOP | Gravity.START;
        backLp.setMargins(dp(8), dp(8), 0, 0);
        root.addView(backButton, backLp);

        titleView = new TextView(this);
        titleView.setText(R.string.edit);
        titleView.setTextColor(Color.WHITE);
        titleView.setTextSize(18);
        titleLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        titleLp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        titleLp.topMargin = dp(16);
        root.addView(titleView, titleLp);
    }

    private void buildBottomBar(FrameLayout root) {
        bottomBar = new LinearLayout(this);
        bottomBar.setOrientation(LinearLayout.VERTICAL);
        bottomBar.setPadding(dp(8), dp(6), dp(8), dp(6));

        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        btnRotateLeft = makeButton("↺ Links");
        btnRotateRight = makeButton("↻ Rechts");
        btnRotateLeft.setOnClickListener(v -> rotate(-90));
        btnRotateRight.setOnClickListener(v -> rotate(90));
        row1.addView(btnRotateLeft, halfLp());
        row1.addView(btnRotateRight, halfLp());

        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        btnCrop = makeButton("Zuschneiden");
        btnSave = makeButton("Speichern");
        btnCrop.setOnClickListener(v -> toggleCrop());
        btnSave.setOnClickListener(v -> save());
        row2.addView(btnCrop, halfLp());
        row2.addView(btnSave, halfLp());

        bottomBar.addView(row1);
        bottomBar.addView(row2);

        FrameLayout.LayoutParams blp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        blp.gravity = Gravity.BOTTOM;
        root.addView(bottomBar, blp);
    }

    private LinearLayout.LayoutParams halfLp() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.setMargins(dp(4), dp(4), dp(4), dp(4));
        return lp;
    }

    private TextView makeButton(String label) {
        TextView t = new TextView(this);
        t.setText(label);
        t.setTextColor(Color.WHITE);
        t.setTextSize(17);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(12), dp(14), dp(12), dp(14));
        GradientDrawable g = new GradientDrawable();
        g.setColor(0x88000000);
        g.setCornerRadius(dp(12));
        t.setBackground(g);
        t.setClickable(true);
        return t;
    }

    private void applyInsets(FrameLayout root) {
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            if (backButton != null && backLp != null) {
                backLp.setMargins(dp(8), bars.top + dp(8), 0, 0);
                backButton.setLayoutParams(backLp);
            }
            if (titleView != null && titleLp != null) {
                titleLp.topMargin = bars.top + dp(16);
                titleView.setLayoutParams(titleLp);
            }
            if (bottomBar != null) {
                bottomBar.setPadding(dp(8), dp(8), dp(8), bars.bottom + dp(8));
            }
            return insets;
        });
    }

    private Bitmap decodeSampled(String path, int maxDim) {
        try {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, opts);
            int sample = 1;
            while (opts.outWidth / sample > maxDim || opts.outHeight / sample > maxDim) {
                sample *= 2;
            }
            opts.inJustDecodeBounds = false;
            opts.inSampleSize = sample;
            return BitmapFactory.decodeFile(path, opts);
        } catch (Exception e) {
            return null;
        }
    }

    private void refreshImage() {
        imageView.setImageBitmap(bitmap);
        imageView.post(this::syncImageBounds);
    }

    private void syncImageBounds() {
        cropView.setImageBounds(computeDisplayRect());
    }

    private RectF computeDisplayRect() {
        float vw = imageView.getWidth();
        float vh = imageView.getHeight();
        if (bitmap == null || vw <= 0 || vh <= 0) return new RectF(0, 0, vw, vh);
        float bw = bitmap.getWidth();
        float bh = bitmap.getHeight();
        float scale = Math.min(vw / bw, vh / bh);
        float dw = bw * scale;
        float dh = bh * scale;
        float left = (vw - dw) / 2f;
        float top = (vh - dh) / 2f;
        return new RectF(left, top, left + dw, top + dh);
    }

    private void rotate(int degrees) {
        if (cropMode) return;
        Matrix m = new Matrix();
        m.postRotate(degrees);
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
        refreshImage();
    }

    private void toggleCrop() {
        if (cropMode) {
            applyCrop();
        } else {
            enterCropMode();
        }
    }

    private void enterCropMode() {
        cropMode = true;
        syncImageBounds();
        cropView.setVisibility(View.VISIBLE);
        btnCrop.setText("Übernehmen");
        btnRotateLeft.setEnabled(false);
        btnRotateRight.setEnabled(false);
        btnSave.setEnabled(false);
    }

    private void applyCrop() {
        RectF c = cropView.getCrop();
        RectF d = computeDisplayRect();
        if (d.width() > 0 && d.height() > 0) {
            float scale = (float) bitmap.getWidth() / d.width();
            int left = Math.round((c.left - d.left) * scale);
            int top = Math.round((c.top - d.top) * scale);
            int right = Math.round((c.right - d.left) * scale);
            int bottom = Math.round((c.bottom - d.top) * scale);
            left = Math.max(0, left);
            top = Math.max(0, top);
            right = Math.min(bitmap.getWidth(), right);
            bottom = Math.min(bitmap.getHeight(), bottom);
            if (right > left && bottom > top) {
                bitmap = Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top);
            }
        }
        exitCropMode();
        refreshImage();
    }

    private void exitCropMode() {
        cropMode = false;
        cropView.setVisibility(View.GONE);
        btnCrop.setText("Zuschneiden");
        btnRotateLeft.setEnabled(true);
        btnRotateRight.setEnabled(true);
        btnSave.setEnabled(true);
    }

    private void save() {
        try {
            saveBitmap(bitmap, new File(path));
            setResult(RESULT_OK);
            finish();
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.error) + ": " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void saveBitmap(Bitmap bmp, File file) throws IOException {
        String n = file.getName().toLowerCase();
        Bitmap.CompressFormat fmt = Bitmap.CompressFormat.JPEG;
        if (n.endsWith(".png")) fmt = Bitmap.CompressFormat.PNG;
        else if (n.endsWith(".webp")) fmt = Bitmap.CompressFormat.WEBP;
        FileOutputStream out = new FileOutputStream(file);
        bmp.compress(fmt, 95, out);
        out.flush();
        out.close();
    }
}
