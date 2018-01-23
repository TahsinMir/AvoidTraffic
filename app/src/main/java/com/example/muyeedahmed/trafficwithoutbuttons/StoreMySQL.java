package com.example.muyeedahmed.trafficwithoutbuttons;

import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;

import java.util.HashMap;
import java.util.Map;

public class StoreMySQL extends StringRequest {
    private static final String STORE_URL = "https://huecom.000webhostapp.com/latlng.php";

    private Map<String, String> params;

    public StoreMySQL(String situation, String lat, String lng, String dist, String x, String y, String z,
                      String pss, String time, String did, Response.Listener<String> listener) {
        super(Method.POST, STORE_URL, listener, null);
        params = new HashMap<>();


        //////params.put("deviceID", did);
        params.put("situation", situation);
        params.put("lat", lat);
        params.put("lng", lng);
        params.put("dist", dist);
        params.put("x", x);
        params.put("y", y);
        params.put("z", z);
        params.put("pss", pss);
        params.put("time", time);
        params.put("did", did);

    }


    @Override
    public Map<String, String> getParams() {
        return params;
    }


}