package com.murataygun.podsbattery.activity;

import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.murataygun.podsbattery.BuildConfig;
import com.murataygun.podsbattery.R;
import java.util.Calendar;
import mehdi.sakout.aboutpage.AboutPage;
import mehdi.sakout.aboutpage.Element;

public class AboutActivity extends AppCompatActivity {
    private InterstitialAd mInterstitialAd;
    private AdView mAdView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about_activity);
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

        bannerAds();

        ImageView imgExt = findViewById(R.id.imgExit);
        imgExt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showNewGec();
                finish();
            }
        });

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        showNewGec();
        finish();
    }

    public void showNewGec() {
        if (mInterstitialAd.isLoaded()) {
            mInterstitialAd.show();
        } else {
            mInterstitialAd.loadAd(new AdRequest.Builder().build());
        }
    }

    public void bannerAds() {
        mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
        mAdView.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {

                super.onAdClosed();
            }

            @Override
            public void onAdFailedToLoad(LoadAdError adError) {
                Log.d("FAil",String.valueOf(adError));
            }

            @Override
            public void onAdLeftApplication() {

                super.onAdLeftApplication();
            }

            @Override
            public void onAdOpened() {

                super.onAdOpened();
            }

            @Override
            public void onAdLoaded() {
                mAdView.setVisibility(View.VISIBLE);
                super.onAdLoaded();
            }

            @Override
            public void onAdClicked() {
                super.onAdClicked();
            }

            @Override
            public void onAdImpression() {
                super.onAdImpression();
            }
        });
    }
}
