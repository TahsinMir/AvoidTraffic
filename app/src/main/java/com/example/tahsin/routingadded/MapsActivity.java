package com.example.tahsin.routingadded;

import android.Manifest;
import android.app.ActivityManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
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
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static java.lang.Math.*;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, DirectionFinderListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener, View.OnClickListener {


    private List<Marker> originMarkers = new ArrayList<>();
    private List<Marker> destinationMarkers = new ArrayList<>();
    private List<Polyline> polylinePaths = new ArrayList<>();
    private ProgressDialog progressDialog;

    String source;
    private GoogleMap mMap;
    View mapView;
    GoogleApiClient mGoogleApiClient;
    LocationRequest mLocationRequest;
    Location mLastLocation;
    Marker mCurrLocationMarker;
    String CurrentAddressString;
    int startClusterNo = -1, endClusterNo = -1;

    //service buttons
    private EditText destinationText;
    //private Button on, off;
    private Button sb;
    double[][] trafficData;
    //Online Database
    RetrieveMySQL retmysql;
    int cameraStopper = 1;

    Location pL = new Location("PreviousLocation");

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        boolean perm = checkLocationPermission();

        /* service buttons creation
        on = (Button) findViewById(R.id.on);
        off = (Button) findViewById(R.id.off);

        on.setOnClickListener(this);
        off.setOnClickListener(this);
*/
        sb = (Button) findViewById(R.id.sb);
        sb.setOnClickListener(this);
        destinationText = (EditText) findViewById(R.id.dText);

        pL.setLatitude(0);
        pL.setLongitude(0);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        /*change position of the current location icon
        mapView = mapFragment.getView();
        */
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
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
        } else {
            buildGoogleApiClient();
            mMap.setMyLocationEnabled(true);
        }

        //repositioning the current location icon
        if (mapView != null &&
                mapView.findViewById(Integer.parseInt("1")) != null) {
            // Get the button view
            View locationButton = ((View) mapView.findViewById(Integer.parseInt("1")).getParent()).findViewById(Integer.parseInt("2"));
            // and next place it, on bottom right (as Google Maps app)
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams)
                    locationButton.getLayoutParams();
            // position on right bottom
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
            layoutParams.setMargins(0, 0, 30, 30);
        }
        //
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this).addConnectionCallbacks(this).addOnConnectionFailedListener(this).addApi(LocationServices.API).build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onLocationChanged(Location location) {
        int to_stop_camera_zoom_in = 0;
        mLastLocation = location;
        if (mCurrLocationMarker != null) {
            mCurrLocationMarker.remove();
            to_stop_camera_zoom_in = 1;
        }

        //Place current location marker
        double lat = location.getLatitude();
        double lng = location.getLongitude();

        CurrentAddressString = Double.toString(lat) + ", " + Double.toString(lng);
        LatLng latLng = new LatLng(lat, lng);
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.title("Current Position");
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA));
        mCurrLocationMarker = mMap.addMarker(markerOptions);

        //move map camera

        if(cameraStopper<3)
        {
            cameraStopper++;
            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            if (to_stop_camera_zoom_in == 0)
                mMap.animateCamera(CameraUpdateFactory.zoomTo(15));
        }

        //stop location updates
        /*
        if (mGoogleApiClient != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }
        */
        //mMap.setTrafficEnabled(true);


        Response.Listener<String> responseListener = new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    JSONArray jsonResponse = new JSONArray(response);
                    JSONObject jo = null;
                    String hour = "C" + "";

                    //Toast.makeText(MapsActivity.this, "Hue", Toast.LENGTH_LONG).show();
                    trafficData = new double[jsonResponse.length()][5];
                    for (int i = 0; i < jsonResponse.length(); i++) {
                        jo = jsonResponse.getJSONObject(i);
                        trafficData[i][0] = Double.parseDouble(jo.getString("ID"));
                        trafficData[i][1] = Double.parseDouble(jo.getString("Latitude"));
                        trafficData[i][2] = Double.parseDouble(jo.getString("Longitude"));
                        trafficData[i][3] = Double.parseDouble(jo.getString("Jam"));
                        /*
                        LatLng ll = new LatLng(trafficData[i][0], trafficData[i][1]);
                        MarkerOptions markerOptions = new MarkerOptions();
                        markerOptions.position(ll);
                        markerOptions.title("Previous Positions");
                        if(trafficData[i][3] == 2)
                            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                        else if(trafficData[i][3] == 1)
                            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
                        else if(trafficData[i][3] == 0)
                            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                        else
                            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                        mCurrLocationMarker = mMap.addMarker(markerOptions);
                        */
                        /*
                        LatLng ll = new LatLng(trafficData[i][0], trafficData[i][1]);
                        MarkerOptions markerOptions = new MarkerOptions();
                        markerOptions.position(ll);
                        markerOptions.title("Previous Positions");
                        if(trafficData[i][3] < 500)
                            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                        else if(trafficData[i][3] >= 500 && trafficData[i][3] <= 800)
                            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
                        else
                            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));

                        mCurrLocationMarker = mMap.addMarker(markerOptions);
                        */
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
    public void onConnected(@Nullable Bundle bundle) {
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

    public boolean checkLocationPermission() {
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
                    Toast.makeText(this, "permission denied", Toast.LENGTH_LONG).show();
                }
                return;
            }
        }
    }


    @Override
    public void onClick(View view) {
        /*if (view == on) {
            if (isMyServiceRunning(LocationService.class)) {
                Toast.makeText(this, "Data is already storing", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Data storing started", Toast.LENGTH_LONG).show();
                startService(new Intent(this, LocationService.class));
            }
        } else if (view == off) {
            Toast.makeText(this, "End of data storing", Toast.LENGTH_LONG).show();
            stopService(new Intent(this, LocationService.class));
        } else */if (view == sb) {
            try {

                mMap.clear();
                sendRequest();
            } catch (IOException e) {
                e.printStackTrace();
            }
            //Toast.makeText(this, "this is a test", Toast.LENGTH_LONG).show();
        }

    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void sendRequest() throws IOException {


        String origin = CurrentAddressString;
        String destination = destinationText.getText().toString();
        if (origin.isEmpty()) {
            Toast.makeText(this, "Please enter origin address!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (destination.isEmpty()) {
            Toast.makeText(this, "Please enter destination address!", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            //Toast.makeText(this, "Okay up to this", Toast.LENGTH_SHORT).show();
            new DirectionFinder(this, origin, destination).execute();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDirectionFinderStart() {
        progressDialog = ProgressDialog.show(this, "Please wait.",
                "Finding direction..!", true);

        if (originMarkers != null) {
            for (Marker marker : originMarkers) {
                marker.remove();
            }
        }

        if (destinationMarkers != null) {
            for (Marker marker : destinationMarkers) {
                marker.remove();
            }
        }

        if (polylinePaths != null) {
            for (Polyline polyline:polylinePaths ) {
                polyline.remove();
            }
        }
    }

    @Override
    public void onDirectionFinderSuccess(List<Route> routes) {

        int cText = routes.size();

        progressDialog.dismiss();

        originMarkers = new ArrayList<>();
        destinationMarkers = new ArrayList<>();
        double sourceLat=0, sourceLong=0, destinationLat=0, destinationLong=0;
        int checker = 0;

        for (Route route : routes) {

            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(route.startLocation, 16));

            //Toast.makeText(this, "points == " + route.points.size(), Toast.LENGTH_LONG).show();

            //Toast.makeText(this, "Required Time " + route.duration.text, Toast.LENGTH_LONG).show();
            //((TextView) findViewById(R.id.tvDuration)).setText(route.duration.text);
            //((TextView) findViewById(R.id.tvDistance)).setText(route.distance.text);

            originMarkers.add(mMap.addMarker(new MarkerOptions()
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                    .title(route.startAddress)
                    .position(route.startLocation)));
            destinationMarkers.add(mMap.addMarker(new MarkerOptions()
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                    .title(route.endAddress)
                    .position(route.endLocation)));

            int indexOfFirstPoint = -1;
            int indexOfSecondPoint = -1;
            int firstPointInCluster, secondPointInCluster;
            double tempDistance;
            //finding first and last point for camera focus

            String srcPoint = route.points.get(0).toString();
            //splitting the points to get their lat and long
            //firstly first point
            String[] partsT = srcPoint.split(":");

            String part1T = partsT[0];// lat/lng
            String part2T = partsT[1];// (23.3434343,90.56546)

            String[] parts3T = partsT[1].split(",");

            String latT = parts3T[0].substring(2);
            String lngT = parts3T[1].substring(0, parts3T[1].length() - 1);
            //Toast.makeText(this, lat+" "+lng, Toast.LENGTH_SHORT).show();
            sourceLat = Double.parseDouble(latT);
            sourceLong = Double.parseDouble(lngT);
            //first point done
            //now the 2nd point
            String desPoint = route.points.get(route.points.size()-1).toString();
            String[] partssT = desPoint.split(":");

            String part11T = partssT[0];// lat/lng
            String part21T = partssT[1];// (23.3434343,90.56546)

            String[] parts31T = partssT[1].split(",");

            String lat2T = parts31T[0].substring(2);
            String lng2T = parts31T[1].substring(0, parts31T[1].length() - 1);

            destinationLat = Double.parseDouble(lat2T);
            destinationLong = Double.parseDouble(lng2T);

            for (int i = 0; i < route.points.size()-1; i++)
            {
                firstPointInCluster = -1;
                secondPointInCluster = -1;
                String firstPoint = route.points.get(i).toString();
                String secondPoint = route.points.get(i+1).toString();

                //splitting the points to get their lat and long
                //firstly first point
                String[] parts = firstPoint.split(":");

                String part1 = parts[0];// lat/lng
                String part2 = parts[1];// (23.3434343,90.56546)

                String[] parts3 = parts[1].split(",");

                String lat = parts3[0].substring(2);
                String lng = parts3[1].substring(0, parts3[1].length() - 1);
                //Toast.makeText(this, lat+" "+lng, Toast.LENGTH_SHORT).show();
                double firstPointLat = Double.parseDouble(lat);
                double firstPointLong = Double.parseDouble(lng);
                //Toast.makeText(this, "lat="+firstPointLat+" long="+firstPointLong, Toast.LENGTH_SHORT).show();
                //finding closest index of first point
                double distance = 1000000;
                indexOfFirstPoint = -1;
                for (int k=0;k<30;k++)
                {
                    tempDistance = returnDistance(firstPointLat, firstPointLong, trafficData[k][1],trafficData[k][2]);
                    if (tempDistance < distance)
                    {
                        distance = tempDistance;
                        indexOfFirstPoint = k;
                    }
                }
                if(distance>2.7)
                {
                    firstPointInCluster = -1;
                }
                else
                {
                    firstPointInCluster = 1;
                }
                //find start cluster
                if(i==0 && firstPointInCluster == 1)
                {
                    startClusterNo = indexOfFirstPoint;
                }

                //first point process complete
                //now second point
                String[] partss = secondPoint.split(":");

                String part11 = partss[0];// lat/lng
                String part21 = partss[1];// (23.3434343,90.56546)

                String[] parts31 = partss[1].split(",");

                String lat2 = parts31[0].substring(2);
                String lng2 = parts31[1].substring(0, parts31[1].length() - 1);

                double secondPointLat = Double.parseDouble(lat2);
                double secondPointLong = Double.parseDouble(lng2);
                //Toast.makeText(this, "2nd lat="+secondPointLat+" long="+secondPointLong, Toast.LENGTH_SHORT).show();
                //finding closest index of first point
                distance = 1000000;
                indexOfSecondPoint = -1;
                for (int k=0;k<30;k++)
                {
                    tempDistance = returnDistance(secondPointLat, secondPointLong, trafficData[k][1],trafficData[k][2]);
                    if (tempDistance < distance)
                    {
                        distance = tempDistance;
                        indexOfSecondPoint = k;
                    }
                }
                if(distance>2.7)
                {
                    secondPointInCluster = -1;
                }
                else
                {
                    secondPointInCluster = 1;
                }
                if(i+1 == route.points.size()-1 && secondPointInCluster == 1)
                {
                    endClusterNo = indexOfSecondPoint;
                }

                if(firstPointInCluster==secondPointInCluster)
                {
                    if(firstPointInCluster==-1)
                    {
                        polylinePaths = new ArrayList<>();
                        PolylineOptions polylineOptions = new PolylineOptions().
                                geodesic(true).
                                color(Color.BLUE).
                                width(10);
                        polylineOptions.add(route.points.get(i));
                        polylineOptions.add(route.points.get(i+1));
                        polylinePaths.add(mMap.addPolyline(polylineOptions));
                    }
                    else
                    {
                        double speedRange = trafficData[indexOfFirstPoint][3];

                        if(speedRange==0)
                        {
                            polylinePaths = new ArrayList<>();
                            PolylineOptions polylineOptions = new PolylineOptions().
                                    geodesic(true).
                                    color(Color.GREEN).
                                    width(10);
                            polylineOptions.add(route.points.get(i));
                            polylineOptions.add(route.points.get(i+1));
                            polylinePaths.add(mMap.addPolyline(polylineOptions));
                        }
                        else if(speedRange==1)
                        {
                            polylinePaths = new ArrayList<>();
                            PolylineOptions polylineOptions = new PolylineOptions().
                                    geodesic(true).
                                    color(Color.YELLOW).
                                    width(10);
                            polylineOptions.add(route.points.get(i));
                            polylineOptions.add(route.points.get(i+1));
                            polylinePaths.add(mMap.addPolyline(polylineOptions));
                        }
                        else
                        {
                            polylinePaths = new ArrayList<>();
                            PolylineOptions polylineOptions = new PolylineOptions().
                                    geodesic(true).
                                    color(Color.RED).
                                    width(10);
                            polylineOptions.add(route.points.get(i));
                            polylineOptions.add(route.points.get(i+1));
                            polylinePaths.add(mMap.addPolyline(polylineOptions));
                        }
                    }
                }
                else
                {
                    double midLat = (firstPointLat + secondPointLat)/2;
                    double midLong = (firstPointLong + secondPointLong)/2;
                    if(firstPointInCluster!=-1 && secondPointInCluster!=-1)
                    {
                        double speedRange1 = trafficData[indexOfFirstPoint][3];
                        double speedRange2 = trafficData[indexOfSecondPoint][3];

                        if(speedRange1==0)
                        {
                            polylinePaths = new ArrayList<>();
                            PolylineOptions polylineOptions = new PolylineOptions().
                                    geodesic(true).
                                    color(Color.GREEN).
                                    width(10);
                            polylineOptions.add(route.points.get(i));
                            polylineOptions.add(new LatLng(midLat, midLong));
                            polylinePaths.add(mMap.addPolyline(polylineOptions));
                        }
                        else if(speedRange1==1)
                        {
                            polylinePaths = new ArrayList<>();
                            PolylineOptions polylineOptions = new PolylineOptions().
                                    geodesic(true).
                                    color(Color.YELLOW).
                                    width(10);
                            polylineOptions.add(route.points.get(i));
                            polylineOptions.add(new LatLng(midLat, midLong));
                            polylinePaths.add(mMap.addPolyline(polylineOptions));
                        }
                        else
                        {
                            polylinePaths = new ArrayList<>();
                            PolylineOptions polylineOptions = new PolylineOptions().
                                    geodesic(true).
                                    color(Color.RED).
                                    width(10);
                            polylineOptions.add(route.points.get(i));
                            polylineOptions.add(new LatLng(midLat, midLong));
                            polylinePaths.add(mMap.addPolyline(polylineOptions));
                        }

                        if(speedRange2==0)
                        {
                            polylinePaths = new ArrayList<>();
                            PolylineOptions polylineOptions = new PolylineOptions().
                                    geodesic(true).
                                    color(Color.GREEN).
                                    width(10);
                            polylineOptions.add(new LatLng(midLat, midLong));
                            polylineOptions.add(route.points.get(i+1));
                            polylinePaths.add(mMap.addPolyline(polylineOptions));
                        }
                        else if(speedRange2==1)
                        {
                            polylinePaths = new ArrayList<>();
                            PolylineOptions polylineOptions = new PolylineOptions().
                                    geodesic(true).
                                    color(Color.YELLOW).
                                    width(10);
                            polylineOptions.add(new LatLng(midLat, midLong));
                            polylineOptions.add(route.points.get(i+1));
                            polylinePaths.add(mMap.addPolyline(polylineOptions));
                        }
                        else
                        {
                            polylinePaths = new ArrayList<>();
                            PolylineOptions polylineOptions = new PolylineOptions().
                                    geodesic(true).
                                    color(Color.RED).
                                    width(10);
                            polylineOptions.add(new LatLng(midLat, midLong));
                            polylineOptions.add(route.points.get(i+1));
                            polylinePaths.add(mMap.addPolyline(polylineOptions));
                        }
                    }
                    else if(firstPointInCluster==-1)
                    {
                        //
                        //double speedRange1 = trafficData[indexOfFirstPoint][3];
                        double speedRange2 = trafficData[indexOfSecondPoint][3];



                        if(speedRange2==0)
                        {
                            polylinePaths = new ArrayList<>();
                            PolylineOptions polylineOptions = new PolylineOptions().
                                    geodesic(true).
                                    color(Color.GREEN).
                                    width(10);
                            polylineOptions.add(new LatLng(midLat, midLong));
                            polylineOptions.add(route.points.get(i+1));
                            polylinePaths.add(mMap.addPolyline(polylineOptions));
                        }
                        else if(speedRange2==1)
                        {
                            polylinePaths = new ArrayList<>();
                            PolylineOptions polylineOptions = new PolylineOptions().
                                    geodesic(true).
                                    color(Color.YELLOW).
                                    width(10);
                            polylineOptions.add(new LatLng(midLat, midLong));
                            polylineOptions.add(route.points.get(i+1));
                            polylinePaths.add(mMap.addPolyline(polylineOptions));
                        }
                        else
                        {
                            polylinePaths = new ArrayList<>();
                            PolylineOptions polylineOptions = new PolylineOptions().
                                    geodesic(true).
                                    color(Color.RED).
                                    width(10);
                            polylineOptions.add(new LatLng(midLat, midLong));
                            polylineOptions.add(route.points.get(i+1));
                            polylinePaths.add(mMap.addPolyline(polylineOptions));
                        }
                        polylinePaths = new ArrayList<>();
                        PolylineOptions polylineOptions = new PolylineOptions().
                                geodesic(true).
                                color(Color.BLUE).
                                width(10);
                        polylineOptions.add(route.points.get(i));
                        polylineOptions.add(new LatLng(midLat, midLong));
                        polylinePaths.add(mMap.addPolyline(polylineOptions));
                        //
                    }
                    else
                    {
                        //
                        double speedRange1 = trafficData[indexOfFirstPoint][3];
                        //double speedRange2 = trafficData[indexOfSecondPoint][3];

                        if(speedRange1==0)
                        {
                            polylinePaths = new ArrayList<>();
                            PolylineOptions polylineOptions = new PolylineOptions().
                                    geodesic(true).
                                    color(Color.GREEN).
                                    width(10);
                            polylineOptions.add(route.points.get(i));
                            polylineOptions.add(new LatLng(midLat, midLong));
                            polylinePaths.add(mMap.addPolyline(polylineOptions));
                        }
                        else if(speedRange1==1)
                        {
                            polylinePaths = new ArrayList<>();
                            PolylineOptions polylineOptions = new PolylineOptions().
                                    geodesic(true).
                                    color(Color.YELLOW).
                                    width(10);
                            polylineOptions.add(route.points.get(i));
                            polylineOptions.add(new LatLng(midLat, midLong));
                            polylinePaths.add(mMap.addPolyline(polylineOptions));
                        }
                        else
                        {
                            polylinePaths = new ArrayList<>();
                            PolylineOptions polylineOptions = new PolylineOptions().
                                    geodesic(true).
                                    color(Color.RED).
                                    width(10);
                            polylineOptions.add(route.points.get(i));
                            polylineOptions.add(new LatLng(midLat, midLong));
                            polylinePaths.add(mMap.addPolyline(polylineOptions));
                        }

                        polylinePaths = new ArrayList<>();
                        PolylineOptions polylineOptions = new PolylineOptions().
                                geodesic(true).
                                color(Color.BLUE).
                                width(10);
                        polylineOptions.add(new LatLng(midLat, midLong));
                        polylineOptions.add(route.points.get(i+1));
                        polylinePaths.add(mMap.addPolyline(polylineOptions));
                        //
                    }
                }

            }
            if(route.points.size()>=2)
            {
                checker = 2;
            }
        }
        if(checker==2)
        {
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            LatLng latLng_1 = new LatLng(sourceLat, sourceLong);
            LatLng latLng_2 = new LatLng(destinationLat, destinationLong);
            builder.include(latLng_1);
            builder.include(latLng_2);
            mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(
                    builder.build(), 555, 555, 0));
        }


        //Toast.makeText(this, "source C=" + startClusterNo, Toast.LENGTH_SHORT).show();
        //Toast.makeText(this, "end C=" + endClusterNo, Toast.LENGTH_SHORT).show();
    }
    public double returnDistance(double lat1, double lon1, double lat2, double lon2)
    {
        double R = 6373.0;

        lat1 = toRadians(lat1);
        lon1 = toRadians(lon1);
        lat2 = toRadians(lat2);
        lon2 = toRadians(lon2);

        double dlon = lon2 - lon1;
        double dlat = lat2 - lat1;

        double a = ((sin(dlat / 2))*(sin(dlat / 2))) + cos(lat1) * cos(lat2) * ((sin(dlon / 2))*(sin(dlon / 2)));
        double c = 2 * atan2(sqrt(a), sqrt(1 - a));

        double distance = R * c;
        return distance;
    }





}