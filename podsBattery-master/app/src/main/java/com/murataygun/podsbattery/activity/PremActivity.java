package com.murataygun.podsbattery.activity;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.android.billingclient.api.Purchase;
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
import com.murataygun.podsbattery.market.MarketConf;
import com.murataygun.podsbattery.util.AdsManager;
import com.murataygun.podsbattery.util.Tools;
import com.revenuecat.purchases.EntitlementInfo;
import com.revenuecat.purchases.Offering;
import com.revenuecat.purchases.Offerings;
import com.revenuecat.purchases.Package;
import com.revenuecat.purchases.PurchaserInfo;
import com.revenuecat.purchases.Purchases;
import com.revenuecat.purchases.PurchasesError;
import com.revenuecat.purchases.interfaces.MakePurchaseListener;
import com.revenuecat.purchases.interfaces.ReceiveOfferingsListener;
import com.revenuecat.purchases.interfaces.ReceivePurchaserInfoListener;

import java.util.List;

import es.dmoral.toasty.Toasty;

public class PremActivity extends AppCompatActivity {

    ImageView imageView;
    CardView buttonSubscribe;
    TextView txtRestore, txtPrice;
    Dialog load;

    @Nullable
    private Offering offering;
    private FirebaseAnalytics mFirebaseAnalytics;
    private FirebaseRemoteConfig mFirebaseRemoteConfig;

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private final String premStatus = "premStatus";
    boolean isPremiums = false;

    private InterstitialAd mInterstitialAd;
    private AdsManager adsManager;

    Boolean yearlyTest, monthlyold, monthlynew;

    public static final String MONTHLYNEW = "monthlynew";
    public static final String MONTHLYOLD = "monthlyold";
    public static final String YEARLY = "yearly";

    private Package packs;

    @SuppressLint("CommitPrefEdits")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_prem);

        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {

            }
        });

        adsManager = AdsManager.getInstance(this);
        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId(getResources().getString(R.string.podsbattery_gecis_1));
        mInterstitialAd.loadAd(new AdRequest.Builder().build());
        mInterstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                mInterstitialAd.loadAd(new AdRequest.Builder().build());
            }
        });

        changeStatusBarColor();
        dialogShow();

        size();
        RelativeLayout no = findViewById(R.id.relative);
        ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.MATCH_PARENT);
        params.setMargins(0,0,0, size());
        no.setLayoutParams(params);

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        mFirebaseAnalytics.logEvent("Market_Show", null);

        buttonSubscribe = findViewById(R.id.buttonSubscribe);
        imageView = findViewById(R.id.imgExit);
        txtPrice = findViewById(R.id.priceTxt);
        txtRestore = findViewById(R.id.restore);

        sharedPreferences = this.getSharedPreferences(premStatus, MODE_PRIVATE);
        editor = sharedPreferences.edit();
        getPremiumStatus(premStatus);

        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(0)
                .build();
        mFirebaseRemoteConfig.setConfigSettingsAsync(configSettings);
        mFirebaseRemoteConfig.setDefaultsAsync(R.xml.remote_config_defaults);
        fetchAndActivate();

        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showNewIns();
                finish();
            }
        });

        buttonSubscribe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (offering != null) {
                    mFirebaseAnalytics.logEvent("Market_Annual_Click", null);
                    makePurchase(packs);
                }
            }
        });

        txtRestore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                restorePurchases();
            }
        });


        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            public void run() {
                imageView.setVisibility(View.VISIBLE);
            }
        }, 3000);

        getAvailableProducts();
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

    private void unlockProFeatures() {
        savePremiumStatus(premStatus, true);
        mFirebaseAnalytics.logEvent("All_Buy_Successful", null);
        if (yearlyTest) {
            mFirebaseAnalytics.logEvent("Yearly_Successful", null);
        } else if (monthlynew) {
            mFirebaseAnalytics.logEvent("MonthlyNew_Successful", null);
        } else if (monthlyold) {
            mFirebaseAnalytics.logEvent("MonthlyOld_Successful", null);
        } else {
            mFirebaseAnalytics.logEvent("MonthlyOld_Default_Successful", null);
        }
        Toasty.success(this, getString(R.string.pro_thanks), Toast.LENGTH_SHORT).show();
        finish();
    }

    private void getAvailableProducts() {
        Purchases.getSharedInstance().getOfferings(new ReceiveOfferingsListener() {
            @Override
            public void onReceived(@NonNull Offerings offerings) {
                offering = offerings.getCurrent();
                buttonSubscribe.setEnabled(true);
                if (offering != null) {
                    Package packageMonthly = offering.getPackage("MonthlyNew");
                    packs = packageMonthly;
                    if (txtPrice != null) {
                        String text = getResources().getString(R.string.onlyone) + " " + packageMonthly.getProduct().getPrice() + " " + getResources().getString(R.string.onlytwo);
                        txtPrice.setText(text);
                    }
                    mFirebaseAnalytics.logEvent("MonthlyNew_Show", null);
                }
            }

            @Override
            public void onError(@NonNull PurchasesError error) {
                Log.d("###", "Error: " + error);
                buttonSubscribe.setEnabled(false);
            }
        });
    }

    private void makePurchase(@Nullable Package pack) {
        if (pack == null) {
            return;
        }
        load.show();
        Purchases.getSharedInstance().purchasePackage(this, pack, new MakePurchaseListener() {
            @Override
            public void onCompleted(@NonNull Purchase purchase, @NonNull PurchaserInfo purchaserInfo) {
                if (load != null) {
                    load.dismiss();
                }

                EntitlementInfo proEntitlement = purchaserInfo.getEntitlements().get(MarketConf.PRO_ENTITLEMENT);
                if (proEntitlement != null && proEntitlement.isActive()) {
                    unlockProFeatures();
                }
            }

            @Override
            public void onError(@NonNull PurchasesError error, boolean userCancelled) {
                Log.d("###", "Error: " + error);
                Toasty.warning(PremActivity.this, getString(R.string.error_toast), Toast.LENGTH_SHORT).show();
                if (load != null) {
                    load.dismiss();
                }
            }
        });
    }

    private void restorePurchases() {
        load.show();
        mFirebaseAnalytics.logEvent("Market_Restore_Click", null);
        Purchases.getSharedInstance().restorePurchases(new ReceivePurchaserInfoListener() {
            @Override
            public void onReceived(@NonNull PurchaserInfo purchaserInfo) {
                EntitlementInfo entitlement = purchaserInfo.getEntitlements().get(MarketConf.PRO_ENTITLEMENT);
                if (load != null) {
                    load.dismiss();
                }

                if (entitlement != null && entitlement.isActive()) {
                    savePremiumStatus(premStatus, true);
                    Toasty.success(PremActivity.this, getString(R.string.restore_thanks), Toast.LENGTH_SHORT).show();
                    mFirebaseAnalytics.logEvent("Restore_Successful", null);
                    finish();
                }
            }

            @Override
            public void onError(@NonNull PurchasesError error) {
                Log.d("###", "Error: " + error);
                Toasty.warning(PremActivity.this, getString(R.string.error_toast), Toast.LENGTH_SHORT).show();
                if (load != null) {
                    load.dismiss();
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        showNewIns();
        finish();
    }

    private int size() {
        Resources resources = this.getResources();
        int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return resources.getDimensionPixelSize(resourceId);
        }
        return 0;
    }

    private void dialogShow () {
        load = new Dialog(PremActivity.this);
        load.requestWindowFeature(Window.FEATURE_NO_TITLE);
        load.setContentView(R.layout.dialog_progress);
        load.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        load.setCancelable(false);
    }

    private void changeStatusBarColor() {
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.setStatusBarColor(Color.TRANSPARENT);
        Tools.clearSystemBarLight(this);
    }

    public void showNewIns() {
        mFirebaseAnalytics.logEvent("gecis_goster", null);
        if (mInterstitialAd.isLoaded()) {
            mInterstitialAd.show();
        } else {
            mInterstitialAd.loadAd(new AdRequest.Builder().build());
        }
    }

    public void fetchAndActivate() {
        mFirebaseRemoteConfig.fetchAndActivate().addOnCompleteListener(this, new com.google.android.gms.tasks.OnCompleteListener<Boolean>() {
            @Override
            public void onComplete(@NonNull com.google.android.gms.tasks.Task<Boolean> task) {
                if (task.isSuccessful()) {
                    yearlyTest = mFirebaseRemoteConfig.getBoolean(YEARLY);
                    monthlynew = mFirebaseRemoteConfig.getBoolean(MONTHLYNEW);
                    monthlyold = mFirebaseRemoteConfig.getBoolean(MONTHLYOLD);

                    editor.putBoolean(YEARLY, yearlyTest);
                    editor.putBoolean(MONTHLYNEW, monthlynew);
                    editor.putBoolean(MONTHLYOLD, monthlyold);
                    editor.commit();
                }
            }
        });
    }
}