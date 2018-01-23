package com.example.muyeedahmed.trafficwithoutbuttons;


import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static java.lang.Math.abs;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener, SensorEventListener {

    private GoogleMap mMap;
    GoogleApiClient mGoogleApiClient;
    LocationRequest mLocationRequest;
    Location mLastLocation;
    Marker mCurrLocationMarker;
    Marker mTrafficLocationMarker;
    double[][] trafficData;
    //Device ID
    String did;
    //Online Database
    StoreMySQL mysql;
    RetrieveMySQL retmysql;
    //Local Database
    SQLDB sqlite;

    Location pL = new Location("PreviousLocation");
    String situation = "";

    //Sensors
    double x = 0, y = 0, z = 0;
    private Sensor mySensor;
    private Sensor mProximity;
    private SensorManager SM;
    double xd = 0, yd = 0, zd = 0;
    double ps = 0, psi = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        /**** Sensor Config Start****/
        // Create our Sensor Manager
        did = generateID();
        sqlite = new SQLDB(this);
        SM = (SensorManager)getSystemService(SENSOR_SERVICE);
        // Accelerometer Sensor
        mySensor = SM.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        // Proximity Sensor
        mProximity = SM.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        // Register sensor Listener
        SM.registerListener(this, mySensor, SensorManager.SENSOR_DELAY_NORMAL);
        SM.registerListener(this, mProximity, SensorManager.SENSOR_DELAY_NORMAL);
        /**** Sensor Config End****/
        pL.setLatitude(0);
        pL.setLongitude(0);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap)
    {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);

        //Initialize Google Play Services
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                buildGoogleApiClient();
                mMap.setMyLocationEnabled(true);
            }
        }
        else {
            buildGoogleApiClient();
            mMap.setMyLocationEnabled(true);
        }
    }
    protected synchronized void buildGoogleApiClient()
    {
        mGoogleApiClient = new GoogleApiClient.Builder(this).addConnectionCallbacks(this).addOnConnectionFailedListener(this).addApi(LocationServices.API).build();
        mGoogleApiClient.connect();
    }


    @Override
    public void onLocationChanged(Location location)
    {
        int to_stop_camera_zoom_in = 0;
        mLastLocation = location;
        if (mCurrLocationMarker != null) {
            mCurrLocationMarker.remove();
            to_stop_camera_zoom_in = 1;
        }

        //Place current location marker
        double lat = location.getLatitude();
        double lng = location.getLongitude();


        LatLng latLng = new LatLng(lat, lng);
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.title("Current Position");
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA));
        mCurrLocationMarker = mMap.addMarker(markerOptions);


        //double a = sMeter.getTheAmplitude();
        boolean datStored = storeData(lat, lng);


        //move map camera

        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        if(to_stop_camera_zoom_in == 0)
            mMap.animateCamera(CameraUpdateFactory.zoomTo(17));

        // START // Get data from database

        Response.Listener<String> responseListener = new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    JSONArray jsonResponse = new JSONArray(response);
                    JSONObject jo = null;

                    trafficData = new double[jsonResponse.length()][3];
                    for(int i = 0; i < jsonResponse.length(); i++) {
                        jo = jsonResponse.getJSONObject(i);
                        trafficData[i][0] = Double.parseDouble(jo.getString("LatestLat"));
                        trafficData[i][1] = Double.parseDouble(jo.getString("LatestLng"));
                        trafficData[i][2] = Double.parseDouble(jo.getString("TPI"));

                        LatLng ll = new LatLng(trafficData[i][0], trafficData[i][1]);
                        MarkerOptions markerOptionsT = new MarkerOptions();
                        markerOptionsT.position(ll);
                        markerOptionsT.title("Previous Positions");
                        if(trafficData[i][2] > 0.7)
                            markerOptionsT.icon(BitmapDescriptorFactory.fromResource(R.mipmap.red));
                        else if(trafficData[i][2] >= 0.3 && trafficData[i][2] <= 0.7)
                            markerOptionsT.icon(BitmapDescriptorFactory.fromResource(R.mipmap.yellow));
                        else
                            markerOptionsT.icon(BitmapDescriptorFactory.fromResource(R.mipmap.green));

                        mTrafficLocationMarker = mMap.addMarker(markerOptionsT);

                    }

                } catch (JSONException e) {
                    Toast.makeText(MapsActivity.this, "Cant", Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
            }
        };

        retmysql = new RetrieveMySQL(responseListener);
        RequestQueue queue = Volley.newRequestQueue(MapsActivity.this);
        queue.add(retmysql);

        // END // Get data from database

        //stop location updates
        /*
        if (mGoogleApiClient != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }
        */
    }


    /*@Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }*/

    @Override
    public void onConnected(@Nullable Bundle bundle)
    {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(10000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;

    public boolean checkLocationPermission()
    {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Asking user if explanation is needed
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

                //Prompt the user once explanation has been shown
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
            return false;
        } else {
            return true;
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // Permission was granted.
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                        if (mGoogleApiClient == null) {
                            buildGoogleApiClient();
                        }
                        mMap.setMyLocationEnabled(true);
                    }

                } else {

                    // Permission denied, Disable the functionality that depends on this permission.
                    Toast.makeText(this, "permission denied", Toast.LENGTH_LONG).show();
                }
                return;
            }

            // other 'case' lines to check for other permissions this app might request.
            //You can add here other case statements according to your requirement.
        }
    }

    public boolean storeData(double lat, double lng){
        /*
        if(isInserted == true)
            Toast.makeText(MapsActivity.this, "Data Inserted", Toast.LENGTH_LONG).show();
        */
        String currentDateTimeString = currentTime();
        double dist = calculateDistance(lat, lng);

        String pss = "" + ps/psi;

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
                    mysql = new StoreMySQL(situation, res.getString(2), res.getString(3), res.getString(1),
                            res.getString(4), res.getString(5), res.getString(6), pss, res.getString(7), did, responseListener);
                    RequestQueue queue = Volley.newRequestQueue(MapsActivity.this);
                    queue.add(mysql);
                }
                sqlite.deleteAllData();
            }
            /*
            mysql = new StoreMySQL(situation, Double.toString(lat), Double.toString(lng), Double.toString(dist),
                    Double.toStrin
                    g(xd), Double.toString(yd), Double.toString(zd), pss, currentDateTimeString, did, responseListener);
            RequestQueue queue = Volley.newRequestQueue(MapsActivity.this);
            queue.add(mysql);
            */
        }
        else{
            boolean isInserted = sqlite.insertData(Double.toString(dist), Double.toString(lat), Double.toString(lng),
                    Double.toString(xd), Double.toString(yd), Double.toString(zd), currentDateTimeString);
            /*
            if(isInserted == true)
                Toast.makeText(MapsActivity.this, "Data Inserted", Toast.LENGTH_LONG).show();
            else
                Toast.makeText(MapsActivity.this,"Data not Inserted",Toast.LENGTH_LONG).show();
            */
        }

        x = 0;
        y = 0;
        z = 0;
        xd = 0;
        yd = 0;
        zd = 0;
        ps = 0;
        psi = 0;
        return true;
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
    public String currentTime(){
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date();
        return fmt.format(date);
    }
    // To generate ID For every Phone
    private String generateID() {
        String deviceId = android.provider.Settings.Secure.getString(this.getContentResolver(),
                android.provider.Settings.Secure.ANDROID_ID);
        /*
        if ("9774d56d682e549c".equals(deviceId) || deviceId == null) {
            deviceId = ((TelephonyManager) this
                    .getSystemService(Context.TELEPHONY_SERVICE))
                    .getDeviceId();
            if (deviceId == null) {
                Random tmpRand = new Random();
                deviceId = String.valueOf(tmpRand.nextLong());
            }
        }
        */
        return deviceId;
    }
    //Accelerometer
    /*****Start*****/



    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        Sensor sensor = sensorEvent.sensor;

        if(sensor.getType() == Sensor.TYPE_PROXIMITY) {
            if(sensorEvent.values[0] >= -4 && sensorEvent.values[0] <= 4);
            else
                ps += 1;
            psi++;
        }
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
            //    Toast.makeText(MapsActivity.this, "x: "  + x + ", y: " +  y + ", z: " + z, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    public void geoLocate(View view) {
        EditText et = (EditText) findViewById(R.id.editText);
        String location = et.getText().toString();

        Geocoder gc = new Geocoder(this);
        try {
            List<Address> list = gc.getFromLocationName(location, 1);
            Address address = list.get(0);
            String locality = address.getLocality();
            Toast.makeText(this, locality, Toast.LENGTH_LONG).show();

            double slat = address.getLatitude();
            double slng = address.getLongitude();

            LatLng sll = new LatLng(slat, slng);
            mMap.moveCamera(CameraUpdateFactory.newLatLng(sll));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(15));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /*
    public boolean hasActiveInternetConnection() {

        try {
            HttpURLConnection urlc = (HttpURLConnection) (new URL("http://www.google.com").openConnection());
            urlc.setRequestProperty("User-Agent", "Test");
            urlc.setRequestProperty("Connection", "close");
            urlc.setConnectTimeout(1500);
            urlc.connect();
            return (urlc.getResponseCode() == 200);
        } catch (IOException e) {
            return false;
            //Log.e(LOG_TAG, "Error checking internet connection", e);
        }
        //return false;
    }
    */
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}
