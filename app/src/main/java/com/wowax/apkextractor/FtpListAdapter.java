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

public class FtpListAdapter extends RecyclerView.Adapter<FtpListAdapter.VH> {

    public interface Listener {
        void onClick(FtpItem item);
        void onToggle(FtpItem item);
    }

    private final List<FtpItem> items;
    private final Listener listener;
    private final Set<FtpItem> selected = new HashSet<>();

    public FtpListAdapter(List<FtpItem> items, Listener listener) {
        this.items = items;
        this.listener = listener;
    }

    public void setSelected(Set<FtpItem> set) {
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
        FtpItem item = items.get(position);
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

        h.checkbox.setVisibility(View.VISIBLE);
        h.checkbox.setChecked(selected.contains(item));
        h.checkbox.setOnClickListener(v -> listener.onToggle(item));
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

        VH(View itemView) {
            super(itemView);
            checkbox = itemView.findViewById(R.id.item_checkbox);
            icon = itemView.findViewById(R.id.file_icon);
            name = itemView.findViewById(R.id.file_name);
            meta = itemView.findViewById(R.id.file_meta);
        }
    }
}
