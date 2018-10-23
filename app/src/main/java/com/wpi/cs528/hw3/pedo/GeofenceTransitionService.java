
package com.wpi.cs528.hw3.pedo;

import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofenceStatusCodes;
import com.google.android.gms.location.GeofencingEvent;

import java.util.List;

public class GeofenceTransitionService extends IntentService {
    private static final String TAG = GeofenceTransitionService.class.getSimpleName();
    public static String GEOFENCE_ACTION = "GEOFENCE_ACTION";
    public static String GEOFENCE_TRANS_KEY = "GEOFENCE_TRANS_KEY";
    public static String GEOFENCE_ID_KEY = "GEOFENCE_ID_KEY";
    public GeofenceTransitionService() {
        super("GeofenceTransitionService");
    }

    protected void onHandleIntent(Intent intent) {
        GeofencingEvent geofenceEvent = GeofencingEvent.fromIntent(intent);
        if (geofenceEvent.hasError()) {
            Log.e(TAG, GeofenceStatusCodes.getStatusCodeString(geofenceEvent.getErrorCode()));
            return;
        }

        // Get the transition type.
        int geofenceTransition = geofenceEvent.getGeofenceTransition();

        // Test that the reported transition was of interest.
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ||
                geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {

            // Get the geofences that were triggered. A single event can trigger multiple geofences.
            List<Geofence> triggeringGeofences = geofenceEvent.getTriggeringGeofences();
            if (triggeringGeofences.size() == 0) return;
            //HomeActivity is listening for this broadcast
            Intent data = new Intent(GEOFENCE_ACTION)
                    .putExtra(GEOFENCE_TRANS_KEY, geofenceTransition)
                    .putExtra(GEOFENCE_ID_KEY, triggeringGeofences.get(0).getRequestId());

            LocalBroadcastManager
                    .getInstance(this)
                    .sendBroadcast(data);

            // Send notification and log the transition details.
            Log.i(TAG, geofenceEvent.toString());
        } else {
            // Log the error.
            Log.e(TAG, "invalid geofencing transition type" + geofenceTransition);
        }
    }
}
