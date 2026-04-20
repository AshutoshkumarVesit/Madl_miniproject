package com.movemate.notification;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.movemate.DBHelper;
import com.movemate.RunModel;

import java.util.List;

/**
 * Periodic worker that checks if weekly or monthly distance goals have been reached
 * and sends a congratulatory notification.
 */
public class GoalCheckWorker extends Worker {

    private static final String GOALS_PREFS = "goals_prefs";
    private static final String WEEKLY_KEY = "weekly_goal";
    private static final String MONTHLY_KEY = "monthly_goal";

    private static final String NOTIFIED_PREFS = "goal_notified_prefs";
    private static final String KEY_WEEKLY_NOTIFIED = "weekly_notified";
    private static final String KEY_MONTHLY_NOTIFIED = "monthly_notified";

    public GoalCheckWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();

        if (!NotificationHelper.areNotificationsEnabled(context)) {
            return Result.success();
        }

        NotificationHelper.createChannels(context);

        SharedPreferences goalPrefs = context.getSharedPreferences(GOALS_PREFS, Context.MODE_PRIVATE);
        SharedPreferences notifiedPrefs = context.getSharedPreferences(NOTIFIED_PREFS, Context.MODE_PRIVATE);

        float weeklyGoal = goalPrefs.getFloat(WEEKLY_KEY, 20f);
        float monthlyGoal = goalPrefs.getFloat(MONTHLY_KEY, 80f);

        DBHelper dbHelper = new DBHelper(context);
        List<RunModel> runs = dbHelper.getRuns();
        float totalDistance = 0;
        for (RunModel run : runs) {
            totalDistance += run.getDistanceKm();
        }

        // Check weekly goal
        float lastNotifiedWeekly = notifiedPrefs.getFloat(KEY_WEEKLY_NOTIFIED, -1f);
        if (totalDistance >= weeklyGoal && lastNotifiedWeekly < weeklyGoal) {
            NotificationHelper.notify(context, NotificationHelper.ID_GOAL_ACHIEVED,
                    NotificationHelper.buildGoalAchieved(context, "Weekly",
                            String.format("%.1f km", totalDistance)));
            notifiedPrefs.edit().putFloat(KEY_WEEKLY_NOTIFIED, weeklyGoal).apply();
        }

        // Check monthly goal
        float lastNotifiedMonthly = notifiedPrefs.getFloat(KEY_MONTHLY_NOTIFIED, -1f);
        if (totalDistance >= monthlyGoal && lastNotifiedMonthly < monthlyGoal) {
            NotificationHelper.notify(context, NotificationHelper.ID_GOAL_ACHIEVED + 1,
                    NotificationHelper.buildGoalAchieved(context, "Monthly",
                            String.format("%.1f km", totalDistance)));
            notifiedPrefs.edit().putFloat(KEY_MONTHLY_NOTIFIED, monthlyGoal).apply();
        }

        return Result.success();
    }
}
