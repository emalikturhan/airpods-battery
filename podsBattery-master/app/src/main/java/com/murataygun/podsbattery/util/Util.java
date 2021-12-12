package com.murataygun.podsbattery.util;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.bluetooth.BluetoothDevice;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.viewpager.widget.ViewPager;

import com.murataygun.podsbattery.BuildConfig;
import com.murataygun.podsbattery.R;
import com.murataygun.podsbattery.activity.AboutActivity;
import com.murataygun.podsbattery.activity.SettingsActivity;
import com.murataygun.podsbattery.data.PodsData;
import com.murataygun.podsbattery.data.PodsModel;

import java.util.Objects;

public class Util {
    public static final boolean ENABLE_LOGGING = BuildConfig.DEBUG;
    public static final long TIMEOUT_CONNECTED = 30000;

    private Util() {
        throw new IllegalStateException("Util class");
    }

    public static boolean checkBluetoothPermission(Context context) {
        boolean check = true;

        try {
            if (!Objects.requireNonNull(context.getSystemService(PowerManager.class)).isIgnoringBatteryOptimizations(context.getPackageName()))
                check = false;

        } catch (Throwable ignored) {
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            check = false;

        return check;
    }

    public static void checkMiUi(Context context) {
        try {
            @SuppressLint("PrivateApi") Class<?> c = Class.forName("android.os.SystemProperties");
            String miUiVersion = (String) c.getMethod("get", String.class).invoke(c, "ro.miui.ui.version.code");
            if (miUiVersion != null && !miUiVersion.isEmpty()) {
                try {
                    context.getApplicationContext().openFileInput("miuiwarn").close();
                } catch (Throwable ignored) {
                    final Dialog dialog = new Dialog(context);
                    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                    dialog.setContentView(R.layout.permission_temp);
                    dialog.getWindow().setLayout(ViewPager.LayoutParams.MATCH_PARENT, ViewPager.LayoutParams.WRAP_CONTENT);
                    dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                    dialog.setCancelable(false);
                    final Button button = dialog.findViewById(R.id.btn_next);
                    final TextView title = dialog.findViewById(R.id.dialogTitle);
                    final TextView desc = dialog.findViewById(R.id.dialogDesc);
                    title.setText(R.string.miui_warning);
                    desc.setText(R.string.miui_warning_desc);
                    button.setText(R.string.miui_warning_continue);
                    button.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            try {
                                context.getApplicationContext().openFileOutput("miuiwarn", Context.MODE_PRIVATE).close();
                            } catch (Throwable ignored2) {

                            }
                            dialog.dismiss();
                        }
                    });
                    dialog.show();
                }
            }
        } catch (Throwable ignored) {
        }
    }

    public static boolean checkUUID(BluetoothDevice bluetoothDevice) {
        ParcelUuid[] parcelUuidArr = {ParcelUuid.fromString("74ec2172-0bad-4d01-8f77-997b2be0722a"),
                ParcelUuid.fromString("2a72e02b-7b99-778f-014d-ad0b7221ec74"),
                ParcelUuid.fromString("9BD708D7-64C7-4E9F-9DED-F6B6C4551967"),
                ParcelUuid.fromString("0000111e-0000-1000-8000-00805f9b34fb")};

        ParcelUuid[] uuids = bluetoothDevice.getUuids();

        if (uuids == null)
            return false;

        for (ParcelUuid u : uuids)
            for (ParcelUuid v : parcelUuidArr)
                if (u.equals(v)) return true;

        return false;
    }

    public static boolean isFlipped(String str) {
        return (Integer.parseInt("" + str.charAt(10), 16) & 0x02) == 0;
    }

    public static String decodeHex(byte[] bArr) {
        StringBuilder ret = new StringBuilder();

        for (byte b : bArr)
            ret.append(String.format("%02X", b));

        return ret.toString();
    }

    public static boolean isLocationEnabled(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            context.getApplicationContext();

            LocationManager service = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            return service != null && service.isLocationEnabled();
        } else {
            try {
                return Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE) != Settings.Secure.LOCATION_MODE_OFF;
            } catch (Throwable t) {
                return true;
            }
        }
    }

    public static void createNotificationView(PodsData podsData, NotificationCompat.Builder mBuilder, Context context) {
        RemoteViews notificationBig = new RemoteViews(context.getPackageName(), R.layout.status_big);
        RemoteViews notificationSmall = new RemoteViews(context.getPackageName(), R.layout.status_small);
        RemoteViews locationDisabledBig = new RemoteViews(context.getPackageName(), R.layout.location_disabled_big);
        RemoteViews locationDisabledSmall = new RemoteViews(context.getPackageName(), R.layout.location_disabled_small);
        RemoteViews notPurchasesBig = new RemoteViews(context.getPackageName(), R.layout.not_purchases_big);
        RemoteViews notPurchasesSmall = new RemoteViews(context.getPackageName(), R.layout.not_purchases_small);

        if (Util.getShowAds(context)) {
            mBuilder.setCustomContentView(notPurchasesSmall);
            mBuilder.setCustomBigContentView(notPurchasesBig);
            return;
        } else if (Util.isLocationEnabled(context) || Build.VERSION.SDK_INT >= 29) {
            mBuilder.setCustomContentView(notificationSmall);
            mBuilder.setCustomBigContentView(notificationBig);
        } else {
            mBuilder.setCustomContentView(locationDisabledSmall);
            mBuilder.setCustomBigContentView(locationDisabledBig);
            return;
        }

        boolean leftConnect = podsData.getLeftStatus() <= 10;
        boolean rightConnect = podsData.getRightStatus() <= 10;
        boolean caseConnect = podsData.getCaseStatus() <= 10;

        if (podsData.getModel().equals(PodsModel.AIR_PODS_NORMAL)) {
            notificationBig.setImageViewResource(R.id.leftPodImg, leftConnect ? R.drawable.pod : R.drawable.pod_disconnected);
            notificationBig.setImageViewResource(R.id.rightPodImg, rightConnect ? R.drawable.pod : R.drawable.pod_disconnected);
            notificationBig.setImageViewResource(R.id.podCaseImg, caseConnect ? R.drawable.pod_case : R.drawable.pod_case_disconnected);
            notificationSmall.setImageViewResource(R.id.leftPodImg, leftConnect ? R.drawable.pod : R.drawable.pod_disconnected);
            notificationSmall.setImageViewResource(R.id.rightPodImg, rightConnect ? R.drawable.pod : R.drawable.pod_disconnected);
            notificationSmall.setImageViewResource(R.id.podCaseImg, caseConnect ? R.drawable.pod_case : R.drawable.pod_case_disconnected);
        } else if (podsData.getModel().equals(PodsModel.AIR_PODS_PRO)) {
            notificationBig.setImageViewResource(R.id.leftPodImg, leftConnect ? R.drawable.podpro : R.drawable.podpro_disconnected);
            notificationBig.setImageViewResource(R.id.rightPodImg, rightConnect ? R.drawable.podpro : R.drawable.podpro_disconnected);
            notificationBig.setImageViewResource(R.id.podCaseImg, caseConnect ? R.drawable.podpro_case : R.drawable.podpro_case_disconnected);
            notificationSmall.setImageViewResource(R.id.leftPodImg, leftConnect ? R.drawable.podpro : R.drawable.podpro_disconnected);
            notificationSmall.setImageViewResource(R.id.rightPodImg, rightConnect ? R.drawable.podpro : R.drawable.podpro_disconnected);
            notificationSmall.setImageViewResource(R.id.podCaseImg, caseConnect ? R.drawable.podpro_case : R.drawable.podpro_case_disconnected);
        }

        if (System.currentTimeMillis() - podsData.getLastSeenConnected() < TIMEOUT_CONNECTED) {

            notificationBig.setViewVisibility(R.id.leftPodText, View.VISIBLE);
            notificationBig.setViewVisibility(R.id.rightPodText, View.VISIBLE);
            notificationBig.setViewVisibility(R.id.podCaseText, View.VISIBLE);
            notificationBig.setViewVisibility(R.id.leftPodUpdating, View.INVISIBLE);
            notificationBig.setViewVisibility(R.id.rightPodUpdating, View.INVISIBLE);
            notificationBig.setViewVisibility(R.id.podCaseUpdating, View.INVISIBLE);

            notificationSmall.setViewVisibility(R.id.leftPodText, View.VISIBLE);
            notificationSmall.setViewVisibility(R.id.rightPodText, View.VISIBLE);
            notificationSmall.setViewVisibility(R.id.podCaseText, View.VISIBLE);
            notificationSmall.setViewVisibility(R.id.leftPodUpdating, View.INVISIBLE);
            notificationSmall.setViewVisibility(R.id.rightPodUpdating, View.INVISIBLE);
            notificationSmall.setViewVisibility(R.id.podCaseUpdating, View.INVISIBLE);

            int leftBattery = getBatteryStatus(podsData.getLeftStatus());
            int rightBattery = getBatteryStatus(podsData.getRightStatus());
            int caseBattery = getBatteryStatus(podsData.getCaseStatus());

            notificationBig.setTextViewText(R.id.leftPodText, leftBattery + "%");
            notificationBig.setTextViewText(R.id.rightPodText, rightBattery + "%");
            notificationBig.setTextViewText(R.id.podCaseText, caseBattery + "%");

            notificationSmall.setTextViewText(R.id.leftPodText, leftBattery + "%");
            notificationSmall.setTextViewText(R.id.rightPodText, rightBattery + "%");
            notificationSmall.setTextViewText(R.id.podCaseText, caseBattery + "%");

            notificationBig.setImageViewResource(R.id.leftBatImg, podsData.isChargeL() ? R.drawable.ic_battery_charging_full_green_24dp : R.drawable.ic_battery_alert_red_24dp);
            notificationBig.setImageViewResource(R.id.rightBatImg, podsData.isChargeR() ? R.drawable.ic_battery_charging_full_green_24dp : R.drawable.ic_battery_alert_red_24dp);
            notificationBig.setImageViewResource(R.id.caseBatImg, podsData.isChargeCase() ? R.drawable.ic_battery_charging_full_green_24dp : R.drawable.ic_battery_alert_red_24dp);

            notificationSmall.setImageViewResource(R.id.leftBatImg, podsData.isChargeL() ? R.drawable.ic_battery_charging_full_green_24dp : R.drawable.ic_battery_alert_red_24dp);
            notificationSmall.setImageViewResource(R.id.rightBatImg, podsData.isChargeR() ? R.drawable.ic_battery_charging_full_green_24dp : R.drawable.ic_battery_alert_red_24dp);
            notificationSmall.setImageViewResource(R.id.caseBatImg, podsData.isChargeCase() ? R.drawable.ic_battery_charging_full_green_24dp : R.drawable.ic_battery_alert_red_24dp);

            notificationBig.setViewVisibility(R.id.leftBatImg, ((podsData.isChargeL() && leftConnect) || (podsData.getLeftStatus() <= 1) ? View.VISIBLE : View.GONE));
            notificationBig.setViewVisibility(R.id.rightBatImg, ((podsData.isChargeR() && rightConnect) || (podsData.getRightStatus() <= 1) ? View.VISIBLE : View.GONE));
            notificationBig.setViewVisibility(R.id.caseBatImg, ((podsData.isChargeCase() && caseConnect) || (podsData.getCaseStatus() <= 1) ? View.VISIBLE : View.GONE));
            notificationBig.setViewVisibility(R.id.leftPodText, leftConnect ? View.VISIBLE : View.GONE);
            notificationBig.setViewVisibility(R.id.rightPodText, rightConnect ? View.VISIBLE : View.GONE);
            notificationBig.setViewVisibility(R.id.podCaseText, caseConnect ? View.VISIBLE : View.GONE);

            notificationSmall.setViewVisibility(R.id.leftBatImg, ((podsData.isChargeL() && leftConnect) || (podsData.getLeftStatus() <= 1) ? View.VISIBLE : View.GONE));
            notificationSmall.setViewVisibility(R.id.rightBatImg, ((podsData.isChargeR() && rightConnect) || (podsData.getRightStatus() <= 1) ? View.VISIBLE : View.GONE));
            notificationSmall.setViewVisibility(R.id.caseBatImg, ((podsData.isChargeCase() && caseConnect) || (podsData.getCaseStatus() <= 1) ? View.VISIBLE : View.GONE));
            notificationSmall.setViewVisibility(R.id.leftPodText, leftConnect ? View.VISIBLE : View.GONE);
            notificationSmall.setViewVisibility(R.id.rightPodText, rightConnect ? View.VISIBLE : View.GONE);
            notificationSmall.setViewVisibility(R.id.podCaseText, caseConnect ? View.VISIBLE : View.GONE);
        } else {
            notificationBig.setViewVisibility(R.id.leftPodText, View.INVISIBLE);
            notificationBig.setViewVisibility(R.id.rightPodText, View.INVISIBLE);
            notificationBig.setViewVisibility(R.id.podCaseText, View.INVISIBLE);
            notificationBig.setViewVisibility(R.id.leftBatImg, View.GONE);
            notificationBig.setViewVisibility(R.id.rightBatImg, View.GONE);
            notificationBig.setViewVisibility(R.id.caseBatImg, View.GONE);
            notificationBig.setViewVisibility(R.id.leftPodUpdating, View.VISIBLE);
            notificationBig.setViewVisibility(R.id.rightPodUpdating, View.VISIBLE);
            notificationBig.setViewVisibility(R.id.podCaseUpdating, View.VISIBLE);

            notificationSmall.setViewVisibility(R.id.leftPodText, View.INVISIBLE);
            notificationSmall.setViewVisibility(R.id.rightPodText, View.INVISIBLE);
            notificationSmall.setViewVisibility(R.id.podCaseText, View.INVISIBLE);
            notificationSmall.setViewVisibility(R.id.leftBatImg, View.GONE);
            notificationSmall.setViewVisibility(R.id.rightBatImg, View.GONE);
            notificationSmall.setViewVisibility(R.id.caseBatImg, View.GONE);
            notificationSmall.setViewVisibility(R.id.leftPodUpdating, View.VISIBLE);
            notificationSmall.setViewVisibility(R.id.rightPodUpdating, View.VISIBLE);
            notificationSmall.setViewVisibility(R.id.podCaseUpdating, View.VISIBLE);
        }

    }

    private static int getBatteryStatus(int leftStatus) {
        if (leftStatus == 10)
            return 100;

        return (leftStatus < 10) ? (leftStatus * 10 + 5) : 0;
    }

    @SuppressLint({"WrongConstant", "ResourceType"})
    public static void setBtnLoveOnclick(Activity activity) {
        activity.startActivity(new Intent(activity, SettingsActivity.class));
    }

    @SuppressLint("CommitPrefEdits")
    public static void setShowAds(Context context, boolean removeAds) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("remove_ads", removeAds);
        editor.apply();
    }

    public static boolean getShowAds(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean("remove_ads", true);
    }
}