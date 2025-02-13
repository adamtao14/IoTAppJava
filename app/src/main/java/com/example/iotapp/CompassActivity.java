package com.example.iotapp;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.iotapp.database.AppDatabase;
import com.example.iotapp.models.Coordinates;
import com.example.iotapp.models.Message;

import java.util.ArrayList;
import java.util.List;

public class CompassActivity extends AppCompatActivity implements BluetoothManager.BluetoothConnectionListener{
    private CompassView compassView;
    private SensorManager sensorManager;
    private LocationManager locationManager;
    private CompassView.UserLocation userLocation;
    private Button usersListButton;
    private List<CompassView.UserLocation> userLocations;
    private static final String PREFS_NAME = "signals_prefs";
    private static final String KEY_UNIQUE_ID = "unique_id";
    private String ID;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compass);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Compass");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }


        // Handle generation or retrieval of unique user id
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        ID = prefs.getString(KEY_UNIQUE_ID, null);
        if(ID == null){
            ID = SignalsActivity.generateUniqueId();
            prefs.edit().putString(KEY_UNIQUE_ID, ID).apply();
        }


        usersListButton = findViewById(R.id.usersButton);
        usersListButton.setOnClickListener(view -> showUserPopup());

        compassView = findViewById(R.id.compassView);

        // Initialize sensor manager
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        
        // Initialize location manager
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        userLocation = new CompassView.UserLocation(ID,0,0);
        // Fetch current GPS coordinates
        fetchCurrentLocation();

        //Set initial position
        compassView.setCurrentLat(userLocation.getLatitude());
        compassView.setCurrentLon(userLocation.getLongitude());

        // Generate dummy user locations near the current location
        //userLocations = generateNearbyLocations(userLocation, 3); // Generate 3 test locations
        userLocations = getUsersCoordinates();
        compassView.updateUserLocations(userLocations);

        // Request location updates
        requestLocationUpdates();
    }
    private void showUserPopup() {
        // Inflate the popup layout
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.popup_users, null);

        // Create the PopupWindow
        PopupWindow popupWindow = new PopupWindow(
                popupView,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                true
        );

        // Set the user list
        ListView userListView = popupView.findViewById(R.id.userListView);
        UserAdapter adapter = new UserAdapter(this, userLocations);
        userListView.setAdapter(adapter);

        // Close button
        Button closePopupButton = popupView.findViewById(R.id.closePopupButton);
        closePopupButton.setOnClickListener(v -> popupWindow.dismiss());

        // Show the popup
        popupWindow.setElevation(10);
        popupWindow.showAtLocation(findViewById(R.id.compass), Gravity.CENTER, 0, 0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Register the sensor listener
        if (sensorManager != null) {
            Sensor orientationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
            if (orientationSensor != null) {
                sensorManager.registerListener(compassView, orientationSensor, SensorManager.SENSOR_DELAY_UI);
            }
        }

        // Resume location updates
        requestLocationUpdates();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Navigate back to the previous activity
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unregister the sensor listener
        if (sensorManager != null) {
            sensorManager.unregisterListener(compassView);
        }

        // Stop location updates
        if (locationManager != null) {
            locationManager.removeUpdates(locationListener);
        }
    }

    private void fetchCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Request location permissions
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }

        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (location != null) {
            userLocation.setLatitude(location.getLatitude());
            userLocation.setLongitude(location.getLongitude());
            compassView.setUserLocation(location.getLatitude(), location.getLongitude());
        }
    }

    private void requestLocationUpdates() {
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, locationListener);
        } catch (SecurityException e) {
            e.printStackTrace(); // Handle permission issues
        }
    }

    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {

            if (userLocation != null) {
                // Call setUserLocation with latitude and longitude
                compassView.setUserLocation(userLocation.getLatitude(), userLocation.getLongitude());
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {}

        @Override
        public void onProviderEnabled(String provider) {}

        @Override
        public void onProviderDisabled(String provider) {}
    };

    private List<CompassView.UserLocation> getUsersCoordinates(){
        List<CompassView.UserLocation> locations = new ArrayList<>();

        new Thread(() -> {
            List<Coordinates> allCoordinates = AppDatabase.getInstance(this).coordinateDao().getAllCoordinates();
            for (Coordinates coordinates : allCoordinates) {
                CompassView.UserLocation loc = new CompassView.UserLocation(coordinates.getId(), coordinates.getLatitude(), coordinates.getLongitude());
                locations.add(loc);
            }
        }).start();

        return locations;
    }
    /*
    private List<CompassView.UserLocation> generateNearbyLocations(CompassView.UserLocation location, int count) {
        List<CompassView.UserLocation> locations = new ArrayList<>();

        double baseLat = location.getLatitude();
        double baseLon = location.getLongitude();

        for (int i = 0; i < count; i++) {
            double offsetLat = (Math.random() - 0.5) * 0.01; // Random offset within ~1 km
            double offsetLon = (Math.random() - 0.5) * 0.01; // Random offset within ~1 km

            double lat = baseLat + offsetLat;
            double lon = baseLon + offsetLon;
            CompassView.UserLocation loc = new CompassView.UserLocation(SignalsActivity.generateUniqueId(), lat, lon);
            locations.add(loc);
        }

        return locations;
    }
    */

    @Override
    public void onConnectionSuccess(String deviceName) {

    }

    @Override
    public void onConnectionFailed(String errorMessage) {

    }

    @Override
    public void onDisconnected() {
        Toast.makeText(this, "Disconnected from device", Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    public void onMessageReceived(String message) {
        Message receivedMessage = Message.fromBluetoothMessage(message);
        if (receivedMessage != null) {
            runOnUiThread(() -> {
                // Add it to the database
                new Thread(() -> {
                    AppDatabase.getInstance(this).messageDao().insert(receivedMessage);
                    if(receivedMessage.getType().equals("1")){
                        String coords[] = receivedMessage.getPayload().split("-");
                        String latitude = coords[0];
                        String longitude = coords[1];
                        Coordinates coordinates = AppDatabase.getInstance(this).coordinateDao().getCoordinatesById(receivedMessage.getId());
                        if(coordinates != null){
                            // Id already exists so update them
                            AppDatabase.getInstance(this).coordinateDao().updateCoordinatesById(receivedMessage.getId(), Double.valueOf(latitude), Double.valueOf(longitude));

                        }else{
                            // Create them
                            Coordinates newCoordinates = new Coordinates(receivedMessage.getId(), Double.valueOf(latitude), Double.valueOf(longitude));
                            AppDatabase.getInstance(this).coordinateDao().insert(newCoordinates);
                        }
                    }
                }).start();
            });
        }
    }
}
