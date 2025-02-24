package com.example.iotapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import com.example.iotapp.database.AppDatabase;
import com.example.iotapp.models.Coordinates;
import com.example.iotapp.models.Message;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class HomeActivity extends AppCompatActivity implements BluetoothManager.BluetoothConnectionListener {
    private TextView connectionStatus;
    private Button chatButton, devicesButton, sendSignalButton, sosButton;
    private static final String TAG = "BluetoothTest";
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private BluetoothSocket bluetoothSocket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private boolean isConnected = false;
    private BluetoothManager bluetoothManager;
    private static final String PREFS_NAME = "signals_prefs";
    private static final String KEY_UNIQUE_ID = "unique_id";
    private String ID;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Enable the back button
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("LoRescue");
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        ID = prefs.getString(KEY_UNIQUE_ID, null);
        if(ID == null){
            ID = SignalsActivity.generateUniqueId();
            prefs.edit().putString(KEY_UNIQUE_ID, ID).apply();
        }

        // Initialize views
        connectionStatus = findViewById(R.id.connectionStatus);
        chatButton = findViewById(R.id.chatButton);
        devicesButton = findViewById(R.id.devicesButton);
        sendSignalButton = findViewById(R.id.sendSignalButton);
        sosButton = findViewById(R.id.sosButton);
        runOnUiThread(this::disableMenu);

        // Initialize BluetoothManager
        bluetoothManager = BluetoothManager.getInstance();
        bluetoothManager.setConnectionListener((BluetoothManager.BluetoothConnectionListener) this);

        if (bluetoothManager == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!bluetoothManager.isBluetoothAvailable()) {
            Toast.makeText(this, "Please enable Bluetooth", Toast.LENGTH_SHORT).show();
        }

        // Set initial text for connectionStatus
        connectionStatus.setText(getString(R.string.click_to_connect));

        // Set up click listener for connectionStatus
        connectionStatus.setOnClickListener(v -> {
            if (!bluetoothManager.isConnected()) {
                showDeviceSelectionDialog();
            } else {
                Toast.makeText(this, "Disconnecting", Toast.LENGTH_SHORT).show();
                bluetoothManager.disconnect();
            }
        });

        // Set up listeners for other buttons
        chatButton.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, MessageListActivity.class);
            startActivity(intent);
        });
        devicesButton.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, CompassActivity.class);
            startActivity(intent);
        });
        sendSignalButton.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, SignalsActivity.class);
            startActivity(intent);
        });

        sosButton.setOnClickListener(v -> {
            if(bluetoothManager.isConnected()){
                Message sosMessage = new Message(ID,Long.toString(System.currentTimeMillis()),"2","14");
                bluetoothManager.sendMessage(sosMessage.toBluetoothMessage());
                new Thread(() -> {
                    AppDatabase.getInstance(this).messageDao().insert(sosMessage);
                }).start();
                Toast.makeText(this, "SOS triggered!", Toast.LENGTH_SHORT).show();
            }else{
                Toast.makeText(this, "No device connected", Toast.LENGTH_SHORT).show();
            }
        });
    }



    private void showDeviceSelectionDialog() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.BLUETOOTH_CONNECT},
                    1);
            return;
        }

        ArrayList<BluetoothDevice> pairedDevices = bluetoothManager.getPairedDevices(this);
        if (pairedDevices.isEmpty()) {
            Toast.makeText(this, "No paired Bluetooth devices found", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create a list of paired devices
        ArrayList<String> deviceNames = new ArrayList<>();
        ArrayList<BluetoothDevice> deviceList = new ArrayList<>();
        for (BluetoothDevice device : pairedDevices) {
            deviceNames.add(device.getName());
            deviceList.add(device);
        }

        // Show device selection dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select a Device");
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, deviceNames);
        ListView listView = new ListView(this);
        listView.setAdapter(adapter);
        builder.setView(listView);
        AlertDialog dialog = builder.create();

        listView.setOnItemClickListener((parent, view, position, id) -> {
            dialog.dismiss();
            BluetoothDevice selectedDevice = deviceList.get(position);
            bluetoothManager.connectToDevice(this, selectedDevice);
        });

        dialog.show();
    }




    // BluetoothConnectionListener implementations
    @Override
    public void onConnectionSuccess(String deviceName) {
        runOnUiThread(() -> {
            connectionStatus.setText(getString(R.string.connected_to, deviceName));
            enableMenu();
        });
    }

    @Override
    public void onConnectionFailed(String errorMessage) {
        runOnUiThread(() -> {
            Toast.makeText(this, "Connection failed: " + errorMessage, Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onDisconnected() {
        runOnUiThread(() -> {
            connectionStatus.setText(R.string.click_to_connect);
            disableMenu();
        });
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


    @Override
    protected void onDestroy() {
        super.onDestroy();
        isConnected = false;
        runOnUiThread(this::disableMenu);
        try {
            if (bluetoothSocket != null) bluetoothSocket.close();
        } catch (Exception e) {
            Log.e(TAG, "Error closing socket", e);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Bluetooth permission granted", Toast.LENGTH_SHORT).show();
                showDeviceSelectionDialog();
            } else {
                Toast.makeText(this, "Bluetooth permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
    public void disableMenu(){
        //devicesButton.setEnabled(false);
        //chatButton.setEnabled(false);
        //sendSignalButton.setEnabled(false);
        //sosButton.setEnabled(false);
    }
    public void enableMenu(){
        //devicesButton.setEnabled(true);
        chatButton.setEnabled(true);
        //sendSignalButton.setEnabled(true);
        sosButton.setEnabled(true);
    }
    @Override
    public boolean onSupportNavigateUp() {
        // Handle the back button in the action bar
        onBackPressed();
        return true;
    }
}