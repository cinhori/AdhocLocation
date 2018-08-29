package com.dantou.adhoc;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.DotOptions;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.model.LatLng;
import com.dantou.model.Point;
import com.dantou.util.StringToLatLong;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with {@code BluetoothLeService}, which in turn interacts with the
 * Bluetooth LE API.
 */
//public class BleSppActivity extends AppCompatActivity implements View.OnClickListener {//
public class BleSppActivity extends AppCompatActivity {
    private final static String TAG = BleSppActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    static long recv_cnt = 0;

    private MapView mapView;

    private BaiduMap baiduMap;
    private boolean isFirstLocate = true;

    private String mDeviceName;
    private String mDeviceAddress;
    private BluetoothLeService mBluetoothLeService;
    //private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    //private boolean mConnected = false;

    //private final String LIST_NAME = "NAME";
    //private final String LIST_UUID = "UUID";

    private Point myPoint;
    private ArrayList<Point> otherPoints = new ArrayList<>();
    //private ArrayList<Point> points;


    /*private TextView mDataRecvText;
    private TextView mNotify_speed_text;
    private EditText mEditBox;
    private TextView mSendBytes;
    private TextView mDataSendFormat;

    private long recvBytes = 0;
    private long lastSecondBytes = 0;*/
    private StringBuilder mData;

    /*private long sendBytes;
    int sendIndex = 0;
    int sendDataLen=0;
    byte[] sendBuf;*/

    //测速
    /*private Timer timer;
    private TimerTask task;*/

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {

            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                /*mConnected = false;
                updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();*/
                mBluetoothLeService.connect(mDeviceAddress);
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                /*//特征值找到才代表连接成功
                mConnected = true;
                invalidateOptionsMenu();
                updateConnectionState(R.string.connected);*/
            }else if (BluetoothLeService.ACTION_GATT_SERVICES_NO_DISCOVERED.equals(action)){
                mBluetoothLeService.connect(mDeviceAddress);
            }else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
//                final byte[] data = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
//                final StringBuilder stringBuilder = new StringBuilder();
//                 for(byte byteChar : data)
//                      stringBuilder.append(String.format("%02X ", byteChar));
//                Log.v("log",stringBuilder.toString());
                displayData(intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA));
            }/*else if (BluetoothLeService.ACTION_WRITE_SUCCESSFUL.equals(action)) {
                mSendBytes.setText(sendBytes + " ");
                if (sendDataLen>0)
                {
                    Log.v("log","Write OK,Send again");
                    onSendBtnClicked();
                }
                else {
                    Log.v("log","Write Finish");
                }
            }*/

        }
    };



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
       // setContentView(R.layout.gatt_services_characteristics);
        SDKInitializer.initialize(getApplicationContext());
        setContentView(R.layout.ble_spp);

        mapView = findViewById(R.id.baiduMapView);
        baiduMap = mapView.getMap();
        baiduMap.setMyLocationEnabled(true);

        getPermission();

        //获取蓝牙的名字和地址
        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        //mDataRecvText = (TextView) findViewById(R.id.data_read_text);
        /*mNotify_speed_text = (TextView) findViewById(R.id.notify_speed_text);*/
        Button mShow = findViewById(R.id.show_map);

        /*mEditBox = (EditText) findViewById(R.id.data_edit_box);
        mSendBytes = (TextView) findViewById(R.id.byte_send_text);
        mDataSendFormat = (TextView) findViewById(R.id.data_sended_format);
        Button mSendBtn = (Button) findViewById(R.id.send_data_btn);
        Button mCleanTextBtn = (Button) findViewById(R.id.clean_text_btn);*/

/*
        mDataRecvFormat.setOnClickListener(this);
*/
        /*mDataSendFormat.setOnClickListener(this);
        mSendBytes.setOnClickListener(this);*/
        //mShow.setOnClickListener(this);

        /*mSendBtn.setOnClickListener(this);
        mCleanTextBtn.setOnClickListener(this);*/
        //mDataRecvText.setMovementMethod(ScrollingMovementMethod.getInstance());
        mData = new StringBuilder();

        final int SPEED = 1;
        /*final Handler handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case SPEED:
                        lastSecondBytes = recvBytes - lastSecondBytes;
                        mNotify_speed_text.setText(String.valueOf(lastSecondBytes)+ " B/s");
                        lastSecondBytes = recvBytes;
                        break;
                }
            }
        };*/

        /*task = new TimerTask() {
            @Override
            public void run() {
                Message message = new Message();
                message.what = SPEED;
                message.obj = System.currentTimeMillis();
                handler.sendMessage(message);
            }
        };*/

        /*timer = new Timer();
        // 参数：
        // 1000，延时1秒后执行。
        // 1000，每隔2秒执行1次task。
        timer.schedule(task, 1000, 1000);*/

        //getActionBar().setTitle(mDeviceName);
        //getActionBar().setDisplayHomeAsUpEnabled(true);

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }

        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
        mapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;

        mapView.onDestroy();
        baiduMap.setMyLocationEnabled(false);
    }

    /*@Override
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
    }*/

    /*@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_connect:
                mBluetoothLeService.connect(mDeviceAddress);
                return true;
            case R.id.menu_disconnect:
                mBluetoothLeService.connect(mDeviceAddress);
                //mBluetoothLeService.disconnect();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }*/

    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
               // mConnectionState.setText(resourceId);
            }
        });
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_WRITE_SUCCESSFUL);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_NO_DISCOVERED);
        return intentFilter;
    }

    /*//动态效果
    public void convertText(final TextView textView, final int convertTextId) {
        final Animation scaleIn = AnimationUtils.loadAnimation(this,
                R.anim.text_scale_in);
        Animation scaleOut = AnimationUtils.loadAnimation(this,
                R.anim.text_scale_out);
        scaleOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                textView.setText(convertTextId);
                textView.startAnimation(scaleIn);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        textView.startAnimation(scaleOut);
    }*/

    //获取输入框十六进制格式
    /*private String getHexString() {
        String s = mEditBox.getText().toString();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (('0' <= c && c <= '9') || ('a' <= c && c <= 'f') ||
                    ('A' <= c && c <= 'F')) {
                sb.append(c);
            }
        }
        if ((sb.length() % 2) != 0) {
            sb.deleteCharAt(sb.length());
        }
        return sb.toString();
    }*/


    /*private byte[] stringToBytes(String s) {
        byte[] buf = new byte[s.length() / 2];
        for (int i = 0; i < buf.length; i++) {
            try {
                buf[i] = (byte) Integer.parseInt(s.substring(i * 2, i * 2 + 2), 16);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
        return buf;
    }*/

    /*public String asciiToString(byte[] bytes) {
        char[] buf = new char[bytes.length];
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < buf.length; i++) {
            buf[i] = (char) bytes[i];
            sb.append(buf[i]);
        }
        return sb.toString();
    }*/

    public String bytesToString(byte[] bytes) {
        final char[] hexArray = "0123456789ABCDEF".toCharArray();
        char[] hexChars = new char[bytes.length * 2];
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            hexChars[i * 2] = hexArray[v >>> 4];
            hexChars[i * 2 + 1] = hexArray[v & 0x0F];

            sb.append(hexChars[i * 2]);
            sb.append(hexChars[i * 2 + 1]);
        }
        return sb.toString();
    }


    /*private void getSendBuf(){
        sendIndex = 0;
        if (mDataSendFormat.getText().equals(getResources().getString(R.string.data_format_default))) {
            sendBuf = mEditBox.getText().toString().trim().getBytes();
        } else {
            sendBuf = stringToBytes(getHexString());
        }
        sendDataLen = sendBuf.length;
    }
    private void onSendBtnClicked() {
        if (sendDataLen>20) {
            sendBytes += 20;
            final byte[] buf = new byte[20];
           // System.arraycopy(buffer, 0, tmpBuf, 0, writeLength);
            for (int i=0;i<20;i++)
            {
                buf[i] = sendBuf[sendIndex+i];
            }
            sendIndex+=20;
            mBluetoothLeService.writeData(buf);
            sendDataLen -= 20;
        }
        else {
            sendBytes += sendDataLen;
            final byte[] buf = new byte[sendDataLen];
            for (int i=0;i<sendDataLen;i++)
            {
                buf[i] = sendBuf[sendIndex+i];
            }
            mBluetoothLeService.writeData(buf);
            sendDataLen = 0;
            sendIndex = 0;
        }
    }*/

    private void displayData(byte[] buf) {
        //recvBytes += buf.length;
        recv_cnt += buf.length;

        /*if (recv_cnt >= 1024)
        {
            recv_cnt = 0;
            mData.delete(0,mData.length()/2); //UI界面只保留512个字节，免得APP卡顿
        }*/

        String s = bytesToString(buf);
        mData.append(s);

        //mDataRecvText.setText(mData.toString());

        cut(mData);

        /*points =  StringToLatLong.toLatLongs(mData.toString());

        for(Point p : points){
            if(p.getId() == 1){
                myPoint = p;
            }else{
                otherPoints.add(p);
            }
        }*/

        if(myPoint != null){

            LatLng myLatLong = new LatLng(myPoint.getLatitude(), myPoint.getLongitude());

            if(isFirstLocate){
                MapStatusUpdate update = MapStatusUpdateFactory.zoomTo(18.0f);//3-19
                baiduMap.animateMapStatus(update);

                update = MapStatusUpdateFactory.newLatLng(myLatLong);
                baiduMap.animateMapStatus(update);
            }

            //将当前节点显示在地图上
            MyLocationData.Builder locationBuilder = new MyLocationData.Builder();
            locationBuilder.latitude(myLatLong.latitude);
            locationBuilder.longitude(myLatLong.longitude);
            MyLocationData locationData = locationBuilder.build();
            baiduMap.setMyLocationData(locationData);
        }

        List<LatLng> others = new LinkedList<>();
        if(otherPoints != null){
            for(Point p : otherPoints){
                others.add(new LatLng(p.getLatitude(), p.getLongitude()));
            }
            //将其他节点显示在地图上
            OverlayOptions ooDot;
            for(LatLng other : others) {
                ooDot = new DotOptions().center(other).radius(15).color(Color.RED);//红色，从Color类中获取
                baiduMap.addOverlay(ooDot);
            }
        }
    }

    private StringBuilder cut(StringBuilder sb){

        //剪去字符串头部乱码
        if(sb.length() > 0 && !sb.toString().startsWith("050082")){
            int location = sb.toString().indexOf("050082");
            if (location == -1) {
                return sb.delete(0, sb.length());
            }else {
                sb = sb.delete(0, sb.toString().indexOf("050082"));
            }

            Log.e("删除头部噪音字符串后", sb.toString());
        }

        while (sb.toString().startsWith("050082") && sb.length() >= 48){
            String tempString = sb.substring(0, 48);
            Log.d("剪切的完整节点串", tempString);
            Point tempPoint = StringToLatLong.toLatLong(tempString);
            Log.d("剪切的节点", tempPoint.toString());

            if (tempPoint.getId() == 1){
                myPoint = tempPoint;
            }else {
                otherPoints.add(tempPoint);
            }

            sb = sb.delete(0, 48);
            Log.e("剩余的16进制串", sb.toString());

            //剪去字符串尾部乱码
            if(sb.length() > 0 && !sb.toString().startsWith("050082")){
                int location = sb.toString().indexOf("050082");
                if (location == -1) {
                    return sb.delete(0, sb.length());
                }else {
                    sb = sb.delete(0, sb.toString().indexOf("050082"));
                }

                Log.e("删除尾部噪音字符串后", sb.toString());
            }

        }

        return sb;

    }

    /*@Override
    public void onClick(View v) {
        switch (v.getId()) {
            *//*case R.id.data_received_format:
                if (mDataRecvFormat.getText().equals(getResources().getString(R.string.data_format_default))) {
                    convertText(mDataRecvFormat, R.string.data_format_hex);
                } else {
                  convertText(mDataRecvFormat,R.string.data_format_default);
                }
                break;*//*

            *//*case R.id.data_sended_format:
                if (mDataSendFormat.getText().equals(getResources().getString(R.string.data_format_default)))  {
                    convertText(mDataSendFormat, R.string.data_format_hex);
                } else {
                    convertText(mDataSendFormat, R.string.data_format_default);
                }
                break;*//*

            *//*case R.id.byte_send_text:
                sendBytes = 0;
                convertText(mSendBytes, R.string.zero);
                break;*//*

            *//*case R.id.send_data_btn:
                getSendBuf();
                onSendBtnClicked();
                break;*//*

            *//*case R.id.clean_text_btn:
                mEditBox.setText("");
                break;*//*

            case R.id.show_map:
                Log.v("显示map按钮", "show map");
                Intent mapIntent = new Intent(BleSppActivity.this, MapActivity.class);
                mapIntent.putExtra("myPoint", myPoint);
                mapIntent.putParcelableArrayListExtra("otherPoints", otherPoints);
                startActivity(mapIntent);
                break;

            default:
                break;
        }
    }*/

    private void getPermission(){
        List<String> permissionList = new ArrayList<>();
        if(ContextCompat.checkSelfPermission(BleSppActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        if(ContextCompat.checkSelfPermission(BleSppActivity.this,
                Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.READ_PHONE_STATE);
        }

        if(ContextCompat.checkSelfPermission(BleSppActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        if(!permissionList.isEmpty()){
            String[] permissions = permissionList.toArray(new String[permissionList.size()]);
            ActivityCompat.requestPermissions(BleSppActivity.this, permissions, 1);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case 1:
                if (grantResults.length > 0) {
                    for (int result : grantResults) {
                        if (result != PackageManager.PERMISSION_GRANTED) {
                            Toast.makeText(this, "you must agree all the previleges",
                                    Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }
                    }
                } else {
                    Toast.makeText(this, "Unknown Error", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            default:
        }
    }
}
