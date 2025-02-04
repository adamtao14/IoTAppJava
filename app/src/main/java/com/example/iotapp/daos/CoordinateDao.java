package com.example.iotapp.daos;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.iotapp.models.Coordinates;

import java.util.List;

@Dao
public interface CoordinateDao {
    // Insert a new Coordinates object
    @Insert
    void insert(Coordinates coordinates);

    // Update latitude and longitude based on the id field
    @Query("UPDATE coordinates SET latitude = :latitude, longitude = :longitude WHERE id = :id")
    void updateCoordinatesById(String id, double latitude, double longitude);

    // Optional: Get Coordinates by id
    @Query("SELECT * FROM coordinates WHERE id = :id")
    Coordinates getCoordinatesById(String id);

    // Get all coordinates
    @Query("SELECT * FROM coordinates")
    List<Coordinates> getAllCoordinates();
}
