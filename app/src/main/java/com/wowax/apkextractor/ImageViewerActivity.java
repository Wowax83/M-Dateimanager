package com.wowax.apkextractor;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
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

    /** Einfacher Bild-Viewer mit Pinch-Zoom, Doppel-Tipp und Verschieben. */
    static class ZoomableImageView extends AppCompatImageView {
        private final Matrix matrix = new Matrix();
        private final float[] last = new float[2];
        private float scale = 1f;
        private final float minScale = 1f;
        private final float maxScale = 8f;
        private int mode = 0; // 0 = nichts, 1 = verschieben, 2 = zoomen
        private final ScaleGestureDetector scaleDetector;
        private final GestureDetector gestureDetector;

        ZoomableImageView(Context c) {
            super(c);
            setScaleType(ScaleType.MATRIX);
            setImageMatrix(matrix);

            scaleDetector = new ScaleGestureDetector(c, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                @Override
                public boolean onScale(ScaleGestureDetector detector) {
                    scale *= detector.getScaleFactor();
                    scale = Math.max(minScale, Math.min(scale, maxScale));
                    matrix.setScale(scale, scale);
                    setImageMatrix(matrix);
                    return true;
                }
            });

            gestureDetector = new GestureDetector(c, new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onDoubleTap(MotionEvent e) {
                    scale = scale > 1f ? minScale : 3f;
                    matrix.setScale(scale, scale);
                    setImageMatrix(matrix);
                    return true;
                }

                @Override
                public boolean onScroll(MotionEvent e1, MotionEvent e2, float dx, float dy) {
                    if (scale > 1f) {
                        matrix.postTranslate(-dx, -dy);
                        setImageMatrix(matrix);
                        return true;
                    }
                    return false;
                }
            });
        }

        @Override
        public boolean onTouchEvent(MotionEvent ev) {
            scaleDetector.onTouchEvent(ev);
            gestureDetector.onTouchEvent(ev);
            if (ev.getPointerCount() == 1) {
                if (ev.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    mode = 1;
                    last[0] = ev.getX();
                    last[1] = ev.getY();
                }
            }
            return true;
        }
    }
}
