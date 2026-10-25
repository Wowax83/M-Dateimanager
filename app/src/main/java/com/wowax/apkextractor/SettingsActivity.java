package com.wowax.apkextractor;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.RadioGroup;

import androidx.appcompat.widget.Toolbar;

public class SettingsActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        InsetsHelper.applySystemBars(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String current = prefs.getString(KEY_THEME, "system");

        RadioGroup group = findViewById(R.id.theme_group);
        int checkedId;
        if ("light".equals(current)) checkedId = R.id.theme_light;
        else if ("dark".equals(current)) checkedId = R.id.theme_dark;
        else checkedId = R.id.theme_system;
        group.check(checkedId);

        group.setOnCheckedChangeListener((g, id) -> {
            String theme;
            if (id == R.id.theme_light) theme = "light";
            else if (id == R.id.theme_dark) theme = "dark";
            else theme = "system";
            prefs.edit().putString(KEY_THEME, theme).apply();
            recreate();
        });
    }
}
