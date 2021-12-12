package com.murataygun.podsbattery.service;

public class ActionConstant {
    private ActionConstant() {
        throw new IllegalStateException("Utility class");
    }

    public static final String ACTION_BLUETOOTH_TURNED_OFF = ActionConstant.class.getCanonicalName() + ".ACTION_BLUETOOTH_TURNED_OFF";
    public static final String ACTION_BLUETOOTH_TURNED_ON = ActionConstant.class.getCanonicalName() + ".ACTION_BLUETOOTH_TURNED_ON";
    public static final String ACTION_CONNECTED_AIR_PODS = ActionConstant.class.getCanonicalName() + ".ACTION_CONNECTED_AIR_PODS";
    public static final String ACTION_DISCONNECTED_AIR_PODS = ActionConstant.class.getCanonicalName() + ".ACTION_DISCONNECTED_AIR_PODS";
    public static final String ACTION_UPDATE_AIR_PODS = ActionConstant.class.getCanonicalName() + ".ACTION_UPDATE_AIR_PODS";

    public static final String KEY_DATA = ActionConstant.class.getCanonicalName() + ".DATA";

    public static final String ACTION_REMOVE_ADS_PURCHASED = ActionConstant.class.getCanonicalName() + ".ACTION_REMOVE_ADS_PURCHASED";
    public static final String ACTION_SHOW_AD = ActionConstant.class.getCanonicalName() + ".ACTION_SHOW_AD";

}
