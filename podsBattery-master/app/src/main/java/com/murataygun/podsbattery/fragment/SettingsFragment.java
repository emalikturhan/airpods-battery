package com.murataygun.podsbattery.fragment;

import android.content.Intent;
import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.murataygun.podsbattery.BuildConfig;
import com.murataygun.podsbattery.R;
import com.murataygun.podsbattery.activity.AboutActivity;
import com.murataygun.podsbattery.activity.MainActivity;
import com.murataygun.podsbattery.service.PodsBatteryReceiver;
import com.murataygun.podsbattery.service.PodsBatteryService;
import com.murataygun.podsbattery.util.AdsManager;
import com.murataygun.podsbattery.util.Util;

public class SettingsFragment extends PreferenceFragmentCompat {


    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);

        Preference mRemoveAds = getPreferenceManager().findPreference("removeAds");
        assert mRemoveAds != null;
        mRemoveAds.setOnPreferenceClickListener(preference -> {
            // ((MainActivity) SettingsFragment.this.requireActivity()).purchaseRemoveAds();
            return true;
        });

        if (!AdsManager.getInstance(requireContext()).showAds()) {
            mRemoveAds.setVisible(false);
        }

        Preference mBatterySaver = getPreferenceManager().findPreference("batterySaver");
        assert mBatterySaver != null;
        mBatterySaver.setOnPreferenceClickListener(preference -> {
            PodsBatteryReceiver.restartPodsService(requireContext());
            return true;
        });

        Preference mShowNotification = getPreferenceManager().findPreference("showNotification");
        assert mShowNotification != null;
        mShowNotification.setOnPreferenceClickListener(preference -> {
            PodsBatteryService.notificationThread.cancelNotification();
            return true;
        });

        Preference mRatePreference = getPreferenceManager().findPreference("rate");
        assert mRatePreference != null;
        mRatePreference.setOnPreferenceClickListener(preference -> {
            Util.setBtnLoveOnclick(requireActivity());
            return true;
        });

        Preference mAboutPreference = getPreferenceManager().findPreference("about");
        assert mAboutPreference != null;
        mAboutPreference.setSummary(String.format("%s v%s", getString(R.string.app_name), BuildConfig.VERSION_NAME));
        mAboutPreference.setOnPreferenceClickListener(preference -> {
            requireActivity().startActivity(new Intent(getContext(), AboutActivity.class));
            return true;
        });

    }
}