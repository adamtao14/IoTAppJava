package com.example.iotapp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
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
    private Paint compassBorderPaint;
    private Paint textPaint;
    private Paint headingTextPaint;
    private Paint markerPaint;
    private Paint pointerPaint;
    private Paint directionPaint;

    private SensorManager sensorManager;
    private float azimuth; // Current rotation in degrees
    private float filteredAzimuth; // Smoothed azimuth value
    private static final float ALPHA = 0.1f; // Smoothing factor
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
        compassPaint.setColor(Color.rgb(176, 186, 191));
        compassPaint.setStyle(Paint.Style.FILL);
        compassPaint.setStrokeWidth(16);

        // Paint for compass border
        compassBorderPaint = new Paint();
        compassBorderPaint.setColor(Color.rgb(50, 51, 51));
        compassBorderPaint.setStyle(Paint.Style.STROKE);
        compassBorderPaint.setStrokeWidth(20);

        // Paint for text
        textPaint = new Paint();
        textPaint.setColor(Color.rgb(7, 99, 22));
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
        if (currentLat == 0 || currentLon == 0) {
            // Skip rendering until valid coordinates are set
            return;
        }
        float width = getWidth();
        float height = getHeight();
        float radius = Math.min(width, height) / 2 - 50;

        // Draw the compass circle
        canvas.drawCircle(width / 2, height / 2, radius, compassPaint);
        // Draw the border
        canvas.drawCircle(width / 2, height / 2, radius + 2, compassBorderPaint);
        // Draw the current azimuth value
        String azimuthText = String.format(Locale.ENGLISH, "%.1f°", azimuth);
        canvas.drawText(azimuthText, width / 2, 150, headingTextPaint);

        // Save the canvas state and rotate the compass circle and labels
        canvas.save();
        canvas.rotate(-azimuth, width / 2, height / 2);

        // Draw compass labels
        canvas.drawText("N", width / 2, height / 2 - radius + 120, directionPaint);
        canvas.drawText("S", width / 2, height / 2 + radius - 100, directionPaint);
        canvas.drawText("E", width / 2 + radius - 90, height / 2 + 30, directionPaint);
        canvas.drawText("W", width / 2 - radius + 90, height / 2 + 30, directionPaint);

        // Draw ticks and degree labels
        Paint tickPaint = new Paint();
        tickPaint.setColor(Color.WHITE);
        tickPaint.setStrokeWidth(10);
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
            /*
            if (angle % 20 == 0) {
                float labelX = (float) (width / 2 + (radius - 50) * Math.sin(angleRad));
                float labelY = (float) (height / 2 - (radius - 50) * Math.cos(angleRad));
                canvas.drawText(String.valueOf(angle), labelX, labelY, tickPaint);
            }

             */
        }

        // Restore canvas state before drawing user markers
        canvas.restore();

        // Draw user markers (fixed relative to their geographic location)
        for (UserLocation user : userLocations) {
            // Calculate the angle relative to the compass (true bearing - azimuth)
            double angle = Math.toRadians(user.bearing - azimuth);

            // Determine marker position
            float x = (float) (width / 2 + radius * Math.sin(angle));
            float y = (float) (height / 2 - radius * Math.cos(angle));

            // Get random color for marker
            markerPaint.setColor(user.color);
            // Draw the marker
            canvas.drawCircle(x, y, 20, markerPaint);
            canvas.drawText(user.id + " (" + Math.round(user.distance) + "m)", x, y - 30, textPaint);
        }
        drawFixedCenteredCompassPointer(canvas, width, height);

    }

    private void drawFixedCenteredCompassPointer(Canvas canvas, float width, float height) {
        // Define the size and shape of the compass pointer
        float pointerSize = 70; // Size of the pointer (width of the triangle base)
        float pointerLength = 100; // Length of the pointer (height of the triangle)

        // Create a path for the compass pointer (triangle shape)
        Path pointerPath = new Path();
        pointerPath.moveTo(width / 2, height / 2 - pointerLength / 2); // Top point
        pointerPath.lineTo(width / 2 - pointerSize / 2, height / 2 + pointerLength / 2); // Bottom-left point
        pointerPath.lineTo(width / 2 + pointerSize / 2, height / 2 + pointerLength / 2); // Bottom-right point
        pointerPath.close(); // Close the path to form a triangle

        // Draw the compass pointer
        Paint pointerPaint = new Paint();
        pointerPaint.setColor(Color.RED); // Color of the pointer
        pointerPaint.setStyle(Paint.Style.FILL); // Fill the triangle
        pointerPaint.setAntiAlias(true); // Smooth edges
        canvas.drawPath(pointerPath, pointerPaint);
    }


    // Function to make compass updates smoother
    private float lowPassFilter(float input, float output) {
        return output + ALPHA * (input - output);
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        float newAzimuth = event.values[0]; // Raw azimuth value from the sensor

        // Normalize the azimuth to the range [0, 360)
        newAzimuth = (newAzimuth + 360) % 360;

        // Handle the 0°/360° boundary
        float delta = newAzimuth - filteredAzimuth;
        if (delta > 180) {
            delta -= 360; // Handle wrap-around from 0° to 360°
        } else if (delta < -180) {
            delta += 360; // Handle wrap-around from 360° to 0°
        }

        // Apply the low-pass filter
        filteredAzimuth = lowPassFilter(filteredAzimuth + delta, filteredAzimuth);

        // Normalize the filtered azimuth to the range [0, 360)
        filteredAzimuth = (filteredAzimuth + 360) % 360;

        azimuth = filteredAzimuth; // Use the filtered value for rendering

        recalculateDistances();
        invalidate();
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
