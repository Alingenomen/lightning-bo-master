package com.example.android.lightningtest;

import android.os.CountDownTimer;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.api.Response;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private OkHttpClient client;
    private ArrayList<Lightning> lightningArraylist = new ArrayList<>();

    private final class EchoWebSocketListener extends WebSocketListener {
        private static final int NORMAL_CLOSURE_STATUS = 1000;

        String msgToSend = "{\"west\":-11,\"east\":20,\"north\":60,\"south\":40}";

        @Override
        public void onOpen(WebSocket webSocket, okhttp3.Response response) {
            webSocket.send(msgToSend);
        }

        @Override
        public void onMessage(WebSocket webSocket, String text) {
            parseJsonObject(text);
        }

        @Override
        public void onMessage(WebSocket webSocket, ByteString bytes) {
            // output("Receiving bytes : " + bytes.hex());
        }

        @Override
        public void onClosing(WebSocket webSocket, int code, String reason) {
            webSocket.close(NORMAL_CLOSURE_STATUS, null);
            // output("Closing : " + code + " / " + reason);
        }

        @Override
        public void onFailure(WebSocket webSocket, Throwable t, okhttp3.Response response) {
            // output("Error : " + t.getMessage());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    private void checkLightningAge(){
        new CountDownTimer(300000, 60000) {

            public void onTick(long millisUntilFinished) {
                checkLightningAgeAndChangeColor();
            }
            public void onFinish() {
                checkLightningAge();
            }
        }.start();
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
        client = new OkHttpClient();
        startWebSocket();

        // Add a marker in Sydney and move the camera
        LatLng illinois = new LatLng(40.8173, -88.8981);
        mMap.addMarker(new MarkerOptions().position(illinois).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(illinois));
        googleMap.animateCamera(CameraUpdateFactory.zoomTo(6.0f));

        styleTheMap();

        checkLightningAge();
    }

    /**
     * Set map style using the stylefile in
     * the resource folder raw/mapstyle.json
     */
    private void styleTheMap() {
        mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.mapstyle));
    }

    /**
     * Creates a websocket to read
     * the data from Blitzortung
     */
    private void startWebSocket() {
        // URL to place the request
        Request request = new Request.Builder().url("ws://ws.blitzortung.org:8068").build();
        // Create and initialise a listener
        // to get lightningmessages from the websocket
        EchoWebSocketListener listener = new EchoWebSocketListener();
        WebSocket ws = client.newWebSocket(request, listener);
        client.dispatcher().executorService().shutdown();
    }

    /**
     * Parse the json string, sent through
     * the websocket
     * This method gets started when receiving a message (lightning)
     */
    private void parseJsonObject(String jsonString){
        Log.d("onmessage",jsonString);

        String timeStampAsString;
        Long timeStampAsLong;
        Double strikeLat = 0.0;
        Double strikeLon = 0.0;

        // Try to parse the Json
        try {
            JSONObject jObj = new JSONObject(jsonString);
            timeStampAsLong = Long.parseLong(jObj.getString("time"))/1000000;
            Date timeStamp = new Date(timeStampAsLong);
            Log.d("timestamp as date", timeStamp.toString());
            strikeLat = jObj.getDouble("lat");
            strikeLon = jObj.getDouble("lon");

            // Create a lightning object
            Log.d("strike location: ", "latitude: " + Double.toString(strikeLat) + ", longitude: " + Double.toString(strikeLon));
            LatLng lightning = new LatLng(strikeLat,strikeLon);

            // Create and add the lightningobject to the arraylist and
            // place it on the google map
            addLightning(lightning, timeStampAsLong, strikeLat, strikeLon);

        } catch (JSONException e) {
            Log.e("Jsonparsing", "unexpected JSON exception", e);
        }

    }

    private void addLightning(final LatLng position, final Long time, final Double lat, final Double lon) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                BitmapDescriptor icon = BitmapDescriptorFactory.fromResource(R.drawable.lightning_purple);
                MarkerOptions markerOptions = new MarkerOptions().position(position).icon(icon);
                Marker lightningMarker = mMap.addMarker(markerOptions);

                Lightning lightning = new Lightning(time,lat,lon,lightningMarker);
                lightningArraylist.add(lightning);
            }
        });
    }

    private void checkLightningAgeAndChangeColor(){

        Long dateNowInMillis = System.currentTimeMillis();
        long lightningAge = 0;

        Log.d("Current tim millis: ", dateNowInMillis.toString());

        for (int i=0;i<lightningArraylist.size();i++){
            lightningAge = dateNowInMillis - lightningArraylist.get(i).getTimeStamp();
            Log.d("lightningAge: ", Long.toString(lightningAge));

            if(lightningAge >= 3600000){
                lightningArraylist.get(i).getLightningMarker().remove();
                //lightningArraylist.get(i).getLightningMarker().remove();
                Log.d("lightning age", " greater than 300000");
            }else if (lightningAge >= 3000000){
                lightningArraylist.get(i).getLightningMarker().setIcon(BitmapDescriptorFactory.fromResource(R.drawable.lightning_blue));
                //lightningArraylist.get(i).getLightningMarker().remove();
                Log.d("lightning age", " greater than 250000");
            } else if (lightningAge >= 2400000){
                lightningArraylist.get(i).getLightningMarker().setIcon(BitmapDescriptorFactory.fromResource(R.drawable.lightning_green));
                //lightningArraylist.get(i).getLightningMarker().remove();
                Log.d("lightning age", " greater than 200000");
            } else if (lightningAge >= 1800000){
                lightningArraylist.get(i).getLightningMarker().setIcon(BitmapDescriptorFactory.fromResource(R.drawable.lightning_yellow));
                //lightningArraylist.get(i).getLightningMarker().remove();
                Log.d("lightning age", " greater than 150000");
            } else if (lightningAge >= 1200000){
                lightningArraylist.get(i).getLightningMarker().setIcon(BitmapDescriptorFactory.fromResource(R.drawable.lightning_orange));
                //lightningArraylist.get(i).getLightningMarker().remove();
                Log.d("lightning age", " greater than 100000");
            } else if (lightningAge >= 600000){
                lightningArraylist.get(i).getLightningMarker().setIcon(BitmapDescriptorFactory.fromResource(R.drawable.lightning_red));
                //lightningArraylist.get(i).getLightningMarker().remove();
                Log.d("lightning age", " greater than 50000");
            }
        }
    }
}
