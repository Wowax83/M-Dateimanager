package com.wowax.apkextractor;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

public abstract class BaseActivity extends AppCompatActivity {

    public static final String PREFS = "settings";
    public static final String KEY_THEME = "theme";

    private int appliedNightMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyTheme();
        appliedNightMode = AppCompatDelegate.getDefaultNightMode();
        super.onCreate(savedInstanceState);
    }

    protected void applyTheme() {
        String theme = getSharedPreferences(PREFS, MODE_PRIVATE).getString(KEY_THEME, "system");
        if ("light".equals(theme)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else if ("dark".equals(theme)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (AppCompatDelegate.getDefaultNightMode() != appliedNightMode) {
            recreate();
        }
    }
}
