package com.wpi.cs528.hw3.pedo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.DetectedActivity;

public class PedoActivity extends AppCompatActivity implements SensorEventListener, Steplistner {


    private TextView TvSteps;
    private StepDetector simpleStepDetector;
    private SensorManager sensorManager;
    private Sensor accel;
    private static final String TEXT_NUM_STEPS = "Number of Steps: ";
    private int numSteps;
    private static int library_count;
    private static int fuller_count;
    private int geofence_trig = 1;
    //Activity Recognition Variables
    private String TAG = PedoActivity.class.getSimpleName();
    BroadcastReceiver broadcastReceiver;
    private ImageView imgActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pedo);

        // Get an instance of the SensorManager
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        simpleStepDetector = new StepDetector();
        simpleStepDetector.registerListener(this);

        TvSteps = (TextView) findViewById(R.id.tv_steps);
        Button BtnStart = (Button) findViewById(R.id.btn_start);
        Button BtnStop = (Button) findViewById(R.id.btn_stop);
        BtnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                numSteps = 0;
                Log.i("activity started", "activity started");
                System.out.println("activity started");
                sensorManager.registerListener(PedoActivity.this, accel, SensorManager.SENSOR_DELAY_FASTEST);
            }
        });
        BtnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Log.i("activity stopped", "activity stopped");
                sensorManager.unregisterListener(PedoActivity.this);
            }
        });

        //Activity Recognition
        imgActivity = findViewById(R.id.img_activity);
        Button btnStartTrcking = findViewById(R.id.btn_start_tracking);
        Button btnStopTracking = findViewById(R.id.btn_stop_tracking);

        btnStartTrcking.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startTracking();
            }
        });

        btnStopTracking.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopTracking();
            }
        });

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(Constants.BROADCAST_DETECTED_ACTIVITY)) {
                    int type = intent.getIntExtra("type", -1);
                    int confidence = intent.getIntExtra("confidence", 0);
                    handleUserActivity(type, confidence);
                }
            }
        };

        startTracking();

    }

    @Override
    public void step(long timeNs) {
        numSteps++;
        String text = TEXT_NUM_STEPS + numSteps;
        System.out.println("number of steps=" + numSteps);
        TvSteps.setText(text);
        if (numSteps >= 6) {
            String toast_text = null;
            //geofence_trig is used for the differenciation between the geofence entry triggers
            if (geofence_trig == 1) {
                toast_text = getResources().getString(R.string.gordon);
                library_count++;
            } else {
                toast_text = getResources().getString(R.string.fuller);
                fuller_count++;
            }
            Toast.makeText(PedoActivity.this, toast_text, Toast.LENGTH_SHORT).show();
            sensorManager.unregisterListener(PedoActivity.this);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Log.i("activity started", "activity detected");
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            Log.i("activity started", "accelerometer activity detected");
            simpleStepDetector.updateAccel(
                    event.timestamp, event.values[0], event.values[1], event.values[2]);
        }
    }

    private void handleUserActivity(int type, int confidence) {
        String label = getString(R.string.activity_unknown);

        switch (type) {
            case DetectedActivity.RUNNING: {
                label = getString(R.string.activity_running);
                break;
            }
            case DetectedActivity.STILL: {
                label = getString(R.string.activity_still);
                break;
            }
            case DetectedActivity.WALKING: {
                label = getString(R.string.activity_walking);
                break;
            }
            case DetectedActivity.UNKNOWN: {
                label = getString(R.string.activity_unknown);
                break;
            }
        }

        Log.e(TAG, "User activity: " + label + ", Confidence: " + confidence);

        if (confidence > Constants.CONFIDENCE) {
            Toast toast = Toast.makeText(this.getApplicationContext(),getString(R.string.activity_toast)+" "+label,Toast.LENGTH_SHORT);
            toast.show();
            if(label.equals(getString(R.string.activity_running))){
                imgActivity.setImageResource(R.drawable.run);
            }else if(label.equals(getString(R.string.activity_walking))){
                imgActivity.setImageResource(R.drawable.walk);
            }else if(label.equals(getString(R.string.activity_still))){
                imgActivity.setImageResource(R.drawable.still);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver,
                new IntentFilter(Constants.BROADCAST_DETECTED_ACTIVITY));
    }

    @Override
    protected void onPause() {
        super.onPause();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
    }

    private void startTracking() {
        Intent intent = new Intent(PedoActivity.this, BackgroundDetectedActivitiesService.class);
        startService(intent);
    }

    private void stopTracking() {
        Intent intent = new Intent(PedoActivity.this, BackgroundDetectedActivitiesService.class);
        stopService(intent);
    }
}
