package com.example.tahsin.routingadded;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;

import java.util.HashMap;
import java.util.Map;


public class RetrieveMySQL extends StringRequest {
    private static final String RETRIEVE_URL = "https://huecom.000webhostapp.com/GetJam.php";
    private Map<String, String> params;

    public RetrieveMySQL(Response.Listener<String> listener) {
        super(Request.Method.POST, RETRIEVE_URL, listener, null);
        params = new HashMap<>();
    }

    @Override
    public Map<String, String> getParams() {
        return params;
    }
}
