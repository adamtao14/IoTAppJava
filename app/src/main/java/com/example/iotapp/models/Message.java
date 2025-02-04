package com.example.iotapp.models;

import android.util.Log;

import java.io.Serializable;
import java.util.Arrays;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
@Entity(tableName = "messages")
public class Message implements Serializable {

    @PrimaryKey(autoGenerate = true) // Auto-generate unique ID for each message
    private int uid; // New field for Room's primary key

    private String payload;
    private String id;
    private String timestamp;
    private String type;


    public Message(String id, String timestamp, String type, String payload) {
        this.id = id;
        this.timestamp = timestamp;
        this.type = type;
        this.payload = payload;
    }

    // Getters and setters for all fields (including uid)
    public int getUid() { return uid; }
    public void setUid(int uid) { this.uid = uid; }

    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    // Bluetooth methods (unchanged)
    public String toBluetoothMessage() {
        return String.format("%s-%s-%s-%s", id, timestamp, type, payload);
    }

    public static Message fromBluetoothMessage(String bluetoothMessage) {
        String[] parts = bluetoothMessage.split("-");
        for(String p: parts){
            Log.println(Log.INFO,"MESSAGE", "Rec: "+p);
        }
        if (parts.length == 4) {
            return new Message(parts[0], parts[1], parts[2], parts[3]);
        }else if(parts.length == 5){
            return new Message(parts[0], parts[1], parts[2], parts[3] + "-" + parts[4]);
        }
        return null;
    }
}