package com.wowax.apkextractor;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import java.io.File;
import java.io.IOException;

/** Interne PDF-Vorschau: rendert Seiten via PdfRenderer, swipebar + pinch-zoom. */
public class PdfViewerActivity extends BaseActivity {

    private PdfRenderer renderer;
    private ParcelFileDescriptor pfd;
    private int pageCount;
    private ViewPager2 pager;
    private TextView pageIndicator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String path = getIntent().getStringExtra("path");
        if (path == null) {
            finish();
            return;
        }

        try {
            pfd = ParcelFileDescriptor.open(new File(path), ParcelFileDescriptor.MODE_READ_ONLY);
            renderer = new PdfRenderer(pfd);
        } catch (Exception e) {
            Toast.makeText(this, R.string.error, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        pageCount = renderer.getPageCount();

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        pager = new ViewPager2(this);
        pager.setAdapter(new PdfPagerAdapter());
        pager.setOffscreenPageLimit(1);
        root.addView(pager, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        addBackButton(root);
        addPageIndicator(root);
        setContentView(root);

        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateIndicator(position);
            }
        });
        updateIndicator(0);
    }

    private void updateIndicator(int position) {
        if (pageIndicator != null) {
            pageIndicator.setText(getString(R.string.pdf_page, position + 1, pageCount));
        }
    }

    private void addBackButton(FrameLayout root) {
        ImageButton back = new ImageButton(this);
        back.setImageResource(R.drawable.ic_back);
        back.setBackgroundResource(R.drawable.bg_back_button);
        back.setScaleType(ImageView.ScaleType.CENTER);
        back.setContentDescription(getString(R.string.back));
        back.setOnClickListener(v -> finish());
        float d = getResources().getDisplayMetrics().density;
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                (int) (40 * d), (int) (40 * d));
        lp.gravity = Gravity.TOP | Gravity.START;
        lp.setMargins((int) (8 * d), (int) (8 * d), 0, 0);
        root.addView(back, lp);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            lp.setMargins((int) (8 * d), bars.top + (int) (8 * d), 0, 0);
            back.setLayoutParams(lp);
            return insets;
        });
    }

    private void addPageIndicator(FrameLayout root) {
        pageIndicator = new TextView(this);
        pageIndicator.setTextColor(Color.WHITE);
        pageIndicator.setTextSize(14);
        pageIndicator.setBackgroundColor(0x99000000);
        pageIndicator.setPadding(dp(12), dp(4), dp(12), dp(4));
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        lp.bottomMargin = dp(16);
        root.addView(pageIndicator, lp);
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }

    private Bitmap renderPage(int index) {
        try {
            PdfRenderer.Page page = renderer.openPage(index);
            int pw = page.getWidth();
            int ph = page.getHeight();
            int maxDim = 2000;
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (renderer != null) {
            try {
                renderer.close();
            } catch (Exception ignored) {
            }
        }
        if (pfd != null) {
            try {
                pfd.close();
            } catch (IOException ignored) {
            }
        }
    }

    private class PdfPagerAdapter extends RecyclerView.Adapter<PdfPagerAdapter.VH> {

        @Override
        public int getItemCount() {
            return pageCount;
        }

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            FrameLayout container = new FrameLayout(parent.getContext());
            container.setLayoutParams(new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            return new VH(container);
        }

        @Override
        public void onBindViewHolder(VH holder, int position) {
            holder.container.removeAllViews();
            holder.image = null;

            ZoomableImageView iv = new ZoomableImageView(holder.container.getContext());
            iv.setBackgroundColor(Color.BLACK);
            Bitmap bmp = renderPage(position);
            if (bmp != null) {
                iv.setImageBitmap(bmp);
            } else {
                Toast.makeText(PdfViewerActivity.this, R.string.error,
                        Toast.LENGTH_SHORT).show();
            }
            iv.setZoomListener((scale, minScale) ->
                    pager.setUserInputEnabled(scale <= minScale * 1.01f));
            holder.container.addView(iv, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            holder.image = iv;
        }

        class VH extends RecyclerView.ViewHolder {
            final FrameLayout container;
            ZoomableImageView image;

            VH(FrameLayout container) {
                super(container);
                this.container = container;
            }
        }
    }
}
