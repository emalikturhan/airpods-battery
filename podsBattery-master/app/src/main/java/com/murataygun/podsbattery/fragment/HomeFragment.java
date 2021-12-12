package com.murataygun.podsbattery.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Lifecycle;

import com.murataygun.podsbattery.R;
import com.murataygun.podsbattery.data.PodsData;
import com.murataygun.podsbattery.service.ActionConstant;
import com.murataygun.podsbattery.service.PodsBatteryService;
import com.murataygun.podsbattery.util.AdsManager;
import com.murataygun.podsbattery.util.AdsUtil;

import java.util.List;
import java.util.Objects;

public class HomeFragment extends Fragment {

    private final BroadcastReceiver bluetoothLeUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!HomeFragment.this.getLifecycle().getCurrentState().equals(Lifecycle.State.DESTROYED) && (Objects.requireNonNull(requireActivity().getSupportFragmentManager().getPrimaryNavigationFragment()).getChildFragmentManager().getPrimaryNavigationFragment() instanceof HomeFragment)) {
                AdsManager adsManager = AdsManager.getInstance(requireActivity());
                if (adsManager.shouldShowAnAd() && adsManager.isSecondLaunchInterstitialAdLoaded()) {
                    adsManager.setLastAdShownTime(System.currentTimeMillis());
                    requireActivity().sendBroadcast(new Intent(ActionConstant.ACTION_SHOW_AD));
                }

                PodsData podsData = (PodsData) intent.getSerializableExtra(ActionConstant.KEY_DATA);
                HomeFragment.this.updateView(podsData);
            }
        }
    };


    public static HomeFragment newInstance() {
        return new HomeFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.home_fragment, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ActionConstant.ACTION_UPDATE_AIR_PODS);
        intentFilter.addAction(ActionConstant.ACTION_CONNECTED_AIR_PODS);
        intentFilter.addAction(ActionConstant.ACTION_DISCONNECTED_AIR_PODS);
        intentFilter.addAction(ActionConstant.ACTION_BLUETOOTH_TURNED_ON);
        intentFilter.addAction(ActionConstant.ACTION_BLUETOOTH_TURNED_OFF);
        requireActivity().registerReceiver(this.bluetoothLeUpdateReceiver, intentFilter);
        updateView(PodsBatteryService.lastPodsData);
    }

    @Override
    public void onPause() {
        super.onPause();
        requireActivity().unregisterReceiver(this.bluetoothLeUpdateReceiver);
    }

    private void updateView(PodsData podsData) {
        FragmentManager fragmentManager = Objects.requireNonNull(requireActivity().getSupportFragmentManager().getPrimaryNavigationFragment()).getChildFragmentManager();
        if (podsData == null || !podsData.checkPodsConnected()) {
            List<Fragment> fragments = fragmentManager.getFragments();
            if (!(fragments.get(fragments.size() - 1) instanceof WaitingForConnectionFragment)) {
                FragmentTransaction beginTransaction = fragmentManager.beginTransaction();
                beginTransaction.setCustomAnimations(R.anim.enter_from_left, R.anim.exit_to_right, R.anim.enter_from_right, R.anim.exit_to_left);
                beginTransaction.replace(R.id.fragment1, WaitingForConnectionFragment.newInstance());
                beginTransaction.commit();
            }
        } else {
            List<Fragment> fragments2 = fragmentManager.getFragments();
            if (!(fragments2.get(fragments2.size() - 1) instanceof AllInCaseChargingFragment)) {
                FragmentTransaction beginTransaction2 = fragmentManager.beginTransaction();
                beginTransaction2.setCustomAnimations(R.anim.enter_from_left, R.anim.exit_to_right, R.anim.enter_from_right, R.anim.exit_to_left);
                beginTransaction2.replace(R.id.fragment1, AllInCaseChargingFragment.newInstance(podsData));
                beginTransaction2.commit();
            }
        }
    }
}