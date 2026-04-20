package com.movemate.notification;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.movemate.DashboardActivity;
import com.movemate.MainActivity;
import com.movemate.R;

import java.util.concurrent.TimeUnit;

/**
 * Central notification helper for MoveMate.
 * Creates channels, builds notifications, and manages WorkManager scheduling.
 */
public class NotificationHelper {

    // Channel IDs
    public static final String CHANNEL_REMINDERS = "movemate_reminders";
    public static final String CHANNEL_TRACKING = "movemate_tracking";
    public static final String CHANNEL_ACHIEVEMENTS = "movemate_achievements";

    // Notification IDs
    public static final int ID_DAILY_REMINDER = 1001;
    public static final int ID_RUN_TRACKING = 1002;
    public static final int ID_RUN_COMPLETED = 1003;
    public static final int ID_GOAL_ACHIEVED = 1004;
    public static final int ID_INACTIVITY_NUDGE = 1005;

    // WorkManager unique work names
    public static final String WORK_DAILY_REMINDER = "daily_run_reminder";
    public static final String WORK_GOAL_CHECK = "goal_check";

    private static final String SETTINGS_PREFS = "settings_prefs";
    private static final String KEY_NOTIFY = "notify";

    /**
     * Creates all notification channels. Safe to call multiple times.
     */
    public static void createChannels(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) return;

        NotificationChannel reminders = new NotificationChannel(
                CHANNEL_REMINDERS,
                "Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
        );
        reminders.setDescription("Daily run reminders and inactivity nudges");

        NotificationChannel tracking = new NotificationChannel(
                CHANNEL_TRACKING,
                "Run Tracking",
                NotificationManager.IMPORTANCE_LOW
        );
        tracking.setDescription("Persistent notification while a run is in progress");

        NotificationChannel achievements = new NotificationChannel(
                CHANNEL_ACHIEVEMENTS,
                "Achievements",
                NotificationManager.IMPORTANCE_HIGH
        );
        achievements.setDescription("Goal milestones and achievement notifications");

        manager.createNotificationChannel(reminders);
        manager.createNotificationChannel(tracking);
        manager.createNotificationChannel(achievements);
    }

    /**
     * Checks whether notifications are enabled in app settings.
     */
    public static boolean areNotificationsEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(SETTINGS_PREFS, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_NOTIFY, true);
    }

    /**
     * Sends a notification, respecting the user's notification preference.
     */
    public static void notify(Context context, int notificationId, Notification notification) {
        if (!areNotificationsEnabled(context)) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }
        NotificationManagerCompat.from(context).notify(notificationId, notification);
    }

    // ─── Notification Builders ──────────────────────────────────────────

    /**
     * Builds a daily run reminder notification.
     */
    public static Notification buildDailyReminder(Context context, String title, String message) {
        PendingIntent pi = PendingIntent.getActivity(context, 0,
                new Intent(context, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(context, CHANNEL_REMINDERS)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setContentIntent(pi)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build();
    }

    /**
     * Builds a foreground tracking notification (persistent during run).
     */
    public static Notification buildTrackingNotification(Context context, String distance, String duration) {
        PendingIntent pi = PendingIntent.getActivity(context, 0,
                new Intent(context, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(context, CHANNEL_TRACKING)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Run in progress")
                .setContentText(distance + "  •  " + duration)
                .setContentIntent(pi)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .build();
    }

    /**
     * Builds a run-completed notification with summary.
     */
    public static Notification buildRunCompleted(Context context, String distance, String duration, int calories) {
        PendingIntent pi = PendingIntent.getActivity(context, 0,
                new Intent(context, DashboardActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(context, CHANNEL_ACHIEVEMENTS)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Run Completed! \uD83C\uDFC3")
                .setContentText(distance + "  •  " + duration + "  •  " + calories + " kcal")
                .setContentIntent(pi)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build();
    }

    /**
     * Builds a goal-achieved notification.
     */
    public static Notification buildGoalAchieved(Context context, String goalType, String distance) {
        PendingIntent pi = PendingIntent.getActivity(context, 0,
                new Intent(context, DashboardActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(context, CHANNEL_ACHIEVEMENTS)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(goalType + " Goal Achieved! \uD83C\uDFC6")
                .setContentText("You've reached " + distance + " — keep going!")
                .setContentIntent(pi)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build();
    }

    /**
     * Builds an inactivity nudge notification.
     */
    public static Notification buildInactivityNudge(Context context) {
        PendingIntent pi = PendingIntent.getActivity(context, 0,
                new Intent(context, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(context, CHANNEL_REMINDERS)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("We miss you! \uD83D\uDC4B")
                .setContentText("You haven't logged a run recently. Time to lace up!")
                .setContentIntent(pi)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build();
    }

    // ─── WorkManager Scheduling ─────────────────────────────────────────

    /**
     * Schedules both the daily reminder and goal-check periodic workers.
     */
    public static void scheduleAllWorkers(Context context) {
        scheduleDailyReminder(context);
        scheduleGoalCheck(context);
    }

    /**
     * Cancels all scheduled notification workers.
     */
    public static void cancelAllWorkers(Context context) {
        WorkManager wm = WorkManager.getInstance(context);
        wm.cancelUniqueWork(WORK_DAILY_REMINDER);
        wm.cancelUniqueWork(WORK_GOAL_CHECK);
    }

    private static void scheduleDailyReminder(Context context) {
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                RunReminderWorker.class, 24, TimeUnit.HOURS)
                .setInitialDelay(1, TimeUnit.HOURS)
                .build();
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_DAILY_REMINDER,
                ExistingPeriodicWorkPolicy.KEEP,
                request
        );
    }

    private static void scheduleGoalCheck(Context context) {
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                GoalCheckWorker.class, 12, TimeUnit.HOURS)
                .setInitialDelay(30, TimeUnit.MINUTES)
                .build();
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_GOAL_CHECK,
                ExistingPeriodicWorkPolicy.KEEP,
                request
        );
    }
}
