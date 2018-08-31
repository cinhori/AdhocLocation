package com.dantou.adhoc;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.CircleOptions;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.map.Stroke;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.utils.CoordinateConverter;
import com.baidu.mapapi.utils.DistanceUtil;
import com.dantou.model.Point;
import com.dantou.util.StringToLatLong;
import com.dantou.util.XorVerification;

import java.util.ArrayList;
import java.util.List;

/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with {@code BluetoothLeService}, which in turn interacts with the
 * Bluetooth LE API.
 */

public class BleSppActivity extends AppCompatActivity {
    private final static String TAG = BleSppActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    public static int safeDistance = 500;

    static long recv_cnt = 0;

    private MapView mapView;

    private BaiduMap baiduMap;
    private boolean isFirstLocate = true;

    private String mDeviceName;
    private String mDeviceAddress;
    private BluetoothLeService mBluetoothLeService;

    private Point myPoint;
    private LatLng myLatLong;
    private ArrayList<Point> otherPoints = new ArrayList<>();

    private TextView allCount;
    private TextView outOfSafetyCount;

    private StringBuilder mData;

    //坐标转换
    CoordinateConverter converter;

    //构建Marker图标
    BitmapDescriptor guest_in;
    BitmapDescriptor guest_out;
    BitmapDescriptor leader;
    OverlayOptions ooCircle;
    OverlayOptions ooMarker;

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
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SDKInitializer.initialize(getApplicationContext());
        setContentView(R.layout.ble_spp);

        mapView = findViewById(R.id.baiduMapView);
        baiduMap = mapView.getMap();
        baiduMap.setMyLocationEnabled(true);

        allCount = findViewById(R.id.all_count);
        allCount.setText("0");
        outOfSafetyCount = findViewById(R.id.out_safety);
        outOfSafetyCount.setText("0");

        guest_in = BitmapDescriptorFactory.fromResource(R.drawable.guest_2_green_32);
        guest_out = BitmapDescriptorFactory.fromResource(R.drawable.guest_2_red_32);
        leader = BitmapDescriptorFactory.fromResource(R.drawable.leader_48);

        //转换工具初始化
        converter = new CoordinateConverter();
        converter.from(CoordinateConverter.CoordType.GPS);

        getPermission();

        //获取蓝牙的名字和地址
        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        mData = new StringBuilder();

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        baiduMap.setOnMarkerClickListener(new BaiduMap.OnMarkerClickListener() {

            @Override
            public boolean onMarkerClick(Marker marker) {
                Bundle bundle = marker.getExtraInfo();
                String info = bundle.getString("info");
                Log.d("显示bundle信息", info);
                Toast.makeText(BleSppActivity.this, info, Toast.LENGTH_LONG).show();
                return false;

            }
        });
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

        guest_out.recycle();
        guest_in.recycle();

        mapView.onDestroy();
        baiduMap.setMyLocationEnabled(false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.expand_menu:
                safeDistance += 50;
                baiduMap.clear();
                addMarker();
                return true;
            case R.id.reduce_menu:
                if (safeDistance < 50){
                    Toast.makeText(BleSppActivity.this,
                            "安全半径已经为0，不能继续缩小", Toast.LENGTH_SHORT).show();
                    return true;
                }else {
                    safeDistance -= 50;
                    baiduMap.clear();
                    addMarker();
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /*private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
               // mConnectionState.setText(resourceId);
            }
        });
    }*/

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

    private void displayData(byte[] buf) {
        recv_cnt += buf.length;

        String s = bytesToString(buf);
        mData.append(s);

        cut(mData);

        baiduMap.clear();

        addMarker();

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

            String checkSum = XorVerification.getChecksum(tempString.substring(0, tempString.length()-2));
            Log.d("计算出的校验和", checkSum);
            String rightCS = tempString.substring(tempString.length()-2, tempString.length());
            Log.d("正确的的校验和", rightCS);
            if(!checkSum.equalsIgnoreCase(rightCS)) {
                Log.d("校验和结果", "错误，直接丢弃");
                sb = sb.delete(0, 48);
                continue;
            }

            Point tempPoint = StringToLatLong.toLatLong(tempString);
            Log.d("剪切的节点", tempPoint.toString());

            if (tempPoint.getId() == 1){
                myPoint = tempPoint;
            }else {
                for (Point p : otherPoints){
                    if (p.getId() == tempPoint.getId()){
                        Log.e("发现重复节点", "更新原节点位置");
                        Log.d("原节点信息", p.toString());
                        Log.d("现节点信息", tempPoint.toString());
                        p.setLatitude(tempPoint.getLatitude());
                        p.setLongitude(tempPoint.getLongitude());
                        p.setDate(tempPoint.getDate());
                        tempPoint = null;
                        break;
                    }
                }
                if (tempPoint != null) otherPoints.add(tempPoint);
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

    private void addMarker(){
        if(myPoint != null){

            converter.coord(new LatLng(myPoint.getLatitude(), myPoint.getLongitude()));
            myLatLong = converter.convert();
            Log.d("获得在BaiduMap中的地址",
                    "latitude:" + myLatLong.latitude + "longitude:" + myLatLong.longitude);

            if(isFirstLocate){
                MapStatusUpdate update = MapStatusUpdateFactory.zoomTo(18.0f);//3-19
                baiduMap.animateMapStatus(update);

                update = MapStatusUpdateFactory.newLatLng(myLatLong);
                baiduMap.animateMapStatus(update);

                isFirstLocate = false;
            }

            //画圆
            ooCircle = new CircleOptions().center(myLatLong).fillColor(0x00FF0000)
                    .radius(safeDistance).stroke(new Stroke(3, Color.RED));
            Log.d("画圆", "半径" + safeDistance + "，透明度1，颜色无");
            baiduMap.addOverlay(ooCircle);

            ooMarker = new MarkerOptions().position(myLatLong).icon(leader);
            Marker leaderMarker = (Marker) baiduMap.addOverlay(ooMarker);
            Log.d("为从节点添加bundle", myPoint.toString());
            Bundle bundle = new Bundle();
            bundle.putString("info", "节点ID为1"
                    + ";\n经度：" + myLatLong.longitude + ";\n纬度：" + myLatLong.latitude);
            leaderMarker.setExtraInfo(bundle);
        }

        int unsafety = 0;
        if(otherPoints != null){
            int all = otherPoints.size() + 1;
            allCount.setText("" + all);
            for(Point p : otherPoints){
                LatLng other = converter.coord(new LatLng(p.getLatitude(), p.getLongitude())).convert();
                //将其他节点显示在地图上
                //ooDot = new DotOptions().center(other).radius(15).color(Color.RED);//红色，从Color类中获取
                double distance = DistanceUtil.getDistance(other, myLatLong);
                Log.e("节点到中心点距离", distance + "");
                if (distance < safeDistance){
                    ooMarker = new MarkerOptions().position(other).icon(guest_in);
                }else {
                    ooMarker = new MarkerOptions().position(other).icon(guest_out);
                    if (other.latitude > 0 && other.longitude > 0)  unsafety++;
                }
                Marker marker = (Marker)baiduMap.addOverlay(ooMarker);
                Log.d("为从节点添加bundle", p.toString());
                Bundle bundle = new Bundle();
                bundle.putString("info", "节点ID为" + p.getId()
                        + ";\n经度：" + other.longitude + ";\n纬度：" + other.latitude);
                marker.setExtraInfo(bundle);
            }
            outOfSafetyCount.setText("" + unsafety);
        }else{
            allCount.setText("0");
        }
    }

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
