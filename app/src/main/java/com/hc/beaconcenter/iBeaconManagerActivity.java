package com.hc.beaconcenter;

import android.app.Activity;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

/**
 * Created by ZS on 2017/1/12.
 */
public class iBeaconManagerActivity extends Activity {
    private final static String TAG = iBeaconManagerActivity.class .getSimpleName();
    //BLE 基本数据
    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    //iBeacon设备数据
    public static final String EXTRAS_IBEACON_MAJOR = "IBEACON_MAJOR";
    public static final String EXTRAS_IBEACON_MINOR = "IBEACON_MINOR";
    public static final String EXTRAS_IBEACON_UUID = "IBEACON_UUID";
    //数据获取变量
    private String mDeviceName;
    private String mDeviceAddress;
    private String iBeacon_major;
    private String iBeacon_minor;
    private String iBeacon_uuid;

    private TextView mConnectionState;
    private TextView mDataField;
    private ExpandableListView mGattServicesList;
    private BluetoothLeService mBluetoothLeService;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private boolean mConnected = false;
    private BluetoothGattCharacteristic mNotifyCharacteristic;

    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ibeacon_manager);
        //提取intent传过来的数据
        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        iBeacon_major = intent.getStringExtra(EXTRAS_IBEACON_MAJOR);
        iBeacon_minor = intent.getStringExtra(EXTRAS_IBEACON_MINOR);
        iBeacon_uuid = intent.getStringExtra(EXTRAS_IBEACON_UUID);

        ((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress+"("+"iBeacon"+")");

        ((TextView) findViewById(R.id.deviceControl_major)).setText(iBeacon_major);
        ((TextView) findViewById(R.id.deviceControl_minor)).setText(iBeacon_minor);
        ((TextView) findViewById(R.id.deviceControl_uuid)).setText(iBeacon_uuid);

        mGattServicesList = (ExpandableListView) findViewById(R.id.gatt_services_list);
        mGattServicesList.setOnChildClickListener(servicesListClickListner);
        mConnectionState = (TextView) findViewById(R.id.connection_state);
        mDataField = (TextView) findViewById(R.id.data_value);

        getActionBar().setTitle(mDeviceName);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        boolean bll = bindService(gattServiceIntent, mServiceConnection,
                BIND_AUTO_CREATE);
        if (bll) {
            System.out.println("---------------");
        } else {
            System.out.println("===============");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        if (mConnected) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_connect:
                mBluetoothLeService.connect(mDeviceAddress);
                return true;
            case R.id.menu_disconnect:
                mBluetoothLeService.disconnect();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName,
                                       IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service) .getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // 自动连接
            Toast.makeText(iBeaconManagerActivity.this,  "正在连接...", Toast.LENGTH_SHORT).show();
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    //处理BluetoothLeService触发的事件（回调方法）
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device. This can be a
    // result of read
    // or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            //System.out.println("action = " + action);
            int status = intent.getIntExtra(BluetoothLeService.EXTRA_STATUS, BluetoothGatt.GATT_SUCCESS);

            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                updateConnectionState(R.string.connected);
                invalidateOptionsMenu();
                Toast.makeText(iBeaconManagerActivity.this,  "正在获取服务...", Toast.LENGTH_SHORT).show();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
                clearUI();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the
                // user interface.
                displayGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (BluetoothLeService.ACTION_DATA_NOTIFY.equals(action)) {
                // Notification
                byte  [] value = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                onCharacteristicChanged(value);
            }else if (BluetoothLeService.ACTION_DATA_READ.equals(action)) {
                onCharacteristicRead(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
            }
            else if (BluetoothLeService.ACTION_DATA_WRITE.equals(action)) {
                String uuidStr = intent.getStringExtra(BluetoothLeService.EXTRA_UUID);
                onCharacteristicWrite(uuidStr,status);
            }

        }
    };

    //数据读取成功后：显示
    private void onCharacteristicRead(String data) {
        if (data != null) {
            mDataField.setText(data);
        }
        Toast.makeText(iBeaconManagerActivity.this,  "数据读取成功！", Toast.LENGTH_SHORT).show();
    }

    //写入数据成功后
    private void onCharacteristicWrite(String uuidStr, int status) {
        Toast.makeText(iBeaconManagerActivity.this,  "数据写入成功！", Toast.LENGTH_SHORT).show();
    }

    //通知数据获取成功后：
    private void onCharacteristicChanged( byte[] value) {
        Toast.makeText(iBeaconManagerActivity.this,  "通知打开成功！", Toast.LENGTH_SHORT).show();
    }

    // If a given GATT characteristic is selected, check for supported features.
    // This sample
    // demonstrates 'Read' and 'Notify' features. See
    // http://d.android.com/reference/android/bluetooth/BluetoothGatt.html for
    // the complete
    // list of supported characteristic features.
    //当服务&特征值列表的二级项目被点击时，触发（二级项目点击事件监听者）
    //触发后根据uuid来选择相应的特征值进行读写或接收通知
    private final ExpandableListView.OnChildClickListener servicesListClickListner = new ExpandableListView.OnChildClickListener() {
        @Override
        public boolean onChildClick(ExpandableListView parent, View v,
                                    int groupPosition, int childPosition, long id) {

            if (mGattCharacteristics != null) {
                final BluetoothGattCharacteristic characteristic = mGattCharacteristics
                        .get(groupPosition).get(childPosition);
                final int charaProp = characteristic.getProperties();
                System.out.println("charaProp = " + charaProp + ",UUID = "
                        + characteristic.getUuid().toString());
                Random r = new Random();
                //写特征值示例
                if (characteristic.getUuid().toString().equals("00002a00-0000-1000-8000-00805f9b34fb")) {
                    String data1 = "send data test1:123";
                    System.out.println(data1);
                    characteristic.setValue(data1);
                    mBluetoothLeService.wirteCharacteristic(characteristic);
                }
                else {
                    if ((charaProp & BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                        // If there is an active notification on a
                        // characteristic, clear
                        // it first so it doesn't update the data field on the
                        // user interface.
                        //读特征值示例
                        if (mNotifyCharacteristic != null) {
                            mBluetoothLeService.setCharacteristicNotification(
                                    mNotifyCharacteristic, false);
                            mNotifyCharacteristic = null;
                        }
                        mBluetoothLeService.readCharacteristic(characteristic);

                    }
                }
                /*
                //设置通知示例（当该特征值改变时会触发回调onCharacteristicChanged()）
                if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {

                    if (characteristic.getUuid().toString().equals("00002a00-0000-1000-8000-00805f9b34fb")) {
                        System.out.println("enable notification");
                        Log.e(TAG, characteristic.getUuid().toString()+"-------->enable notification" );
                        mNotifyCharacteristic = characteristic;
                        mBluetoothLeService.setCharacteristicNotification(characteristic, true);

                    }
                }
                */
                return true;
            }
            return false;
        }
    };

    private void clearUI() {
        mGattServicesList.setAdapter((SimpleExpandableListAdapter) null);
        mDataField.setText(R.string.no_data);
    }

    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mConnectionState.setText(resourceId);
            }
        });
    }

    // Demonstrates how to iterate through the supported GATT
    // Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the
    // ExpandableListView
    // on the UI.
    //展示所有的服务和特征值，通过可展开的二级列表展示
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null)
            return;
        String uuid = null;
        String unknownServiceString = getResources().getString(
                R.string.unknown_service);
        String unknownCharaString = getResources().getString(
                R.string.unknown_characteristic);
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData = new ArrayList<ArrayList<HashMap<String, String>>>();
        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

        // 遍历GATT 所有的Services
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();
            currentServiceData.put(LIST_NAME,
                    SampleGattAttributes.lookup(uuid, unknownServiceString));
            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData = new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> gattCharacteristics = gattService
                    .getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas = new ArrayList<BluetoothGattCharacteristic>();

            // 遍历GATT 某个Service的所有的Characteristics
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                charas.add(gattCharacteristic);
                HashMap<String, String> currentCharaData = new HashMap<String, String>();
                uuid = gattCharacteristic.getUuid().toString();
                currentCharaData.put(LIST_NAME,
                        SampleGattAttributes.lookup(uuid, unknownCharaString));
                currentCharaData.put(LIST_UUID, uuid);
                gattCharacteristicGroupData.add(currentCharaData);
            }
            mGattCharacteristics.add(charas);
            gattCharacteristicData.add(gattCharacteristicGroupData);
        }

        SimpleExpandableListAdapter gattServiceAdapter = new SimpleExpandableListAdapter(
                this, gattServiceData,
                R.layout.listitem_services, new String[] {
                LIST_NAME, LIST_UUID }, new int[] { android.R.id.text1,
                android.R.id.text2 }, gattCharacteristicData,
                R.layout.listitem_services, new String[] {
                LIST_NAME, LIST_UUID }, new int[] { android.R.id.text1,
                android.R.id.text2 });
        mGattServicesList.setAdapter(gattServiceAdapter);
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_NOTIFY);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_READ);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_WRITE);
        return intentFilter;
    }
}
