package com.wowax.apkextractor;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.pdf.PdfRenderer;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
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

import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle;

import java.io.File;
import java.io.IOException;

/** PDF-Bearbeitung: Seite drehen + beschneiden, speichert als echtes PDF (Text bleibt). */
public class PdfEditActivity extends BaseActivity {

    private File file;
    private int pageIndex;
    private int pageCount;

    private float mediaW, mediaH;
    private int origRotation;
    private int rotation;          // aktueller Gesamt-Rotationswert (0/90/180/270)
    private PDRectangle cropBox;   // Zuschnitt in Medien-Koordinaten (Punkte, Ursprung unten-links), oder null

    private PdfRenderer renderer;
    private ParcelFileDescriptor pfd;

    private Bitmap displayBitmap;
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
        String path = getIntent().getStringExtra("path");
        pageIndex = getIntent().getIntExtra("page", 0);
        if (path == null) {
            finish();
            return;
        }
        file = new File(path);

        // Metadaten via PDFBox lesen
        try {
            PDFBoxResourceLoader.init(getApplicationContext());
            PDDocument doc = PDDocument.load(file);
            try {
                pageCount = doc.getNumberOfPages();
                if (pageIndex < 0 || pageIndex >= pageCount) pageIndex = 0;
                PDPage page = doc.getPage(pageIndex);
                PDRectangle mb = page.getMediaBox();
                mediaW = mb.getWidth();
                mediaH = mb.getHeight();
                origRotation = page.getRotation();
            } finally {
                doc.close();
            }
        } catch (Exception e) {
            Toast.makeText(this, R.string.error, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        rotation = origRotation;

        // Renderer öffnen
        try {
            pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
            renderer = new PdfRenderer(pfd);
        } catch (Exception e) {
            Toast.makeText(this, R.string.error, Toast.LENGTH_SHORT).show();
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

    // Rendert die Seite (ursprüngliche Rotation) als Bitmap.
    private Bitmap renderPage(int maxDim) {
        try {
            PdfRenderer.Page page = renderer.openPage(pageIndex);
            int pw = page.getWidth();
            int ph = page.getHeight();
            float scale = 1f;
            if (pw > maxDim || ph > maxDim) {
                scale = Math.min((float) maxDim / pw, (float) maxDim / ph);
            }
            int w = Math.max(1, (int) (pw * scale));
            int h = Math.max(1, (int) (ph * scale));
            Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            bmp.eraseColor(Color.WHITE);
            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
            page.close();
            return bmp;
        } catch (Exception e) {
            return null;
        }
    }

    private void refreshImage() {
        Bitmap base = renderPage(2000);
        if (base == null) {
            finish();
            return;
        }
        int added = ((rotation - origRotation) % 360 + 360) % 360;
        if (added != 0) {
            Matrix m = new Matrix();
            m.postRotate(added);
            base = Bitmap.createBitmap(base, 0, 0, base.getWidth(), base.getHeight(), m, true);
        }
        displayBitmap = base;
        imageView.setImageBitmap(displayBitmap);
        imageView.post(this::syncImageBounds);
    }

    private void syncImageBounds() {
        cropView.setImageBounds(computeDisplayRect());
    }

    private RectF computeDisplayRect() {
        float vw = imageView.getWidth();
        float vh = imageView.getHeight();
        if (displayBitmap == null || vw <= 0 || vh <= 0) return new RectF(0, 0, vw, vh);
        float bw = displayBitmap.getWidth();
        float bh = displayBitmap.getHeight();
        float scale = Math.min(vw / bw, vh / bh);
        float dw = bw * scale;
        float dh = bh * scale;
        float left = (vw - dw) / 2f;
        float top = (vh - dh) / 2f;
        return new RectF(left, top, left + dw, top + dh);
    }

    private void rotate(int degrees) {
        if (cropMode) return;
        rotation = ((rotation + degrees) % 360 + 360) % 360;
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
        RectF dr = computeDisplayRect();
        if (dr.width() > 0 && dr.height() > 0) {
            // Anzeige-Koordinaten (View) -> angezeigte Punkte
            float dispW = (rotation % 180 == 0) ? mediaW : mediaH;
            float dispH = (rotation % 180 == 0) ? mediaH : mediaW;
            float sx = dispW / dr.width();
            float sy = dispH / dr.height();
            float rx1 = (c.left - dr.left) * sx;
            float ry1 = (c.top - dr.top) * sy;
            float rx2 = (c.right - dr.left) * sx;
            float ry2 = (c.bottom - dr.top) * sy;
            cropBox = displayToMedia(new RectF(rx1, ry1, rx2, ry2));

            // Vorschau: Bitmap entsprechend zuschneiden (reine Anzeige)
            int bmpLeft = Math.round((c.left - dr.left) / dr.width() * displayBitmap.getWidth());
            int bmpTop = Math.round((c.top - dr.top) / dr.height() * displayBitmap.getHeight());
            int bmpRight = Math.round((c.right - dr.left) / dr.width() * displayBitmap.getWidth());
            int bmpBottom = Math.round((c.bottom - dr.top) / dr.height() * displayBitmap.getHeight());
            bmpLeft = Math.max(0, bmpLeft);
            bmpTop = Math.max(0, bmpTop);
            bmpRight = Math.min(displayBitmap.getWidth(), bmpRight);
            bmpBottom = Math.min(displayBitmap.getHeight(), bmpBottom);
            if (bmpRight > bmpLeft && bmpBottom > bmpTop) {
                displayBitmap = Bitmap.createBitmap(displayBitmap,
                        bmpLeft, bmpTop, bmpRight - bmpLeft, bmpBottom - bmpTop);
            }
        }
        exitCropMode();
        refreshImage();
    }

    // Angezeigte Punkte (Ursprung oben-links) -> Medien-Koordinaten (Ursprung unten-links).
    private PDRectangle displayToMedia(RectF d) {
        float rx1 = d.left, ry1 = d.top, rx2 = d.right, ry2 = d.bottom;
        float left, bottom, right, top;
        switch (rotation % 360) {
            case 90:
                left = ry1; bottom = rx1; right = ry2; top = rx2; break;
            case 180:
                left = mediaW - rx2; bottom = mediaH - ry2; right = mediaW - rx1; top = mediaH - ry1; break;
            case 270:
                left = mediaW - ry2; bottom = mediaH - rx2; right = mediaW - ry1; top = mediaH - rx1; break;
            default:
                left = rx1; bottom = mediaH - ry2; right = rx2; top = mediaH - ry1; break;
        }
        return new PDRectangle(left, bottom, right - left, top - bottom);
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
        closeRenderer();
        try {
            PDDocument doc = PDDocument.load(file);
            try {
                PDPage page = doc.getPage(pageIndex);
                page.setRotation(rotation);
                if (cropBox != null && cropBox.getWidth() > 0 && cropBox.getHeight() > 0) {
                    page.setCropBox(cropBox);
                }
                File tmp = new File(file.getParentFile(), file.getName() + ".tmp");
                doc.save(tmp);
                doc.close();
                if (!file.delete()) {
                    throw new IOException("alte Datei nicht löschbar");
                }
                if (!tmp.renameTo(file)) {
                    throw new IOException("Temp-Datei nicht umbenennbar");
                }
                setResult(RESULT_OK);
                finish();
            } finally {
                try { doc.close(); } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.error) + ": " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void closeRenderer() {
        if (renderer != null) {
            try { renderer.close(); } catch (Exception ignored) {}
            renderer = null;
        }
        if (pfd != null) {
            try { pfd.close(); } catch (Exception ignored) {}
            pfd = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        closeRenderer();
    }
}
