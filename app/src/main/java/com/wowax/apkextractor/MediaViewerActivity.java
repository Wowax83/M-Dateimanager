package com.wowax.apkextractor;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import java.io.File;
import java.util.ArrayList;

/** Einheitliche Media-Vorschau: per Swipe zwischen Bildern und Videos blättern. */
public class MediaViewerActivity extends BaseActivity {

    private ArrayList<String> paths = new ArrayList<>();
    private int index = 0;
    private ViewPager2 pager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ArrayList<String> p = getIntent().getStringArrayListExtra("paths");
        if (p != null) paths = p;
        index = getIntent().getIntExtra("index", 0);
        if (paths.isEmpty()) {
            finish();
            return;
        }

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        pager = new ViewPager2(this);
        pager.setAdapter(new MediaPagerAdapter());
        pager.setOffscreenPageLimit(1);
        root.addView(pager, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        addBackButton(root);
        setContentView(root);

        pager.setCurrentItem(index, false);
        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                index = position;
            }
        });
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

    private boolean isVideo(String name) {
        String l = name.toLowerCase();
        return l.endsWith(".mp4") || l.endsWith(".mkv") || l.endsWith(".webm")
                || l.endsWith(".avi") || l.endsWith(".mov") || l.endsWith(".3gp");
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

    private class MediaPagerAdapter extends RecyclerView.Adapter<MediaPagerAdapter.VH> {
        private static final int TYPE_IMAGE = 0;
        private static final int TYPE_VIDEO = 1;

        @Override
        public int getItemCount() {
            return paths.size();
        }

        @Override
        public int getItemViewType(int position) {
            return isVideo(paths.get(position)) ? TYPE_VIDEO : TYPE_IMAGE;
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
            String path = paths.get(position);
            holder.container.removeAllViews();
            holder.video = null;
            holder.image = null;

            if (isVideo(path)) {
                VideoView vv = new VideoView(holder.container.getContext());
                MediaController mc = new MediaController(MediaViewerActivity.this);
                mc.setAnchorView(vv);
                vv.setMediaController(mc);
                vv.setVideoURI(Uri.fromFile(new File(path)));
                vv.setOnPreparedListener(mp -> mc.show());
                FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                lp.gravity = Gravity.CENTER;
                holder.container.addView(vv, lp);
                holder.video = vv;
            } else {
                ZoomableImageView iv = new ZoomableImageView(holder.container.getContext());
                iv.setBackgroundColor(Color.BLACK);
                Bitmap bmp = decodeSampled(path, 2000);
                if (bmp != null) {
                    iv.setImageBitmap(bmp);
                } else {
                    Toast.makeText(MediaViewerActivity.this, R.string.error,
                            Toast.LENGTH_SHORT).show();
                }
                iv.setZoomListener((scale, minScale) ->
                        pager.setUserInputEnabled(scale <= minScale * 1.01f));
                holder.container.addView(iv, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                holder.image = iv;
            }
        }

        @Override
        public void onViewRecycled(VH holder) {
            super.onViewRecycled(holder);
            if (holder.video != null) {
                holder.video.stopPlayback();
                holder.video = null;
            }
        }

        class VH extends RecyclerView.ViewHolder {
            final FrameLayout container;
            VideoView video;
            ZoomableImageView image;

            VH(FrameLayout c) {
                super(c);
                container = c;
            }
        }
    }
}
