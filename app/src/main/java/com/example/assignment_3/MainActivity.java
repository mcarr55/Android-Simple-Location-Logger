package com.example.assignment_3;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;

import com.google.android.gms.location.LocationRequest;

import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.assignment_3.databinding.ActivityMainBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, LocationListener {

    private MyForegroundService myForegroundService_;

    private ActivityMainBinding binding;

    private boolean trackingEnabled;

    private Button startButton, stopButton;
    private TextView latitude_, longitude_, recentValues;

    private double latValue, longValue;

    private LinkedList<String> recentList;

    private static final int locationRequestCode = 1000;

    private BroadcastReceiver locationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            //  runs every time the Service sends the ping of changed location
            updateText();

            try {
                addRecent();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    };



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());



        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        startButton = findViewById(R.id.startMaterialButton);
        stopButton = findViewById(R.id.stopMaterialButton);

        startButton.setOnClickListener(this);
        stopButton.setOnClickListener(this);

        latitude_ = findViewById(R.id.latitudeTextView);
        longitude_ = findViewById(R.id.longitudeTextView);

        recentValues = findViewById(R.id.savedLocationsTextView);

        recentList = new LinkedList<>();

        //request the use of notifcations upon opening the app, for the service later.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        //read from internal storage to display existing values
        //and also load the list of saved locatons
        try {
            readInternalStorage();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public void onLocationChanged(@NonNull Location location) {

    }

    @Override
    public void onClick(View view) {

        if(view.getId() == R.id.startMaterialButton){

            //check for permissions first
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                String[] permissions = {
                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                };

                ActivityCompat.requestPermissions(this, permissions, locationRequestCode);
                return;
            }

            //if permission is true start tracking
            trackingEnabled = true;

            //check for notification permission if location permissions are available
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Notifications required for tracking service.", Toast.LENGTH_SHORT).show();
                    return; // Don't start the service yet
                }
            }

            //send intent to service to start it
            Intent startIntent = new Intent(this, MyForegroundService.class);
            startIntent.setAction("ACTION_START_LOCATION_SERVICE");
            ContextCompat.startForegroundService(this, startIntent);
        }

        //disable permissions if the user wants to stop
        if(view.getId() == R.id.stopMaterialButton){

            trackingEnabled = false;
            Toast.makeText(this, "Tracking is disabled", Toast.LENGTH_SHORT).show();

            //send intent to service
            Intent stopIntent = new Intent(this, MyForegroundService.class);
            stopIntent.setAction("ACTION_STOP_LOCATION_SERVICE");
            stopService(stopIntent);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        //for asking about location permissions
        if (requestCode == locationRequestCode) {

            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //user confirms
                Toast.makeText(this, "Permission Granted. Press again to check your location.", Toast.LENGTH_LONG).show();


            } else {
                // user denies
                Toast.makeText(this, "Location permission is required to show position values.", Toast.LENGTH_LONG).show();

            }
        }
    }

    @Override
    protected void onResume() {

        super.onResume();

        registerReceiver(locationReceiver, new IntentFilter("DATA_UPDATED"), RECEIVER_EXPORTED);

    }


    @Override
    protected void onPause() {

        super.onPause();

        //unregister receiver to preserve resources
        if (locationReceiver != null) {
            unregisterReceiver(locationReceiver);
        }
    }

    private void updateText() {
        String latValue =   getSharedPreferences("latitudePrefs", MODE_PRIVATE)
                .getString("lastLatitude", "Waiting...");

        latitude_ .setText(latValue);

        String longValue =  getSharedPreferences("longitudePrefs", MODE_PRIVATE)
                .getString("lastLongitude", "Waiting...");
        longitude_.setText(longValue);
    }

    private void addRecent() throws IOException {

        String latValue =   getSharedPreferences("latitudePrefs", MODE_PRIVATE)
                .getString("lastLatitude", "Waiting...");

        String longValue =  getSharedPreferences("longitudePrefs", MODE_PRIVATE)
                .getString("lastLongitude", "Waiting...");

        //if the last 5 entries not full yet, add it to the lst
        if (recentList.size() < 5){
            recentList.add(latValue + "   " + longValue);

        }

        //if last 5 entries are full, replace the oldest
        else{
            recentList.pop();

            recentList.add(latValue + "   " + longValue);
        }

        //save data to file
        FileOutputStream fos = openFileOutput("recent.txt", Context.MODE_PRIVATE);

        for (String location : recentList){

            String entry = location + "\n";

            fos.write(entry.getBytes());
        }

        fos.close();

        //read from file to update recents textview using list
        StringBuilder sb = new StringBuilder();

        try(FileInputStream fis = openFileInput("recent.txt");
        InputStreamReader isr = new InputStreamReader(fis);
        BufferedReader br = new BufferedReader(isr)){


        String line;

        while ((line = br.readLine()) != null) {
            sb.append(line).append("\n");
        }

        } catch (IOException e) {
            e.printStackTrace();
        }


        //update text view display
        recentValues.setText(sb.toString());
    }

    //fill the list back up when the user reopens the app.
    private void readInternalStorage() throws IOException {

        //read from file to update recents textview using list
        StringBuilder sb = new StringBuilder();

        try(FileInputStream fis = openFileInput("recent.txt");
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr)){


            String line;

            while ((line = br.readLine()) != null) {

                // fills list object
                recentList.add(line);

                //adds to lines that will be displayed in textview
                sb.append(line).append("\n");
            }

            } catch (IOException e) {
                e.printStackTrace();
            }

        //update text view display
        recentValues.setText(sb.toString());
    }

}







