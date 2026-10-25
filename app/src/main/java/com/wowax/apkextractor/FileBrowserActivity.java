package com.wowax.apkextractor;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.content.res.AppCompatResources;
import me.zhanghai.android.fastscroll.FastScrollerBuilder;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class FileBrowserActivity extends AppCompatActivity {

    private static final int REQ_READ = 2001;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private RecyclerView recycler;
    private TextView emptyView;
    private Toolbar toolbar;
    private FileListAdapter adapter;
    private View actionBar;
    private Button btnCopy, btnMove, btnRename, btnDelete, btnExtract, btnMore;

    private File currentDir;
    private File storageRoot;
    private final List<FileItem> files = new ArrayList<>();
    private final List<File> storageRoots = new ArrayList<>();

    private final Set<FileItem> selected = new LinkedHashSet<>();
    private boolean selectionMode = false;

    private int sortMode = 0; // 0 Name, 1 Größe, 2 Datum, 3 Typ
    private boolean sortAsc = true;
    private boolean showHidden = false;
    private String searchQuery = "";
    private final List<File> allChildren = new ArrayList<>();
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_browser);
        InsetsHelper.applySystemBars(this);
        prefs = getSharedPreferences("file_favorites", MODE_PRIVATE);

        recycler = findViewById(R.id.recycler);
        emptyView = findViewById(R.id.empty_view);
        toolbar = findViewById(R.id.toolbar);
        actionBar = findViewById(R.id.action_bar);
        btnCopy = findViewById(R.id.btn_copy);
        btnMove = findViewById(R.id.btn_move);
        btnRename = findViewById(R.id.btn_rename);
        btnDelete = findViewById(R.id.btn_delete);
        btnExtract = findViewById(R.id.btn_extract);
        btnMore = findViewById(R.id.btn_more);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> goUpOrFinish());

        recycler.setLayoutManager(new LinearLayoutManager(this));
        new FastScrollerBuilder(recycler)
                .setThumbDrawable(AppCompatResources.getDrawable(this, R.drawable.fastscroll_thumb))
                .setTrackDrawable(AppCompatResources.getDrawable(this, R.drawable.fastscroll_track))
                .build();
        adapter = new FileListAdapter(files, new FileListAdapter.Listener() {
            @Override public void onClick(FileItem item) { onItemClick(item); }
            @Override public boolean onLongClick(FileItem item) { onToggleSelection(item); return true; }
            @Override public void onToggle(FileItem item) { onToggleSelection(item); }
        });
        recycler.setAdapter(adapter);

        btnCopy.setOnClickListener(v -> onCopy());
        btnMove.setOnClickListener(v -> onMove());
        btnRename.setOnClickListener(v -> onRename());
        btnDelete.setOnClickListener(v -> onDelete());
        btnExtract.setOnClickListener(v -> onExtract());
        btnMore.setOnClickListener(v -> onMore());

        storageRoots.clear();
        storageRoots.add(Environment.getExternalStorageDirectory());
        File storage = new File("/storage");
        File[] entries = storage.listFiles();
        if (entries != null) {
            for (File e : entries) {
                if (e.isDirectory() && !e.getName().equals("emulated")
                        && !e.getName().equals("self") && e.canRead()) {
                    storageRoots.add(e);
                }
            }
        }

        storageRoot = storageRoots.isEmpty()
                ? Environment.getExternalStorageDirectory()
                : storageRoots.get(0);
        currentDir = storageRoot;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (hasStorageAccess()) {
            loadDir(currentDir);
        } else {
            requestStorageAccess();
        }
    }

    private boolean hasStorageAccess() {
        if (Build.VERSION.SDK_INT >= 30) {
            return Environment.isExternalStorageManager();
        }
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestStorageAccess() {
        if (Build.VERSION.SDK_INT >= 30) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.file_browser)
                    .setMessage(R.string.need_all_files)
                    .setPositiveButton(R.string.ok, (d, w) -> {
                        Intent i = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                                Uri.parse("package:" + getPackageName()));
                        startActivity(i);
                    })
                    .setNegativeButton(android.R.string.cancel, (d, w) -> finish())
                    .show();
        } else {
            requestPermissions(new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            }, REQ_READ);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_READ) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadDir(currentDir);
            } else {
                finish();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_file_browser, menu);
        MenuItem hiddenItem = menu.findItem(R.id.action_hidden);
        hiddenItem.setChecked(showHidden);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView sv = (SearchView) searchItem.getActionView();
        if (sv != null) {
            sv.setQueryHint(getString(R.string.search_files_hint));
            sv.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override public boolean onQueryTextSubmit(String q) {
                    searchQuery = q == null ? "" : q.trim();
                    applyFilterAndSort();
                    return true;
                }
                @Override public boolean onQueryTextChange(String q) {
                    searchQuery = q == null ? "" : q.trim();
                    applyFilterAndSort();
                    return true;
                }
            });
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_storage) {
            showStoragePicker();
            return true;
        }
        if (id == R.id.action_sort) {
            showSortDialog();
            return true;
        }
        if (id == R.id.action_hidden) {
            showHidden = !showHidden;
            item.setChecked(showHidden);
            applyFilterAndSort();
            return true;
        }
        if (id == R.id.action_favorites) {
            showFavoritesDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showStoragePicker() {
        String[] labels = new String[storageRoots.size()];
        for (int i = 0; i < storageRoots.size(); i++) {
            File r = storageRoots.get(i);
            labels[i] = (i == 0)
                    ? getString(R.string.storage_internal) + " (" + r.getPath() + ")"
                    : getString(R.string.storage_sd) + " (" + r.getPath() + ")";
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.file_browser)
                .setItems(labels, (d, which) -> {
                    storageRoot = storageRoots.get(which);
                    currentDir = storageRoot;
                    loadDir(currentDir);
                })
                .show();
    }

    private void loadDir(File dir) {
        currentDir = dir;
        if (getSupportActionBar() != null) {
            String title = dir.equals(storageRoot)
                    ? getString(R.string.storage_internal) : dir.getName();
            getSupportActionBar().setTitle(title);
        }
        allChildren.clear();
        File[] children = dir.listFiles();
        if (children != null) allChildren.addAll(Arrays.asList(children));
        applyFilterAndSort();
    }

    private void applyFilterAndSort() {
        files.clear();
        List<File> list = new ArrayList<>();
        for (File f : allChildren) {
            if (f.isHidden() && !showHidden) continue;
            if (!searchQuery.isEmpty()
                    && !f.getName().toLowerCase().contains(searchQuery.toLowerCase())) continue;
            list.add(f);
        }
        sortList(list);
        for (File f : list) files.add(new FileItem(f));
        adapter.notifyDataSetChanged();
        emptyView.setVisibility(files.isEmpty() ? View.VISIBLE : View.GONE);
        recycler.setVisibility(files.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void sortList(List<File> list) {
        Comparator<File> cmp;
        switch (sortMode) {
            case 1: cmp = Comparator.comparingLong(File::length); break;
            case 2: cmp = Comparator.comparingLong(File::lastModified); break;
            case 3: cmp = Comparator.comparing(f -> extension(f.getName())); break;
            default: cmp = Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER);
        }
        list.sort((a, b) -> {
            if (a.isDirectory() != b.isDirectory()) return a.isDirectory() ? -1 : 1;
            int c = cmp.compare(a, b);
            return sortAsc ? c : -c;
        });
    }

    private String extension(String name) {
        int i = name.lastIndexOf('.');
        return i < 0 ? "" : name.substring(i + 1).toLowerCase(Locale.ROOT);
    }

    private void goUpOrFinish() {
        if (selectionMode) { exitSelectionMode(); return; }
        File parent = currentDir.getParentFile();
        boolean aboveRoot = currentDir.getPath().length() > storageRoot.getPath().length();
        if (parent != null && aboveRoot && parent.canRead()) {
            loadDir(parent);
        } else {
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        if (selectionMode) { exitSelectionMode(); return; }
        super.onBackPressed();
    }

    private void onItemClick(FileItem item) {
        if (selectionMode) { toggleSelection(item); return; }
        if (item.isDirectory) loadDir(item.file);
        else openFile(item.file);
    }

    private void onToggleSelection(FileItem item) {
        if (!selectionMode) enterSelectionMode(item);
        else toggleSelection(item);
    }

    private void enterSelectionMode(FileItem first) {
        selectionMode = true;
        selected.clear();
        if (first != null) selected.add(first);
        adapter.setSelected(selected);
        updateSelectionUi();
    }

    private void exitSelectionMode() {
        selectionMode = false;
        selected.clear();
        adapter.setSelected(selected);
        updateSelectionUi();
    }

    private void toggleSelection(FileItem item) {
        if (selected.contains(item)) selected.remove(item);
        else selected.add(item);
        if (selected.isEmpty()) { exitSelectionMode(); return; }
        adapter.setSelected(selected);
        updateSelectionUi();
    }

    private void updateSelectionUi() {
        int n = selected.size();
        boolean one = n == 1;
        boolean oneArchive = one && ArchiveExtractor.isSupported(selected.iterator().next().name);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(getString(R.string.selected_count, n));
        }
        boolean show = selectionMode;
        actionBar.setVisibility(show ? View.VISIBLE : View.GONE);
        if (!show) return;
        btnCopy.setEnabled(n > 0);
        btnMove.setEnabled(n > 0);
        btnRename.setEnabled(one);
        btnDelete.setEnabled(n > 0);
        btnExtract.setEnabled(oneArchive);
        btnMore.setEnabled(n > 0);
    }

    private void onCopy() {
        startCopyOrMove(false);
    }

    private void onMove() {
        startCopyOrMove(true);
    }

    private void startCopyOrMove(boolean move) {
        List<FileItem> items = new ArrayList<>(selected);
        FolderPickerDialog.show(this, currentDir, dest -> {
            for (FileItem it : items) {
                if (it.file.isDirectory()
                        && dest.getAbsolutePath().startsWith(it.file.getAbsolutePath())) {
                    Toast.makeText(this, "Ziel liegt innerhalb der Quelle", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            runBatch(items, dest, move);
        });
    }

    private void runBatch(List<FileItem> items, File dest, boolean move) {
        new Thread(() -> {
            int total = 0;
            for (FileItem it : items) total += countFiles(it.file);
            final int fTotal = Math.max(1, total);
            mainHandler.post(() -> {
                ProgressDialog pd = new ProgressDialog(this,
                        move ? getString(R.string.move) : getString(R.string.copy), fTotal);
                pd.show();
                new Thread(() -> {
                    int[] counter = new int[1];
                    boolean cancelled = false;
                    try {
                        for (FileItem it : items) {
                            if (pd.isCancelled()) { cancelled = true; break; }
                            if (move) moveRecursiveProg(it.file, dest, pd, counter);
                            else copyRecursiveProg(it.file, dest, pd, counter);
                        }
                    } catch (Exception e) {
                        final String m = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
                        mainHandler.post(() -> {
                            pd.dismiss();
                            Toast.makeText(this, getString(R.string.error) + ": " + m, Toast.LENGTH_LONG).show();
                        });
                        return;
                    }
                    final boolean fCancelled = cancelled;
                    mainHandler.post(() -> {
                        pd.dismiss();
                        Toast.makeText(this,
                                fCancelled ? R.string.cancel : (move ? R.string.move_success : R.string.copy_success),
                                Toast.LENGTH_SHORT).show();
                        exitSelectionMode();
                        loadDir(currentDir);
                    });
                }).start();
            });
        }).start();
    }

    private int countFiles(File f) {
        if (!f.isDirectory()) return 1;
        File[] c = f.listFiles();
        int n = 0;
        if (c != null) for (File x : c) n += countFiles(x);
        return n;
    }

    private void onRename() {
        if (selected.size() != 1) return;
        rename(selected.iterator().next());
    }

    private void onDelete() {
        List<FileItem> items = new ArrayList<>(selected);
        String label = items.size() == 1 ? items.get(0).name : items.size() + " Elemente";
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete)
                .setMessage(getString(R.string.delete_confirm, label))
                .setPositiveButton(R.string.delete, (d, w) -> runFileOp(() -> {
                    for (FileItem it : items) {
                        boolean ok = it.isDirectory ? deleteRecursive(it.file) : it.file.delete();
                        if (!ok) throw new IOException("Löschen fehlgeschlagen: " + it.name);
                    }
                    return getString(R.string.delete);
                }))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void onExtract() {
        if (selected.size() != 1) return;
        FileItem item = selected.iterator().next();
        ArchiveExtractor.extract(item.file, new ArchiveExtractor.Callback() {
            @Override public void onSuccess(String outDir) {
                Toast.makeText(FileBrowserActivity.this,
                        getString(R.string.extract_success) + " " + outDir, Toast.LENGTH_SHORT).show();
                exitSelectionMode();
                loadDir(currentDir);
            }
            @Override public void onError(String message) {
                Toast.makeText(FileBrowserActivity.this,
                        getString(R.string.error) + ": " + message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showSortDialog() {
        String[] modes = { getString(R.string.sort_name), getString(R.string.sort_size),
                getString(R.string.sort_date), getString(R.string.sort_type) };
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.sort)
                .setSingleChoiceItems(modes, sortMode, (d, which) -> {
                    sortMode = which;
                    d.dismiss();
                    showSortDirectionDialog();
                })
                .show();
    }

    private void showSortDirectionDialog() {
        String[] dirs = { getString(R.string.sort_asc), getString(R.string.sort_desc) };
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.sort)
                .setSingleChoiceItems(dirs, sortAsc ? 0 : 1, (d, which) -> {
                    sortAsc = which == 0;
                    d.dismiss();
                    applyFilterAndSort();
                })
                .show();
    }

    private Set<String> getFavorites() {
        return new LinkedHashSet<>(prefs.getStringSet("favorites", new LinkedHashSet<>()));
    }

    private void saveFavorites(Set<String> set) {
        prefs.edit().putStringSet("favorites", set).apply();
    }

    private void showFavoritesDialog() {
        final List<String> favList = new ArrayList<>(getFavorites());
        String[] labels = new String[favList.size() + 1];
        for (int i = 0; i < favList.size(); i++) {
            labels[i] = new File(favList.get(i)).getName() + "  (" + favList.get(i) + ")";
        }
        labels[favList.size()] = "+ " + getString(R.string.add_favorite);
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.favorites)
                .setItems(labels, (d, which) -> {
                    if (which == favList.size()) {
                        addCurrentToFavorites();
                    } else {
                        File f = new File(favList.get(which));
                        if (f.isDirectory() && f.canRead()) {
                            currentDir = f;
                            loadDir(f);
                        } else {
                            Toast.makeText(this, R.string.error, Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .show();
    }

    private void addCurrentToFavorites() {
        Set<String> favs = getFavorites();
        favs.add(currentDir.getAbsolutePath());
        saveFavorites(favs);
        Toast.makeText(this, R.string.favorite_added, Toast.LENGTH_SHORT).show();
    }

    private void onCompress() {
        final List<FileItem> items = new ArrayList<>(selected);
        final EditText input = new EditText(this);
        input.setHint(R.string.compress_hint);
        input.setText(getString(R.string.compress_hint));
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.compress)
                .setView(input)
                .setPositiveButton(R.string.ok, (d, w) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) name = getString(R.string.compress_hint);
                    String lower = name.toLowerCase();
                    if (!lower.endsWith(".zip") && !lower.endsWith(".tar")
                            && !lower.endsWith(".tar.gz") && !lower.endsWith(".tgz")) {
                        name += ".zip";
                    }
                    File dest = new File(currentDir, name);
                    if (dest.exists()) {
                        Toast.makeText(this, R.string.error, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    List<File> sources = new ArrayList<>();
                    for (FileItem it : items) sources.add(it.file);
                    ArchiveCreator.create(sources, dest, new ArchiveCreator.Callback() {
                        @Override public void onSuccess(String path) {
                            Toast.makeText(FileBrowserActivity.this,
                                    getString(R.string.compress_success) + " " + path,
                                    Toast.LENGTH_SHORT).show();
                            exitSelectionMode();
                            loadDir(currentDir);
                        }
                        @Override public void onError(String message) {
                            Toast.makeText(FileBrowserActivity.this,
                                    getString(R.string.error) + ": " + message,
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void onEncrypt() {
        final List<FileItem> items = new ArrayList<>(selected);
        final EditText input = new EditText(this);
        input.setHint(R.string.password);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.encrypt)
                .setView(input)
                .setPositiveButton(R.string.ok, (d, w) -> {
                    final String pw = input.getText().toString();
                    if (pw.isEmpty()) {
                        Toast.makeText(this, R.string.password_required, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    new Thread(() -> {
                        int ok = 0;
                        int fail = 0;
                        for (FileItem it : items) {
                            if (it.isDirectory) continue;
                            try {
                                FileCrypto.encrypt(it.file, pw);
                                ok++;
                            } catch (Exception e) {
                                fail++;
                            }
                        }
                        final int fok = ok;
                        final int ffail = fail;
                        mainHandler.post(() -> {
                            Toast.makeText(this, getString(R.string.encrypt_success) + " " + fok
                                    + (ffail > 0 ? " (" + ffail + " übersprungen)" : ""),
                                    Toast.LENGTH_LONG).show();
                            exitSelectionMode();
                            loadDir(currentDir);
                        });
                    }).start();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void onDecrypt() {
        if (selected.size() != 1) return;
        final FileItem item = selected.iterator().next();
        final EditText input = new EditText(this);
        input.setHint(R.string.password);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.decrypt)
                .setView(input)
                .setPositiveButton(R.string.ok, (d, w) -> {
                    final String pw = input.getText().toString();
                    if (pw.isEmpty()) {
                        Toast.makeText(this, R.string.password_required, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    new Thread(() -> {
                        try {
                            FileCrypto.decrypt(item.file, pw);
                            mainHandler.post(() -> {
                                Toast.makeText(this, R.string.decrypt_success, Toast.LENGTH_SHORT).show();
                                exitSelectionMode();
                                loadDir(currentDir);
                            });
                        } catch (Exception e) {
                            mainHandler.post(() -> Toast.makeText(this,
                                    R.string.decrypt_failed, Toast.LENGTH_LONG).show());
                        }
                    }).start();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void onMore() {
        if (selected.size() == 1) {
            final FileItem item = selected.iterator().next();
            List<String> labels = new ArrayList<>();
            List<Runnable> actions = new ArrayList<>();
            if (item.isDirectory) {
                labels.add(getString(R.string.compress));
                actions.add(this::onCompress);
            } else if (FileCrypto.isEncrypted(item.name)) {
                labels.add(getString(R.string.decrypt));
                actions.add(this::onDecrypt);
            } else {
                labels.add(getString(R.string.encrypt));
                actions.add(this::onEncrypt);
                labels.add(getString(R.string.compress));
                actions.add(this::onCompress);
            }
            labels.add(getString(R.string.share));
            actions.add(() -> share(item.file));
            labels.add(getString(R.string.details));
            actions.add(() -> showDetails(item));
            final List<Runnable> fActions = actions;
            new MaterialAlertDialogBuilder(this)
                    .setTitle(item.name)
                    .setItems(labels.toArray(new String[0]), (d, w) -> fActions.get(w).run())
                    .show();
        } else {
            String[] opts = { getString(R.string.encrypt), getString(R.string.compress),
                    getString(R.string.select_all), getString(R.string.deselect_all) };
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.more)
                    .setItems(opts, (d, w) -> {
                        if (w == 0) onEncrypt();
                        else if (w == 1) onCompress();
                        else {
                            if (w == 2) { selected.clear(); selected.addAll(files); }
                            else { selected.clear(); }
                            adapter.setSelected(selected);
                            updateSelectionUi();
                        }
                    })
                    .show();
        }
    }

    private interface FileOp {
        String run() throws Exception;
    }

    private void runFileOp(FileOp op) {
        new Thread(() -> {
            try {
                String msg = op.run();
                mainHandler.post(() -> {
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                    exitSelectionMode();
                    loadDir(currentDir);
                });
            } catch (Exception e) {
                String m = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
                mainHandler.post(() -> Toast.makeText(this,
                        getString(R.string.error) + ": " + m, Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void copyRecursiveProg(File src, File dstDir, ProgressDialog pd, int[] counter) throws IOException {
        if (pd.isCancelled()) return;
        if (src.isDirectory()) {
            File target = new File(dstDir, src.getName());
            if (target.getAbsolutePath().equals(src.getAbsolutePath())) return;
            target.mkdirs();
            File[] c = src.listFiles();
            if (c != null) for (File x : c) copyRecursiveProg(x, target, pd, counter);
        } else {
            copyFile(src, new File(dstDir, src.getName()));
            counter[0]++;
            pd.update(counter[0], src.getName());
        }
    }

    private void copyFile(File src, File dst) throws IOException {
        try (InputStream in = new FileInputStream(src);
             OutputStream out = new FileOutputStream(dst)) {
            byte[] buf = new byte[64 * 1024];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        }
    }

    private void moveRecursiveProg(File src, File dstDir, ProgressDialog pd, int[] counter) throws IOException {
        File target = new File(dstDir, src.getName());
        if (src.renameTo(target)) return;
        copyRecursiveProg(src, dstDir, pd, counter);
        if (!pd.isCancelled()) deleteRecursive(src);
    }

    private void openFile(File file) {
        String name = file.getName().toLowerCase();
        if (isImage(name) || isVideo(name)) {
            openMedia(file);
            return;
        }
        try {
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setDataAndType(uri, guessMime(file.getName()));
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(i);
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.error) + ": " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void openMedia(File file) {
        ArrayList<String> media = new ArrayList<>();
        int idx = -1;
        for (FileItem it : files) {
            if (it.isDirectory) continue;
            String n = it.name.toLowerCase();
            if (isImage(n) || isVideo(n)) {
                media.add(it.file.getAbsolutePath());
                if (it.file.getAbsolutePath().equals(file.getAbsolutePath())) {
                    idx = media.size() - 1;
                }
            }
        }
        if (media.isEmpty()) {
            media.add(file.getAbsolutePath());
            idx = 0;
        }
        if (idx < 0) idx = 0;
        Intent i = new Intent(this, MediaViewerActivity.class);
        i.putStringArrayListExtra("paths", media);
        i.putExtra("index", idx);
        startActivity(i);
    }

    private boolean isImage(String name) {
        return name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png")
                || name.endsWith(".gif") || name.endsWith(".webp") || name.endsWith(".bmp");
    }

    private boolean isVideo(String name) {
        return name.endsWith(".mp4") || name.endsWith(".mkv") || name.endsWith(".webm")
                || name.endsWith(".avi") || name.endsWith(".mov") || name.endsWith(".3gp");
    }

    private void share(File file) {
        try {
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
            Intent send = new Intent(Intent.ACTION_SEND);
            send.setType(guessMime(file.getName()));
            send.putExtra(Intent.EXTRA_STREAM, uri);
            send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(send, getString(R.string.share)));
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.error) + ": " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void rename(FileItem item) {
        final EditText input = new EditText(this);
        input.setText(item.name);
        int dot = item.name.lastIndexOf('.');
        input.setSelection(dot > 0 ? dot : item.name.length());
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.rename)
                .setView(input)
                .setPositiveButton(R.string.ok, (d, w) -> {
                    String newName = input.getText().toString().trim();
                    if (newName.isEmpty() || newName.equals(item.name)) return;
                    File dst = new File(item.file.getParentFile(), newName);
                    if (item.file.renameTo(dst)) {
                        exitSelectionMode();
                        loadDir(currentDir);
                    } else {
                        Toast.makeText(this, R.string.error, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private boolean deleteRecursive(File f) {
        if (f.isDirectory()) {
            File[] c = f.listFiles();
            if (c != null) {
                for (File x : c) deleteRecursive(x);
            }
        }
        return f.delete();
    }

    private void showDetails(FileItem item) {
        SimpleDateFormat fmt = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
        String info = "Name: " + item.name
                + "\nPfad: " + item.file.getAbsolutePath()
                + "\nTyp: " + (item.isDirectory ? "Ordner" : guessMime(item.name))
                + "\nGröße: " + formatSize(item.size)
                + "\nGeändert: " + fmt.format(new Date(item.lastModified));
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.details)
                .setMessage(info)
                .setPositiveButton(R.string.ok, null)
                .show();
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        double kb = bytes / 1024.0;
        if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb);
        double mb = kb / 1024.0;
        if (mb < 1024) return String.format(Locale.US, "%.1f MB", mb);
        return String.format(Locale.US, "%.2f GB", mb / 1024.0);
    }

    private String guessMime(String name) {
        String l = name.toLowerCase();
        if (l.endsWith(".apk")) return "application/vnd.android.package-archive";
        if (l.endsWith(".jpg") || l.endsWith(".jpeg")) return "image/jpeg";
        if (l.endsWith(".png")) return "image/png";
        if (l.endsWith(".gif")) return "image/gif";
        if (l.endsWith(".webp")) return "image/webp";
        if (l.endsWith(".mp4")) return "video/mp4";
        if (l.endsWith(".mkv")) return "video/x-matroska";
        if (l.endsWith(".mp3")) return "audio/mpeg";
        if (l.endsWith(".m4a")) return "audio/mp4";
        if (l.endsWith(".pdf")) return "application/pdf";
        if (l.endsWith(".txt")) return "text/plain";
        if (l.endsWith(".zip")) return "application/zip";
        return "*/*";
    }
}
