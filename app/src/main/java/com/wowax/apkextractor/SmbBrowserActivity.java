package com.wowax.apkextractor;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import jcifs.CIFSContext;
import jcifs.smb.NtlmPasswordAuthenticator;
import jcifs.smb.SmbFile;

public class SmbBrowserActivity extends AppCompatActivity {

    private final Handler main = new Handler(Looper.getMainLooper());

    private String host;
    private String share;
    private String username;
    private String password;
    private String domain;
    private CIFSContext context;
    private SmbFile currentDir;
    private SmbFile[] entries = new SmbFile[0];

    private final List<SmbItem> items = new ArrayList<>();
    private final Set<SmbItem> selected = new HashSet<>();

    private SmbListAdapter adapter;
    private RecyclerView recycler;
    private TextView emptyView;
    private ProgressBar progress;
    private View actionBar;
    private Throwable initError;
    private volatile boolean currentIsRoot;
    private volatile String currentTitle = "";
    private boolean moveMode;
    private final List<SmbItem> moveItems = new ArrayList<>();
    private View moveBar;
    private TextView moveHint;

    // jcifs-ng wirft gepruefte SmbException -> sichere Hilfsmethoden
    private static String name(SmbFile f) {
        try { return f.getName(); } catch (Exception e) { return ""; }
    }

    private static boolean isDir(SmbFile f) {
        try { return f.isDirectory(); } catch (Exception e) { return false; }
    }

    private static long len(SmbFile f) {
        try { return f.length(); } catch (Exception e) { return 0; }
    }

    private static String path(SmbFile f) {
        try { return f.getPath(); } catch (Exception e) { return ""; }
    }

    private static String parent(SmbFile f) {
        try { return f.getParent(); } catch (Exception e) { return null; }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_smb_browser);
        InsetsHelper.applySystemBars(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> goUpOrFinish());

        recycler = findViewById(R.id.recycler);
        emptyView = findViewById(R.id.empty_view);
        progress = findViewById(R.id.progress);
        actionBar = findViewById(R.id.action_bar);
        moveBar = findViewById(R.id.move_bar);
        moveHint = findViewById(R.id.move_hint);

        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SmbListAdapter(items, new SmbListAdapter.Listener() {
            @Override
            public void onClick(SmbItem item) {
                if (moveMode) {
                    if (item.isParent) navigateUp();
                    else if (item.isDirectory) openDir(item.file);
                    return;
                }
                if (item.isParent) navigateUp();
                else if (item.isDirectory) openDir(item.file);
                else downloadSingle(item);
            }

            @Override
            public void onToggle(SmbItem item) {
                if (moveMode) return;
                if (selected.contains(item)) selected.remove(item);
                else selected.add(item);
                adapter.setSelected(selected);
                updateActionBar();
            }
        });
        recycler.setAdapter(adapter);

        findViewById(R.id.btn_download).setOnClickListener(v -> downloadSelected());
        findViewById(R.id.btn_rename).setOnClickListener(v -> {
            if (selected.size() == 1) renameItem(selected.iterator().next());
            else Toast.makeText(this, "Bitte genau ein Element auswählen", Toast.LENGTH_SHORT).show();
        });
        findViewById(R.id.btn_delete).setOnClickListener(v -> deleteSelected());
        findViewById(R.id.btn_move).setOnClickListener(v -> startMove());
        findViewById(R.id.btn_move_here).setOnClickListener(v -> confirmMoveHere());
        findViewById(R.id.btn_cancel).setOnClickListener(v -> cancelMove());

        host = getIntent().getStringExtra("host");
        share = getIntent().getStringExtra("share");
        username = getIntent().getStringExtra("username");
        password = getIntent().getStringExtra("password");
        domain = getIntent().getStringExtra("domain");

        initContextInBackground();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_smb_browser, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_new_folder) {
            createFolder();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initContextInBackground() {
        setLoading(true);
        new Thread(() -> {
            initContext();
            main.post(() -> {
                if (context == null) {
                    setLoading(false);
                    showError(initError != null ? initError
                            : new RuntimeException("SMB-Kontext konnte nicht initialisiert werden"));
                } else {
                    openRoot();
                }
            });
        }).start();
    }

    private void initContext() {
        try {
            java.util.Properties props = new java.util.Properties();
            props.setProperty("jcifs.smb.client.connTimeout", "8000");
            props.setProperty("jcifs.smb.client.soTimeout", "8000");
            props.setProperty("jcifs.smb.client.responseTimeout", "10000");
            jcifs.config.PropertyConfiguration config =
                    new jcifs.config.PropertyConfiguration(props);
            CIFSContext base = new jcifs.context.BaseContext(config);
            NtlmPasswordAuthenticator auth = new NtlmPasswordAuthenticator(
                    domain == null ? "" : domain,
                    username == null ? "" : username,
                    password == null ? "" : password);
            context = base.withCredentials(auth);
        } catch (Throwable t) {
            context = null;
            initError = t;
        }
    }

    private String serverRoot() {
        return "smb://" + host + "/";
    }

    private boolean atServerRoot() {
        return currentIsRoot;
    }

    private boolean isRoot(SmbFile f) {
        String p = path(f);
        return p.equals("smb://" + host + "/") || p.equals("smb://" + host);
    }

    private void openRoot() {
        if (context == null) return;
        setLoading(true);
        new Thread(() -> {
            try {
                SmbFile root;
                if (share == null || share.isEmpty()) {
                    root = new SmbFile(serverRoot(), context);
                } else {
                    root = new SmbFile(serverRoot() + share + "/", context);
                }
                final SmbFile r = root;
                main.post(() -> {
                    currentDir = r;
                    openDir(r);
                });
            } catch (Exception e) {
                main.post(() -> showError(e));
            }
        }).start();
    }

    private void openDir(SmbFile dir) {
        setLoading(true);
        final SmbFile target = dir;
        new Thread(() -> {
            try {
                SmbFile[] children = target.listFiles();
                List<SmbFile> sorted = new ArrayList<>(Arrays.asList(children));
                sorted.sort((a, b) -> {
                    boolean ad = isDir(a);
                    boolean bd = isDir(b);
                    if (ad != bd) return ad ? -1 : 1;
                    return name(a).compareToIgnoreCase(name(b));
                });

                // Attribut-Abruf (isDir/len/name) komplett im Hintergrund,
                // da er bei jcifs-ng Netzwerkzugriffe ausloesen kann.
                boolean isRoot = isRoot(target);
                List<SmbItem> built = new ArrayList<>();
                if (!isRoot) {
                    built.add(new SmbItem("..", true, true, 0, null));
                }
                for (SmbFile f : sorted) {
                    String n = name(f);
                    if (n.endsWith("/")) n = n.substring(0, n.length() - 1);
                    built.add(new SmbItem(n, isDir(f), false, len(f), f));
                }
                String title = isRoot ? host : name(target);
                if (title.endsWith("/")) title = title.substring(0, title.length() - 1);

                final boolean fIsRoot = isRoot;
                final String fTitle = title;
                final List<SmbItem> fBuilt = built;
                main.post(() -> {
                    currentDir = target;
                    currentIsRoot = fIsRoot;
                    currentTitle = fTitle;
                    render(fBuilt);
                });
            } catch (Exception e) {
                main.post(() -> showError(e));
            }
        }).start();
    }

    private void render(List<SmbItem> builtItems) {
        setLoading(false);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(currentTitle);
        }
        items.clear();
        items.addAll(builtItems);
        clearSelection();
        adapter.notifyDataSetChanged();
        emptyView.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        recycler.setVisibility(items.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void refresh() {
        if (currentDir != null) openDir(currentDir);
    }

    private void navigateUp() {
        try {
            String p = parent(currentDir);
            if (p == null) {
                finish();
                return;
            }
            SmbFile up = new SmbFile(p, context);
            openDir(up);
        } catch (Exception e) {
            showError(e);
        }
    }

    private void goUpOrFinish() {
        if (currentDir != null && !atServerRoot()) {
            navigateUp();
        } else {
            finish();
        }
    }

    private void updateActionBar() {
        if (moveMode) {
            actionBar.setVisibility(View.GONE);
            moveBar.setVisibility(View.VISIBLE);
            moveHint.setVisibility(View.VISIBLE);
        } else {
            actionBar.setVisibility(selected.isEmpty() ? View.GONE : View.VISIBLE);
            moveBar.setVisibility(View.GONE);
            moveHint.setVisibility(View.GONE);
        }
    }

    private void clearSelection() {
        selected.clear();
        adapter.setSelected(selected);
        updateActionBar();
    }

    // --- Aktionen ---

    private void downloadSingle(SmbItem item) {
        String n = item.name;
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.download)
                .setMessage(n + " herunterladen?")
                .setPositiveButton(R.string.ok, (d, w) -> downloadItems(new ArrayList<>(Arrays.asList(item))))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void downloadSelected() {
        downloadItems(new ArrayList<>(selected));
    }

    private void downloadItems(List<SmbItem> toDownload) {
        setLoading(true);
        new Thread(() -> {
            int ok = 0;
            int fail = 0;
            for (SmbItem it : toDownload) {
                try {
                    downloadInto(it.file, "/");
                    ok++;
                } catch (Exception e) {
                    fail++;
                }
            }
            final int fok = ok;
            final int ffail = fail;
            main.post(() -> {
                setLoading(false);
                clearSelection();
                refresh();
                Toast.makeText(this, fok + " heruntergeladen"
                        + (ffail > 0 ? ", " + ffail + " fehlgeschlagen" : ""), Toast.LENGTH_LONG).show();
            });
        }).start();
    }

    private void downloadInto(SmbFile f, String relDir) throws Exception {
        if (isDir(f)) {
            String sub = relDir + "/" + name(f);
            for (SmbFile c : f.listFiles()) {
                downloadInto(c, sub);
            }
        } else {
            saveOne(name(f), relDir, f.getInputStream());
        }
    }

    private void saveOne(String fileName, String relDir, InputStream in) throws Exception {
        if (Build.VERSION.SDK_INT >= 29) {
            ContentResolver resolver = getContentResolver();
            ContentValues v = new ContentValues();
            v.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
            v.put(MediaStore.Downloads.MIME_TYPE, "application/octet-stream");
            v.put(MediaStore.Downloads.RELATIVE_PATH,
                    Environment.DIRECTORY_DOWNLOADS + "/M-Dateimanager" + relDir);
            Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
            if (uri == null) throw new Exception("MediaStore Einfuegung fehlgeschlagen");
            OutputStream os = resolver.openOutputStream(uri);
            pipe(in, os);
            os.close();
        } else {
            File dir = new File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                            + "/M-Dateimanager" + relDir);
            dir.mkdirs();
            File out = new File(dir, fileName);
            OutputStream os = new FileOutputStream(out);
            pipe(in, os);
            os.close();
        }
        in.close();
    }

    private void renameItem(SmbItem item) {
        EditText input = new EditText(this);
        input.setText(item.name);
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.rename)
                .setView(input)
                .setPositiveButton(R.string.ok, (d, w) -> {
                    String newName = input.getText().toString().trim();
                    if (newName.isEmpty() || newName.equals(item.name)) return;
                    new Thread(() -> {
                        try {
                            String p = parent(item.file);
                            String newPath = p + newName + (item.isDirectory ? "/" : "");
                            SmbFile newFile = new SmbFile(newPath, context);
                            item.file.renameTo(newFile);
                            main.post(this::refresh);
                        } catch (Exception e) {
                            main.post(() -> showError(e));
                        }
                    }).start();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void deleteSelected() {
        List<SmbItem> sel = new ArrayList<>(selected);
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete)
                .setMessage(sel.size() + " Element(e) wirklich löschen?")
                .setPositiveButton(R.string.delete, (d, w) -> {
                    setLoading(true);
                    new Thread(() -> {
                        for (SmbItem it : sel) {
                            try {
                                deleteRecursive(it.file);
                            } catch (Exception ignored) {
                            }
                        }
                        main.post(() -> {
                            setLoading(false);
                            clearSelection();
                            refresh();
                        });
                    }).start();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void deleteRecursive(SmbFile f) throws Exception {
        if (isDir(f)) {
            for (SmbFile c : f.listFiles()) {
                deleteRecursive(c);
            }
        }
        f.delete();
    }

    private void startMove() {
        if (selected.isEmpty()) return;
        moveItems.clear();
        moveItems.addAll(selected);
        moveMode = true;
        clearSelection();
        updateActionBar();
    }

    private void confirmMoveHere() {
        if (moveItems.isEmpty() || currentDir == null) return;
        final SmbFile destDir = currentDir;
        final List<SmbItem> toMove = new ArrayList<>(moveItems);
        setLoading(true);
        new Thread(() -> {
            String dest = path(destDir);
            int ok = 0;
            int fail = 0;
            for (SmbItem it : toMove) {
                try {
                    String n = it.name;
                    SmbFile targetFile = new SmbFile(dest + n + (it.isDirectory ? "/" : ""), context);
                    it.file.renameTo(targetFile);
                    ok++;
                } catch (Exception e) {
                    fail++;
                }
            }
            final int fok = ok;
            final int ffail = fail;
            main.post(() -> {
                setLoading(false);
                moveMode = false;
                moveItems.clear();
                updateActionBar();
                refresh();
                Toast.makeText(this, fok + " verschoben"
                        + (ffail > 0 ? ", " + ffail + " fehlgeschlagen" : ""), Toast.LENGTH_LONG).show();
            });
        }).start();
    }

    private void cancelMove() {
        moveMode = false;
        moveItems.clear();
        updateActionBar();
    }

    private void createFolder() {
        EditText input = new EditText(this);
        input.setHint(R.string.new_folder);
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.new_folder)
                .setView(input)
                .setPositiveButton(R.string.ok, (d, w) -> {
                    String n = input.getText().toString().trim();
                    if (n.isEmpty()) return;
                    new Thread(() -> {
                        try {
                            String p = path(currentDir);
                            SmbFile newDir = new SmbFile(p + n + "/", context);
                            newDir.mkdir();
                            main.post(this::refresh);
                        } catch (Exception e) {
                            main.post(() -> showError(e));
                        }
                    }).start();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void pipe(InputStream in, OutputStream os) throws Exception {
        byte[] buf = new byte[64 * 1024];
        int n;
        while ((n = in.read(buf)) > 0) os.write(buf, 0, n);
        os.flush();
    }

    private void setLoading(boolean b) {
        progress.setVisibility(b ? View.VISIBLE : View.GONE);
    }

    private void showError(Throwable t) {
        setLoading(false);
        Log.e("SmbBrowser", "SMB-Fehler", t);
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.smb_error)
                .setMessage(friendlyMessage(t))
                .setPositiveButton(R.string.close, (d, w) -> d.dismiss())
                .setCancelable(true)
                .show();
    }

    private String friendlyMessage(Throwable t) {
        // Ursachenkette nach Netzwerkfehlern durchsuchen -> verständlicher Hinweis
        Throwable cur = t;
        while (cur != null) {
            String m = cur.getMessage();
            if (m != null) {
                String low = m.toLowerCase();
                if (low.contains("connect") || low.contains("timed out")
                        || low.contains("unreachable") || low.contains("no route")
                        || low.contains("unknownhost") || low.contains("failed to connect")) {
                    return "Der Server ist nicht erreichbar.\n\nÜberprüfe, ob der Server eingeschaltet ist und ob IP-Adresse, Benutzername und Passwort stimmen.";
                }
            }
            cur = cur.getCause();
        }
        // Sonst erste sinnvolle Fehlermeldung
        cur = t;
        while (cur != null && (cur.getMessage() == null || cur.getMessage().trim().isEmpty())) {
            cur = cur.getCause();
        }
        String msg = (cur != null && cur.getMessage() != null)
                ? cur.getMessage() : t.getClass().getSimpleName();
        return msg.trim();
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        double kb = bytes / 1024.0;
        if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb);
        double mb = kb / 1024.0;
        if (mb < 1024) return String.format(Locale.US, "%.1f MB", mb);
        return String.format(Locale.US, "%.2f GB", mb / 1024.0);
    }
}
