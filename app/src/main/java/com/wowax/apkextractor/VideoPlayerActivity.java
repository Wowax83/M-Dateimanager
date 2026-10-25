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
}
