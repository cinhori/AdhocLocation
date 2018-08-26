package com.dantou.adhoc;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    private String myLatLongString;
    private String otherLatLongStrings;

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TextView latlongTextView = findViewById(R.id.latlong_textView);
        String message = latlongTextView.getText().toString();
        myLatLongString = "30.62, 114.13";
        //get latitude and longitude:30.62, 114.13
        Log.d("latitude and longitude", myLatLongString);

        otherLatLongStrings = "30.63, 114.13; 30.64, 114.12; 30.63, 114.1; 30.62, 114.15; 30.61, 114.13";

        Button submitButton = findViewById(R.id.submit_button);
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent mapIntent = new Intent(MainActivity.this, MapActivity.class);
                mapIntent.putExtra("myLatLongString", myLatLongString);
                mapIntent.putExtra("otherLatLongStrings", otherLatLongStrings);
                startActivity(mapIntent);
            }
        });
    }

}
