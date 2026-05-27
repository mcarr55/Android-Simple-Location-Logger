package com.example.assignment_3;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.Context;
import android.content.pm.ServiceInfo;
import android.location.Location;
import com.google.android.gms.location.LocationRequest;

import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import com.example.assignment_3.databinding.ActivityMainBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.io.FileOutputStream;
import java.io.IOException;

public class MyForegroundService extends Service {

    private FusedLocationProviderClient fusedLocationClient;     //Transfer to service
    private LocationCallback _locationCallback;     //Transfer to service

    private LocationRequest _locationRequest; // transfer to service


    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("MyService", "onCreate called");

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Rate of obtaining the location if permission is granted
        _locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
                .setMinUpdateIntervalMillis(5000)
                .build();

        //updates the latitude and longitude values and textview when it can obtain a result
        _locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult _locationResult) {

                //get the new locaiton values
                Location loc = _locationResult.getLastLocation();

                String latitudeNumber = "Latitude: "+loc.getLatitude();
                String longitudeNumber = "Longitude: "+loc.getLongitude();


                // update the current values
                getSharedPreferences("latitudePrefs", MODE_PRIVATE).edit().putString("lastLatitude", latitudeNumber).apply();
                getSharedPreferences("longitudePrefs", MODE_PRIVATE).edit().putString("lastLongitude", longitudeNumber).apply();


                // send a ping to MainActivity so it knows to refresh
                sendBroadcast(new Intent("DATA_UPDATED"));

            }
        };

    }



    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {


        Log.d("MyService", "onStartCommand called");

        if(intent != null) {
            String action = intent.getAction();

            //when user presses start button
            if ("ACTION_START_LOCATION_SERVICE".equals(action)) {


                Log.d("Service", "Starting the engine...");

                //create notificaiton to display during active tracking
                Notification notification_ = createNotification();

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    startForeground(1, notification_, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
                } else {
                    startForeground(1, notification_);
                }

                //fetch location values
                fusedLocationClient.requestLocationUpdates(_locationRequest, _locationCallback, Looper.getMainLooper());


                return START_STICKY;
            }

            //when user presses stop button
            else if ("ACTION_STOP_LOCATION_SERVICE".equals(action)) {

                Log.d("Service", "Stopping the engine...");
                stopForeground(true);
                stopSelf();
            }
        }

        return super.onStartCommand(intent, flags, startId);
    }






    public MyForegroundService() {

    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }




    @Override
    public void onDestroy() {
        //cleanup

        super.onDestroy();

        Log.d("MyService", "onDestroy called");

    }

    private Notification createNotification() {

        //create notification channel to show when the service is active
        String CHANNEL_ID = "my_service_channel";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Service Channel", NotificationManager.IMPORTANCE_HIGH);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }

        // Create an intent that opens MainActivity when clicked
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE
        );


        // build the Notification that shows when Tracking is active.
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Tracking Service Active")
                .setContentText("This service is currently running.")
                .setSmallIcon(android.R.drawable.ic_menu_info_details)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build();
    }
}