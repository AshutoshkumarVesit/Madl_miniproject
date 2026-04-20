package com.movemate.notification;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.movemate.R;

/**
 * Foreground service that shows a persistent notification while a run is in progress.
 * Receives update intents to refresh the displayed distance and duration.
 */
public class RunTrackingService extends Service {

    public static final String ACTION_START = "com.movemate.ACTION_START_TRACKING";
    public static final String ACTION_UPDATE = "com.movemate.ACTION_UPDATE_TRACKING";
    public static final String ACTION_STOP = "com.movemate.ACTION_STOP_TRACKING";

    public static final String EXTRA_DISTANCE = "extra_distance";
    public static final String EXTRA_DURATION = "extra_duration";

    @Override
    public void onCreate() {
        super.onCreate();
        NotificationHelper.createChannels(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || intent.getAction() == null) {
            stopSelf();
            return START_NOT_STICKY;
        }

        switch (intent.getAction()) {
            case ACTION_START:
                startForeground(NotificationHelper.ID_RUN_TRACKING,
                        NotificationHelper.buildTrackingNotification(this, "0.00 km", "00:00:00"));
                break;

            case ACTION_UPDATE:
                String distance = intent.getStringExtra(EXTRA_DISTANCE);
                String duration = intent.getStringExtra(EXTRA_DURATION);
                if (distance == null) distance = "0.00 km";
                if (duration == null) duration = "00:00:00";
                Notification updated = NotificationHelper.buildTrackingNotification(this, distance, duration);
                NotificationManagerCompat.from(this).notify(NotificationHelper.ID_RUN_TRACKING, updated);
                break;

            case ACTION_STOP:
                stopForeground(true);
                stopSelf();
                break;

            default:
                stopSelf();
                break;
        }

        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
