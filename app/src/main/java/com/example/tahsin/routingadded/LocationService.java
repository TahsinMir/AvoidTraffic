package com.example.tahsin.routingadded;

/**
 * Created by Tahsin on 12/3/2017.
 */

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.Volley;

import java.text.SimpleDateFormat;
import java.util.Date;

import static java.lang.Math.abs;

/**
 * Created by Tahsin on 9/29/2017.
 */

public class LocationService extends Service implements SensorEventListener {

    private static final String TAG = "BOOMBOOMTESTGPS";
    private LocationManager mLocationManager = null;
    private static final int LOCATION_INTERVAL = 4000;
    private static final float LOCATION_DISTANCE = 0;
    private double lat,lng;

    //Online Database
    StoreMySQL mysql;
    //Local Database
    SQLDB sqlite;
    //Sensors
    double x = 0, y = 0, z = 0;
    private Sensor mySensor;
    private SensorManager SM;
    double xd = 0, yd = 0, zd = 0;
    double ps = 0, psi = 0;

    String situation = "Bus/Car";
    String did = "";
    Location pL = new Location("PreviousLocation");
    Date currentDate = null, previousDate = null;
    private class LocationListener implements android.location.LocationListener
    {
        Location mLastLocation;

        public LocationListener(String provider)
        {
            Log.e(TAG, "LocationListener " + provider);
            mLastLocation = new Location(provider);
        }

        @Override
        public void onLocationChanged(Location location)
        {

            lat = location.getLatitude();
            lng = location.getLongitude();
            boolean datStored = storeData(lat, lng);

            Log.e(TAG, "onLocationChanged: " + location);
            mLastLocation.set(location);
        }

        @Override
        public void onProviderDisabled(String provider)
        {
            Log.e(TAG, "onProviderDisabled: " + provider);
        }

        @Override
        public void onProviderEnabled(String provider)
        {
            Log.e(TAG, "onProviderEnabled: " + provider);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras)
        {
            Log.e(TAG, "onStatusChanged: " + provider);
        }
    }

    LocationListener[] mLocationListeners = new LocationListener[] {
            new LocationListener(LocationManager.GPS_PROVIDER),
            new LocationListener(LocationManager.NETWORK_PROVIDER)
    };

    @Override
    public IBinder onBind(Intent arg0)
    {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Log.e(TAG, "onStartCommand");
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @Override
    public void onCreate()
    {
        Log.e(TAG, "onCreate");
        initializeLocationManager();

        sqlite = new SQLDB(this);

        /**** Sensor Config Starts****/
        SM = (SensorManager)getSystemService(SENSOR_SERVICE);
        // Accelerometer Sensor
        mySensor = SM.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        // Register sensor Listener
        SM.registerListener(this, mySensor, SensorManager.SENSOR_DELAY_NORMAL);
        /**** Sensor Config Ends****/
        pL.setLatitude(0);
        pL.setLongitude(0);
        try {
            mLocationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE,
                    mLocationListeners[1]);
        } catch (java.lang.SecurityException ex) {
            Log.i(TAG, "fail to request location update, ignore", ex);
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "network provider does not exist, " + ex.getMessage());
        }
        try {
            mLocationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE,
                    mLocationListeners[0]);
        } catch (java.lang.SecurityException ex) {
            Log.i(TAG, "fail to request location update, ignore", ex);
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "gps provider does not exist " + ex.getMessage());
        }
    }

    @Override
    public void onDestroy()
    {
        Log.e(TAG, "onDestroy");
        super.onDestroy();
        if (mLocationManager != null) {
            for (int i = 0; i < mLocationListeners.length; i++) {
                try {
                    mLocationManager.removeUpdates(mLocationListeners[i]);
                } catch (Exception ex) {
                    Log.i(TAG, "fail to remove location listners, ignore", ex);
                }
            }
        }
    }

    private void initializeLocationManager() {
        Log.e(TAG, "initializeLocationManager");
        if (mLocationManager == null) {
            mLocationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        }
    }


    public boolean storeData(double lat, double lng){
        //Toast.makeText(this, "Data being stored", Toast.LENGTH_LONG).show();
        String currentDateTimeString = currentTime();
        double dist = 0;
        double speed = 0;

        if(previousDate == null) {
            previousDate = currentDate;
        }
        else{
            long millis = currentDate.getTime() - previousDate.getTime();
            double timeDiff = (double) (millis/1000);
            dist = calculateDistance(lat, lng);
            speed = dist/timeDiff;
            xd = xd/timeDiff;
            yd = yd/timeDiff;
            zd = zd/timeDiff;
        }

        /*
        if(isNetworkAvailable()) {
            Response.Listener<String> responseListener = new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                }
            };
            boolean isInserted = sqlite.insertData(Double.toString(dist), Double.toString(lat), Double.toString(lng),
                    Double.toString(xd), Double.toString(yd), Double.toString(zd), currentDateTimeString);
            Cursor res = sqlite.getAllData();
            if(res.getCount() != 0){
                while (res.moveToNext()) {
                    mysql = new StoreMySQL(res.getString(2), res.getString(3), res.getString(1),
                            res.getString(4), res.getString(5), res.getString(6), res.getString(7), did, responseListener);
                    RequestQueue queue = Volley.newRequestQueue(LocationService.this);
                    queue.add(mysql);
                }
                sqlite.deleteAllData();
            }

        }
        else{
            boolean isInserted = sqlite.insertData(Double.toString(dist), Double.toString(lat), Double.toString(lng),
                    Double.toString(xd), Double.toString(yd), Double.toString(zd), currentDateTimeString);
        }*/

        Response.Listener<String> responseListener = new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {}
        };
        mysql = new StoreMySQL(Double.toString(lat), Double.toString(lng), Double.toString(dist),
                Double.toString(xd), Double.toString(yd), Double.toString(zd), currentDateTimeString, responseListener);
        RequestQueue queue = Volley.newRequestQueue(LocationService.this);
        queue.add(mysql);
        x = 0;
        y = 0;
        z = 0;
        xd = 0;
        yd = 0;
        zd = 0;
        ps = 0;
        psi = 0;
        previousDate = currentDate;
        return true;
    }

    public String currentTime(){
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date();
        currentDate = date;
        return fmt.format(date);
    }
    public double calculateDistance(double lat, double lng){
        double dist = 0;

        Location nL = new Location("NewLocation");
        nL.setLatitude(lat);
        nL.setLongitude(lng);
        dist = nL.distanceTo(pL);
        if(pL.getLatitude() == 0 && pL.getLongitude() == 0){
            pL = nL;
            return 0;
        }
        pL = nL;
        return dist;
    }
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        Sensor sensor = sensorEvent.sensor;
        if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            if(x == 0 && y == 0 && z == 0){
                x = sensorEvent.values[0];
                y = sensorEvent.values[1];
                z = sensorEvent.values[2];
            }
            xd += abs(x - sensorEvent.values[0]);
            yd += abs(y - sensorEvent.values[1]);
            zd += abs(z - sensorEvent.values[2]);
            x = sensorEvent.values[0];
            y = sensorEvent.values[1];
            z = sensorEvent.values[2];
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

}
