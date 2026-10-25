package com.wowax.apkextractor;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;

public class ImageViewerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String path = getIntent().getStringExtra("path");
        ZoomableImageView view = new ZoomableImageView(this);
        view.setBackgroundColor(0xFF000000);

        Bitmap bmp = decodeSampled(path, 2000);
        if (bmp == null) {
            Toast.makeText(this, R.string.error, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        view.setImageBitmap(bmp);
        setContentView(view);
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

    /** Bild-Viewer: zentriert eingepasst, Pinch-Zoom um den Fokuspunkt, Doppel-Tipp, Pan mit Begrenzung. */
    static class ZoomableImageView extends AppCompatImageView {
        private final Matrix matrix = new Matrix();
        private final float[] values = new float[9];
        private float minScale = 1f;
        private final float maxScale = 8f;
        private final ScaleGestureDetector scaleDetector;
        private final GestureDetector gestureDetector;
        private boolean fitted = false;

        ZoomableImageView(Context c) {
            super(c);
            setScaleType(ScaleType.MATRIX);

            scaleDetector = new ScaleGestureDetector(c, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                @Override
                public boolean onScale(ScaleGestureDetector d) {
                    float cur = getScale();
                    float target = Math.max(minScale, Math.min(cur * d.getScaleFactor(), maxScale));
                    float factor = target / cur;
                    matrix.postScale(factor, factor, d.getFocusX(), d.getFocusY());
                    clamp();
                    setImageMatrix(matrix);
                    return true;
                }
            });

            gestureDetector = new GestureDetector(c, new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onDoubleTap(MotionEvent e) {
                    if (getScale() > minScale * 1.01f) {
                        fit();
                    } else {
                        zoomAt(Math.min(minScale * 3f, maxScale), e.getX(), e.getY());
                    }
                    return true;
                }

                @Override
                public boolean onScroll(MotionEvent e1, MotionEvent e2, float dx, float dy) {
                    if (getScale() > minScale) {
                        matrix.postTranslate(-dx, -dy);
                        clamp();
                        setImageMatrix(matrix);
                        return true;
                    }
                    return false;
                }
            });
        }

        @Override
        public void setImageBitmap(Bitmap bm) {
            super.setImageBitmap(bm);
            fitted = false;
            requestLayout();
        }

        @Override
        protected void onLayout(boolean changed, int l, int t, int r, int b) {
            super.onLayout(changed, l, t, r, b);
            if (!fitted && getDrawable() != null) {
                fit();
                fitted = true;
            }
        }

        private float getScale() {
            matrix.getValues(values);
            return values[Matrix.MSCALE_X];
        }

        /** Bild zentrieren und so skalieren, dass es in den Bildschirm passt (nie hochskaliert). */
        private void fit() {
            Drawable d = getDrawable();
            if (d == null) return;
            int dw = d.getIntrinsicWidth();
            int dh = d.getIntrinsicHeight();
            int vw = getWidth();
            int vh = getHeight();
            if (dw <= 0 || dh <= 0 || vw <= 0 || vh <= 0) return;

            float s = Math.min((float) vw / dw, (float) vh / dh);
            s = Math.min(s, 1f); // kleine Bilder 1:1, große Bilder eingepasst
            minScale = s;

            matrix.reset();
            matrix.setScale(s, s);
            matrix.postTranslate((vw - dw * s) / 2f, (vh - dh * s) / 2f);
            setImageMatrix(matrix);
        }

        private void zoomAt(float targetScale, float fx, float fy) {
            float cur = getScale();
            targetScale = Math.max(minScale, Math.min(targetScale, maxScale));
            float factor = targetScale / cur;
            matrix.postScale(factor, factor, fx, fy);
            clamp();
            setImageMatrix(matrix);
        }

        /** Hält das Bild im sichtbaren Bereich (kein Wegziehen über den Rand). */
        private void clamp() {
            Drawable d = getDrawable();
            if (d == null) return;
            float s = getScale();
            float cw = d.getIntrinsicWidth() * s;
            float ch = d.getIntrinsicHeight() * s;
            float vw = getWidth();
            float vh = getHeight();

            matrix.getValues(values);
            float tx = values[Matrix.MTRANS_X];
            float ty = values[Matrix.MTRANS_Y];

            float minX = cw <= vw ? (vw - cw) / 2f : vw - cw;
            float maxX = cw <= vw ? (vw - cw) / 2f : 0f;
            float minY = ch <= vh ? (vh - ch) / 2f : vh - ch;
            float maxY = ch <= vh ? (vh - ch) / 2f : 0f;

            tx = Math.min(maxX, Math.max(minX, tx));
            ty = Math.min(maxY, Math.max(minY, ty));

            values[Matrix.MTRANS_X] = tx;
            values[Matrix.MTRANS_Y] = ty;
            matrix.setValues(values);
        }

        @Override
        public boolean onTouchEvent(MotionEvent ev) {
            scaleDetector.onTouchEvent(ev);
            gestureDetector.onTouchEvent(ev);
            return true;
        }
    }
}
