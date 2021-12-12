package com.murataygun.podsbattery.fragment;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.ads.AdView;
import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;
import com.murataygun.podsbattery.R;
import com.murataygun.podsbattery.activity.PremActivity;
import com.murataygun.podsbattery.activity.SettingsActivity;
import com.murataygun.podsbattery.databinding.WaitingForConnectionFragmentBinding;
import com.murataygun.podsbattery.market.MarketConf;
import com.murataygun.podsbattery.util.AdsManager;
import com.murataygun.podsbattery.util.AdsUtil;
import com.murataygun.podsbattery.util.Util;
import com.revenuecat.purchases.EntitlementInfo;
import com.revenuecat.purchases.PurchaserInfo;
import com.revenuecat.purchases.Purchases;
import com.revenuecat.purchases.PurchasesError;
import com.revenuecat.purchases.interfaces.ReceivePurchaserInfoListener;

import es.dmoral.toasty.Toasty;

import static android.content.Context.MODE_PRIVATE;

public class WaitingForConnectionFragment extends Fragment {

    private WaitingForConnectionFragmentBinding binding;
    private AdsManager adsManager;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private final String premStatus = "premStatus";
    boolean isPremiums = false;

    public static WaitingForConnectionFragment newInstance() {
        return new WaitingForConnectionFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        this.binding = WaitingForConnectionFragmentBinding.inflate(inflater, container, false);
        adsManager = AdsManager.getInstance(getContext());
        sharedPreferences = getActivity().getSharedPreferences(premStatus, MODE_PRIVATE);
        editor = sharedPreferences.edit();
        getPremiumStatus(premStatus);
        checkPurchases();
        AdsUtil.loadBanner(getContext(), binding.adView);
        return binding.getRoot();
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

    private void getPremiumStatus(String keyName) {
        isPremiums = sharedPreferences.getBoolean(keyName, false);
        if (isPremiums) {
            binding.buyPremText.setText(getString(R.string.now_prem));
            binding.buyPrem.setClickable(false);
            adsManager.turnOffAds();
        } else {
            adsManager.showAds();
        }
    }

    private void savePremiumStatus(String keyName, boolean status) {
        editor.putBoolean(keyName, status);
        editor.commit();
        getPremiumStatus(keyName);
    }

    @Override
    public void onResume() {
        super.onResume();
        getPremiumStatus(premStatus);
    }

    @SuppressLint("ResourceType")
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        binding.buyPrem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getContext(), PremActivity.class));
            }
        });
        binding.settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getContext(), SettingsActivity.class));
            }
        });
    }

}