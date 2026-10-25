package com.wowax.apkextractor;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.pdf.PdfRenderer;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.text.InputType;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.EditText;
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

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.io.IOException;

/** Interne PDF-Vorschau: rendert Seiten via PdfRenderer, swipebar + pinch-zoom. */
public class PdfViewerActivity extends BaseActivity {

    private PdfRenderer renderer;
    private ParcelFileDescriptor pfd;
    private int pageCount;
    private ViewPager2 pager;
    private TextView pageIndicator;
    private int currentPage = 0;
    private String path;
    private ImageButton backButton;
    private ImageButton editButton;
    private FrameLayout.LayoutParams backLp;
    private FrameLayout.LayoutParams editLp;
    private static final int REQ_EDIT = 7002;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        path = getIntent().getStringExtra("path");
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
        addEditButton(root);
        addPageIndicator(root);
        applyButtonInsets(root);
        setContentView(root);

        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                currentPage = position;
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

    private void reload() {
        if (renderer != null) {
            try { renderer.close(); } catch (Exception ignored) {}
            renderer = null;
        }
        if (pfd != null) {
            try { pfd.close(); } catch (Exception ignored) {}
            pfd = null;
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
        if (pager.getAdapter() != null) {
            pager.getAdapter().notifyDataSetChanged();
        }
        updateIndicator(currentPage);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_EDIT && resultCode == RESULT_OK) {
            reload();
        }
    }

    private void addBackButton(FrameLayout root) {
        backButton = new ImageButton(this);
        backButton.setImageResource(R.drawable.ic_back);
        backButton.setBackgroundResource(R.drawable.bg_back_button);
        backButton.setScaleType(ImageView.ScaleType.CENTER);
        backButton.setContentDescription(getString(R.string.back));
        backButton.setOnClickListener(v -> finish());
        float d = getResources().getDisplayMetrics().density;
        backLp = new FrameLayout.LayoutParams((int) (40 * d), (int) (40 * d));
        backLp.gravity = Gravity.TOP | Gravity.START;
        backLp.setMargins((int) (8 * d), (int) (8 * d), 0, 0);
        root.addView(backButton, backLp);
    }

    private void addEditButton(FrameLayout root) {
        editButton = new ImageButton(this);
        editButton.setImageResource(R.drawable.ic_edit);
        editButton.setBackgroundResource(R.drawable.bg_back_button);
        editButton.setScaleType(ImageView.ScaleType.CENTER);
        editButton.setContentDescription(getString(R.string.edit));
        editButton.setOnClickListener(v -> {
            Intent i = new Intent(this, PdfEditActivity.class);
            i.putExtra("path", path);
            i.putExtra("page", currentPage);
            startActivityForResult(i, REQ_EDIT);
        });
        float d = getResources().getDisplayMetrics().density;
        editLp = new FrameLayout.LayoutParams((int) (40 * d), (int) (40 * d));
        editLp.gravity = Gravity.TOP | Gravity.END;
        editLp.setMargins(0, (int) (8 * d), (int) (8 * d), 0);
        root.addView(editButton, editLp);
    }

    private void applyButtonInsets(FrameLayout root) {
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            float d = getResources().getDisplayMetrics().density;
            backLp.setMargins((int) (8 * d), bars.top + (int) (8 * d), 0, 0);
            backButton.setLayoutParams(backLp);
            editLp.setMargins(0, bars.top + (int) (8 * d), (int) (8 * d), 0);
            editButton.setLayoutParams(editLp);
            return insets;
        });
    }

    private void addPageIndicator(FrameLayout root) {
        pageIndicator = new TextView(this);
        pageIndicator.setTextColor(Color.WHITE);
        pageIndicator.setTextSize(18);
        pageIndicator.setTypeface(null, Typeface.BOLD);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xAA000000);
        bg.setCornerRadius(dp(20));
        pageIndicator.setBackground(bg);
        pageIndicator.setPadding(dp(20), dp(8), dp(20), dp(8));
        pageIndicator.setClickable(true);
        pageIndicator.setOnClickListener(v -> showJumpDialog());
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        lp.bottomMargin = dp(16);
        root.addView(pageIndicator, lp);
    }

    private void showJumpDialog() {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint("1 – " + pageCount);
        input.setText(String.valueOf(currentPage + 1));
        input.setSelectAllOnFocus(true);

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.pdf_jump_title)
                .setView(input)
                .setPositiveButton(R.string.ok, (d, w) -> {
                    try {
                        int p = Integer.parseInt(input.getText().toString().trim());
                        if (p < 1) p = 1;
                        if (p > pageCount) p = pageCount;
                        pager.setCurrentItem(p - 1, true);
                    } catch (NumberFormatException ignored) {
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
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
