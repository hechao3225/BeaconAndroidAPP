package com.hc.beaconcenter;

/**
 * Created by ZS on 2017/1/6.
 */

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import com.hc.beaconcenter.iBeaconClass.iBeacon;
/**
 * Activity for scanning and displaying available Bluetooth LE devices.
 */
@SuppressLint("NewApi")
public class DeviceScanActivity extends ListActivity {

    private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;
    private static final int REQUEST_ENABLE_BT = 1;

    //当前准备连接设备的类型
    private int curDeviceType=0;//0：BLEDevice 1：iBeacon 2：EddyStone

    private EditText editText_circle;
    private EditText editText_overtime;
    // 默认20秒后停止查找搜索.
    private long scanPeriod = 20000;
    //默认扫描1次
    private int scanCircleTimes = 1;
    private int curScanTimes=0;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scan_device);
        getActionBar().setTitle(R.string.title_devices);
        mHandler = new Handler();

        // 初始化 Bluetooth adapter, 通过蓝牙管理器得到一个参考蓝牙适配器(API必须在以上android4.3或以上和版本)
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        // 蓝牙适配器获取失败
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        getActionBar().setDisplayHomeAsUpEnabled(true);//返回键
        //扫描设定监听
        ImageButton btn = (ImageButton) findViewById(R.id.dataCollection);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dataCollectionSetDialog();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        if (!mScanning) {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(null);
        } else {//若开始扫描
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView( R.layout.actionbar_indeterminate_progress);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
                mLeDeviceListAdapter.clear();
                scanLeDevice(true);
                return true;
            case R.id.menu_stop:
                scanLeDevice(false);
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // 为了确保设备上蓝牙能使用, 如果当前蓝牙设备没启用,弹出对话框向用户要求授予权限来启用
        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }

        // Initializes list view adapter.
        mLeDeviceListAdapter = new LeDeviceListAdapter();
        setListAdapter(mLeDeviceListAdapter);
        //scanLeDevice(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
        mLeDeviceListAdapter.clear();
    }

    //打开或关闭扫描
    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // 经过预定扫描期后停止扫描.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);//停止扫描（核心方法）
                    invalidateOptionsMenu();
                    //scanStopDialog();
                    curScanTimes++;//扫描次数+1
                    scanOver();//一次扫描完成
                }
            }, scanPeriod);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);//开始扫描（核心方法）

        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);//停止扫描（核心方法）
        }
        invalidateOptionsMenu();
    }

    public void scanOver(){
        if(curScanTimes<scanCircleTimes){
            //继续循环扫描
            scanLeDevice(true);
        }
        else{//循环扫描已完成一轮
            curScanTimes=0;//恢复循环设定
            scanStopDialog();//扫描停止，打印报告
        }

    }
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        //final BluetoothDevice device = mLeDeviceListAdapter.getDevice(position);
        deviceItemClickedDialog(position);
    }

    //对蓝牙设备再封装，为获取实时RSSI和广播包
    static public class DeviceWithRSSI{
        int rssi;
        ArrayList<byte[]> scanData=new ArrayList<byte[]>();//存放多组广播包，以绘制数据曲线
        BluetoothDevice bleDevice;
        public static DeviceWithRSSI fromBluetoothDevice(int rssi,byte[] scanData,BluetoothDevice device){
            DeviceWithRSSI myDevice = new DeviceWithRSSI();
            myDevice.rssi=rssi;
            myDevice.scanData.add(scanData);
            myDevice.bleDevice=device;
            return myDevice;
        }
    }

    // Adapter for holding devices found through scanning.
    //ListView Adapter,用于在listview里管理扫描到的设备
    private class LeDeviceListAdapter extends BaseAdapter {
        //private ArrayList<BluetoothDevice> mLeDevices;
        private ArrayList<DeviceWithRSSI> mLeDevices;
        private LayoutInflater mInflator;

        public LeDeviceListAdapter() {
            super();

            //mLeDevices = new ArrayList<BluetoothDevice>();
            mLeDevices = new ArrayList<DeviceWithRSSI>();

            mInflator = DeviceScanActivity.this.getLayoutInflater();
        }

        public void addDevice(DeviceWithRSSI device) {
            if(device==null)
                return;

            for(int i=0;i<mLeDevices.size();i++){
                String btAddress = mLeDevices.get(i).bleDevice.getAddress();
                if(btAddress.equals(device.bleDevice.getAddress())){//同一个device
                    DeviceWithRSSI sameDevice=mLeDevices.get(i);
                    sameDevice.scanData.add(device.scanData.get(0));
                    device=sameDevice;
                    mLeDevices.add(i+1, device);//添加更新后的device信息
                    mLeDevices.remove(i);//删除旧device信息
                    return;
                }
            }
            mLeDevices.add(device);
        }

        public DeviceWithRSSI getDevice(int position) {
            return mLeDevices.get(position);
        }


        public void clear() {
            mLeDevices.clear();
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_device, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                viewHolder.deviceRssi = (TextView) view.findViewById(R.id.device_rssi);
                viewHolder.deviceBroadcastPack = (TextView) view.findViewById(R.id.device_broadcastPack);

                viewHolder.iBeaconMajorMinor = (TextView)view.findViewById(R.id.iBeacon_major_minor);
                viewHolder.iBeaconUuid = (TextView)view.findViewById(R.id.iBeacon_uuid);
                viewHolder.iBeaconDistance=(TextView)view.findViewById(R.id.iBeacon_distance);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }
            //获取设备列表和广播包列表项目
            //BluetoothDevice device = mLeDevices.get(i);
            DeviceWithRSSI device = mLeDevices.get(i);
            //解析iBeacon
            iBeacon beaconInfo = iBeaconClass.fromScanData(device,device.scanData.get(0));
            //提取信息并显示
            final String deviceName = "设备名：" + device.bleDevice.getName();
            final String deviceAddr = "Mac地址：" + device.bleDevice.getAddress();
            final String rssiString = "RSSI:" + String.valueOf(device.rssi) + "dB";
            final String broadcastPack ="广播包：" + bytesToHex(device.scanData.get(0));
            final String iBeaconString = "分析结果：iBeacon设备！";
            final String bleDeviceString = "分析结果：非Beacon设备！";
            final String eddyStoneString = "分析结果：EddyStone设备！";//待补
            //显示设备名
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText(R.string.unknown_device);
            //显示设备地址
            viewHolder.deviceAddress.setText(deviceAddr);
            //显示RSSI
            viewHolder.deviceRssi.setText(rssiString);
            //显示广播包及Beacon设备判定结果
            if(beaconInfo!=null)
                viewHolder.deviceBroadcastPack.setText(broadcastPack+iBeaconString);
            else
                viewHolder.deviceBroadcastPack.setText(broadcastPack+bleDeviceString);

            if (beaconInfo!=null) {//如果是iBeacon设备则显示解析后的iBeacon信息

                final String iBeaconMajorMinor = "Major:" + String.valueOf(beaconInfo.major) + "  Minor:" + String.valueOf(beaconInfo.minor);
                final String iBeaconUuid = "UUID:" + beaconInfo.proximityUuid;
                final String iBeaconDistance = "距离设备：" + String.valueOf(calculateAccuracy(-55, device.rssi)) + "m";
                viewHolder.iBeaconMajorMinor.setText(iBeaconMajorMinor);//显示iBeacon的major和minor
                viewHolder.iBeaconUuid.setText(iBeaconUuid);//显示iBeacon的uuid
                viewHolder.iBeaconDistance.setText(iBeaconDistance);//显示与iBeacon设备的距离
            } else{
                viewHolder.iBeaconMajorMinor.setText("");
                viewHolder.iBeaconUuid.setText("");
                viewHolder.iBeaconDistance.setText("");
            }
            return view;
        }
    }

    //scanRecords的格式转换
    static final char[] hexArray = "0123456789ABCDEF".toCharArray();
    private static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    //rssi测距简单实现
    public static double calculateAccuracy(int txPower, double rssi) {
        if (rssi == 0) {
            return -1.0; // if we cannot determine accuracy, return -1.
        }
        double ratio = rssi * 1.0 / txPower;
        if (ratio < 1.0) {
            return Math.pow(ratio, 10);
        } else {
            double accuracy = (0.42093) * Math.pow(ratio, 6.9476) + 0.54992;
            return accuracy;
        }
    }

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
           final DeviceWithRSSI myDevice = DeviceWithRSSI.fromBluetoothDevice(rssi,scanRecord,device);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mLeDeviceListAdapter.addDevice(myDevice);
                    mLeDeviceListAdapter.notifyDataSetChanged();

                }
            });
        }
    };

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
        TextView deviceBroadcastPack;//加入广播包数据显示
        TextView deviceRssi;//加入RSSI显示
        //以下为iBeacon解析显示测试
        TextView iBeaconMajorMinor;
        TextView iBeaconUuid;
        TextView iBeaconDistance;//加入RSSI测距信息显示
        //以下为EddyStone解析显示测试（待补）

    }

    //iBeacon设备数目统计
    public  int iBeaconCount(){
        int iBeaconNum=0;
        for(int i=0;i<mLeDeviceListAdapter.getCount();i++){
            byte[] scanData=mLeDeviceListAdapter.getDevice(i).scanData.get(0);
            iBeacon ibeaconInfo = iBeaconClass.fromScanData(mLeDeviceListAdapter.getDevice(i),scanData);
            if(ibeaconInfo!=null)
                iBeaconNum++;
             else  continue;
        }
        return iBeaconNum;
    }

    /*
    //EddyStone设备数目统计
    public  int eddyStoneCount(){
        int eddyStoneNum=0;
        for(int i=0;i<mLeDeviceListAdapter.getCount();i++){
            byte[] scanData=mLeDeviceListAdapter.getDevice(i).scanData;
            iBeacon eddyStoneInfo = EddyStoneClass.fromScanData(mLeDeviceListAdapter.getDevice(i),scanData);
            if(eddyStoneInfo!=null)
                eddyStoneNum++;
             else  continue;
        }
        return eddyStoneNum;
    }
     */

    //扫描报告提示框
    protected void scanStopDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(DeviceScanActivity.this);
        builder.setMessage("共发现BLE设备："+mLeDeviceListAdapter.getCount()+"\n"
                            +"iBeacon设备："+iBeaconCount());
        builder.setTitle("扫描完成");

        builder.setNegativeButton("知道了", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.create().show();
    }

    //数据采集设定框
    protected void dataCollectionSetDialog(){
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.data_collection_set,null);
        AlertDialog.Builder builder = new AlertDialog.Builder(DeviceScanActivity.this);


        //editText_circle=(EditText)findViewById(R.id.circle_value);
        //editText_overtime=(EditText)findViewById(R.id.overtime_value);
        EditText text1=(EditText)layout.findViewById(R.id.circle_value);
        EditText text2=(EditText)layout.findViewById(R.id.overtime_value);
        editText_circle=text1;
        editText_overtime=text2;
        builder.setView(layout);

        //builder.setMessage("");
        builder.setTitle("扫描设定");

        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                String str1=editText_circle.getText().toString();
                String str2=editText_overtime.getText().toString();
                //设定循环扫描次数
                if(str1.length()!=0)
                    scanCircleTimes=Integer.parseInt(str1);
                //设定扫描周期（扫描超时）
                if(str2.length()!=0)
                    scanPeriod=1000*Integer.parseInt(str2);
            }
        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.create().show();
    }

    //列表项目点击提示框
    protected void deviceItemClickedDialog(final int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(DeviceScanActivity.this);
        //builder.setMessage("");
        builder.setTitle("提示");

        builder.setPositiveButton("数据分析", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //跳转到该Beacon的数据分析活动
                goToBeaconAnalystActivity(position);
            }
        });
        builder.setNegativeButton("配置Beacon", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //跳转到该Beacon的配置活动
                goToDeviceManagerActivity(position);
            }
        });
        builder.create().show();
    }

    //Beacon配置跳转
    public void goToDeviceManagerActivity(int position){

        final DeviceWithRSSI device = mLeDeviceListAdapter.getDevice(position);
        final iBeacon iBeaconInfo=iBeaconClass.fromScanData(device,device.scanData.get(0));//解析iBeacon
        //final EddyStone eddyStoneInfo=EddyStoneClass.fromScanData(device,device.scanData);//解析EddyStone

        //若正在扫描停止扫描，准备跳转活动
        if (mScanning) {
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            mScanning = false;
        }
        if (device == null) return;
        if(iBeaconInfo!=null) {//传iBeacon数据并跳转到iBeacon管理页面
            curDeviceType=1;//iBeacon设备
            final Intent intent = new Intent(this, iBeaconManagerActivity.class);//连接Activity
            intent.putExtra(iBeaconManagerActivity.EXTRAS_DEVICE_NAME, device.bleDevice.getName());
            intent.putExtra(iBeaconManagerActivity.EXTRAS_DEVICE_ADDRESS, device.bleDevice.getAddress());
            intent.putExtra(iBeaconManagerActivity.EXTRAS_IBEACON_MAJOR, String.valueOf(iBeaconInfo.major));
            intent.putExtra(iBeaconManagerActivity.EXTRAS_IBEACON_MINOR, String.valueOf(iBeaconInfo.minor));
            intent.putExtra(iBeaconManagerActivity.EXTRAS_IBEACON_UUID, iBeaconInfo.proximityUuid);
            startActivity(intent);//进入到iBeaconManagerActivity
        }else{
            //非Beacon设备，不支持配置！
            curDeviceType=0;//非Beacon设备（普通BLE Device）
            Toast.makeText(DeviceScanActivity.this,  "非Beacon设备，暂不支持参数配置！", Toast.LENGTH_SHORT).show();
        }
        /*
        if(eddyStoneInfo!=null){//传EddyStone数据并跳转到EddyStone管理页面
            curDeviceType=2;//EddyStone设备
            final Intent intent = new Intent(this, EddyStoneManagerActivity.class);//连接Activity
            //传数据，待补
            startActivity(intent);//进入到EddyStoneManagerActivity
        }
        */
    }
    //Beacon数据分析跳转
    public void goToBeaconAnalystActivity(int position){

        final DeviceWithRSSI device = mLeDeviceListAdapter.getDevice(position);
        final ArrayList<iBeacon> iBeaconInfoAll=new ArrayList<iBeacon>();
        for(int i=0;i<device.scanData.size();i++) {
            final iBeacon iBeaconInfo=iBeaconClass.fromScanData(device, device.scanData.get(i));
            iBeaconInfoAll.add(iBeaconInfo);//解析iBeacon所有广播包
        }
        //final EddyStone eddyStoneInfo=EddyStoneClass.fromScanData(device,device.scanData);//解析EddyStone

        //若正在扫描停止扫描，准备跳转活动
        if (mScanning) {
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            mScanning = false;
        }
        if (device == null) return;
        if(iBeaconInfoAll.get(0)!=null) {//传iBeacon数据并跳转到iBeacon数据分析页面
            curDeviceType=1;//iBeacon设备
            final Intent intent = new Intent(this, iBeaconAnalystActivity.class);//连接Activity
            intent.putExtra(iBeaconAnalystActivity.EXTRAS_DEVICE_NAME, device.bleDevice.getName());
            intent.putExtra(iBeaconAnalystActivity.EXTRAS_DEVICE_ADDRESS, device.bleDevice.getAddress());
            intent.putExtra(iBeaconAnalystActivity.EXTRAS_IBEACON_MAJOR, String.valueOf(iBeaconInfoAll.get(0).major));
            intent.putExtra(iBeaconAnalystActivity.EXTRAS_IBEACON_MINOR, String.valueOf(iBeaconInfoAll.get(0).minor));
            intent.putExtra(iBeaconAnalystActivity.EXTRAS_IBEACON_UUID, iBeaconInfoAll.get(0).proximityUuid);
            /*传感器数据（待补）*/
            ArrayList<String> sensor1datas=new ArrayList<String>();
            ArrayList<String> sensor2datas=new ArrayList<String>();
            ArrayList<String> sensor3datas=new ArrayList<String>();
            ArrayList<String> sensor4datas=new ArrayList<String>();
            for(int i=0;i<iBeaconInfoAll.size();i++) {
                sensor1datas.add(String.valueOf(iBeaconInfoAll.get(i).sensor1));
                sensor2datas.add(String.valueOf(iBeaconInfoAll.get(i).sensor2));
                sensor3datas.add(String.valueOf(iBeaconInfoAll.get(i).sensor3));
                sensor4datas.add(String.valueOf(iBeaconInfoAll.get(i).sensor4));
            }
            intent.putStringArrayListExtra(iBeaconAnalystActivity.EXTRAS_IBEACON_SENSOR1,sensor1datas);
            intent.putStringArrayListExtra(iBeaconAnalystActivity.EXTRAS_IBEACON_SENSOR2,sensor2datas);
            intent.putStringArrayListExtra(iBeaconAnalystActivity.EXTRAS_IBEACON_SENSOR3,sensor3datas);
            intent.putStringArrayListExtra(iBeaconAnalystActivity.EXTRAS_IBEACON_SENSOR4,sensor4datas);
            startActivity(intent);//进入到iBeaconManagerActivity
        }else{
            //非Beacon设备，不支持传感数据传输！
            curDeviceType=0;//非Beacon设备（普通BLE Device）
            Toast.makeText(DeviceScanActivity.this,  "非Beacon设备，暂不支持数据分析！", Toast.LENGTH_SHORT).show();
        }
        /*
        if(eddyStoneInfo!=null){//传EddyStone数据并跳转到EddyStone数据分析页面
            curDeviceType=2;//EddyStone设备
            final Intent intent = new Intent(this, EddyStoneAnalystActivity.class);//连接Activity
            //传数据，待补
            startActivity(intent);//进入到EddyStoneAnalystActivity
        }
        */

    }

}
