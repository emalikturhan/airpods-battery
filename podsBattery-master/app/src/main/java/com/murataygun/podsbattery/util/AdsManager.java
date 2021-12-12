package com.murataygun.podsbattery.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.murataygun.podsbattery.service.ActionConstant;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class AdsManager {
    private static final String TAG = AdsManager.class.getSimpleName();
    @SuppressLint("StaticFieldLeak")
    private static AdsManager adsManager;
    private Context context;
    private boolean showAds;
    private long timeBetweenAds;
    private long lastAdShownTime;
    private boolean secondLaunchInterstitialAdLoaded;
    ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public static AdsManager getInstance(Context context2) {
        if (adsManager == null) {
            adsManager = new AdsManager(context2);
        }
        return adsManager;
    }

    private AdsManager(Context context2) {
        Log.i(TAG, "AdsManager created");
        this.context = context2;
        this.showAds = true;
        this.timeBetweenAds = 120000;
        this.lastAdShownTime = context2.getSharedPreferences("AdsManager", 0).getLong("lastAdShownTime", 0);
    }

    public long getLastAdShownTime() {
        return this.lastAdShownTime;
    }

    public boolean isSecondLaunchInterstitialAdLoaded() {
        return this.secondLaunchInterstitialAdLoaded;
    }

    public void setSecondLaunchInterstitialAdLoaded(boolean secondLaunchInterstitialAdLoaded) {
        this.secondLaunchInterstitialAdLoaded = secondLaunchInterstitialAdLoaded;
    }

    @SuppressLint("CommitPrefEdits")
    public void setLastAdShownTime(long j) {
        this.lastAdShownTime = j;
        SharedPreferences sharedPreferences = this.context.getSharedPreferences("AdsManager", 0);
        sharedPreferences.edit().putLong("lastAdShownTime", this.lastAdShownTime);
        sharedPreferences.edit().apply();
    }

    public boolean shouldShowAnAd() {
        if (!this.showAds) {
            return false;
        }
        long j = this.lastAdShownTime;
        return j == 0 || j + this.timeBetweenAds < System.currentTimeMillis();
    }

    public void turnOffAds() {
        this.showAds = false;
        Util.setShowAds(context, showAds);
        this.context.sendBroadcast(new Intent(ActionConstant.ACTION_REMOVE_ADS_PURCHASED));
        this.scheduler.shutdownNow();
    }

    public boolean showAds() {
        return this.showAds;
    }
}
