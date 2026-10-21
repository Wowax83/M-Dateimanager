package com.wowax.apkextractor;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class SmbListAdapter extends RecyclerView.Adapter<SmbListAdapter.VH> {

    public interface Listener {
        void onClick(SmbItem item);
        void onToggle(SmbItem item);
    }

    private final List<SmbItem> items;
    private final Listener listener;
    private final Set<SmbItem> selected = new HashSet<>();

    public SmbListAdapter(List<SmbItem> items, Listener listener) {
        this.items = items;
        this.listener = listener;
    }

    public void setSelected(Set<SmbItem> set) {
        selected.clear();
        selected.addAll(set);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_file, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        SmbItem item = items.get(position);
        h.name.setText(item.name);

        if (item.isDirectory || item.isParent) {
            h.icon.setImageResource(R.drawable.ic_type_folder);
            h.meta.setText(item.isParent
                    ? h.itemView.getResources().getString(R.string.smb_back)
                    : "Ordner");
        } else {
            h.icon.setImageResource(FileTypeIcon.iconFor(item.name));
            h.meta.setText(formatSize(item.size));
        }

        if (item.isParent) {
            h.checkbox.setVisibility(View.GONE);
        } else {
            h.checkbox.setVisibility(View.VISIBLE);
            h.checkbox.setChecked(selected.contains(item));
            h.checkbox.setOnClickListener(v -> listener.onToggle(item));
        }

        h.itemView.setOnClickListener(v -> listener.onClick(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        double kb = bytes / 1024.0;
        if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb);
        double mb = kb / 1024.0;
        if (mb < 1024) return String.format(Locale.US, "%.1f MB", mb);
        return String.format(Locale.US, "%.2f GB", mb / 1024.0);
    }

    static class VH extends RecyclerView.ViewHolder {
        final CheckBox checkbox;
        final ImageView icon;
        final TextView name;
        final TextView meta;

        VH(View v) {
            super(v);
            checkbox = v.findViewById(R.id.item_checkbox);
            icon = v.findViewById(R.id.file_icon);
            name = v.findViewById(R.id.file_name);
            meta = v.findViewById(R.id.file_meta);
        }
    }
}
