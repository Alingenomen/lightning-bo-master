package com.example.android.lightningtest;

import com.google.android.gms.maps.model.Marker;

import java.util.Date;

/**
 * Created by DTPAdmin on 3/04/2018.
 */

public class Lightning {

    private Long timestamp;
    private Double latitude;
    private Double longitude;
    private Marker lightningMarker;

    public Lightning(Long time, Double lat, Double lon, Marker marker){
        timestamp = time;
        latitude = lat;
        longitude = lon;
        lightningMarker = marker;
    }

    public Long getTimeStamp(){
        return timestamp;
    }

    public Double getLatitude(){
        return latitude;
    }

    public Double getLongitude(){
        return longitude;
    }

    public Marker getLightningMarker(){
        return lightningMarker;
    }

}
