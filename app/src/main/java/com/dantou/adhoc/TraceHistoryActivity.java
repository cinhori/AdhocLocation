package com.dantou.adhoc;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.map.Polyline;
import com.baidu.mapapi.map.PolylineOptions;
import com.baidu.mapapi.map.TextureMapView;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.utils.DistanceUtil;
import com.dantou.model.Point;
import com.dantou.util.CoordinateConvert;
import com.dantou.util.MyDatabaseHelper;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class TraceHistoryActivity extends AppCompatActivity {
    public static final int HISTORY_DISTANCE = 20;

    private MyDatabaseHelper dbHelper;
    private SQLiteDatabase db;
    private SimpleDateFormat simpleDateFormat;

    private TextureMapView mapView;
    private BaiduMap baiduMap;
    private OverlayOptions ooMarker;
    private BitmapDescriptor server;

    private Boolean isFirstLocate = true;

    private DecimalFormat df;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SDKInitializer.initialize(getApplicationContext());
        setContentView(R.layout.activity_trace_history);

        mapView = findViewById(R.id.baiduMapViewHistory);
        baiduMap = mapView.getMap();
        baiduMap.setMyLocationEnabled(true);
        server = BitmapDescriptorFactory.fromResource(R.drawable.marker_red_16);


        dbHelper = new MyDatabaseHelper(this, MyDatabaseHelper.DB_NAME, null, 1);
        db = dbHelper.getReadableDatabase();
        simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

        baiduMap.setOnMarkerClickListener(new BaiduMap.OnMarkerClickListener() {

            @Override
            public boolean onMarkerClick(Marker marker) {
                Bundle bundle = marker.getExtraInfo();
                String info = bundle.getString("info");
                Log.d("显示bundle信息", info);
                Toast.makeText(TraceHistoryActivity.this, info, Toast.LENGTH_LONG).show();
                return false;

            }
        });

        df = new DecimalFormat("#.0000");
    }

    @Override
    protected void onStart() {
        super.onStart();
        LinkedList<Point> pointList = new LinkedList<>();
        Cursor cursor = dbHelper.query(db);
        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndex("user_id"));
                double latitude = Double.parseDouble(cursor.getString(cursor.getColumnIndex("latitude")));
                double longitude = Double.parseDouble(cursor.getString(cursor.getColumnIndex("longitude")));
                String dateString = cursor.getString(cursor.getColumnIndex("date"));
                int safe = cursor.getInt(cursor.getColumnIndex("safe"));
                int located = cursor.getInt(cursor.getColumnIndex("located"));
                try {
                    Point point = new Point(id, latitude, longitude,
                            simpleDateFormat.parse(dateString), safe == 1, located == 1);
                    Log.d("TraceHistoryActivity", point.toString());
                    pointList.add(point);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }while (cursor.moveToNext());
        }
        baiduMap.clear();
        addTrace(pointList);
    }

    public void addTrace(List<Point> points){
        if (points.size() < 2) return;
        List<LatLng> pointsLatLng = new LinkedList<>();
        Point lastPoint = points.get(0);
        LatLng lastLatLng = CoordinateConvert.getLatLng(lastPoint);
        pointsLatLng.add(lastLatLng);

        long retentionTime = 0L;
        for (Point point : points) {
            LatLng tempLatLng = CoordinateConvert.getLatLng(point);
            if (DistanceUtil.getDistance(lastLatLng, tempLatLng) < HISTORY_DISTANCE) {
                retentionTime += point.getDate().getTime() - lastPoint.getDate().getTime();
                continue;
            }
            String retentionTimeString = getRetentionTime(retentionTime);
            pointsLatLng.add(tempLatLng);
            ooMarker = new MarkerOptions().position(lastLatLng).icon(server);
            Marker marker = (Marker)baiduMap.addOverlay(ooMarker);
            Log.d("为从节点添加bundle", lastPoint.toString());
            Bundle bundle = new Bundle();
            bundle.putString("info", "\n经度：" + df.format(lastLatLng.longitude)
                    + ";\n纬度：" + df.format(lastLatLng.latitude)
                    + ";\n开始时间：" + simpleDateFormat.format(lastPoint.getDate())
                    + ";\n停留时间：" + retentionTimeString);
            marker.setExtraInfo(bundle);

            lastLatLng = tempLatLng;
            retentionTime = 0L;

        }

        ooMarker = new MarkerOptions().position(lastLatLng).icon(server);
        Marker marker = (Marker)baiduMap.addOverlay(ooMarker);
        Log.d("为最后一个节点添加bundle", lastPoint.toString());
        Bundle bundle = new Bundle();
        if (retentionTime == 0L) {
            bundle.putString("info", "经度：" + df.format(lastLatLng.longitude)
                    + ";\n纬度：" + df.format(lastLatLng.latitude)
                    + ";\n开始时间：" + simpleDateFormat.format(lastPoint.getDate())
                    + ";\n停留时间：0s");
            marker.setExtraInfo(bundle);
        }else {
            bundle.putString("info", "经度：" + df.format(lastLatLng.longitude)
                    + ";\n纬度：" + df.format(lastLatLng.latitude)
                    + ";\n开始时间：" + simpleDateFormat.format(lastPoint.getDate())
                    + ";\n停留时间：" + getRetentionTime(retentionTime));
            marker.setExtraInfo(bundle);
        }

        if(isFirstLocate){
            MapStatusUpdate update = MapStatusUpdateFactory.zoomTo(18.0f);//3-19
            baiduMap.animateMapStatus(update);

            update = MapStatusUpdateFactory.newLatLng(pointsLatLng.get(0));
            baiduMap.animateMapStatus(update);

            isFirstLocate = false;
        }

        if (pointsLatLng.size() < 2) {
            Toast.makeText(TraceHistoryActivity.this, "采集的数据太少，不能绘制轨迹", Toast.LENGTH_SHORT).show();
        }else {
            OverlayOptions ooPolyline = new PolylineOptions().width(10).color(Color.GREEN).points(pointsLatLng);
            Polyline mPolyline = (Polyline) baiduMap.addOverlay(ooPolyline);
        }

    }

    public String getRetentionTime(long retentionMilliseconds){
        String retentionTime = "";
        long retentionSeconds = retentionMilliseconds / 1000;
        if (retentionSeconds >= 3600) {
            retentionTime += (retentionSeconds / 3600) + "小时";
            retentionSeconds %= 3600;
        }
        if (retentionSeconds >= 60) {
            retentionTime += (retentionSeconds / 60) + "分钟";
            retentionSeconds %= 60;
        }
        if (retentionSeconds >= 0) {
            retentionTime += retentionSeconds + "秒";
        }
        return retentionTime;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
        baiduMap.setMyLocationEnabled(false);
        server.recycle();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onPause();
    }
}
