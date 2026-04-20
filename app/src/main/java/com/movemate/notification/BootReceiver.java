package com.movemate.notification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Re-schedules notification workers after device reboot.
 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;

        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            if (NotificationHelper.areNotificationsEnabled(context)) {
                NotificationHelper.createChannels(context);
                NotificationHelper.scheduleAllWorkers(context);
            }
        }
    }
}
