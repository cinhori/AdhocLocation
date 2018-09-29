package com.dantou.adhoc;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
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
import com.dantou.util.CoordinateConvert;
import com.dantou.util.MyDatabaseHelper;
import com.dantou.util.XorVerification;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * @author cinhori
 * @date 18-8-21
 * @email lilei93s@163.com
 * @Description 软件的主界面，包括蓝牙、地图等
 */
public class MainActivity extends AppCompatActivity {
    private final static String TAG = MainActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    public static int safeDistance = 500;
    public static final int LIVE_INTERVAL = 60 * 5; //节点在60s * 5没有接收到新消息则删除

    static long recv_cnt = 0;

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
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

    //构建Marker图标
    BitmapDescriptor guest_in;
    BitmapDescriptor guest_out;
    BitmapDescriptor guest_disappear;
    BitmapDescriptor guest_unsafe;
    BitmapDescriptor leader;
    BitmapDescriptor leader_dark;
    OverlayOptions ooCircle;
    OverlayOptions ooMarker;

    private MyDatabaseHelper dbHelper;
    SimpleDateFormat simpleDateFormat;
    SQLiteDatabase db;

    private DecimalFormat df;

    private ProgressDialog progressDialog;
    public Handler pdHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    Log.e("handler接收到消息", "消息为0");
                    progressDialog.dismiss();
                    break;
            }
        }
    };

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

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        df = new DecimalFormat("#.0000");

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.menu_black_32);
        }

        progressDialog = new ProgressDialog(MainActivity.this);
        progressDialog.setTitle("状态");
        progressDialog.setMessage("定位中...");
        progressDialog.setCancelable(true);
        progressDialog.show();

        dbHelper = new MyDatabaseHelper(MainActivity.this, MyDatabaseHelper.DB_NAME, null, 1);
        db = dbHelper.getWritableDatabase();
        simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

        navigationView.setCheckedItem(R.id.nav_history);
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.nav_history:
                        Intent historyIntent
                                = new Intent(MainActivity.this, TraceHistoryActivity.class);
                        startActivity(historyIntent);
                        break;
                    case R.id.nav_delete_all:
                        AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
                        dialog.setTitle("清除历史");
                        dialog.setMessage("确定要删除历史数据吗");
                        dialog.setCancelable(false);
                        dialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dbHelper.deleteAll(db);
                                Toast.makeText(MainActivity.this, "历史数据已删除", Toast.LENGTH_LONG).show();
                                dialog.dismiss();
                                dialog.cancel();
                            }
                        });
                        dialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                dialog.cancel();
                            }
                        });
                        dialog.show();
                        break;
                    case R.id.nav_aboutus:
                        Intent aboutUsIntent
                                = new Intent(MainActivity.this, AboutUsActivity.class);
                        startActivity(aboutUsIntent);
                        break;
                }
                return true;
            }
        });

        mapView = findViewById(R.id.baiduMapView);
        baiduMap = mapView.getMap();
        baiduMap.setMyLocationEnabled(true);

        allCount = findViewById(R.id.all_count);
        allCount.setText("0");
        outOfSafetyCount = findViewById(R.id.out_safety);
        outOfSafetyCount.setText("0");

        guest_in = BitmapDescriptorFactory.fromResource(R.drawable.guest_2_green_32);
        guest_out = BitmapDescriptorFactory.fromResource(R.drawable.guest_2_red_32);
        guest_disappear = BitmapDescriptorFactory.fromResource(R.drawable.guest_2_blue_32);
        guest_unsafe = BitmapDescriptorFactory.fromResource(R.drawable.guest_2_yellow_48);
        leader = BitmapDescriptorFactory.fromResource(R.drawable.leader_48);
        leader_dark = BitmapDescriptorFactory.fromResource(R.drawable.leader_48_dark);

        getPermission();

        //获取蓝牙的名字和地址
        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        mData = new StringBuilder();

        myLatLong = new LatLng(0.0, 0.0);

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        baiduMap.setOnMarkerClickListener(new BaiduMap.OnMarkerClickListener() {

            @Override
            public boolean onMarkerClick(Marker marker) {
                Bundle bundle = marker.getExtraInfo();
                String info = bundle.getString("info");
                Log.d("显示bundle信息", info);
                Toast.makeText(MainActivity.this, info, Toast.LENGTH_LONG).show();
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
            case android.R.id.home:
                drawerLayout.openDrawer(GravityCompat.START);
                break;
            case R.id.expand_menu:
                safeDistance += 50;
                baiduMap.clear();
                addMarker();
                Toast.makeText(MainActivity.this,
                        "当前的安全距离为" + safeDistance, Toast.LENGTH_SHORT).show();
                return true;
            case R.id.reduce_menu:
                if (safeDistance < 50){
                    Toast.makeText(MainActivity.this,
                            "安全半径已经为0，不能继续缩小", Toast.LENGTH_SHORT).show();
                    return true;
                }else {
                    safeDistance -= 50;
                    baiduMap.clear();
                    addMarker();
                    Toast.makeText(MainActivity.this,
                            "当前的安全距离为" + safeDistance, Toast.LENGTH_SHORT).show();
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

        //清除过期节点
        for (Iterator<Point> pointIterator = otherPoints.iterator(); pointIterator.hasNext();) {
            Point temp = pointIterator.next();
            if ((new Date().getTime() - temp.getDate().getTime()) >= LIVE_INTERVAL * 1000) {
                pointIterator.remove();
                Log.d("当前时间", new Date().toString());
                Log.e("清除过期节点", temp.toString());
            }
        }

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

            Point tempPoint = Point.parse(tempString);

            if (tempPoint != null) {
                Log.d("剪切的节点", tempPoint.toString());
            }

            if (tempPoint != null && tempPoint.getId() == 1){
                if (!tempPoint.isLocated() && myPoint != null){
                    myPoint.setLocated(false);
                    myPoint.setDate(tempPoint.getDate());
                    myPoint.setSafe(tempPoint.isSafe());
                }else {
                    myPoint = tempPoint;

                    //如果已经定位，将数据保存到数据库中
                    if (myPoint.isLocated()){
                        ContentValues values = new ContentValues();
                        //组装数据
                        values.put("user_id", 1);
                        values.put("latitude", myPoint.getLatitude());
                        values.put("longitude", myPoint.getLongitude());
                        values.put("date", simpleDateFormat.format(myPoint.getDate()));
                        values.put("safe", myPoint.isSafe() ? 1 : 0);
                        values.put("located", myPoint.isLocated() ? 1 : 0);
                        dbHelper.insert(db, values);
                    }
                }
            } else{
                for (Point p : otherPoints){
                    if (p.getId() == tempPoint.getId()){
                        if (p.isSafe() && !tempPoint.isSafe()) {
                            notify(tempPoint);
                        }

                        if(!tempPoint.isLocated()) {
                            Log.e("节点位置消失", "改变状态");
                            p.setLocated(tempPoint.isLocated());
                        } else {
                            Log.e("发现重复节点", "更新原节点位置");
                            Log.d("原节点信息", p.toString());
                            Log.d("现节点信息", tempPoint.toString());
                            p.setLatitude(tempPoint.getLatitude());
                            p.setLongitude(tempPoint.getLongitude());
                            p.setDate(tempPoint.getDate());
                            p.setLocated(tempPoint.isLocated());
                            p.setSafe(tempPoint.isSafe());
                        }
                        tempPoint = null;
                        break;
                    }
                }
                //节点为新加入节点
                if (tempPoint != null) {
                    otherPoints.add(tempPoint);
                    notify(tempPoint);
                }
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

    private void notify(Point tempPoint){
        if (!tempPoint.isSafe()){
            //振动
            Vibrator vibrator = (Vibrator)this.getSystemService(VIBRATOR_SERVICE);
            vibrator.vibrate(1000);
            //弹窗
            AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
            dialog.setTitle("SOS!!!");
            dialog.setMessage("节点" + tempPoint.getId() +
                    "请求支援！节点位置为（" +
                    tempPoint.getLongitude() + ", " + tempPoint.getLatitude() + "）");
            dialog.setCancelable(true);
            dialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    dialog.cancel();
                }
            });
            dialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    dialog.cancel();
                }
            });
            dialog.show();
        }
    }

    private void addMarker(){
        if(myPoint != null){
            if (!myPoint.isLocated()){

            } else {
                Log.e("进度条消失", "服务端节点已经获取GPS");
                Thread tempThread = new Thread() {
                    @Override
                    public void run() {
                        Message message = new Message();
                        message.what = 0;
                        pdHandler.sendMessage(message);
                    }
                };
                tempThread.start();
            }

            //将GPS信息转换成百度地图中的经纬度坐标
            myLatLong = CoordinateConvert.getLatLng(myPoint);
            Log.d("获得在BaiduMap中的地址",
                    "latitude:" + myLatLong.latitude + "longitude:" + myLatLong.longitude);

            if(isFirstLocate && myPoint.isLocated()){
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

            Bundle bundle = new Bundle();
            if (myPoint.isLocated()) {
                ooMarker = new MarkerOptions().position(myLatLong).icon(leader);
                bundle.putString("info", "\n节点ID为1"
                        + "\n经度：" + df.format(myLatLong.longitude) + "\n纬度：" + df.format(myLatLong.latitude));
            }else {
                ooMarker = new MarkerOptions().position(myLatLong).icon(leader_dark);
                bundle.putString("info", "\n节点ID为1" + "\n中心节点未定位！");
            }
            Marker leaderMarker = (Marker) baiduMap.addOverlay(ooMarker);
            Log.d("为从节点添加bundle", myPoint.toString());
            leaderMarker.setExtraInfo(bundle);
        }

        int unsafety = 0;
        if(otherPoints != null){
            int all = otherPoints.size();
            for(Point p : otherPoints){
                LatLng other = CoordinateConvert.getLatLng(p);
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
                if (!p.isLocated()){
                    ooMarker = new MarkerOptions().position(other).icon(guest_disappear);
                }
                if (!p.isSafe()) {
                    ooMarker = new MarkerOptions().position(other).icon(guest_unsafe);
                }

                Marker marker = (Marker)baiduMap.addOverlay(ooMarker);
                Log.d("为从节点添加bundle", p.toString());
                Bundle bundle = new Bundle();
                if (myLatLong.longitude <= 0 || myLatLong.latitude <= 0){
                    bundle.putString("info", "\n节点ID为" + p.getId()
                            + "\n经度：" + df.format(other.longitude)
                            + "\n纬度：" + df.format(other.latitude));
                } else {
                    bundle.putString("info", "\n节点ID为" + p.getId()
                            + "\n经度：" + df.format(other.longitude)
                            + "\n纬度：" + df.format(other.latitude)
                            + "\n距离：" + df.format(distance));
                }
                marker.setExtraInfo(bundle);
            }
            if (myPoint == null) {
                Log.d("安全区外", "服务端节点未连接，客户端节点已存在");
                outOfSafetyCount.setText("0");
                allCount.setText("" + all);
            }else if(!myPoint.isLocated()){
                Log.d("安全区外", "服务端节点未定位，客户端节点已存在");
                outOfSafetyCount.setText("0");
                allCount.setText("" + (all+1));

            }else {
                Log.d("安全区外", "服务端节点已定位，客户端节点已存在");
                outOfSafetyCount.setText("" + unsafety);
                allCount.setText("" + (all+1));

            }
        }else{
            if (myPoint == null){
                Log.d("总节点数", "服务端节点不存在，客户端节点不存在");
                allCount.setText("0");
            }else {
                Log.d("总节点数", "服务端节点已连接，客户端节点不存在");
                allCount.setText("1");
            }
        }
    }

    private void getPermission(){
        List<String> permissionList = new ArrayList<>();
        if(ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        if(ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.READ_PHONE_STATE);
        }

        if(ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        if(!permissionList.isEmpty()){
            String[] permissions = permissionList.toArray(new String[permissionList.size()]);
            ActivityCompat.requestPermissions(MainActivity.this, permissions, 1);
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

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(navigationView)){
            drawerLayout.closeDrawers();
        }else {
            super.onBackPressed();
        }
    }
}
