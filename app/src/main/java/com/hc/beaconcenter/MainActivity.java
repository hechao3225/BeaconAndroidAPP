package com.hc.beaconcenter;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;
import android.net.Uri;

public class MainActivity extends Activity {

    private boolean bleSwitch;
    private static final int REQUEST_ENABLE_BT = 1;
    private BluetoothAdapter mBluetoothAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        Button scanDevice = (Button) findViewById(R.id.btn_scan);
        Button dataHistory = (Button) findViewById(R.id.btn_history);
        Button bleLocation = (Button) findViewById(R.id.btn_location);
        ImageButton bleBtn = (ImageButton) findViewById(R.id.btn_ble);
        ImageButton aboutBtn = (ImageButton) findViewById(R.id.btn_about);
        ImageButton shareBtn = (ImageButton) findViewById(R.id.btn_share);

        bleBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // 检查当前手机是否支持ble 蓝牙,如果不支持退出程序
                if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
                    Toast.makeText(MainActivity.this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
                    finish();
                }
                // 初始化 Bluetooth adapter, 通过蓝牙管理器得到一个参考蓝牙适配器(API必须在以上android4.3或以上和版本)
                final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
                mBluetoothAdapter = bluetoothManager.getAdapter();
                // 蓝牙适配器获取失败
                if (mBluetoothAdapter == null) {
                    Toast.makeText(MainActivity.this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                // 弹出对话框向用户要求授予权限来启用
                if (!mBluetoothAdapter.isEnabled()) {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                }
                else
                    Toast.makeText(MainActivity.this,  "蓝牙开关已打开", Toast.LENGTH_SHORT).show();
            }
        });

        aboutBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                aboutDialog();
            }
        });

        shareBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

            }
        });

        scanDevice.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, DeviceScanActivity.class);
                startActivity(intent);
                //finish();
            }
        });

        dataHistory.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this,DataHistoryActivity.class);
                startActivity(intent);
                //finish();
            }
        });

        bleLocation.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this,BeaconLocationActivity.class);
                startActivity(intent);
                //finish();
            }
        });

    }

    protected void aboutDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage("BeaconCenter v1.0\n©四川红宇创智信息科技有限公司");
        builder.setTitle("关于");

        builder.setNegativeButton("好的", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builder.setPositiveButton("更多信息", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String url = "http://www.scishine.net/"; // web address
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(url));
                startActivity(intent);
                dialog.dismiss();
            }
        });
        builder.create().show();
    }


}
