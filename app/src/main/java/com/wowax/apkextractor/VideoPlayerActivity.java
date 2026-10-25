package com.wowax.apkextractor;

import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.widget.ImageButton;
import android.widget.ImageView;

import java.io.File;

public class VideoPlayerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String path = getIntent().getStringExtra("path");
        if (path == null || !new File(path).exists()) {
            Toast.makeText(this, R.string.error, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // VideoView zentriert in einem FrameLayout, schwarzer Hintergrund für Letterbox
        VideoView vv = new VideoView(this);
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        lp.gravity = Gravity.CENTER;
        root.addView(vv, lp);
        addBackButton(root);
        setContentView(root);

        // MediaController = Play/Stop + Zeitleiste (Seekbar) + Position/Dauer
        MediaController mc = new MediaController(this);
        mc.setAnchorView(vv);
        vv.setMediaController(mc);

        vv.setVideoURI(Uri.fromFile(new File(path)));
        vv.setOnPreparedListener(mp -> {
            vv.start();
            mc.show(); // Steuerung kurz sichtbar machen
        });
        vv.setOnErrorListener((mp, what, extra) -> {
            Toast.makeText(this, "Video kann nicht abgespielt werden", Toast.LENGTH_SHORT).show();
            finish();
            return true;
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
}
