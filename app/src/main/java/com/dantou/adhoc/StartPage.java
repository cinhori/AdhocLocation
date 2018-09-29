package com.dantou.adhoc;

import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Window;

import com.baidu.mapapi.SDKInitializer;

import java.util.Timer;
import java.util.TimerTask;

public class StartPage extends AppCompatActivity {
    public final static long DELAY = 3 * 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SDKInitializer.initialize(getApplicationContext());
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_start_page);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

    }

    @Override
    protected void onStart() {
        super.onStart();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                final Intent intent = new Intent(StartPage.this, DeviceScanActivity.class);
                startActivity(intent);
                finish();
            }
        }, DELAY);

    }
}
