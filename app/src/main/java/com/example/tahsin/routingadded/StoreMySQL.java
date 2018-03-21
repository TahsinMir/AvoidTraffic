package com.example.tahsin.routingadded;



import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;

import java.util.HashMap;
import java.util.Map;

public class StoreMySQL extends StringRequest {
    private static final String STORE_URL = "https://huecom.000webhostapp.com/latlng.php";

    private Map<String, String> params;

    public StoreMySQL(String lat, String lng, String dist, String x, String y, String z,
                      String time, Response.Listener<String> listener) {
        super(Method.POST, STORE_URL, listener, null);
        params = new HashMap<>();

        params.put("lat", lat);
        params.put("lng", lng);
        params.put("dist", dist);
        params.put("x", x);
        params.put("y", y);
        params.put("z", z);
        params.put("time", time);

    }


    @Override
    public Map<String, String> getParams() {
        return params;
    }


}