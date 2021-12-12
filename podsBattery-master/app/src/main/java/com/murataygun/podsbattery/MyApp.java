package com.murataygun.podsbattery;

import android.app.Application;

import com.murataygun.podsbattery.market.MarketConf;
import com.revenuecat.purchases.Purchases;

public class MyApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Purchases.setDebugLogsEnabled(true);
        Purchases.configure(this, MarketConf.API_KEY);
    }
}
