package com.murataygun.podsbattery.fragment;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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

import com.murataygun.podsbattery.R;
import com.murataygun.podsbattery.activity.PremActivity;
import com.murataygun.podsbattery.activity.SettingsActivity;
import com.murataygun.podsbattery.data.PodsData;
import com.murataygun.podsbattery.data.PodsModel;
import com.murataygun.podsbattery.databinding.AllInCaseChargingFragmentBinding;
import com.murataygun.podsbattery.market.MarketConf;
import com.murataygun.podsbattery.service.ActionConstant;
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

public class AllInCaseChargingFragment extends Fragment {

    private AllInCaseChargingFragmentBinding binding;
    private PodsData podsData;
    private AdsManager adsManager;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private final String premStatus = "premStatus";
    boolean isPremiums = false;
    private final BroadcastReceiver bluetoothLeUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            PodsData podsDataLoc = (PodsData) intent.getSerializableExtra(ActionConstant.KEY_DATA);
            if (ActionConstant.ACTION_UPDATE_AIR_PODS.equals(action)) {
                updatePodsView(podsDataLoc);
            }
        }
    };

    public AllInCaseChargingFragment(PodsData podsData) {
        this.podsData = podsData;
    }

    public static AllInCaseChargingFragment newInstance(PodsData podsData) {
        return new AllInCaseChargingFragment(podsData);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        this.binding = AllInCaseChargingFragmentBinding.inflate(inflater, container, false);
        adsManager = AdsManager.getInstance(getContext());
        sharedPreferences = getActivity().getSharedPreferences(premStatus, MODE_PRIVATE);
        editor = sharedPreferences.edit();
        getPremiumStatus(premStatus);
        checkPurchases();
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
            AdsUtil.loadBanner(getContext(), binding.adView);
        }
    }

    private void savePremiumStatus(String keyName, boolean status) {
        editor.putBoolean(keyName, status);
        editor.commit();
        getPremiumStatus(keyName);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        updatePodsView(podsData);
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

    @Override
    public void onResume() {
        super.onResume();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ActionConstant.ACTION_UPDATE_AIR_PODS);
        intentFilter.addAction(ActionConstant.ACTION_CONNECTED_AIR_PODS);
        intentFilter.addAction(ActionConstant.ACTION_DISCONNECTED_AIR_PODS);
        requireActivity().registerReceiver(this.bluetoothLeUpdateReceiver, intentFilter);
        getPremiumStatus(premStatus);
    }

    @Override
    public void onPause() {
        super.onPause();
        requireActivity().unregisterReceiver(this.bluetoothLeUpdateReceiver);
    }

    @SuppressLint({"SetTextI18n"})
    private void updatePodsView(PodsData podsData) {
        if (podsData != null) {
            boolean leftConnect = podsData.getLeftStatus() <= 10;
            boolean rightConnect = podsData.getRightStatus() <= 10;
            boolean caseConnect = podsData.getCaseStatus() <= 10;

            setPodsType(podsData, leftConnect, rightConnect, caseConnect);

            if (System.currentTimeMillis() - podsData.getLastSeenConnected() < Util.TIMEOUT_CONNECTED) {

                binding.leftPodText.setVisibility(View.VISIBLE);
                binding.rightPodText.setVisibility(View.VISIBLE);
                binding.podCaseText.setVisibility(View.VISIBLE);

                int leftBattery = calculateStatus(podsData.getLeftStatus());
                int rightBattery = calculateStatus(podsData.getRightStatus());
                int caseBattery = calculateStatus(podsData.getCaseStatus());

                binding.txtPodName.setText(podsData.getName());

                binding.leftPodText.setText(leftBattery + "%");
                binding.rightPodText.setText(rightBattery + "%");
                binding.podCaseText.setText(caseBattery + "%");

                binding.leftPodText.setVisibility(leftConnect ? View.VISIBLE : View.INVISIBLE);
                binding.rightPodText.setVisibility(rightConnect ? View.VISIBLE : View.INVISIBLE);
                binding.podCaseText.setVisibility(caseConnect ? View.VISIBLE : View.INVISIBLE);

                binding.leftMeterView.setCharging(podsData.isChargeL());
                binding.rightMeterView.setCharging(podsData.isChargeR());
                binding.caseMeterView.setCharging(podsData.isChargeCase());

                binding.leftMeterView.setChargeLevel(leftBattery);
                binding.rightMeterView.setChargeLevel(rightBattery);
                binding.caseMeterView.setChargeLevel(caseBattery);

            } else {
                binding.leftPodText.setVisibility(View.INVISIBLE);
                binding.rightPodText.setVisibility(View.INVISIBLE);
                binding.podCaseText.setVisibility(View.INVISIBLE);
            }
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private void setPodsType(PodsData podsData, boolean leftConnect, boolean rightConnect, boolean caseConnect) {
        if (podsData.getModel().equals(PodsModel.AIR_PODS_NORMAL)) {
            binding.leftPodImg.setImageDrawable(leftConnect ? requireActivity().getDrawable(R.drawable.pod) : requireActivity().getDrawable(R.drawable.pod_disconnected));
            binding.rightPodImg.setImageDrawable(rightConnect ? requireActivity().getDrawable(R.drawable.pod) : requireActivity().getDrawable(R.drawable.pod_disconnected));
            binding.podCaseImg.setImageDrawable(caseConnect ? requireActivity().getDrawable(R.drawable.pod_case) : requireActivity().getDrawable(R.drawable.pod_case_disconnected));
        } else if (podsData.getModel().equals(PodsModel.AIR_PODS_PRO)) {
            binding.leftPodImg.setImageDrawable(leftConnect ? requireActivity().getDrawable(R.drawable.podpro) : requireActivity().getDrawable(R.drawable.podpro_disconnected));
            binding.rightPodImg.setImageDrawable(rightConnect ? requireActivity().getDrawable(R.drawable.podpro) : requireActivity().getDrawable(R.drawable.podpro_disconnected));
            binding.podCaseImg.setImageDrawable(caseConnect ? requireActivity().getDrawable(R.drawable.podpro_case) : requireActivity().getDrawable(R.drawable.podpro_case_disconnected));
        }
    }

    private int calculateStatus(int status) {
        if (status == 10)
            return 100;
        else
            return ((status < 10) ? status * 10 + 5 : 0);
    }
}