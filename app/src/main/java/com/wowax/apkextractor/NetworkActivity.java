package com.wowax.apkextractor;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class NetworkActivity extends AppCompatActivity {

    private final List<SmbServer> servers = new ArrayList<>();
    private ServerAdapter adapter;
    private RecyclerView recycler;
    private TextView emptyView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_network);
        InsetsHelper.applySystemBars(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        recycler = findViewById(R.id.recycler);
        emptyView = findViewById(R.id.empty_view);
        FloatingActionButton fab = findViewById(R.id.fab_add);

        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ServerAdapter(servers);
        recycler.setAdapter(adapter);

        fab.setOnClickListener(v -> showServerDialog(null));

        loadServers();
    }

    private void loadServers() {
        servers.clear();
        servers.addAll(SmbCredentialStore.load(this));
        adapter.notifyDataSetChanged();
        updateEmpty();
    }

    private void updateEmpty() {
        emptyView.setVisibility(servers.isEmpty() ? View.VISIBLE : View.GONE);
        recycler.setVisibility(servers.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void showServerDialog(SmbServer existing) {
        EditText name = new EditText(this);
        name.setHint(R.string.server_name);
        EditText host = new EditText(this);
        host.setHint(R.string.server_host);
        host.setInputType(InputType.TYPE_CLASS_TEXT);

        Spinner protocol = new Spinner(this);
        String[] protos = { getString(R.string.protocol_smb),
                getString(R.string.protocol_ftp), getString(R.string.protocol_sftp) };
        ArrayAdapter<String> pa = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, protos);
        pa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        protocol.setAdapter(pa);

        EditText port = new EditText(this);
        port.setHint(R.string.port);
        port.setInputType(InputType.TYPE_CLASS_NUMBER);

        EditText share = new EditText(this);
        share.setHint(R.string.server_share);
        EditText username = new EditText(this);
        username.setHint(R.string.server_username);
        EditText password = new EditText(this);
        password.setHint(R.string.server_password);
        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        EditText domain = new EditText(this);
        domain.setHint(R.string.server_domain);

        if (existing != null) {
            name.setText(existing.name);
            host.setText(existing.host);
            int pi = "ftp".equals(existing.protocol) ? 1
                    : "sftp".equals(existing.protocol) ? 2 : 0;
            protocol.setSelection(pi);
            port.setText(existing.port > 0 ? String.valueOf(existing.port) : "");
            share.setText(existing.share);
            username.setText(existing.username);
            password.setText(existing.password);
            domain.setText(existing.domain);
        }

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad / 2, pad, 0);
        layout.addView(name);
        layout.addView(host);
        layout.addView(protocol);
        layout.addView(port);
        layout.addView(share);
        layout.addView(username);
        layout.addView(password);
        layout.addView(domain);

        new MaterialAlertDialogBuilder(this)
                .setTitle(existing == null ? R.string.new_server : R.string.edit)
                .setView(layout)
                .setPositiveButton(R.string.save, (d, w) -> {
                    SmbServer s = existing == null ? new SmbServer() : existing;
                    s.name = name.getText().toString().trim();
                    s.host = host.getText().toString().trim();
                    int pi = protocol.getSelectedItemPosition();
                    s.protocol = (pi == 1) ? "ftp" : (pi == 2) ? "sftp" : "smb";
                    String portStr = port.getText().toString().trim();
                    s.port = portStr.isEmpty() ? 0 : Integer.parseInt(portStr);
                    s.share = share.getText().toString().trim();
                    s.username = username.getText().toString().trim();
                    s.password = password.getText().toString();
                    s.domain = domain.getText().toString().trim();
                    if (s.host.isEmpty()) {
                        Toast.makeText(this, "Server (IP) erforderlich", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (existing == null) servers.add(s);
                    SmbCredentialStore.save(this, servers);
                    loadServers();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void openBrowser(SmbServer s) {
        if ("ftp".equals(s.protocol) || "sftp".equals(s.protocol)) {
            Intent i = new Intent(this, FtpBrowserActivity.class);
            i.putExtra("protocol", s.protocol);
            i.putExtra("host", s.host);
            i.putExtra("port", s.effectivePort());
            i.putExtra("username", s.username);
            i.putExtra("password", s.password);
            startActivity(i);
        } else {
            Intent i = new Intent(this, SmbBrowserActivity.class);
            i.putExtra("name", s.name);
            i.putExtra("host", s.host);
            i.putExtra("share", s.share);
            i.putExtra("username", s.username);
            i.putExtra("password", s.password);
            i.putExtra("domain", s.domain);
            startActivity(i);
        }
    }

    private void showServerActions(SmbServer s) {
        String[] opts = { getString(R.string.edit), getString(R.string.delete) };
        new MaterialAlertDialogBuilder(this)
                .setTitle(s.name.isEmpty() ? s.host : s.name)
                .setItems(opts, (d, w) -> {
                    if (w == 0) {
                        showServerDialog(s);
                    } else {
                        servers.remove(s);
                        SmbCredentialStore.save(this, servers);
                        loadServers();
                    }
                })
                .show();
    }

    private class ServerAdapter extends RecyclerView.Adapter<ServerAdapter.VH> {
        private final List<SmbServer> data;

        ServerAdapter(List<SmbServer> data) {
            this.data = data;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_smb_server, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            SmbServer s = data.get(pos);
            h.name.setText(s.name.isEmpty() ? s.host : s.name);
            String scheme = s.protocol.isEmpty() ? "smb" : s.protocol;
            String sub = scheme + "://" + s.host;
            if ("smb".equals(scheme) && !s.share.isEmpty()) sub += "/" + s.share;
            h.sub.setText(sub);
            h.itemView.setOnClickListener(v -> openBrowser(s));
            h.itemView.setOnLongClickListener(v -> {
                showServerActions(s);
                return true;
            });
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        class VH extends RecyclerView.ViewHolder {
            final TextView name;
            final TextView sub;

            VH(View v) {
                super(v);
                name = v.findViewById(R.id.server_name);
                sub = v.findViewById(R.id.server_sub);
            }
        }
    }
}
