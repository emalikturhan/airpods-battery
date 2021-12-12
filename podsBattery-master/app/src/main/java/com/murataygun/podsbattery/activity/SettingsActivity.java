package com.murataygun.podsbattery.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdCallback;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;
import com.google.android.play.core.tasks.OnCompleteListener;
import com.google.android.play.core.tasks.OnFailureListener;
import com.google.android.play.core.tasks.Task;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.murataygun.podsbattery.R;
import com.murataygun.podsbattery.market.MarketConf;
import com.murataygun.podsbattery.service.PodsBatteryReceiver;
import com.murataygun.podsbattery.service.PodsBatteryService;
import com.murataygun.podsbattery.util.AdsManager;
import com.revenuecat.purchases.EntitlementInfo;
import com.revenuecat.purchases.PurchaserInfo;
import com.revenuecat.purchases.Purchases;
import com.revenuecat.purchases.PurchasesError;
import com.revenuecat.purchases.interfaces.ReceivePurchaserInfoListener;

import java.util.Random;

import es.dmoral.toasty.Toasty;

public class SettingsActivity extends AppCompatActivity {
    RelativeLayout relativeLayout;
    private AdView mAdView;
    ReviewInfo reviewInfo;
    ReviewManager manager;
    private InterstitialAd mInterstitialAd;
    RewardedAd rewardedVideoAd;
    RewardedAdCallback odulluListen;
    private FirebaseAnalytics mFirebaseAnalytics;

    TextView txtAbout, txtSaver, txtNotify, txtShare, txtWatch, txtPrem, txtMore, txtMoreTwo, txtReview;
    CheckBox cbShowBattery, cbShowNotify;
    CardView premCard, reviewCard;
    ImageView imgExt;

    private AdsManager adsManager;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private final String getSaverTxt = "showNotification";
    boolean isPremiums = false;
    boolean getSaver = false;

    @SuppressLint("CommitPrefEdits")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_temp);
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        relativeLayout = findViewById(R.id.relaative);
        manager = ReviewManagerFactory.create(this);
        adsManager = AdsManager.getInstance(this);

        String premStatus = "premStatus";
        sharedPreferences = this.getSharedPreferences(premStatus, MODE_PRIVATE);
        editor = sharedPreferences.edit();
        getPremiumStatus(premStatus);
        checkPurchases();

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
        rewardedAds();
        odulluListen = new RewardedAdCallback() {
            @Override
            public void onRewardedAdOpened() {

            }

            @Override
            public void onRewardedAdClosed() {
                rewardedAds();
            }

            @Override
            public void onUserEarnedReward(@NonNull com.google.android.gms.ads.rewarded.RewardItem rewardItem) {
                mFirebaseAnalytics.logEvent("rewarded_succes", null);
                Toasty.success(SettingsActivity.this, getString(R.string.ads_error), Toasty.LENGTH_SHORT).show();
                savePremiumStatus(true);
            }

            @Override
            public void onRewardedAdFailedToShow(AdError adError) {
                rewardedAds();
            }
        };

        item();
    }

    private void getPremiumStatus(String keyName) {
        isPremiums = sharedPreferences.getBoolean(keyName, false);
        getSaver = sharedPreferences.getBoolean(getSaverTxt, false);
        if (isPremiums) {
            adsManager.turnOffAds();
        } else {
            adsManager.showAds();
        }
    }

    private void savePremiumStatus(boolean status) {
        editor.putBoolean("premStatus", status);
        editor.commit();
        getPremiumStatus("premStatus");
    }

    private void checkPurchases() {
        Purchases.getSharedInstance().restorePurchases(new ReceivePurchaserInfoListener() {
            @Override
            public void onReceived(@NonNull PurchaserInfo purchaserInfo) {
                EntitlementInfo entitlement = purchaserInfo.getEntitlements().get(MarketConf.PRO_ENTITLEMENT);
                if (entitlement != null && entitlement.isActive()) {
                    savePremiumStatus(true);
                } else {
                    savePremiumStatus(false);
                }
            }

            @Override
            public void onError(@NonNull PurchasesError error) {
                Log.d("###", "Error: " + error);
                savePremiumStatus(false);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    //Item
    public void item() {
        txtAbout = findViewById(R.id.about);
        txtSaver = findViewById(R.id.batterySaver);
        txtNotify = findViewById(R.id.showNotif);
        txtShare = findViewById(R.id.shareApp);
        txtWatch = findViewById(R.id.showOdul);
        txtPrem = findViewById(R.id.buyPremText);
        txtMore = findViewById(R.id.moreApp);
        txtMoreTwo = findViewById(R.id.moreAppTwo);
        txtReview = findViewById(R.id.review);

        cbShowBattery = findViewById(R.id.cbBatterySaver);
        cbShowNotify = findViewById(R.id.cbShowNotif);

        premCard = findViewById(R.id.buyPrem);
        reviewCard = findViewById(R.id.rateMeCard);

        imgExt = findViewById(R.id.imgExit);
        imgExt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isPremiums) {
                    showNewGec();
                }
                finish();
            }
        });

        if (isPremiums) {
            txtPrem.setText(getString(R.string.now_prem));
            txtPrem.setClickable(false);
            premCard.setClickable(false);
        }

        txtAbout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(SettingsActivity.this, AboutActivity.class));
            }
        });

        txtSaver.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PodsBatteryReceiver.restartPodsService(SettingsActivity.this);
            }
        });

        txtNotify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isPremiums) {
                    PodsBatteryService.notificationThread.cancelNotification();
                } else {
                    Intent ii = new Intent(SettingsActivity.this, PremActivity.class);
                    startActivity(ii);
                }
            }
        });

        txtShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent share = new Intent(Intent.ACTION_SEND);
                share.putExtra(Intent.EXTRA_TEXT, "Come on, download the Pods Battery!" + "\n" +  "\n" + "https://play.google.com/store/apps/details?id=" + getApplication().getPackageName());
                share.setType("text/plain");
                startActivity(Intent.createChooser(share, getString(R.string.share_app)));
            }
        });

        txtWatch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(rewardedVideoAd.isLoaded()) {
                    if (!isPremiums) {
                        rewardedVideoAd.show(SettingsActivity.this, odulluListen);
                    }
                } else {
                    Toasty.warning(SettingsActivity.this, getString(R.string.error_toast), Toast.LENGTH_SHORT).show();
                    if (!isPremiums) {
                        rewardedAds();
                    }
                }
            }
        });

        txtPrem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent ii = new Intent(SettingsActivity.this, PremActivity.class);
                startActivity(ii);
            }
        });

        txtMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mFirebaseAnalytics.logEvent("download_more1", null);
                String appPackageNames = "privatestory.storystalker.privateprofile";
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageNames)));
                } catch (android.content.ActivityNotFoundException anfe) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageNames)));
                }
            }
        });

        txtMoreTwo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mFirebaseAnalytics.logEvent("download_more2", null);
                String appPackageNames = "profileanalyzer.igstalk.mystalker";
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageNames)));
                } catch (android.content.ActivityNotFoundException anfe) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageNames)));
                }
            }
        });

        txtReview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                review();
            }
        });

        premCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent ii = new Intent(SettingsActivity.this, PremActivity.class);
                startActivity(ii);
            }
        });

        reviewCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                review();
            }
        });

        cbShowNotify.setChecked(isPremiums);
        cbShowNotify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isPremiums) {
                    PodsBatteryService.notificationThread.cancelNotification();
                } else {
                    Intent ii = new Intent(SettingsActivity.this, PremActivity.class);
                    startActivity(ii);
                }
            }
        });

        cbShowBattery.setChecked(getSaver);
        cbShowBattery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (cbShowBattery.isChecked()) {
                    PodsBatteryReceiver.restartPodsService(SettingsActivity.this);
                    cbShowBattery.setChecked(false);
                    editor.putBoolean(getSaverTxt, false);
                    editor.commit();
                } else {
                    PodsBatteryReceiver.restartPodsService(SettingsActivity.this);
                    cbShowBattery.setChecked(true);
                    editor.putBoolean(getSaverTxt, true);
                    editor.commit();
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (!isPremiums) {
            showInters();
        }
        finish();
    }

    //Ads
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

    public void showNewGec() {
        mFirebaseAnalytics.logEvent("gecis_goster", null);
        if (mInterstitialAd.isLoaded()) {
            mInterstitialAd.show();
        } else {
            mInterstitialAd.loadAd(new AdRequest.Builder().build());
        }
    }

    public void showInters() {
        float hitPercent = 0.7f;
        final Random generator = new Random();
        if (generator.nextFloat() <= hitPercent) {
            mFirebaseAnalytics.logEvent("gecis_goster", null);
            if (mInterstitialAd.isLoaded()) {
                mInterstitialAd.show();
            } else {
                mInterstitialAd.loadAd(new AdRequest.Builder().build());
            }
        } else {
            mFirebaseAnalytics.logEvent("gecis_gosterme", null);
        }
    }

    public void rewardedAds() {
        rewardedVideoAd = new RewardedAd(this, getResources().getString(R.string.podsbattery_odullu));
        RewardedAdLoadCallback adLoadCallback = new RewardedAdLoadCallback() {
            @Override
            public void onRewardedAdLoaded() {
                // Ad successfully loaded.
            }

            @Override
            public void onRewardedAdFailedToLoad(LoadAdError adError) {
                // Ad failed to load.
            }
        };
        rewardedVideoAd.loadAd(new AdRequest.Builder().build(), adLoadCallback);
    }

    private void review(){
        manager.requestReviewFlow().addOnCompleteListener(new OnCompleteListener<ReviewInfo>() {
            @Override
            public void onComplete(@NonNull Task<ReviewInfo> task) {
                if(task.isSuccessful()){
                    reviewInfo = task.getResult();
                    manager.launchReviewFlow(SettingsActivity.this, reviewInfo).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(Exception e) {
                            Toasty.warning(SettingsActivity.this, getString(R.string.error_toast), Toast.LENGTH_SHORT).show();
                        }
                    }).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            Toasty.success(SettingsActivity.this, getString(R.string.rev_thanks), Toast.LENGTH_SHORT).show();
                            reviewCard.setVisibility(View.GONE);
                        }
                    });
                }

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                String appPackageName = getPackageName();
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
                } catch (android.content.ActivityNotFoundException anfe) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
                }
            }
        });

    }
}
