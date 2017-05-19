package com.hc.beaconcenter;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ExpandableListView;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by ZS on 2017/1/12.
 */
public class iBeaconAnalystActivity extends Activity{
    //BLE 基本数据
    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    //iBeacon设备数据
    public static final String EXTRAS_IBEACON_MAJOR = "IBEACON_MAJOR";
    public static final String EXTRAS_IBEACON_MINOR = "IBEACON_MINOR";
    public static final String EXTRAS_IBEACON_UUID = "IBEACON_UUID";
    //iBeacon传感器数据
    public static final String EXTRAS_IBEACON_SENSOR1 = "IBEACON_SENSOR1";
    public static final String EXTRAS_IBEACON_SENSOR2 = "IBEACON_SENSOR2";
    public static final String EXTRAS_IBEACON_SENSOR3 = "IBEACON_SENSOR3";
    public static final String EXTRAS_IBEACON_SENSOR4 = "IBEACON_SENSOR4";

    //数据获取变量
    private String mDeviceName;
    private String mDeviceAddress;
    private String iBeacon_major;
    private String iBeacon_minor;
    private String iBeacon_uuid;
    private ArrayList<String> iBeacon_sensor1;
    private ArrayList<String> iBeacon_sensor2;
    private ArrayList<String> iBeacon_sensor3;
    private ArrayList<String> iBeacon_sensor4;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ibeacon_analyst);
        //提取intent传过来的数据
        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        iBeacon_major = intent.getStringExtra(EXTRAS_IBEACON_MAJOR);
        iBeacon_minor = intent.getStringExtra(EXTRAS_IBEACON_MINOR);
        iBeacon_uuid = intent.getStringExtra(EXTRAS_IBEACON_UUID);
        iBeacon_sensor1 =intent.getStringArrayListExtra(EXTRAS_IBEACON_SENSOR1);
        iBeacon_sensor2 =intent.getStringArrayListExtra(EXTRAS_IBEACON_SENSOR2);
        iBeacon_sensor3 =intent.getStringArrayListExtra(EXTRAS_IBEACON_SENSOR3);
        iBeacon_sensor4 =intent.getStringArrayListExtra(EXTRAS_IBEACON_SENSOR4);

        ((TextView) findViewById(R.id.analyst_address)).setText(mDeviceAddress+"("+"iBeacon"+")");

        ((TextView) findViewById(R.id.analyst_major_minor)).setText("major:"+iBeacon_major+"     minor:"+iBeacon_minor);
        ((TextView) findViewById(R.id.analyst_uuid)).setText(iBeacon_uuid);
        int sensor1Avg=0,sensor2Avg=0,sensor3Avg=0,sensor4Avg=0,sum1=0,sum2=0,sum3=0,sum4=0;
        int size=iBeacon_sensor1.size();
        for(int i=0;i<size;i++){
            sum1+=Integer.parseInt(iBeacon_sensor1.get(i));
            sum2+=Integer.parseInt(iBeacon_sensor2.get(i));
            sum3+=Integer.parseInt(iBeacon_sensor3.get(i));
            sum4+=Integer.parseInt(iBeacon_sensor4.get(i));
        }
        sensor1Avg=sum1/size;
        sensor2Avg=sum2/size;
        sensor3Avg=sum3/size;
        sensor4Avg=sum4/size;
        ((TextView) findViewById(R.id.analyst_sensor1)).setText(String.valueOf(sensor1Avg));
        ((TextView) findViewById(R.id.analyst_sensor2)).setText(String.valueOf(sensor2Avg));
        ((TextView) findViewById(R.id.analyst_sensor3)).setText(String.valueOf(sensor3Avg));
        ((TextView) findViewById(R.id.analyst_sensor4)).setText(String.valueOf(sensor4Avg));

        getActionBar().setDisplayHomeAsUpEnabled(true);//返回键
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
