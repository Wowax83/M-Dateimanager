package com.wowax.apkextractor;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

/** Ziehbares Zuschneide-Rechteck mit vier Eck-Griffen. */
public class CropOverlayView extends View {

    private final Paint dimPaint = new Paint();
    private final Paint borderPaint = new Paint();
    private final Paint handlePaint = new Paint();

    private RectF imageBounds = new RectF();
    private RectF crop = new RectF();

    private final float handleRadius;
    private int dragging = -1; // -1 keine, 0..3 Ecken, 4 verschieben
    private float lastX, lastY;

    public CropOverlayView(Context context) {
        super(context);
        float d = getResources().getDisplayMetrics().density;
        handleRadius = 9f * d;
        dimPaint.setColor(0x99000000);
        borderPaint.setColor(Color.WHITE);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(2f * d);
        handlePaint.setColor(Color.WHITE);
    }

    public void setImageBounds(RectF r) {
        imageBounds.set(r);
        float w = r.width() * 0.85f;
        float h = r.height() * 0.85f;
        float cx = r.centerX();
        float cy = r.centerY();
        crop.set(cx - w / 2f, cy - h / 2f, cx + w / 2f, cy + h / 2f);
        invalidate();
    }

    public RectF getCrop() {
        return new RectF(crop);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (crop.width() <= 0 || crop.height() <= 0) return;
        canvas.drawRect(imageBounds.left, imageBounds.top, imageBounds.right, crop.top, dimPaint);
        canvas.drawRect(imageBounds.left, crop.bottom, imageBounds.right, imageBounds.bottom, dimPaint);
        canvas.drawRect(imageBounds.left, crop.top, crop.left, crop.bottom, dimPaint);
        canvas.drawRect(crop.right, crop.top, imageBounds.right, crop.bottom, dimPaint);
        canvas.drawRect(crop, borderPaint);
        canvas.drawCircle(crop.left, crop.top, handleRadius, handlePaint);
        canvas.drawCircle(crop.right, crop.top, handleRadius, handlePaint);
        canvas.drawCircle(crop.left, crop.bottom, handleRadius, handlePaint);
        canvas.drawCircle(crop.right, crop.bottom, handleRadius, handlePaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        float x = e.getX();
        float y = e.getY();
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                dragging = hitTest(x, y);
                lastX = x;
                lastY = y;
                return true;
            case MotionEvent.ACTION_MOVE:
                if (dragging >= 0) {
                    if (dragging == 4) {
                        moveCrop(x - lastX, y - lastY);
                    } else {
                        resizeCrop(dragging, x, y);
                    }
                    lastX = x;
                    lastY = y;
                    invalidate();
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                dragging = -1;
                return true;
        }
        return super.onTouchEvent(e);
    }

    private int hitTest(float x, float y) {
        float d = handleRadius * 2f;
        if (near(x, y, crop.left, crop.top, d)) return 0;
        if (near(x, y, crop.right, crop.top, d)) return 1;
        if (near(x, y, crop.left, crop.bottom, d)) return 2;
        if (near(x, y, crop.right, crop.bottom, d)) return 3;
        if (crop.contains(x, y)) return 4;
        return -1;
    }

    private boolean near(float x, float y, float cx, float cy, float d) {
        return Math.abs(x - cx) <= d && Math.abs(y - cy) <= d;
    }

    private void resizeCrop(int which, float x, float y) {
        x = clamp(x, imageBounds.left, imageBounds.right);
        y = clamp(y, imageBounds.top, imageBounds.bottom);
        float minSize = handleRadius * 3f;
        switch (which) {
            case 0:
                crop.left = Math.min(x, crop.right - minSize);
                crop.top = Math.min(y, crop.bottom - minSize);
                break;
            case 1:
                crop.right = Math.max(x, crop.left + minSize);
                crop.top = Math.min(y, crop.bottom - minSize);
                break;
            case 2:
                crop.left = Math.min(x, crop.right - minSize);
                crop.bottom = Math.max(y, crop.top + minSize);
                break;
            case 3:
                crop.right = Math.max(x, crop.left + minSize);
                crop.bottom = Math.max(y, crop.top + minSize);
                break;
        }
    }

    private void moveCrop(float dx, float dy) {
        float w = crop.width();
        float h = crop.height();
        float newLeft = clamp(crop.left + dx, imageBounds.left, imageBounds.right - w);
        float newTop = clamp(crop.top + dy, imageBounds.top, imageBounds.bottom - h);
        crop.set(newLeft, newTop, newLeft + w, newTop + h);
    }

    private float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }
}
