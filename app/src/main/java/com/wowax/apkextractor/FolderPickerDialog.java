package com.wowax.apkextractor;

import android.app.Activity;
import android.app.AlertDialog;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class FolderPickerDialog {

    public interface OnFolderPicked {
        void onPicked(File folder);
    }

    public static void show(Activity activity, File start, OnFolderPicked callback) {
        final File[] current = { start };
        final List<File> dirs = new ArrayList<>();
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(
                activity, android.R.layout.simple_list_item_1);

        ListView listView = new ListView(activity);
        listView.setAdapter(adapter);

        final AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(start.getPath())
                .setView(listView)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(activity.getString(R.string.here),
                        (d, w) -> callback.onPicked(current[0]))
                .create();

        Runnable refresh = () -> {
            dirs.clear();
            File parent = current[0].getParentFile();
            boolean hasParent = parent != null && parent.canRead();
            if (hasParent) dirs.add(parent);
            File[] children = current[0].listFiles(File::isDirectory);
            if (children != null) {
                Arrays.sort(children,
                        Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER));
                dirs.addAll(Arrays.asList(children));
            }
            adapter.clear();
            for (int i = 0; i < dirs.size(); i++) {
                String label = (i == 0 && hasParent)
                        ? activity.getString(R.string.up_one) + " (" + dirs.get(i).getName() + ")"
                        : dirs.get(i).getName();
                adapter.add(label);
            }
            dialog.setTitle(current[0].getPath());
        };

        listView.setOnItemClickListener((parent, view, position, id) -> {
            current[0] = dirs.get(position);
            refresh.run();
        });

        refresh.run();
        dialog.show();
    }
}
