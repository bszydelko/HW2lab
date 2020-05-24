package com.bs.hw2lab;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.print.PrintDocumentAdapter;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleMap.OnMapLoadedCallback,
        GoogleMap.OnMarkerClickListener,
        GoogleMap.OnMapLongClickListener,
        SensorEventListener {

    private static final int MY_PERMISSION_REQUEST_ACCESS_FINE_LOCATION = 101;
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest mLocationRequest;
    private LocationCallback locationCallback;
    Marker gpsMarker = null;



    List<Marker> markerList;
    List<com.bs.hw2lab.Point> pointList;

    private SensorManager sensorManager;
    private Sensor mSensor;
    private TextView sensorDisplay;
    private FloatingActionButton fabStartAccelerometer;
    private FloatingActionButton fabHideButtons;
    private boolean isSensorWorking;
    private Button btnClearMemory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        markerList = new ArrayList<>();
        pointList = new ArrayList<>();

        // SENSOR
        sensorDisplay = findViewById(R.id.sensorDisplay);
        fabStartAccelerometer = findViewById(R.id.fabStartAccelerometer);
        fabHideButtons = findViewById(R.id.fabHideButtons);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensor = null;

        if(sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null){
            mSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

        isSensorWorking = false;

        fabStartAccelerometer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isSensorWorking = !isSensorWorking;
                startSensor(isSensorWorking);
            }
        });

        fabHideButtons.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //stop sensor
                sensorDisplay.setVisibility(View.INVISIBLE);
                startSensor(false);

                //hide buttons
                fabStartAccelerometer.animate().translationY(120f).alpha(0f).setDuration(1000);
                fabHideButtons.animate().translationY(120f).alpha(0f).setDuration(1000);
            }
        });

        btnClearMemory = findViewById(R.id.btnClearMemory);

        btnClearMemory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMap.clear();
                markerList.clear();
                pointList.clear();

                saveToJson();
            }
        });



    }

    void startSensor(boolean isSensorWorking){
        if(mSensor != null){

            if(isSensorWorking) {
                sensorDisplay.setVisibility(View.VISIBLE);
                sensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
            }else {
                sensorDisplay.setVisibility(View.INVISIBLE);
                sensorManager.unregisterListener(this);
            }

        }
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.setOnMapLoadedCallback(this);
        mMap.setOnMarkerClickListener(this);
        mMap.setOnMapLongClickListener(this);

        //read form JSON
        restoreFromJson();
    }

    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void startLocationUpdates(){
        fusedLocationClient.requestLocationUpdates(mLocationRequest, locationCallback, null);
    }

    private void createLocationCallback(){
        locationCallback = new LocationCallback(){
            @Override
            public void onLocationResult(LocationResult locationResult){
                if(locationResult != null){
                    if(gpsMarker != null)
                        gpsMarker.remove();

                }
            }
        };
    }

    public void zoomInClick(View view) {
        mMap.moveCamera(CameraUpdateFactory.zoomIn());
    }

    public void zoomOutClick(View view) {
        mMap.moveCamera(CameraUpdateFactory.zoomOut());
    }



    @Override
    public void onMapLoaded() {
        Log.i(MapsActivity.class.getSimpleName(), "MapLoaded");
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            //request missing permissions
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSION_REQUEST_ACCESS_FINE_LOCATION);
            return;
        }

        createLocationRequest();
        createLocationCallback();
        startLocationUpdates();
    }

    private void stopLocationUpdates() {
        if(locationCallback != null)
            fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();

        if(mSensor != null)
            sensorManager.unregisterListener(this);
    }

    private BitmapDescriptor bitmapDescriptorFromVector(Context context, int vectorResId) {
        Drawable vectorDrawable = ContextCompat.getDrawable(context, vectorResId);
        vectorDrawable.setBounds(0, 0, vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight());
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        String pattern = "#.##";
        Locale l = new Locale("en", "UK");
        DecimalFormat df = (DecimalFormat) NumberFormat.getNumberInstance(l);
        df.applyPattern(pattern);

        StringBuilder sb = new StringBuilder();
        sb.append("Position: x: ").append(df.format(latLng.latitude)).append(", y: ").append(df.format(latLng.longitude));

        Marker marker = mMap.addMarker(new MarkerOptions()
                .position(new LatLng(latLng.latitude, latLng.longitude))
                .icon(bitmapDescriptorFromVector(this, R.drawable.marker))
                .alpha(0.8f)
                .title(sb.toString()));
        com.bs.hw2lab.Point point = new com.bs.hw2lab.Point(latLng.latitude, latLng.longitude);
        
        markerList.add(marker);
        pointList.add(point);
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        fabStartAccelerometer.animate().translationY(0f).alpha(1f).setDuration(1000);
        fabHideButtons.animate().translationY(0f).alpha(1f).setDuration(1000);
        return false;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        String display = String.format("Acceleration:\n X: %.4f, Y: %.4f", event.values[0], event.values[1]);
        sensorDisplay.setText(display);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private void saveToJson() {
        Gson gson = new Gson();
        String listJson = gson.toJson(pointList);
        FileOutputStream outputStream;

        try {
            outputStream = openFileOutput("points.json", MODE_PRIVATE);
            FileWriter writer = new FileWriter(outputStream.getFD());
            writer.write(listJson);
            writer.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void restoreFromJson() {
        FileInputStream inputStream;
        int DEFAULT_BUFFER_SIZE = 10000;
        Gson gson = new Gson();
        String readJson;

        try {
            inputStream = openFileInput("points.json");
            FileReader reader = new FileReader(inputStream.getFD());
            int n;
            char[] buf = new char[DEFAULT_BUFFER_SIZE];
            StringBuilder builder = new StringBuilder();
            while((n = reader.read(buf)) >= 0){
                String tmp = String.valueOf(buf);
                String substring = (n < DEFAULT_BUFFER_SIZE) ? tmp.substring(0, n) : tmp;
                builder.append(substring);
            }
            reader.close();
            readJson = builder.toString();
            Type collectionType = new TypeToken<List<com.bs.hw2lab.Point>>() { }.getType();
            List<com.bs.hw2lab.Point> o = gson.fromJson(readJson, collectionType);

            String pattern = "#.##";
            Locale l = new Locale("en", "UK");
            DecimalFormat df = (DecimalFormat) NumberFormat.getNumberInstance(l);
            df.applyPattern(pattern);


            if(o != null){
                for (com.bs.hw2lab.Point point : o) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Position: x: ").append(df.format(point.getX())).append(", y: ").append(df.format(point.getY()));
                    pointList.add(point);
                    mMap.addMarker(new MarkerOptions()
                            .position(new LatLng(point.getX(), point.getY()))
                            .icon(bitmapDescriptorFromVector(this, R.drawable.marker))
                            .alpha(0.8f)
                            .title(sb.toString()));
                }
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    @Override
    protected void onDestroy() {
        saveToJson();
        super.onDestroy();
    }
}
