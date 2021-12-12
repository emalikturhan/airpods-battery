package com.murataygun.podsbattery.service;

import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;

import com.murataygun.podsbattery.data.PodsData;
import com.murataygun.podsbattery.util.Util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class PodsBatteryService extends Service {
    private static String TAG = PodsBatteryService.class.getSimpleName();

    private BroadcastReceiver btReceiver = null, screenReceiver = null;
    private static ArrayList<ScanResult> recentBeacons = new ArrayList<>();
    private static BluetoothLeScanner btScanner;
    private static final long RECENT_BEACONS_MAX_T_NS = 10000000000L; //10s
    @SuppressLint("StaticFieldLeak")
    public static NotificationThread notificationThread = null;

    public String connectedPodName;
    public static PodsData lastPodsData;

    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onBatchScanResults(List<ScanResult> scanResults) {
            for (ScanResult result : scanResults)
                onScanResult(-1, result);
            super.onBatchScanResults(scanResults);
        }

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            try {
                byte[] data = Objects.requireNonNull(result.getScanRecord()).getManufacturerSpecificData(76);

                if (data == null || data.length != 27)
                    return;

                recentBeacons.add(result);

                if (Util.ENABLE_LOGGING) {
                    Log.d(TAG, "" + result.getRssi() + "db");
                    Log.d(TAG, Util.decodeHex(data));
                }

                ScanResult strongestBeacon = null;
                for (int i = 0; i < recentBeacons.size(); i++) {
                    if (SystemClock.elapsedRealtimeNanos() - recentBeacons.get(i).getTimestampNanos() > RECENT_BEACONS_MAX_T_NS) {
                        recentBeacons.remove(i);
                        i--;
                        continue;
                    }
                    if (strongestBeacon == null || strongestBeacon.getRssi() < recentBeacons.get(i).getRssi())
                        strongestBeacon = recentBeacons.get(i);
                }

                if (strongestBeacon != null && strongestBeacon.getDevice().getAddress().equals(result.getDevice().getAddress()))
                    strongestBeacon = result;

                result = strongestBeacon;
                assert result != null;
                if (result.getRssi() < -60)
                    return;

                broadcastUpdate(result, connectedPodName);
                Log.i(TAG, lastPodsData.toString());
            } catch (Throwable t) {
                if (Util.ENABLE_LOGGING)
                    Log.d(TAG, "" + t);
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.bluetooth.device.action.ACL_CONNECTED");
        intentFilter.addAction("android.bluetooth.device.action.ACL_DISCONNECTED");
        intentFilter.addAction("android.bluetooth.device.action.BOND_STATE_CHANGED");
        intentFilter.addAction("android.bluetooth.device.action.NAME_CHANGED");
        intentFilter.addAction("android.bluetooth.adapter.action.CONNECTION_STATE_CHANGED");
        intentFilter.addAction("android.bluetooth.adapter.action.STATE_CHANGED");
        intentFilter.addAction("android.bluetooth.headset.profile.action.CONNECTION_STATE_CHANGED");
        intentFilter.addAction("android.bluetooth.headset.action.VENDOR_SPECIFIC_HEADSET_EVENT");
        intentFilter.addAction("android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED");
        intentFilter.addAction("android.bluetooth.a2dp.profile.action.PLAYING_STATE_CHANGED");
        intentFilter.addCategory("android.bluetooth.headset.intent.category.companyid.76");

        try {
            unregisterReceiver(btReceiver);
        } catch (Throwable ignored) {
        }

        btReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                BluetoothDevice bluetoothDevice = intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
                String action = intent.getAction();
                assert action != null;
                if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                    int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                    if (lastPodsData != null) {
                        lastPodsData = new PodsData(lastPodsData.getAddress(), lastPodsData.getName(), false);
                    }
                    if (state == BluetoothAdapter.STATE_OFF || state == BluetoothAdapter.STATE_TURNING_OFF) {
//                        maybeConnected = false;
//                        recentBeacons.clear();
                        broadcastUpdate(ActionConstant.ACTION_BLUETOOTH_TURNED_OFF);
                        stopAirPodsScanner();
                    }

                    if (state == BluetoothAdapter.STATE_ON) {
                        broadcastUpdate(ActionConstant.ACTION_BLUETOOTH_TURNED_ON);
                        startAirPodsScanner();
                    }
                } else if (action.equals(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)) {
                    int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                    assert bluetoothDevice != null;
                    if (state == 2 && Util.checkUUID(bluetoothDevice)) {
                        PodsBatteryService.this.connectedPodName = bluetoothDevice.getName();
                        broadcastUpdate(ActionConstant.ACTION_CONNECTED_AIR_PODS, bluetoothDevice, bluetoothDevice.getName(), true);
                        startAirPodsScanner();
                    } else if (state == 0) {
                        PodsBatteryService.this.connectedPodName = bluetoothDevice.getName();
                        broadcastUpdate(ActionConstant.ACTION_DISCONNECTED_AIR_PODS, bluetoothDevice, bluetoothDevice.getName(), false);
                        stopAirPodsScanner();
                    }
                }

                if (bluetoothDevice != null && !action.isEmpty() && Util.checkUUID(bluetoothDevice)) {
                    if (action.equals(BluetoothDevice.ACTION_ACL_CONNECTED)) {
                        PodsBatteryService.this.connectedPodName = bluetoothDevice.getName();
                        broadcastUpdate(ActionConstant.ACTION_CONNECTED_AIR_PODS, bluetoothDevice, bluetoothDevice.getName(), true);
                        startAirPodsScanner();

                    }

                    if (action.equals(BluetoothDevice.ACTION_ACL_DISCONNECTED) || action.equals(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED)) {
                        PodsBatteryService.this.connectedPodName = bluetoothDevice.getName();
                        broadcastUpdate(ActionConstant.ACTION_DISCONNECTED_AIR_PODS, bluetoothDevice, bluetoothDevice.getName(), false);
                        stopAirPodsScanner();
                    }
                }


            }
        };

        try {
            registerReceiver(btReceiver, intentFilter);
        } catch (Throwable ignored) {
        }

        BluetoothAdapter ba = ((BluetoothManager) Objects.requireNonNull(getSystemService(Context.BLUETOOTH_SERVICE))).getAdapter();
        ba.getProfileProxy(getApplicationContext(), new BluetoothProfile.ServiceListener() {
            @Override
            public void onServiceConnected(int i, BluetoothProfile bluetoothProfile) {
                if (i == BluetoothProfile.HEADSET) {
                    BluetoothHeadset h = (BluetoothHeadset) bluetoothProfile;
                    for (BluetoothDevice bluetoothDevice : h.getConnectedDevices())
                        if (Util.checkUUID(bluetoothDevice)) {
                            PodsBatteryService.this.connectedPodName = bluetoothDevice.getName();
                            broadcastUpdate(ActionConstant.ACTION_CONNECTED_AIR_PODS, bluetoothDevice, bluetoothDevice.getName(), true);
                            startAirPodsScanner();
                            break;
                        }
                }
            }

            @Override
            public void onServiceDisconnected(int i) {
                if (i == BluetoothProfile.HEADSET) {
                    stopAirPodsScanner();
                }
            }
        }, BluetoothProfile.HEADSET);

        if (ba.isEnabled())
            startAirPodsScanner();

        try {
            unregisterReceiver(screenReceiver);
        } catch (Throwable ignored) {
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        if (prefs.getBoolean("batterySaver", false)) {
            IntentFilter screenIntentFilter = new IntentFilter();
            screenIntentFilter.addAction(Intent.ACTION_SCREEN_ON);
            screenIntentFilter.addAction(Intent.ACTION_SCREEN_OFF);
            screenReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (Objects.equals(intent.getAction(), Intent.ACTION_SCREEN_OFF)) {
                        stopAirPodsScanner();
                    } else if (Objects.equals(intent.getAction(), Intent.ACTION_SCREEN_ON)) {
                        BluetoothAdapter ba = ((BluetoothManager) Objects.requireNonNull(getSystemService(Context.BLUETOOTH_SERVICE))).getAdapter();
                        if (ba.isEnabled())
                            startAirPodsScanner();
                    }
                }
            };

            try {
                registerReceiver(screenReceiver, screenIntentFilter);
            } catch (Throwable ignored) {
            }
        }

    }

    private void startAirPodsScanner() {
        try {
            if (Util.ENABLE_LOGGING)
                Log.d(TAG, "START SCANNER");

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            BluetoothManager btManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            assert btManager != null;
            BluetoothAdapter btAdapter = btManager.getAdapter();

            if (prefs.getBoolean("batterySaver", false)) {
                if (btScanner != null) {
                    btScanner.stopScan(new ScanCallback() {
                        @Override
                        public void onScanResult(int callbackType, ScanResult result) {
                        }
                    });
                }
            }

            btScanner = btAdapter.getBluetoothLeScanner();

            if (!btAdapter.isEnabled())
                throw new Exception("BT Off");

            List<ScanFilter> filters = getScanFilters();
            ScanSettings settings;

            if (prefs.getBoolean("batterySaver", false))
                settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_POWER).setReportDelay(0).build();
            else
                settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).setReportDelay(2).build();

            btScanner.startScan(filters, settings, scanCallback);

        } catch (Throwable t) {
            if (Util.ENABLE_LOGGING)
                Log.d(TAG, "" + t);
        }
    }

    private List<ScanFilter> getScanFilters() {
        byte[] manufacturerData = new byte[27];
        byte[] manufacturerDataMask = new byte[27];

        manufacturerData[0] = 7;
        manufacturerData[1] = 25;

        manufacturerDataMask[0] = -1;
        manufacturerDataMask[1] = -1;

        ScanFilter.Builder builder = new ScanFilter.Builder();
        builder.setManufacturerData(76, manufacturerData, manufacturerDataMask);

        return Collections.singletonList(builder.build());
    }

    private void stopAirPodsScanner() {
        try {
            if (btScanner != null) {
                if (Util.ENABLE_LOGGING)
                    Log.d(TAG, "STOP SCANNER");

                btScanner.stopScan(scanCallback);
            }
        } catch (Throwable ignored) {
            Log.d(TAG, "Throwable" + ignored.getMessage());
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (btReceiver != null) unregisterReceiver(btReceiver);
        if (screenReceiver != null) unregisterReceiver(screenReceiver);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (notificationThread == null || !notificationThread.isAlive()) {
            notificationThread = new NotificationThread(this);
            notificationThread.start();
        }
        return START_STICKY;
    }

    private void broadcastUpdate(ScanResult scanResult, String name) {
        Intent intent = new Intent(ActionConstant.ACTION_UPDATE_AIR_PODS);
        lastPodsData = new PodsData(scanResult, name, lastPodsData.isConnected());
        intent.putExtra(ActionConstant.KEY_DATA, lastPodsData);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(String action, BluetoothDevice bluetoothDevice, String name, boolean connected) {
        Intent intent = new Intent(action);
        lastPodsData = new PodsData(bluetoothDevice.getAddress(), name, connected);
        intent.putExtra(ActionConstant.KEY_DATA, lastPodsData);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(String action) {
        Intent intent = new Intent(action);
        sendBroadcast(intent);
    }
}

