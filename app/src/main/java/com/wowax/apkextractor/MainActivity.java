package com.wowax.apkextractor;

import android.Manifest;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_WRITE = 1001;

    private RecyclerView recycler;
    private TextView emptyView;
    private EditText searchBox;
    private MaterialSwitch switchSystem;
    private AppListAdapter adapter;

    private final List<AppInfo> allApps = new ArrayList<>();
    private final List<AppInfo> filtered = new ArrayList<>();

    private AppInfo pendingExtract = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        InsetsHelper.applySystemBars(this);

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        searchBox = findViewById(R.id.search_box);
        recycler = findViewById(R.id.recycler);
        emptyView = findViewById(R.id.empty_view);
        switchSystem = findViewById(R.id.switch_system);

        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AppListAdapter(filtered, this::onAppClick);
        recycler.setAdapter(adapter);

        searchBox.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) { applyFilter(); }
            public void afterTextChanged(Editable s) {}
        });

        switchSystem.setOnCheckedChangeListener((btn, checked) -> applyFilter());

        loadApps();
    }

    private void loadApps() {
        new Thread(() -> {
            PackageManager pm = getPackageManager();
            List<AppInfo> list = new ArrayList<>();
            for (ApplicationInfo ai : pm.getInstalledApplications(PackageManager.GET_META_DATA)) {
                if (pm.getLaunchIntentForPackage(ai.packageName) == null) continue;
                AppInfo info = new AppInfo();
                info.packageName = ai.packageName;
                info.appName = ai.loadLabel(pm).toString();
                info.icon = ai.loadIcon(pm);
                info.sourceDir = ai.publicSourceDir != null ? ai.publicSourceDir : ai.sourceDir;
                info.splitSourceDirs = ai.splitPublicSourceDirs != null
                        ? ai.splitPublicSourceDirs : ai.splitSourceDirs;
                info.isSystemApp = (ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
                try {
                    PackageInfo pi = pm.getPackageInfo(ai.packageName, 0);
                    info.versionName = pi.versionName;
                } catch (Exception ignored) {
                    info.versionName = null;
                }
                list.add(info);
            }
            list.sort((x, y) -> x.appName.compareToIgnoreCase(y.appName));
            runOnUiThread(() -> {
                allApps.clear();
                allApps.addAll(list);
                applyFilter();
            });
        }).start();
    }

    private void applyFilter() {
        String q = searchBox.getText().toString().trim().toLowerCase();
        boolean showSystem = switchSystem.isChecked();

        filtered.clear();
        for (AppInfo a : allApps) {
            if (a.isSystemApp && !showSystem) continue;
            if (q.isEmpty()
                    || a.appName.toLowerCase().contains(q)
                    || a.packageName.toLowerCase().contains(q)) {
                filtered.add(a);
            }
        }
        adapter.notifyDataSetChanged();
        emptyView.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        recycler.setVisibility(filtered.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void onAppClick(AppInfo app) {
        if (Build.VERSION.SDK_INT < 29) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                pendingExtract = app;
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQ_WRITE);
                return;
            }
        }
        extract(app);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_WRITE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && pendingExtract != null) {
                extract(pendingExtract);
            }
            pendingExtract = null;
        }
    }

    private void extract(AppInfo app) {
        new ApkExtractor(this, app, new ApkExtractor.Callback() {
            @Override
            public void onSuccess(ExtractResult result) {
                showResult(result);
            }

            @Override
            public void onError(String message) {
                new MaterialAlertDialogBuilder(MainActivity.this)
                        .setTitle(R.string.error)
                        .setMessage(message)
                        .setPositiveButton(R.string.ok, null)
                        .show();
            }
        }).start();
    }

    private void showResult(ExtractResult result) {
        String text = result.isSplit
                ? "APK-Dateien gespeichert unter:\n" + result.outputPath
                : "APK gespeichert unter:\n" + result.outputPath;
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.saved)
                .setMessage(text)
                .setNegativeButton(R.string.ok, null)
                .setPositiveButton(R.string.share, (d, w) -> share(result))
                .show();
    }

    private void share(ExtractResult result) {
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("application/vnd.android.package-archive");
        send.putExtra(Intent.EXTRA_STREAM, result.shareUri);
        send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(send, "APK teilen"));
    }
}
