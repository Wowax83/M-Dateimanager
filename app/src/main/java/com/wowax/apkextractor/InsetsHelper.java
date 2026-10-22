package com.wowax.apkextractor;

import android.app.Activity;
import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Korrigiert das Layout unter targetSdk 35+ (Edge-to-Edge-Enforcement):
 * Inhalte werden unter die Statusleiste bzw. über die Navigationsleiste
 * geschoben, sodass Toolbar und untere Leisten nicht mehr überlagert werden.
 */
public class InsetsHelper {

    public static void applySystemBars(Activity activity) {
        View content = activity.findViewById(android.R.id.content);
        if (content == null) return;
        ViewCompat.setOnApplyWindowInsetsListener(content, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
    }
}
