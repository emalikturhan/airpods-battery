package com.murataygun.podsbattery.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.Objects;

public class PodsBatteryReceiver extends BroadcastReceiver {
    private static String TAG = PodsBatteryReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        String s = Objects.requireNonNull(intent.getAction());
        if (Intent.ACTION_MY_PACKAGE_REPLACED.equals(s) || Intent.ACTION_BOOT_COMPLETED.equals(s)) {
            startPodsBatteryService(context);
        }
    }

    public static void startPodsBatteryService(Context context) {
        context.startService(new Intent(context, PodsBatteryService.class));
    }

    public static void restartPodsService (Context context) {
        context.stopService(new Intent(context, PodsBatteryService.class));
        try {
            Thread.sleep(250);
        } catch (InterruptedException e) {
            Log.i(TAG, "Interrupted!", e);
            Thread.currentThread().interrupt();
        }

        context.startService(new Intent(context, PodsBatteryService.class));
    }
}
