package com.wowax.apkextractor;

import android.net.Uri;
import android.os.Bundle;
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

        VideoView vv = new VideoView(this);
        MediaController mc = new MediaController(this);
        mc.setAnchorView(vv);
        vv.setMediaController(mc);
        vv.setVideoURI(Uri.fromFile(new File(path)));
        setContentView(vv);

        vv.setOnPreparedListener(mp -> vv.start());
        vv.setOnErrorListener((mp, what, extra) -> {
            Toast.makeText(this, "Video kann nicht abgespielt werden", Toast.LENGTH_SHORT).show();
            finish();
            return true;
        });
    }
}
