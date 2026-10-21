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

public class FileListAdapter extends RecyclerView.Adapter<FileListAdapter.ViewHolder> {

    public interface Listener {
        void onClick(FileItem item);
        boolean onLongClick(FileItem item);
        void onToggle(FileItem item);
    }

    private final List<FileItem> items;
    private final Listener listener;
    private Set<FileItem> selected = new HashSet<>();

    public FileListAdapter(List<FileItem> items, Listener listener) {
        this.items = items;
        this.listener = listener;
    }

    public void setSelected(Set<FileItem> set) {
        selected = set;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_file, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FileItem item = items.get(position);
        holder.name.setText(item.name);

        if (item.isDirectory) {
            holder.icon.setImageResource(R.drawable.ic_type_folder);
            holder.meta.setText("Ordner");
        } else {
            holder.icon.setImageResource(FileTypeIcon.iconFor(item.name));
            holder.meta.setText(formatSize(item.size));
        }

        holder.checkbox.setVisibility(View.VISIBLE);
        holder.checkbox.setChecked(selected.contains(item));
        holder.checkbox.setOnClickListener(v -> listener.onToggle(item));

        holder.itemView.setOnClickListener(v -> listener.onClick(item));
        holder.itemView.setOnLongClickListener(v -> listener.onLongClick(item));
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

    static class ViewHolder extends RecyclerView.ViewHolder {
        final CheckBox checkbox;
        final ImageView icon;
        final TextView name;
        final TextView meta;

        ViewHolder(View itemView) {
            super(itemView);
            checkbox = itemView.findViewById(R.id.item_checkbox);
            icon = itemView.findViewById(R.id.file_icon);
            name = itemView.findViewById(R.id.file_name);
            meta = itemView.findViewById(R.id.file_meta);
        }
    }
}
