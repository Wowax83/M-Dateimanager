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
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

public class FtpBrowserActivity extends AppCompatActivity {

    private final Handler main = new Handler(Looper.getMainLooper());

    private String protocol; // "ftp" oder "sftp"
    private String host;
    private int port;
    private String username;
    private String password;

    private FTPClient ftp;
    private Session session;
    private ChannelSftp sftp;

    private String currentPath = "/";
    private boolean atRoot = true;

    private final List<FtpItem> items = new ArrayList<>();
    private final Set<FtpItem> selected = new HashSet<>();

    private RecyclerView recycler;
    private TextView emptyView;
    private ProgressBar progress;
    private View actionBar;
    private View moveBar;
    private TextView moveHint;
    private FtpListAdapter adapter;

    private boolean moveMode;
    private final List<FtpItem> moveItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ftp_browser);
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
        adapter = new FtpListAdapter(items, new FtpListAdapter.Listener() {
            @Override
            public void onClick(FtpItem item) {
                if (moveMode) {
                    if (item.isParent) navigateUp();
                    else if (item.isDirectory) openDir(item.path, false);
                    return;
                }
                if (item.isParent) navigateUp();
                else if (item.isDirectory) openDir(item.path, false);
                else downloadSingle(item);
            }

            @Override
            public void onToggle(FtpItem item) {
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

        protocol = getIntent().getStringExtra("protocol");
        host = getIntent().getStringExtra("host");
        port = getIntent().getIntExtra("port", 22);
        username = getIntent().getStringExtra("username");
        password = getIntent().getStringExtra("password");
        if (protocol == null) protocol = "ftp";

        connectInBackground();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_ftp_browser, menu);
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

    // --- Verbindung ---

    private void connectInBackground() {
        setLoading(true);
        new Thread(() -> {
            try {
                if ("ftp".equals(protocol)) {
                    ftp = new FTPClient();
                    ftp.setConnectTimeout(8000);
                    ftp.setDefaultTimeout(8000);
                    ftp.setDataTimeout(8000);
                    ftp.connect(host, port);
                    if (!ftp.login(username, password)) {
                        ftp.logout();
                        throw new Exception("Login fehlgeschlagen");
                    }
                    ftp.enterLocalPassiveMode();
                    ftp.setFileType(FTP.BINARY_FILE_TYPE);
                } else {
                    JSch jsch = new JSch();
                    session = jsch.getSession(username, host, port);
                    session.setPassword(password);
                    session.setConfig("StrictHostKeyChecking", "no");
                    session.setConfig("PreferredAuthentications", "password,keyboard-interactive");
                    session.setTimeout(8000);
                    session.connect(8000);
                    Channel channel = session.openChannel("sftp");
                    channel.connect(8000);
                    sftp = (ChannelSftp) channel;
                }
                main.post(() -> {
                    setLoading(false);
                    openDir("/", true);
                });
            } catch (Exception e) {
                main.post(() -> {
                    setLoading(false);
                    showError(e);
                });
            }
        }).start();
    }

    // --- Auflisten & Navigation ---

    private void openDir(String path, boolean root) {
        setLoading(true);
        new Thread(() -> {
            try {
                final List<FtpItem> built = doList(path);
                final String title = root ? host : lastSegment(path);
                final boolean fRoot = root;
                final String fPath = path;
                main.post(() -> {
                    currentPath = fPath;
                    atRoot = fRoot;
                    render(built, title);
                });
            } catch (Exception e) {
                main.post(() -> showError(e));
            }
        }).start();
    }

    private List<FtpItem> doList(String path) throws Exception {
        List<FtpItem> out = new ArrayList<>();
        if ("ftp".equals(protocol)) {
            FTPFile[] fs = ftp.listFiles(path);
            if (fs != null) {
                for (FTPFile f : fs) {
                    out.add(new FtpItem(f.getName(), f.isDirectory(), false,
                            f.getSize(), joinPath(path, f.getName())));
                }
            }
        } else {
            Vector<?> v = sftp.ls(path);
            for (Object o : v) {
                ChannelSftp.LsEntry e = (ChannelSftp.LsEntry) o;
                String n = e.getFilename();
                if (".".equals(n) || "..".equals(n)) continue;
                boolean dir = e.getAttrs().isDir();
                out.add(new FtpItem(n, dir, false, e.getAttrs().getSize(), joinPath(path, n)));
            }
        }
        out.sort((a, b) -> {
            if (a.isDirectory != b.isDirectory) return a.isDirectory ? -1 : 1;
            return a.name.compareToIgnoreCase(b.name);
        });
        return out;
    }

    private void render(List<FtpItem> built, String title) {
        setLoading(false);
        if (getSupportActionBar() != null) getSupportActionBar().setTitle(title);
        items.clear();
        if (!atRoot) items.add(new FtpItem("..", true, true, 0, null));
        items.addAll(built);
        clearSelection();
        adapter.notifyDataSetChanged();
        emptyView.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        recycler.setVisibility(items.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void refresh() {
        if (currentPath != null) openDir(currentPath, atRoot);
    }

    private void navigateUp() {
        String p = parentPath(currentPath);
        if (p == null) {
            finish();
            return;
        }
        openDir(p, "/".equals(p));
    }

    private void goUpOrFinish() {
        if (atRoot) finish();
        else navigateUp();
    }

    private String joinPath(String dir, String name) {
        if ("/".equals(dir)) return "/" + name;
        return dir + "/" + name;
    }

    private String parentPath(String p) {
        if ("/".equals(p)) return null;
        int i = p.lastIndexOf('/');
        if (i <= 0) return "/";
        return p.substring(0, i);
    }

    private String lastSegment(String p) {
        if ("/".equals(p)) return "/";
        int i = p.lastIndexOf('/');
        return i < 0 ? p : p.substring(i + 1);
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

    private void downloadSingle(FtpItem item) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.download)
                .setMessage(item.name + " herunterladen?")
                .setPositiveButton(R.string.ok, (d, w) -> downloadItems(new ArrayList<>(java.util.Arrays.asList(item))))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void downloadSelected() {
        downloadItems(new ArrayList<>(selected));
    }

    private void downloadItems(List<FtpItem> toDownload) {
        setLoading(true);
        new Thread(() -> {
            int ok = 0;
            int fail = 0;
            for (FtpItem it : toDownload) {
                try {
                    downloadInto(it, "/");
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

    private void downloadInto(FtpItem item, String relDir) throws Exception {
        if (item.isDirectory) {
            List<FtpItem> children = doList(item.path);
            for (FtpItem c : children) downloadInto(c, relDir + "/" + item.name);
        } else {
            downloadOneFile(item, relDir);
        }
    }

    private void downloadOneFile(FtpItem item, String relDir) throws Exception {
        InputStream in;
        if ("ftp".equals(protocol)) {
            in = ftp.retrieveFileStream(item.path);
            if (in == null) throw new Exception("Datei nicht gefunden");
            saveOne(item.name, relDir, in);
            ftp.completePendingCommand();
        } else {
            in = sftp.get(item.path);
            saveOne(item.name, relDir, in);
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

    private void renameItem(FtpItem item) {
        EditText input = new EditText(this);
        input.setText(item.name);
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.rename)
                .setView(input)
                .setPositiveButton(R.string.ok, (d, w) -> {
                    String newName = input.getText().toString().trim();
                    if (newName.isEmpty() || newName.equals(item.name)) return;
                    String from = item.path;
                    String to = joinPath(parentPath(item.path), newName);
                    new Thread(() -> {
                        try {
                            if ("ftp".equals(protocol)) ftp.rename(from, to);
                            else sftp.rename(from, to);
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
        List<FtpItem> sel = new ArrayList<>(selected);
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete)
                .setMessage(sel.size() + " Element(e) wirklich löschen?")
                .setPositiveButton(R.string.delete, (d, w) -> {
                    setLoading(true);
                    new Thread(() -> {
                        for (FtpItem it : sel) {
                            try {
                                deleteRecursive(it);
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

    private void deleteRecursive(FtpItem it) throws Exception {
        if (it.isDirectory) {
            List<FtpItem> children = doList(it.path);
            for (FtpItem c : children) deleteRecursive(c);
            if ("ftp".equals(protocol)) ftp.removeDirectory(it.path);
            else sftp.rmdir(it.path);
        } else {
            if ("ftp".equals(protocol)) ftp.deleteFile(it.path);
            else sftp.rm(it.path);
        }
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
        if (moveItems.isEmpty()) return;
        final String destDir = currentPath;
        final List<FtpItem> toMove = new ArrayList<>(moveItems);
        setLoading(true);
        new Thread(() -> {
            int ok = 0;
            int fail = 0;
            for (FtpItem it : toMove) {
                try {
                    String from = it.path;
                    String to = joinPath(destDir, it.name);
                    if ("ftp".equals(protocol)) ftp.rename(from, to);
                    else sftp.rename(from, to);
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
                    String p = joinPath(currentPath, n);
                    new Thread(() -> {
                        try {
                            if ("ftp".equals(protocol)) ftp.makeDirectory(p);
                            else sftp.mkdir(p);
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
        Log.e("FtpBrowser", "FTP/SFTP-Fehler", t);
        if (isUnreachable(t)) {
            Toast.makeText(this, R.string.server_unreachable, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.ftp_error)
                .setMessage(friendlyMessage(t))
                .setPositiveButton(R.string.close, (d, w) -> d.dismiss())
                .setCancelable(true)
                .show();
    }

    private boolean isUnreachable(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            String m = cur.getMessage();
            if (m != null) {
                String low = m.toLowerCase();
                if (low.contains("connect") || low.contains("timed out")
                        || low.contains("timeout") || low.contains("unreachable")
                        || low.contains("no route") || low.contains("unknownhost")
                        || low.contains("unknown host") || low.contains("refused")
                        || low.contains("failed to connect")) {
                    return true;
                }
            }
            cur = cur.getCause();
        }
        return false;
    }

    private String friendlyMessage(Throwable t) {
        Throwable cur = t;
        while (cur != null && (cur.getMessage() == null || cur.getMessage().trim().isEmpty())) {
            cur = cur.getCause();
        }
        String msg = (cur != null && cur.getMessage() != null)
                ? cur.getMessage() : t.getClass().getSimpleName();
        return msg.trim();
    }

    private void disconnect() {
        try {
            if (ftp != null && ftp.isConnected()) {
                try { ftp.logout(); } catch (Exception ignored) {}
                ftp.disconnect();
            }
        } catch (Exception ignored) {}
        try { if (sftp != null) sftp.disconnect(); } catch (Exception ignored) {}
        try { if (session != null) session.disconnect(); } catch (Exception ignored) {}
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disconnect();
    }
}
