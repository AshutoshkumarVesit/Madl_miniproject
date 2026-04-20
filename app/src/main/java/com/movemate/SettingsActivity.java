package com.movemate;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.movemate.notification.NotificationHelper;

public class SettingsActivity extends AppCompatActivity {
    private static final String PREFS = "settings_prefs";
    private static final String KEY_DARK = "dark_mode";
    private static final String KEY_UNITS = "units_km";
    private static final String KEY_NOTIFY = "notify";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        NotificationHelper.createChannels(this);

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);

        Switch darkSwitch = findViewById(R.id.switchDark);
        Switch unitSwitch = findViewById(R.id.switchUnits);
        Switch notifySwitch = findViewById(R.id.switchNotify);
        TextView editProfile = findViewById(R.id.editProfileText);

        darkSwitch.setChecked(prefs.getBoolean(KEY_DARK, true));
        unitSwitch.setChecked(prefs.getBoolean(KEY_UNITS, true));
        notifySwitch.setChecked(prefs.getBoolean(KEY_NOTIFY, true));

        darkSwitch.setOnCheckedChangeListener((b, checked) -> {
            prefs.edit().putBoolean(KEY_DARK, checked).apply();
            AppCompatDelegate.setDefaultNightMode(checked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        });
        unitSwitch.setOnCheckedChangeListener((b, checked) -> prefs.edit().putBoolean(KEY_UNITS, checked).apply());
        notifySwitch.setOnCheckedChangeListener((b, checked) -> {
            prefs.edit().putBoolean(KEY_NOTIFY, checked).apply();
            if (checked) {
                NotificationHelper.scheduleAllWorkers(this);
            } else {
                NotificationHelper.cancelAllWorkers(this);
            }
        });

        editProfile.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_profile);
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_home) { startActivity(new Intent(this, DashboardActivity.class)); return true; }
                if (id == R.id.nav_stats) { startActivity(new Intent(this, StatsActivity.class)); return true; }
                if (id == R.id.nav_run) { startActivity(new Intent(this, MainActivity.class)); return true; }
                if (id == R.id.nav_community) { startActivity(new Intent(this, CommunityActivity.class)); return true; }
                if (id == R.id.nav_profile) return true;
                return false;
            });
        }
    }
}


