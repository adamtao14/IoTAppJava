package com.example.iotapp.models;
import java.io.Serializable;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
@Entity(tableName = "coordinates")
public class Coordinates implements Serializable {
    @PrimaryKey(autoGenerate = true) // Auto-generate unique ID for each message
    private int uid; // New field for Room's primary key
    private String id;
    private double latitude;
    private double longitude;

    public Coordinates(String id, double latitude, double longitude) {
        this.id = id;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
    public int getUid() { return uid; }
    public void setUid(int uid) { this.uid = uid; }

    @Override
    public String toString() {
        return String.valueOf(latitude) + '-' + String.valueOf(longitude);
    }
}
