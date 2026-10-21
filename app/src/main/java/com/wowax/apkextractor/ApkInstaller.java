package com.wowax.apkextractor;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class ApkInstaller {

    public interface Callback {
        void onStart();
        void onError(String message);
    }

    private final Context context;
    private final List<File> files; // base first, then splits
    private final Callback callback;
    private final Handler main = new Handler(Looper.getMainLooper());

    public ApkInstaller(Context context, List<File> files, Callback callback) {
        this.context = context;
        this.files = files;
        this.callback = callback;
    }

    public void start() {
        new Thread(() -> {
            try {
                PackageInstaller installer = context.getPackageManager().getPackageInstaller();
                PackageInstaller.SessionParams params =
                        new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
                int sessionId = installer.createSession(params);
                try (PackageInstaller.Session session = installer.openSession(sessionId)) {
                    int index = 0;
                    for (File f : files) {
                        String writeName = (index == 0) ? "base.apk" : f.getName();
                        try (InputStream in = new FileInputStream(f);
                             OutputStream out = session.openWrite(writeName, 0, f.length())) {
                            byte[] buf = new byte[64 * 1024];
                            int n;
                            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                        }
                        index++;
                    }
                    Intent receiverIntent = new Intent(context, InstallResultReceiver.class);
                    PendingIntent pending = PendingIntent.getBroadcast(
                            context, sessionId, receiverIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                    session.commit(pending.getIntentSender());
                }
                main.post(callback::onStart);
            } catch (Exception e) {
                String m = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
                main.post(() -> callback.onError(m));
            }
        }).start();
    }
}
