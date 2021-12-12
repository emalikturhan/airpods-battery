package com.murataygun.podsbattery.util;

import android.app.Activity;
import android.content.Context;
import android.util.DisplayMetrics;
import android.view.Display;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.murataygun.podsbattery.R;

public class AdsUtil {

    private AdsUtil() {
        throw new IllegalStateException("AdsUtil class");
    }

    public static void loadBanner(Context context, AdView view) {
        AdView adView = new AdView(context);
        adView.setAdUnitId(context.getString(R.string.podsbattery_banner_1));
        view.removeAllViews();
        view.addView(adView);
        adView.setAdSize(AdSize.SMART_BANNER);
        adView.loadAd(new AdRequest.Builder().build());
    }

    private static AdSize getAdSize(Context context, AdView view) {
        Display defaultDisplay = ((Activity) context).getWindowManager().getDefaultDisplay();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        defaultDisplay.getMetrics(displayMetrics);
        float f = displayMetrics.density;
        float width = (float) view.getWidth();
        if (width == 0.0f) {
            width = (float) displayMetrics.widthPixels;
        }
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, (int) (width / f));
    }
}