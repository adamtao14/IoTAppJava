package com.example.iotapp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;

import com.example.iotapp.models.Coordinates;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class CompassView extends View implements SensorEventListener {
    private Paint compassPaint;
    private Paint textPaint;
    private Paint headingTextPaint;
    private Paint markerPaint;
    private Paint pointerPaint;
    private Paint directionPaint;

    private SensorManager sensorManager;
    private float azimuth; // Current rotation in degrees
    private double currentLat; // Current user's latitude
    private double currentLon; // Current user's longitude
    private List<UserLocation> userLocations = new ArrayList<>();

    public CompassView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public CompassView(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {

        // Paint for compass circle
        compassPaint = new Paint();
        compassPaint.setColor(Color.rgb(242,228,154));
        compassPaint.setStyle(Paint.Style.FILL);
        compassPaint.setStrokeWidth(16);

        // Paint for text
        textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(40);
        textPaint.setTextAlign(Paint.Align.CENTER);

        // Paint for direction
        directionPaint = new Paint();
        directionPaint.setColor(Color.BLACK);
        directionPaint.setTextSize(70);
        directionPaint.setTextAlign(Paint.Align.CENTER);

        // Paint for heading text
        headingTextPaint = new Paint();
        headingTextPaint.setColor(Color.BLACK);
        headingTextPaint.setTextSize(100); // Larger text size
        headingTextPaint.setTypeface(Typeface.DEFAULT_BOLD); // Bold text
        headingTextPaint.setTextAlign(Paint.Align.CENTER);

        // Paint for user markers
        markerPaint = new Paint();
        markerPaint.setStyle(Paint.Style.FILL);

        // Pointer paint
        pointerPaint = new Paint();
        pointerPaint.setColor(Color.RED);
        pointerPaint.setStrokeWidth(8f);
        pointerPaint.setStyle(Paint.Style.STROKE);
        pointerPaint.setAntiAlias(true);

        // Sensor initialization
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        Sensor orientationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        if (orientationSensor != null) {
            sensorManager.registerListener(this, orientationSensor, SensorManager.SENSOR_DELAY_UI);
        }
    }

    public void setUserLocation(double latitude, double longitude) {
        this.currentLat = latitude;
        this.currentLon = longitude;
        recalculateDistances();
        invalidate();
    }

    public void updateUserLocations(List<UserLocation> locations) {
        this.userLocations = locations;
        recalculateDistances();
        invalidate();
    }

    private void recalculateDistances() {
        for (UserLocation user : userLocations) {
            float[] results = new float[2];
            android.location.Location.distanceBetween(currentLat, currentLon, user.latitude, user.longitude, results);
            user.distance = results[0]; // Distance in meters
            user.bearing = results[1]; // Bearing in degrees
        }
    }

    public double getCurrentLat() {
        return currentLat;
    }

    public double getCurrentLon() {
        return currentLon;
    }

    public void setCurrentLat(double currentLat) {
        this.currentLat = currentLat;
    }

    public void setCurrentLon(double currentLon) {
        this.currentLon = currentLon;
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        float width = getWidth();
        float height = getHeight();
        float radius = Math.min(width, height) / 2 - 50;

        // Draw the compass circle
        canvas.drawCircle(width / 2, height / 2, radius, compassPaint);

        // Draw the pointer (fixed central stick)
        float pointerLength = radius / 3 - 20; // Make the pointer smaller (1/3rd of the radius)
        float pointerX = width / 2;
        float pointerTopY = radius + 100; // Top of the pointer
        float pointerBottomY = height / 2 - pointerLength - 200; // Pointer ends at the center
        canvas.drawLine(pointerX, pointerBottomY, pointerX, pointerTopY, pointerPaint);

        // Draw the current azimuth value
        String azimuthText = String.format(Locale.ENGLISH, "%.1f°", azimuth);
        canvas.drawText(azimuthText, width / 2, 150, headingTextPaint);

        // Save the canvas state and rotate the compass circle and labels
        canvas.save();
        canvas.rotate(-azimuth, width / 2, height / 2);

        // Draw compass labels (N, E, S, W)
        canvas.drawText("N", width / 2, height / 2 - radius + 120, directionPaint);
        canvas.drawText("S", width / 2, height / 2 + radius - 100, directionPaint);
        canvas.drawText("E", width / 2 + radius - 90, height / 2 + 30, directionPaint);
        canvas.drawText("W", width / 2 - radius + 90, height / 2 + 30, directionPaint);

        // Draw ticks and degree labels
        Paint tickPaint = new Paint();
        tickPaint.setColor(Color.BLACK);
        tickPaint.setStrokeWidth(2f);
        tickPaint.setTextSize(30);
        tickPaint.setTextAlign(Paint.Align.CENTER);

        for (int angle = 0; angle < 360; angle += 10) {
            double angleRad = Math.toRadians(angle);

            // Calculate start and end points for the tick
            float startX = (float) (width / 2 + (radius - 20) * Math.sin(angleRad));
            float startY = (float) (height / 2 - (radius - 20) * Math.cos(angleRad));
            float endX = (float) (width / 2 + radius * Math.sin(angleRad));
            float endY = (float) (height / 2 - radius * Math.cos(angleRad));

            // Draw the tick
            canvas.drawLine(startX, startY, endX, endY, tickPaint);

            // Draw degree labels every 20 degrees
            if (angle % 20 == 0) {
                float labelX = (float) (width / 2 + (radius - 50) * Math.sin(angleRad));
                float labelY = (float) (height / 2 - (radius - 50) * Math.cos(angleRad));
                canvas.drawText(String.valueOf(angle), labelX, labelY, tickPaint);
            }
        }

        // Draw user markers
        for (UserLocation user : userLocations) {
            double adjustedBearing = user.bearing - azimuth; // Adjust based on compass rotation
            double angle = Math.toRadians(adjustedBearing);

            // Determine marker position
            float x = (float) (width / 2 + radius * Math.sin(angle));
            float y = (float) (height / 2 - radius * Math.cos(angle));

            // Get random color for marker
            markerPaint.setColor(user.color);
            // Draw the marker
            canvas.drawCircle(x, y, 20, markerPaint);
            canvas.drawText(user.id + " (" + Math.round(user.distance) + "m)", x, y - 30, textPaint);
        }

        // Restore canvas state
        canvas.restore();
    }



    @Override
    public void onSensorChanged(SensorEvent event) {
        azimuth = event.values[0];
        invalidate(); // Redraw compass
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    public static class UserLocation {
        public String id;
        public double latitude;
        public double longitude;
        public float distance;
        public float bearing;
        public int color;
        private Random random;
        public UserLocation(String id, double latitude, double longitude) {
            random = new Random();
            this.id = id;
            this.latitude = latitude;
            this.longitude = longitude;
            this.color = Color.rgb(random.nextInt(256), random.nextInt(256), random.nextInt(256));
        }
        public String getId() {
            return id;
        }

        public double getLatitude() {
            return latitude;
        }

        public double getLongitude() {
            return longitude;
        }

        public void setLatitude(double latitude) {
            this.latitude = latitude;
        }

        public void setLongitude(double longitude) {
            this.longitude = longitude;
        }
    }
}
