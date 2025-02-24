package com.example.iotapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class BluetoothManager {
    private static final String TAG = "BluetoothManager";
    private static final UUID DEFAULT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // Singleton instance
    private static BluetoothManager instance;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private boolean isConnected = false;

    // Listener interface for Bluetooth events
    public interface BluetoothConnectionListener {
        void onConnectionSuccess(String deviceName);
        void onConnectionFailed(String errorMessage);
        void onDisconnected();
        void onMessageReceived(String message);
    }

    private BluetoothConnectionListener connectionListener;

    // Private constructor for singleton pattern
    private BluetoothManager() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    // Singleton getInstance method
    public static synchronized BluetoothManager getInstance() {
        if (instance == null) {
            instance = new BluetoothManager();
        }
        return instance;
    }

    // Set connection listener
    public void setConnectionListener(BluetoothConnectionListener listener) {
        this.connectionListener = listener;
    }

    // Check if Bluetooth is available and enabled
    public boolean isBluetoothAvailable() {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }

    public void startDiscovery(Context context) {
        if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
            if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            bluetoothAdapter.startDiscovery();
        }
    }

    // Get list of paired devices
    public ArrayList<BluetoothDevice> getPairedDevices(Context context) {
        if (!checkBluetoothPermission(context)) {
            return new ArrayList<>();
        }

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        return new ArrayList<>(pairedDevices);
    }

    // Connect to a specific Bluetooth device
    public void connectToDevice(Context context, BluetoothDevice device) {
        new Thread(() -> {
            try {
                if (!checkBluetoothPermission(context)) {
                    return;
                }

                bluetoothSocket = device.createRfcommSocketToServiceRecord(DEFAULT_UUID);
                bluetoothSocket.connect();

                inputStream = bluetoothSocket.getInputStream();
                outputStream = bluetoothSocket.getOutputStream();
                isConnected = true;

                // Notify successful connection
                if (connectionListener != null) {
                    runOnMainThread(() -> connectionListener.onConnectionSuccess(device.getName()));
                }

                // Start listening for messages
                startMessageListener();

            } catch (Exception e) {
                Log.e(TAG, "Connection failed", e);
                if (connectionListener != null) {
                    runOnMainThread(() -> connectionListener.onConnectionFailed(e.getMessage()));
                }
            }
        }).start();
    }

    // Send a message via Bluetooth
    public void sendMessage(String message) {
        if (!isConnected || outputStream == null) {
            Log.e(TAG, "Cannot send message: Not connected");
            return;
        }

        new Thread(() -> {
            try {
                outputStream.write(message.getBytes());
                outputStream.flush();
            } catch (IOException e) {
                Log.e(TAG, "Error sending message", e);
            }
        }).start();
    }

    // Internal method to listen for incoming messages
    private void startMessageListener() {
        new Thread(() -> {
            byte[] buffer = new byte[1024];
            int bytes;

            while (isConnected) {
                try {
                    bytes = inputStream.read(buffer);
                    String message = new String(buffer, 0, bytes);

                    if (!message.isEmpty() && connectionListener != null) {
                        runOnMainThread(() -> connectionListener.onMessageReceived(message));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error reading from Bluetooth", e);
                    disconnect();
                    break;
                }
            }
        }).start();
    }

    // Disconnect from the current device
    public void disconnect() {
        new Thread(() -> {
            try {
                if (bluetoothSocket != null) {
                    bluetoothSocket.close();
                }
                isConnected = false;

                if (connectionListener != null) {
                    runOnMainThread(() -> connectionListener.onDisconnected());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error disconnecting device", e);
            }
        }).start();
    }

    // Check Bluetooth connection permission
    private boolean checkBluetoothPermission(Context context) {
        return ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT)
                == PackageManager.PERMISSION_GRANTED;
    }

    // Utility method to run code on main thread
    private void runOnMainThread(Runnable runnable) {
        new Handler(Looper.getMainLooper()).post(runnable);
    }

    // Getter for connection status
    public boolean isConnected() {
        return isConnected;
    }
}