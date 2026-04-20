package com.movemate.notification;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.movemate.DBHelper;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Periodic worker that sends daily motivational reminders.
 * Also checks for inactivity (no runs in the last 2 days) and nudges the user.
 */
public class RunReminderWorker extends Worker {

    private static final String[] MOTIVATIONAL_MESSAGES = {
            "Every step counts — get out there today!",
            "Your best run is the next one. Let's go!",
            "Consistency beats intensity. Run today!",
            "Fresh air and endorphins await you!",
            "Set a new personal record today!",
            "Your goals won't achieve themselves — start running!",
            "A short run is better than no run. Let's move!"
    };

    public RunReminderWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
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

        if (hasRecentRuns(context)) {
            // User is active — send a motivational daily reminder
            String message = MOTIVATIONAL_MESSAGES[(int) (Math.random() * MOTIVATIONAL_MESSAGES.length)];
            NotificationHelper.notify(context, NotificationHelper.ID_DAILY_REMINDER,
                    NotificationHelper.buildDailyReminder(context, "Time to Run! \uD83C\uDFC3", message));
        } else {
            // User inactive for 2+ days — send inactivity nudge
            NotificationHelper.notify(context, NotificationHelper.ID_INACTIVITY_NUDGE,
                    NotificationHelper.buildInactivityNudge(context));
        }

        return Result.success();
    }

    /**
     * Returns true if there is at least one run in the last 2 days.
     */
    private boolean hasRecentRuns(Context context) {
        try {
            DBHelper dbHelper = new DBHelper(context);
            SQLiteDatabase db = dbHelper.getReadableDatabase();

            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_YEAR, -2);
            String twoDaysAgo = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.getTime());

            Cursor cursor = db.rawQuery(
                    "SELECT COUNT(*) FROM runs WHERE date >= ?",
                    new String[]{twoDaysAgo}
            );
            boolean hasRecent = false;
            if (cursor.moveToFirst()) {
                hasRecent = cursor.getInt(0) > 0;
            }
            cursor.close();
            db.close();
            return hasRecent;
        } catch (Exception e) {
            return true; // Assume active on error to avoid false nudge
        }
    }
}
