package com.murataygun.podsbattery.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.murataygun.podsbattery.R;
import com.murataygun.podsbattery.activity.MainActivity;
import com.murataygun.podsbattery.util.Util;

public class NotificationThread extends Thread {
    private static String TAG = NotificationThread.class.getSimpleName();
    private NotificationManager mNotifyManager;
    private Context context;

    public void cancelNotification() {
        if (mNotifyManager != null)
            mNotifyManager.cancel(1);
    }

    @SuppressWarnings("WeakerAccess")
    public NotificationThread(Context context) {
        this.context = context;
        mNotifyManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        // On Oreo (API27) and newer, create a notification channel.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(TAG, TAG, NotificationManager.IMPORTANCE_LOW);
            channel.setSound(null, null);
            channel.enableVibration(false);
            channel.enableLights(false);
            channel.setShowBadge(true);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            mNotifyManager.createNotificationChannel(channel);
        }
    }

    @Override
    public void run() {
        boolean notificationShowing = false;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean showNotificationSetting;

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, TAG);
        mBuilder.setShowWhen(false);
        mBuilder.setOngoing(true);
        mBuilder.setSmallIcon(R.mipmap.notification_icon);
        mBuilder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        PendingIntent contentIntent = PendingIntent.getActivity(this.context, 0,
                new Intent(this.context, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(contentIntent);

        while (true) {
            showNotificationSetting = prefs.getBoolean("showNotification", false);
            if (!showNotificationSetting) {
                mNotifyManager.cancel(1);
                continue;
            }

            if (PodsBatteryService.lastPodsData != null && PodsBatteryService.lastPodsData.checkPodsConnected()) {
                if (!notificationShowing) {
                    if (Util.ENABLE_LOGGING)
                        Log.d(TAG, "Creating notification");
                    notificationShowing = true;
                    mNotifyManager.notify(1, mBuilder.build());
                }
            } else {
                if (notificationShowing) {
                    if (Util.ENABLE_LOGGING)
                        Log.d(TAG, "Removing notification");
                    notificationShowing = false;
                    continue;
                }
                mNotifyManager.cancel(1);
            }


            if (notificationShowing) {
                Util.createNotificationView(PodsBatteryService.lastPodsData, mBuilder, this.context.getApplicationContext());

                try {
                    mNotifyManager.notify(1, mBuilder.build());
                } catch (Throwable ignored) {
                    mNotifyManager.cancel(1);
                    mNotifyManager.notify(1, mBuilder.build());
                }
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
    }
}