package com.wpi.cs528.hw3.pedo;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

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
}
