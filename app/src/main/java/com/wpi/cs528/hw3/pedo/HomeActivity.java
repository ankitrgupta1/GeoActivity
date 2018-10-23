package com.wpi.cs528.hw3.pedo;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;

public class HomeActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, SensorEventListener, Steplistner{
    private static final String TAG = HomeActivity.class.getSimpleName();
    private static final int REQ_PERMISSION = 200;
    private static final long GEO_DURATION = 60 * 60 * 1000;
    private static final float GEOFENCE_RADIUS = 50.0f; // in meters
    private Toolbar toolbar;
    private GoogleMap map;
    /**
     * The a unique ID for the library geofence that prevents us form adding the geofence twice.
     */
    private static final String GEOFENCE_LIB_ID = "LIBRARY";
    /**
     * The a unique ID for the Fuller Labs geofence that prevents us form adding the geofence twice.
     */
    private static final String GEOFENCE_FLR_ID = "FULLER";
    /**
     * Used for polling our current location.
     */
    private FusedLocationProviderClient locationClient;
    /**
     * The GoogleApiClient needed for location updates.
     */
    private GoogleApiClient googleApiClient;


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

    BroadcastReceiver broadcastReceiver;

    private ImageView imgActivity;

    public static final String BROADCAST_DETECTED_ACTIVITY = "activity_intent";

    static final long DETECTION_INTERVAL_IN_MILLISECONDS = 30 * 1000;

    public static final int CONFIDENCE = 70;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        startReceivingTransitions();
        connectToGoogleApi();


        //pedo activity and activity recognition code

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        simpleStepDetector = new StepDetector();
        simpleStepDetector.registerListener(this);

        TvSteps = (TextView) findViewById(R.id.tv_steps);
        /*
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
        */

        //Activity Recognition
        imgActivity = findViewById(R.id.img_activity);
        /*
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
        */

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(BROADCAST_DETECTED_ACTIVITY)) {
                    int type = intent.getIntExtra("type", -1);
                    int confidence = intent.getIntExtra("confidence", 0);
                    handleUserActivity(type, confidence);
                }
            }
        };

        startTracking();
    }

    @Override protected void onStart() {
        super.onStart();
        // Call GoogleApiClient connection when starting the Activity
        googleApiClient.connect();
    }

    @Override protected void onStop() {
        super.onStop();
        // Disconnect GoogleApiClient when stopping Activity
        googleApiClient.disconnect();
    }

    /**
     * This method is called when our GoogleApiClient connects successfully.
     */
    @Override public void onConnected(@Nullable Bundle bundle) {
        beginLocationOperations();
    }

    @Override public void onConnectionSuspended(int code) {
        Log.d(TAG, "Nothing to do but hope we reconnect");
    }

    @Override public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e(TAG, connectionResult.getErrorMessage());
    }

    /**
     * Because the geofence transitions (enter and exit events) are supplied to us inside
     * {@link GeofenceTransitionService} we need a way of displaying new updates in the UI. We do so
     * by registering a BroadcastReceiver to observe the transitions.
     */
    private void startReceivingTransitions() {
        //Listen for broadcasts form the GeofenceTransitionService
        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(this);
        broadcastManager.registerReceiver(new BroadcastReceiver() {
            @Override public void onReceive(Context context, Intent input) {
                int transition = input.getIntExtra(GeofenceTransitionService.GEOFENCE_TRANS_KEY, -1);
                String id = input.getStringExtra(GeofenceTransitionService.GEOFENCE_ID_KEY);

                if (transition == Geofence.GEOFENCE_TRANSITION_ENTER) {
                    Log.d("HomeActivity", "Entering " + id);
                } else if (transition == Geofence.GEOFENCE_TRANSITION_EXIT) {
                    Log.d("HomeActivity", "Exiting " + id);
                }
            }
        }, new IntentFilter(GeofenceTransitionService.GEOFENCE_ACTION));
    }

    /**
     * This method creates the two geofences from the project spec. It will do nothing if we don't
     * have location permission.
     */
    private void initGeofences() {
        if (!checkPermission()) return;
        Geofence libGeoFence = createGeofence(
                new LatLng(42.274192, -71.806354),
                GEOFENCE_RADIUS, GEOFENCE_LIB_ID);
        Geofence fullerGeoFence = createGeofence(
                new LatLng(42.274954, -71.806397),
                GEOFENCE_RADIUS, GEOFENCE_FLR_ID);
        addGeofence(createGeofenceRequest(libGeoFence));
        addGeofence(createGeofenceRequest(fullerGeoFence));
    }

    /**
     * This method attempts to connect to the GoogleApiClient if we don't have an instance already.
     * If it is successful we start performing our location reliant operations from #onConnected().
     */
    private void connectToGoogleApi() {
        //Get the current location which is then displayed in the map
        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
    }

    /**
     * This method will handle requesting location permission, initializing our geofences, and
     * updating the map with our current location.
     */
    private void beginLocationOperations() {
        if (checkPermission()) {
            initGeofences();
            locationClient = LocationServices.getFusedLocationProviderClient(this);
            locationClient.getLastLocation()
                    .addOnSuccessListener(new OnSuccessListener<Location>() {
                        @Override public void onSuccess(Location location) {
                            subscribeToLocationUpdates();
                        }
                    });

        } else askPermission();
    }

    /**
     * Starts listening for location updates every second. The new location is displayed in the map.
     */
    private void subscribeToLocationUpdates() {
        LocationRequest locationRequest;
        // Defined in milli seconds. This number in extremely low, and should be used only for debug
        final int UPDATE_INTERVAL = 1000;
        final int FASTEST_INTERVAL = 900;
        Log.i(TAG, "subscribeToLocationUpdates()");
        locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(UPDATE_INTERVAL)
                .setFastestInterval(FASTEST_INTERVAL);

        if (checkPermission()) {
            locationClient.requestLocationUpdates(locationRequest, new LocationCallback() {
                @Override public void onLocationResult(LocationResult locationResult) {
                    for (Location l : locationResult.getLocations()) {
                        Log.d(TAG, "Lat:" + l.getLatitude() + "Long:" + l.getLongitude());
                    }
                }
            }, null);
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near WPI.
     */
    @Override public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        // Add a marker and move the camera
        LatLng wpi = new LatLng(42.2732381, -71.8105257);
        map.addMarker(new MarkerOptions().position(wpi).title("Marker at WPI"));
        map.moveCamera(CameraUpdateFactory.newLatLng(wpi));
    }

    /**
     * A convenience method for creating geofences.
     */
    private void addGeofence(GeofencingRequest request) {
        if (checkPermission()) {
            Intent intent = new Intent(this, GeofenceTransitionService.class);
            PendingIntent pending = PendingIntent.getService(
                    this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            LocationServices.getGeofencingClient(this)
                    .addGeofences(request, pending);
        }
    }

    /**
     * Utility method for creating a Geofence Request.
     */
    private GeofencingRequest createGeofenceRequest(Geofence geofence) {
        return new GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(geofence)
                .build();
    }

    /**
     * This method conveniently creates a Geofence object.
     */
    private Geofence createGeofence(LatLng latLng, float radius, String reqId) {
        return new Geofence.Builder()
                .setRequestId(reqId)
                .setCircularRegion(latLng.latitude, latLng.longitude, radius)
                .setExpirationDuration(GEO_DURATION)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER
                        | Geofence.GEOFENCE_TRANSITION_EXIT)
                .build();
    }

    @Override public void onRequestPermissionsResult(int requestCode,
                                                     @NonNull String[] permissions,
                                                     @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQ_PERMISSION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted
                    beginLocationOperations();
                } else {
                    // Permission denied
                    Log.w(TAG, "permissions denied ");
                }
                break;
            }
        }
    }

    private boolean checkPermission() {
        // Ask for permission if it wasn't granted yet
        return (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED);
    }

    private void askPermission() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                REQ_PERMISSION
        );
    }



    @Override
    public void step(long timeNs) {
        numSteps++;
        String text = TEXT_NUM_STEPS + numSteps;
        System.out.println("number of steps="+ numSteps);
        TvSteps.setText(text);
        if(numSteps >=6) {
            String toast_text = null;
            //geofence_trig is used for the differenciation between the geofence entry triggers
            if(geofence_trig == 1) {
                toast_text = getResources().getString(R.string.gordon);
                library_count++;
            }
            else {
                toast_text = getResources().getString(R.string.fuller);
                fuller_count++;
            }
            Toast.makeText(HomeActivity.this,toast_text, Toast.LENGTH_SHORT).show();
            sensorManager.unregisterListener(HomeActivity.this);
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

        if (confidence > CONFIDENCE) {
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
                new IntentFilter(BROADCAST_DETECTED_ACTIVITY));
    }

    @Override
    protected void onPause() {
        super.onPause();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
    }

    private void startTracking() {
        Intent intent = new Intent(HomeActivity.this, BackgroundDetectedActivitiesService.class);
        startService(intent);
    }

    private void stopTracking() {
        Intent intent = new Intent(HomeActivity.this, BackgroundDetectedActivitiesService.class);
        stopService(intent);
    }

    private void registerPedoSensor() {
        numSteps = 0;
        Log.i("activity started", "activity started");
        System.out.println("activity started");
        sensorManager.registerListener(HomeActivity.this, accel, SensorManager.SENSOR_DELAY_FASTEST);
    }
}
