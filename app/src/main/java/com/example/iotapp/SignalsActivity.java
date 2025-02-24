package com.example.iotapp;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.iotapp.database.AppDatabase;
import com.example.iotapp.models.Coordinates;
import com.example.iotapp.models.Message;
import com.example.iotapp.utils.PeriodicTask;

import java.security.SecureRandom;
import java.text.DecimalFormat;

public class SignalsActivity extends AppCompatActivity implements BluetoothManager.BluetoothConnectionListener{
    private Spinner spinnerMessageType;
    private SeekBar seekBarFrequency;
    private TextView textFrequency;
    private TextView currentCoordinates;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private BluetoothManager bluetoothManager;
    private Spinner spinnerStatus;
    private double latitude;
    private double longitude;
    private String ID;
    private boolean alreadyRunningTask = false;
    private int currentFrequency;
    private boolean frequencySet;
    private int currentStatusIndex;
    private static final SecureRandom random = new SecureRandom();
    private static final String PREFS_NAME = "signals_prefs";
    private static final String KEY_UNIQUE_ID = "unique_id";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signals);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Signals");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        initializeBluetoothManager();

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        spinnerMessageType = findViewById(R.id.spinner_message_type);
        seekBarFrequency = findViewById(R.id.seekbar_frequency);
        textFrequency = findViewById(R.id.text_frequency);
        Button buttonSendMessage = findViewById(R.id.button_send_message);
        currentCoordinates = findViewById(R.id.current_coordinates);
        SwitchCompat frequencySwitch = findViewById(R.id.frequency_switch);
        spinnerStatus = findViewById(R.id.spinner_status);

        // Default frequency
        currentFrequency = 0;
        frequencySet = false;

        // Handle generation or retrieval of unique user id
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String ID = prefs.getString(KEY_UNIQUE_ID, null);
        if(ID == null){
            ID = generateUniqueId();
            prefs.edit().putString(KEY_UNIQUE_ID, ID).apply();
        }

        // Populate Message Type Dropdown
        ArrayAdapter<CharSequence> messageTypeAdapter = ArrayAdapter.createFromResource(this,
                R.array.message_types, android.R.layout.simple_spinner_item);
        messageTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMessageType.setAdapter(messageTypeAdapter);

        // Populate status dropdown
        ArrayAdapter<CharSequence> statusTypeAdapter = ArrayAdapter.createFromResource(this,
                R.array.user_statuses, android.R.layout.simple_spinner_item);
        statusTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStatus.setAdapter(statusTypeAdapter);


        // Default for frequency seekbar
        seekBarFrequency.setEnabled(false);
        // Default for status update dropdown
        spinnerStatus.setEnabled(false);
        // Default for current location
        currentCoordinates.setText(R.string.waiting_for_location);

        spinnerMessageType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                String selectedMessageType = (String) adapterView.getItemAtPosition(i);
                if(selectedMessageType.equals("Status Update") || selectedMessageType.equals("Aggiornamento stato")){
                    spinnerStatus.setEnabled(true);
                    stopLocationUpdates();
                }else{
                    spinnerStatus.setEnabled(false);
                    requestLocationUpdates();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        spinnerStatus.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                currentStatusIndex = i;
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        frequencySwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                seekBarFrequency.setEnabled(isChecked);
                frequencySet = isChecked;
            }
        });


        // Frequency Slider Logic
        seekBarFrequency.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                currentFrequency = Math.max(progress, 1); // Minimum 1 minute
                if(currentFrequency == 1){
                    textFrequency.setText(String.format("Frequency: %d minute", currentFrequency));
                }else{
                    textFrequency.setText(String.format("Frequency: %d minutes", currentFrequency));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Button Click Logic
        String finalID = ID;
        buttonSendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String messageType = spinnerMessageType.getSelectedItem().toString();
                // Check if the user selected send with frequency
                // If yes, frequency has to be at least 1
                int frequency = 0;
                String type;
                String payload;

                if(messageType.equals("Status Update") || messageType.equals("Aggiornamento stato")){
                    type = "2";
                    payload = String.format("%d", currentStatusIndex);
                }else{
                    // Coordinate sharing
                    type = "1";
                    String lat = String.format("%.4f", latitude);
                    String lon = String.format("%.4f", longitude);
                    payload = lat + "-" + lon;
                    Log.println(Log.INFO, "SIGACT", String.valueOf(latitude) +" | " + lat);
                }
                if(frequencySet){
                    if(alreadyRunningTask){
                        Toast.makeText(SignalsActivity.this, "Please wait for the previous task to finish!", Toast.LENGTH_LONG).show();
                    }else{
                        frequency = Math.max(currentFrequency,1);
                        PeriodicTask periodicTask = new PeriodicTask();
                        Runnable task = () -> sendMessage(finalID,type,payload);
                        Runnable callback = () -> {Toast.makeText(SignalsActivity.this, "Task finished!", Toast.LENGTH_SHORT).show(); alreadyRunningTask=false;};
                        alreadyRunningTask = true;
                        periodicTask.scheduleTask(task,3,frequency,callback);
                        Toast.makeText(SignalsActivity.this, "Task started: repeating 3 times for " + frequency + " minutes", Toast.LENGTH_LONG).show();
                    }
                }else{
                    sendMessage(finalID,type,payload);
                    Toast.makeText(SignalsActivity.this, "Message sent!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }


    public static String generateUniqueId() {

        // Generate a random 5-digit number (range 00000 to 99999)
        int randomNumberInt = random.nextInt(100000);

        return String.format("%d", randomNumberInt);
    }


    private void sendMessage(String id, String messageType, String payload) {

        String timestamp = Long.toString(System.currentTimeMillis());
        Message newMessage = new Message(id,timestamp,messageType,payload);
        new Thread(() -> {
            AppDatabase.getInstance(this).messageDao().insert(newMessage);
        }).start();
        Toast.makeText(this,"Sending message!",Toast.LENGTH_LONG).show();
        bluetoothManager.sendMessage(newMessage.toBluetoothMessage());

    }


    private void requestLocationUpdates() {
        // Check if GPS is enabled
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (lastKnownLocation != null) {
                    latitude = lastKnownLocation.getLatitude();
                    longitude = lastKnownLocation.getLongitude();
                    currentCoordinates.setText(String.format("%s %s", latitude, longitude)); // Display cached location
                } else {
                    currentCoordinates.setText(R.string.waiting_for_location);
                }
            }

            locationListener = new LocationListener() {
                @Override
                public void onLocationChanged(@NonNull Location location) {
                    // Get current latitude and longitude
                    latitude = location.getLatitude();
                    longitude = location.getLongitude();
                    // Update the coordinates
                    currentCoordinates.setText(latitude + " " + longitude);
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {
                    // Handle status changes (optional)
                }

                @Override
                public void onProviderEnabled(String provider) {
                    // Handle provider enabled
                }

                @Override
                public void onProviderDisabled(String provider) {
                    // Handle provider disabled
                }
            };

            // Request location updates
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // Request location permissions if not granted
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                return;
            }

            // Start receiving location updates
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        } else {
            // GPS is not enabled
            Toast.makeText(this, "Please enable GPS", Toast.LENGTH_SHORT).show();
        }
    }

    private void initializeBluetoothManager() {
        bluetoothManager = BluetoothManager.getInstance();
        bluetoothManager.setConnectionListener((BluetoothManager.BluetoothConnectionListener) this);

        // Check if Bluetooth is available and connected
        if (!bluetoothManager.isBluetoothAvailable()) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (!bluetoothManager.isConnected()) {
            Toast.makeText(this, "Not connected to any device", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void stopLocationUpdates() {
        if (locationManager != null && locationListener != null) {
            locationManager.removeUpdates(locationListener);  // Stop receiving location updates
            currentCoordinates.setText(R.string.select_coordinate_sharing);  // Clear the coordinates when stopping updates
        }
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
        stopLocationUpdates();
    }


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
