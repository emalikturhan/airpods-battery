package com.murataygun.podsbattery.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.murataygun.podsbattery.R;
import com.murataygun.podsbattery.databinding.ActivityCheckPermissionsBinding;
import com.murataygun.podsbattery.util.AdsManager;
import com.murataygun.podsbattery.util.AdsUtil;
import com.murataygun.podsbattery.util.FireBaseLogEvent;
import com.murataygun.podsbattery.util.Util;

import java.util.Objects;

public class CheckPermissionsActivity extends AppCompatActivity {
    FirebaseAnalytics mFirebaseAnalytics;
    private FirebaseRemoteConfig mFirebaseRemoteConfig;
    private InterstitialAd mInterstitialAd;
    private boolean navigateToMainScreen = true;
    private static final int REQ_CODE_ACCESS_COARSE_LOCATION = 99;
    private static final int REQ_CODE_ACTION_REQUEST_ENABLE = 11;

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    @SuppressLint({"BatteryLife"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        com.murataygun.podsbattery.databinding.ActivityCheckPermissionsBinding binding = ActivityCheckPermissionsBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        setDefaultValueOfPreferences();
        Util.setShowAds(getApplicationContext(), true);

        if (Util.checkBluetoothPermission(this)) {
            navigateMainActivity();
            return;
        }

        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {

            }
        });

        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId(getResources().getString(R.string.podsbattery_gecis_1));
        mInterstitialAd.loadAd(new AdRequest.Builder().build());
        mInterstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                mInterstitialAd.loadAd(new AdRequest.Builder().build());
            }
        });

        AdsUtil.loadBanner(this, binding.adView);

        binding.btnAllow.setOnClickListener(v -> {
            if (!Objects.requireNonNull(getSystemService(PowerManager.class)).isIgnoringBatteryOptimizations(getPackageName())) {
                Intent intent = new Intent();
                getSystemService(Context.POWER_SERVICE);
                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            }
            checkBluetooth();
        });
    }

    public void showNewGec() {
        if (mInterstitialAd.isLoaded()) {
            mInterstitialAd.show();
        } else {
            mInterstitialAd.loadAd(new AdRequest.Builder().build());
        }
    }

    public void checkBluetooth() {
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager.getAdapter() == null) {
            final Dialog dialog = new Dialog(this);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.permission_temp);
            dialog.getWindow().setLayout(ViewPager.LayoutParams.MATCH_PARENT, ViewPager.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.setCancelable(false);
            final Button button = dialog.findViewById(R.id.btn_next);
            final TextView title = dialog.findViewById(R.id.dialogTitle);
            final TextView desc = dialog.findViewById(R.id.dialogDesc);
            title.setText(R.string.required);
            desc.setText(R.string.error_bluetooth_not_supported);
            button.setText(R.string.miui_warning_continue);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    navigateToMainScreen = false;
                    showNewGec();
                    dialog.dismiss();
                }
            });
            dialog.show();
        } else if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            final Dialog dialog = new Dialog(this);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.permission_temp);
            dialog.getWindow().setLayout(ViewPager.LayoutParams.MATCH_PARENT, ViewPager.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.setCancelable(false);
            final Button button = dialog.findViewById(R.id.btn_next);
            final TextView title = dialog.findViewById(R.id.dialogTitle);
            final TextView desc = dialog.findViewById(R.id.dialogDesc);
            title.setText(R.string.required);
            desc.setText(R.string.ble_not_supported);
            button.setText(android.R.string.ok);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    navigateToMainScreen = false;
                    showNewGec();
                    dialog.dismiss();
                }
            });
            dialog.show();
        } else if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            final Dialog dialog = new Dialog(this);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.permission_temp);
            dialog.getWindow().setLayout(ViewPager.LayoutParams.MATCH_PARENT, ViewPager.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.setCancelable(false);
            final Button button = dialog.findViewById(R.id.btn_next);
            final TextView title = dialog.findViewById(R.id.dialogTitle);
            final TextView desc = dialog.findViewById(R.id.dialogDesc);
            title.setText(R.string.need_location_access);
            desc.setText(R.string.grant_location_access);
            button.setText(android.R.string.ok);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQ_CODE_ACCESS_COARSE_LOCATION); // Location (for BLE)
                    dialog.dismiss();
                }
            });
            dialog.show();
        } else if (!bluetoothManager.getAdapter().isEnabled()) {
            final Dialog dialog = new Dialog(this);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.permission_temp);
            dialog.getWindow().setLayout(ViewPager.LayoutParams.MATCH_PARENT, ViewPager.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.setCancelable(false);
            final Button button = dialog.findViewById(R.id.btn_next);
            final TextView title = dialog.findViewById(R.id.dialogTitle);
            final TextView desc = dialog.findViewById(R.id.dialogDesc);
            title.setText(R.string.bluetooth_disabled);
            desc.setText(R.string.bluetooth_needs_to_be_enabled);
            button.setText(android.R.string.ok);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQ_CODE_ACTION_REQUEST_ENABLE);
                    dialog.dismiss();
                }
            });
            dialog.show();
        } else {
            verifyBluetooth();
        }
    }

    private void verifyBluetooth() {
        if (!BluetoothAdapter.getDefaultAdapter().isOffloadedScanBatchingSupported()) {
            Dialog dialog = new Dialog(this);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.permission_temp);
            dialog.getWindow().setLayout(ViewPager.LayoutParams.MATCH_PARENT, ViewPager.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.setCancelable(false);
            final Button button = dialog.findViewById(R.id.btn_next);
            final TextView title = dialog.findViewById(R.id.dialogTitle);
            final TextView desc = dialog.findViewById(R.id.dialogDesc);
            title.setText(R.string.not_supported);
            desc.setText(R.string.bluetooth_does_not_have_required);
            button.setText(android.R.string.ok);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showNewGec();
                    dialog.dismiss();
                    navigateMainActivity();
                }
            });
            dialog.show();
            return;
        }

        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.permission_temp);
        dialog.getWindow().setLayout(ViewPager.LayoutParams.MATCH_PARENT, ViewPager.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.setCancelable(false);
        final Button button = dialog.findViewById(R.id.btn_next);
        final TextView title = dialog.findViewById(R.id.dialogTitle);
        final TextView desc = dialog.findViewById(R.id.dialogDesc);
        title.setText(R.string.success);
        desc.setText(R.string.bluetooth_needs_to_be_enabled);
        button.setText(android.R.string.ok);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navigateToMainScreen = true;
                showNewGec();
                dialog.dismiss();
                navigateMainActivity();
            }
        });
        dialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQ_CODE_ACTION_REQUEST_ENABLE) {
            if (resultCode == -1) {
                this.mFirebaseAnalytics.logEvent("bluetooth_not_enabled_enabled", new Bundle());
                this.navigateToMainScreen = true;
                verifyBluetooth();
            } else if (resultCode == 0) {
                this.mFirebaseAnalytics.logEvent("bluetooth_not_enabled_denied", new Bundle());
                this.navigateToMainScreen = false;
                showNewGec();
                return;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQ_CODE_ACCESS_COARSE_LOCATION) {
            if (grantResults.length <= 0 || grantResults[0] != 0) {
                Dialog dialog = new Dialog(this);
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                dialog.setContentView(R.layout.permission_temp);
                dialog.getWindow().setLayout(ViewPager.LayoutParams.MATCH_PARENT, ViewPager.LayoutParams.WRAP_CONTENT);
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                dialog.setCancelable(false);
                final Button button = dialog.findViewById(R.id.btn_next);
                final TextView title = dialog.findViewById(R.id.dialogTitle);
                final TextView desc = dialog.findViewById(R.id.dialogDesc);
                title.setText(R.string.cannot_continue);
                desc.setText(R.string.location_access_not_granted);
                button.setText(android.R.string.ok);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        navigateToMainScreen = false;
                        showNewGec();
                        dialog.dismiss();
                    }
                });
                dialog.show();
            }
            this.navigateToMainScreen = true;
            checkBluetooth();
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void navigateMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    @SuppressLint("CommitPrefEdits")
    private void setDefaultValueOfPreferences() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("showNotification", true);
        editor.apply();
    }
}