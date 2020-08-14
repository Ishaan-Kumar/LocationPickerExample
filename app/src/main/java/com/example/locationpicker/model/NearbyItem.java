package com.example.locationpicker.model;

import com.google.android.gms.maps.model.LatLng;

public class NearbyItem {
    private String id;
    private String name;
    private String address;
    private LatLng latlng;

    public NearbyItem(String id, String name, String address, LatLng latlng) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.latlng = latlng;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public LatLng getLatlng() {
        return latlng;
    }

    public void setLatlng(LatLng latlng) {
        this.latlng = latlng;
    }
}
