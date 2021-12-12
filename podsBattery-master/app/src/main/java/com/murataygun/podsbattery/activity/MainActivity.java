package com.murataygun.podsbattery.activity;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.murataygun.podsbattery.R;
import com.murataygun.podsbattery.databinding.ActivityMainBinding;
import com.murataygun.podsbattery.market.MarketConf;
import com.murataygun.podsbattery.service.ActionConstant;
import com.murataygun.podsbattery.service.PodsBatteryReceiver;
import com.murataygun.podsbattery.util.AdsManager;
import com.murataygun.podsbattery.util.FireBaseLogEvent;
import com.murataygun.podsbattery.util.Util;
import com.revenuecat.purchases.EntitlementInfo;
import com.revenuecat.purchases.PurchaserInfo;
import com.revenuecat.purchases.Purchases;
import com.revenuecat.purchases.PurchasesError;
import com.revenuecat.purchases.interfaces.ReceivePurchaserInfoListener;

import java.util.Map;

import es.dmoral.toasty.Toasty;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private InterstitialAd mInterstitialAd;
    private FirebaseAnalytics mFirebaseAnalytics;
    private AdsManager adsManager;
    private static final int REQ_CODE_ACTION_REQUEST_ENABLE = 11;

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private final String premStatus = "premStatus";
    boolean isPremiums = false;

    Boolean yearlyTest, monthlyold, monthlynew;

    public static final String MONTHLYNEW = "monthlynew";
    public static final String MONTHLYOLD = "monthlyold";
    public static final String YEARLY = "yearly";

    private final BroadcastReceiver removeAdsBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            assert action != null;
            if (action.equals(ActionConstant.ACTION_REMOVE_ADS_PURCHASED)) {
                //Todo unutma
                //binding.adViewNav.removeAllViews();
            } else if (action.equals(ActionConstant.ACTION_SHOW_AD) && adsManager.showAds()) {
                MainActivity.this.showNewGec();
            }
        }
    };

    private void navigateMainActivity() {
        Intent intent = new Intent(this, PremActivity.class);
        startActivity(intent);
    }

    @SuppressLint("CommitPrefEdits")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        com.murataygun.podsbattery.databinding.ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        adsManager = AdsManager.getInstance(this);
        Util.setShowAds(getApplicationContext(), true);

        sharedPreferences = this.getSharedPreferences(premStatus, MODE_PRIVATE);
        editor = sharedPreferences.edit();
        getPremiumStatus(premStatus);
        checkPurchases();

        MobileAds.initialize((Context) this, initializationStatus -> { });

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

        if (Util.checkBluetoothPermission(this)) {
            mFirebaseAnalytics.logEvent(FireBaseLogEvent.HAVE_BLUETOOTH_PERMISSION.getKey(), new Bundle());
            PodsBatteryReceiver.startPodsBatteryService(getApplicationContext());
            Util.checkMiUi(this);
            checkPhone();
        } else {
            mFirebaseAnalytics.logEvent(FireBaseLogEvent.NO_BLUETOOTH_PERMISSION.getKey(), new Bundle());
            startActivity(new Intent(MainActivity.this, CheckPermissionsActivity.class));
            finish();
        }

        if (!isPremiums) {
            navigateMainActivity();
        }
    }

    private void getPremiumStatus(String keyName) {
        isPremiums = sharedPreferences.getBoolean(keyName, false);
        if (isPremiums) {
            adsManager.turnOffAds();
        } else {
            adsManager.showAds();
        }
        yearlyTest = sharedPreferences.getBoolean(YEARLY, false);
        monthlynew = sharedPreferences.getBoolean(MONTHLYNEW, false);
        monthlyold = sharedPreferences.getBoolean(MONTHLYOLD, false);
    }

    private void savePremiumStatus(String keyName, boolean status) {
        editor.putBoolean(keyName, status);
        editor.commit();
        getPremiumStatus(keyName);
    }

    private void checkPurchases() {
        Purchases.getSharedInstance().restorePurchases(new ReceivePurchaserInfoListener() {
            @Override
            public void onReceived(@NonNull PurchaserInfo purchaserInfo) {
                EntitlementInfo entitlement = purchaserInfo.getEntitlements().get(MarketConf.PRO_ENTITLEMENT);
                if (entitlement != null && entitlement.isActive()) {
                    savePremiumStatus(premStatus, true);
                } else {
                    savePremiumStatus(premStatus, false);
                }
            }

            @Override
            public void onError(@NonNull PurchasesError error) {
                Log.d("###", "Error: " + error);
                savePremiumStatus(premStatus, false);
            }
        });
    }

    private void checkPhone() {
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (!bluetoothManager.getAdapter().isEnabled()) {
            Dialog dialog = new Dialog(this);
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
        }

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
                    sendBroadcast(new Intent(ActionConstant.ACTION_SHOW_AD));
                    dialog.dismiss();
                }
            });
            dialog.show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQ_CODE_ACTION_REQUEST_ENABLE) {
            if (resultCode == -1) {
                this.mFirebaseAnalytics.logEvent("bluetooth_not_enabled_enabled", new Bundle());
            } else if (resultCode == 0) {
                this.mFirebaseAnalytics.logEvent("bluetooth_not_enabled_denied", new Bundle());
                sendBroadcast(new Intent(ActionConstant.ACTION_SHOW_AD));
                return;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ActionConstant.ACTION_REMOVE_ADS_PURCHASED);
        intentFilter.addAction(ActionConstant.ACTION_SHOW_AD);
        registerReceiver(this.removeAdsBroadcastReceiver, intentFilter);
        checkPurchases();
        getPremiumStatus(premStatus);
        try {
            getApplicationContext().openFileInput("hidden").close();
            finish();
        } catch (Exception e) {
            Log.e("MainActivity_onResume", "Error: " + e.toString());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(this.removeAdsBroadcastReceiver);
    }

    public void showNewGec() {
        if (mInterstitialAd.isLoaded()) {
            mInterstitialAd.show();
        } else {
            mInterstitialAd.loadAd(new AdRequest.Builder().build());
        }
    }
}