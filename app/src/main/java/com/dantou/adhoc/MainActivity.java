package com.dantou.adhoc;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.dantou.model.Point;
import com.dantou.util.StringToLatLong;

import java.util.ArrayList;
import java.util.LinkedList;

public class MainActivity extends AppCompatActivity {
    //private String myLatLongString;
    //private String otherLatLongStrings;

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    private Point myPoint;
    private ArrayList<Point> otherPoints = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TextView latlongTextView = findViewById(R.id.latlong_textView);
        Intent intent = getIntent();
        String latlong = intent.getStringExtra("data");
        Log.d("原始16进制数据", latlong);
        ArrayList<Point> points =  StringToLatLong.toLatLongs(latlong);
        Log.d("节点信息", points.toString());
        latlongTextView.setText(points.toString());
        //myLatLongString = "30.62, 114.13";
        //get latitude and longitude:30.62, 114.13
        //Log.d("latitude and longitude", myLatLongString);
        for(Point p : points){
            if(p.getId() == 1){
                myPoint = p;
            }else{
                otherPoints.add(p);
            }
        }

        //otherLatLongStrings = "30.63, 114.13; 30.64, 114.12; 30.63, 114.1; 30.62, 114.15; 30.61, 114.13";

        Button submitButton = findViewById(R.id.submit_button);
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent mapIntent = new Intent(MainActivity.this, MapActivity.class);
                //mapIntent.putExtra("myLatLongString", myLatLongString);
                //mapIntent.putExtra("otherLatLongStrings", otherLatLongStrings);
                mapIntent.putExtra("myPoint", myPoint);
                mapIntent.putParcelableArrayListExtra("otherPoints", otherPoints);
                startActivity(mapIntent);
            }
        });
    }

}
