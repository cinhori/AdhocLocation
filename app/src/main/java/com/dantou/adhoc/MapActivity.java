package com.dantou.adhoc;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.Dot;
import com.baidu.mapapi.map.DotOptions;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.model.LatLng;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class MapActivity extends AppCompatActivity {

    public LocationClient locationClient;

    private MapView mapView;

    private BaiduMap baiduMap;
    private boolean isFirstLocate = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SDKInitializer.initialize(getApplicationContext());
        setContentView(R.layout.activity_map);

        locationClient = new LocationClient(getApplicationContext());
        locationClient.registerLocationListener(new MyLocationListener());

        mapView = findViewById(R.id.baiduMapView);
        baiduMap = mapView.getMap();
        baiduMap.setMyLocationEnabled(true);

        Intent intent = getIntent();
        String myLatLongString = intent.getStringExtra("myLatLongString");
        double myLatitude = Double.parseDouble(myLatLongString.split(",")[0]);
        double myLongitude = Double.parseDouble(myLatLongString.split(",")[1]);
        LatLng myLatLong = new LatLng(myLatitude, myLongitude);

        List<LatLng> others = new LinkedList<>();

        String otherLatLongStrings = intent.getStringExtra("otherLatLongStrings");
        String[] otherLatLongs = otherLatLongStrings.split(";");
        for (String o : otherLatLongs){
            others.add(new LatLng(Double.parseDouble(o.split(",")[0]), Double.parseDouble(o.split(",")[1])));
        }

        OverlayOptions ooDot;
        for(LatLng other : others) {
            ooDot = new DotOptions().center(other).radius(15).color(Color.RED);//红色，从Color类中获取
            baiduMap.addOverlay(ooDot);
        }

        /**
         *
         */
        List<String> permissionList = new ArrayList<>();
        if(ContextCompat.checkSelfPermission(MapActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        if(ContextCompat.checkSelfPermission(MapActivity.this,
                Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.READ_PHONE_STATE);
        }

        if(ContextCompat.checkSelfPermission(MapActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        if(!permissionList.isEmpty()){
            String[] permissions = permissionList.toArray(new String[permissionList.size()]);
            ActivityCompat.requestPermissions(MapActivity.this, permissions, 1);
        }else {
            requestLocation();
        }

        if(isFirstLocate){
            MapStatusUpdate update = MapStatusUpdateFactory.zoomTo(18.0f);//3-19
            baiduMap.animateMapStatus(update);

            update = MapStatusUpdateFactory.newLatLng(myLatLong);
            baiduMap.animateMapStatus(update);
        }

        MyLocationData.Builder locationBuilder = new MyLocationData.Builder();
        locationBuilder.latitude(myLatLong.latitude);
        locationBuilder.longitude(myLatLong.longitude);
        MyLocationData locationData = locationBuilder.build();
        baiduMap.setMyLocationData(locationData);

        /*for(LatLng latLng : others){
            locationBuilder.latitude(latLng.latitude);
            locationBuilder.longitude(latLng.longitude);
            locationData = locationBuilder.build();
            baiduMap.setMyLocationData(locationData);
        }*/

    }

    private void requestLocation(){
        locationClient.start();
    }

    private void navigateTo(){

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
                    requestLocation();
                } else {
                    Toast.makeText(this, "Unknown Error", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            default:
        }
    }

    public class MyLocationListener implements BDLocationListener {

        @Override
        public void onReceiveLocation(BDLocation bdLocation) {
            StringBuilder currentPosition = new StringBuilder();
            currentPosition.append("Latitude").append(bdLocation.getLatitude()).append("\n");
            currentPosition.append("Longitude").append(bdLocation.getLongitude()).append("\n");
            currentPosition.append("Location way:");
            if (bdLocation.getLocType() == BDLocation.TypeNetWorkLocation) {
                currentPosition.append("network");
            }else if(bdLocation.getLocType() == BDLocation.TypeGpsLocation) {
                currentPosition.append("GPS");
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
        baiduMap.setMyLocationEnabled(false);
    }
}
