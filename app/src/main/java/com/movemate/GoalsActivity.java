package com.movemate;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.movemate.notification.GoalCheckWorker;
import com.movemate.notification.NotificationHelper;

import java.util.List;

public class GoalsActivity extends AppCompatActivity {
    private static final String PREFS = "goals_prefs";
    private static final String WEEKLY_KEY = "weekly_goal";
    private static final String MONTHLY_KEY = "monthly_goal";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_goals);

        EditText weeklyInput = findViewById(R.id.weeklyGoalInput);
        EditText monthlyInput = findViewById(R.id.monthlyGoalInput);
        Button saveButton = findViewById(R.id.saveGoalButton);
        TextView weeklyProgressText = findViewById(R.id.weeklyProgressText);
        TextView monthlyProgressText = findViewById(R.id.monthlyProgressText);
        ProgressBar weeklyBar = findViewById(R.id.weeklyProgressBar);
        ProgressBar monthlyBar = findViewById(R.id.monthlyProgressBar);

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        float weeklyGoal = prefs.getFloat(WEEKLY_KEY, 20f);
        float monthlyGoal = prefs.getFloat(MONTHLY_KEY, 80f);
        weeklyInput.setText(String.valueOf((int) weeklyGoal));
        monthlyInput.setText(String.valueOf((int) monthlyGoal));

        DBHelper dbHelper = new DBHelper(this);
        List<RunModel> runs = dbHelper.getRuns();
        float totalDistance = 0;
        for (RunModel run : runs) totalDistance += run.getDistanceKm();
        final float totalDistanceVal = totalDistance;
        float weeklyProgress = Math.min(totalDistanceVal, weeklyGoal);
        float monthlyProgress = Math.min(totalDistanceVal, monthlyGoal);

        weeklyBar.setMax((int) weeklyGoal);
        weeklyBar.setProgress((int) weeklyProgress);
        monthlyBar.setMax((int) monthlyGoal);
        monthlyBar.setProgress((int) monthlyProgress);
        weeklyProgressText.setText(String.format("%.1f / %.1f km", weeklyProgress, weeklyGoal));
        monthlyProgressText.setText(String.format("%.1f / %.1f km", monthlyProgress, monthlyGoal));

        saveButton.setOnClickListener(v -> {
            float w = parseFloatSafe(weeklyInput.getText().toString(), weeklyGoal);
            float m = parseFloatSafe(monthlyInput.getText().toString(), monthlyGoal);
            prefs.edit().putFloat(WEEKLY_KEY, w).putFloat(MONTHLY_KEY, m).apply();
            weeklyBar.setMax((int) w);
            monthlyBar.setMax((int) m);
            weeklyBar.setProgress((int) Math.min(totalDistanceVal, w));
            monthlyBar.setProgress((int) Math.min(totalDistanceVal, m));
            weeklyProgressText.setText(String.format("%.1f / %.1f km", Math.min(totalDistanceVal, w), w));
            monthlyProgressText.setText(String.format("%.1f / %.1f km", Math.min(totalDistanceVal, m), m));

            // Trigger immediate goal check for notifications
            if (NotificationHelper.areNotificationsEnabled(GoalsActivity.this)) {
                WorkManager.getInstance(GoalsActivity.this)
                        .enqueue(new OneTimeWorkRequest.Builder(GoalCheckWorker.class).build());
            }
        });

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_stats);
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_home) {
                    startActivity(new android.content.Intent(this, DashboardActivity.class));
                    return true;
                }
                if (id == R.id.nav_stats) return true;
                if (id == R.id.nav_run) {
                    startActivity(new android.content.Intent(this, MainActivity.class));
                    return true;
                }
                if (id == R.id.nav_community) {
                    startActivity(new android.content.Intent(this, CommunityActivity.class));
                    return true;
                }
                if (id == R.id.nav_profile) {
                    startActivity(new android.content.Intent(this, ProfileActivity.class));
                    return true;
                }
                return false;
            });
        }
    }

    private float parseFloatSafe(String input, float fallback) {
        try {
            return Float.parseFloat(input);
        } catch (Exception e) {
            return fallback;
        }
    }
}
