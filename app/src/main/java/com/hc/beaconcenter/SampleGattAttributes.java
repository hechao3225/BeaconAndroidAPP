package com.hc.beaconcenter;

/**
 * Created by ZS on 2017/1/6.
 */

import java.util.HashMap;
import java.util.UUID;

/**
 * This class includes a small subset of standard GATT attributes for demonstration purposes.
 */
public class SampleGattAttributes {
    private static HashMap<String, String> attributes = new HashMap();
    public static String HEART_RATE_MEASUREMENT = "0000C004-0000-1000-8000-00805f9b34fb";//心率计特征值UUID
    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";//Sample Service的通知开关
    public static final UUID CLIENT_CHARACTERISTIC_CONFIG_UUID = UUID.fromString(CLIENT_CHARACTERISTIC_CONFIG);
    public final static UUID HEART_RATE_MEASUREMENT_UUID = UUID.fromString(HEART_RATE_MEASUREMENT);

    static {
        // 服务UUID及名称.
        attributes.put("0000fff00000-1000-8000-00805f9b34fb", "Heart Rate Service");
        attributes.put("0000180a-0000-1000-8000-00805f9b34fb", "Device Information Service");
        //特征值UUID及名称
        attributes.put(HEART_RATE_MEASUREMENT, "Heart Rate Measurement");
        attributes.put("00002a29-0000-1000-8000-00805f9b34fb", "Manufacturer Name String");
    }

    //根据UUID查找名称
    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
}