package com.example.locationpicker;


import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.example.locationpicker.adapters.NearbyListAdapter;
import com.example.locationpicker.model.NearbyItem;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.PlaceLikelihood;
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest;
import com.google.android.libraries.places.api.net.PlacesClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class ChooseLocation extends AppCompatActivity implements View.OnClickListener, OnMapReadyCallback, AdapterView.OnItemClickListener {
    public static final String EXTRA_LAT = "extra_lat";
    public static final String EXTRA_LONG = "extra_long";
    public static final String EXTRA_NAME = "extra_name";
    public static final String EXTRA_ADDRESS = "extra_address";
    private static String TAG = ChooseLocation.class.getSimpleName();

    private static final int REQUEST_CODE_LOCATION_PERMISSION = 500;
    private static final float MIN_DISTANCE_CHANGE_FOR_UPDATES = 0.2f;
    private static final float MIN_DISTANCE_CHANGE_FOR_NEARBY_LOCATIONS = 1f;
    private static final float GOOGLE_MAP_ZOOM_LEVEL = 15;

    private LatLng cache = new LatLng(0, 0);
    private TextView currentLocationTextView;
    private FusedLocationProviderClient fusedLocationClient;
    private GoogleMap googleMap;
    private boolean isGoogleMapReady = false;
    private LocationCallback locationCallback;
    private ListView nearbyPlacesListView;
    private Button sendLocationBtn;
    private boolean isFixAtCurrent = true;
    private LocationRequest locationRequest;

    private LocationToBeSent locationToBeSent = null;
    private LatLng currentLocation = null;
    private PlacesClient placesClient;
    private NearbyListAdapter nearbyListAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_location_with_nearby);


        // Initialize the SDK
        Places.initialize(getApplicationContext(),
                getString(R.string.google_api_key));

        // Create a new PlacesClient instance
        placesClient = Places.createClient(this);


        setResult(RESULT_CANCELED);
        setupToolbar();

        sendLocationBtn = findViewById(R.id.sendLocationBtn);
        sendLocationBtn.setOnClickListener(this);

        currentLocationTextView = findViewById(R.id.current_location_string);
        currentLocationTextView.setOnClickListener(this);

        nearbyPlacesListView = findViewById(R.id.nearbyPlacesList);
        nearbyListAdapter = new NearbyListAdapter(this, new ArrayList<>());
        nearbyPlacesListView.setAdapter(nearbyListAdapter);
        nearbyPlacesListView.setOnItemClickListener(this);


        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);


        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        mapFragment.getMapAsync(this);
        setupLocationResources();


        findViewById(R.id.google_map_marker).bringToFront();
    }

    private void setupToolbar() {
        Toolbar mToolbar = findViewById(R.id.toolbar_choose_location);
        setSupportActionBar(mToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Choose Location");
        }
    }

    private void setupLocationResources() {
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(500);
        locationRequest.setSmallestDisplacement(MIN_DISTANCE_CHANGE_FOR_UPDATES);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    Log.d(TAG, "onLocationResult: Latitude: " + location.getLatitude() + ", " + location.getLongitude());
                    if (isFixAtCurrent)
                        updateCameraViewOnMap(location.getLatitude(), location.getLongitude());
                    updateCurrentLocationText(getCurrentLocationString(location.getLatitude(), location.getLongitude()));
                    if (Math.abs(cache.latitude - location.getLatitude()) > MIN_DISTANCE_CHANGE_FOR_NEARBY_LOCATIONS
                            || Math.abs(cache.longitude - location.getLongitude()) > MIN_DISTANCE_CHANGE_FOR_NEARBY_LOCATIONS) {
                        findNearByPlaces();
                    } else
                        currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                    cache = new LatLng(location.getLatitude(), location.getLongitude());
                }
            }
        };
    }

    private void requestLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestLocationPerms();
            return;
        }
        if (googleMap != null)
            googleMap.setMyLocationEnabled(true);
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void requestLocationPerms() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE_LOCATION_PERMISSION);
    }

    private void showNearbyPlacesList(ArrayList<PlaceLikelihood> nearbyLocationsArrayList) {
        Log.d(TAG, "showNearbyPlacesList: called");

        nearbyListAdapter.clear();
        for (PlaceLikelihood placeLikelihood : nearbyLocationsArrayList) {
            nearbyListAdapter.add(new NearbyItem(placeLikelihood.getPlace().getId()
                    , placeLikelihood.getPlace().getName()
                    , placeLikelihood.getPlace().getAddress()
                    , placeLikelihood.getPlace().getLatLng()));
        }

//        ArrayList<NearbyItem> nearbyItems = new ArrayList<>();
//        for (PlaceLikelihood placeLikelihood : nearbyLocationsArrayList) {
//            nearbyItems.add(new NearbyItem(placeLikelihood.getPlace().getId()
//                    , placeLikelihood.getPlace().getName()
//                    , placeLikelihood.getPlace().getAddress()
//                    , placeLikelihood.getPlace().getLatLng()));
//        }
//        nearbyListAdapter.setData(nearbyItems);
//        ((BaseAdapter) nearbyPlacesListView.getAdapter()).notifyDataSetChanged();
    }

    private void findNearByPlaces() {
        Log.d(TAG, "findNearByPlaces: called");
        List<Place.Field> placeFields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG);
        FindCurrentPlaceRequest request = FindCurrentPlaceRequest.builder(placeFields).build();


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            placesClient.findCurrentPlace(request).addOnSuccessListener(findCurrentPlaceResponse -> {
                Log.d(TAG, "onComplete of findNearbyPlaces: called");
                ArrayList<PlaceLikelihood> placeLikelihoods;
                if (findCurrentPlaceResponse != null) {
                    placeLikelihoods = new ArrayList<>(findCurrentPlaceResponse.getPlaceLikelihoods());
                    Collections.sort(placeLikelihoods, (placeLikelihood, t1) -> Double.compare(placeLikelihood.getLikelihood(), t1.getLikelihood()));
                    Collections.reverse(placeLikelihoods);
                    Log.d(TAG, "onComplete: Nearby Places List");
                    for (PlaceLikelihood place : placeLikelihoods) {
                        Log.d(TAG, "onComplete: ================================");
//                                Log.d(TAG, "onComplete() ID:" + place.getPlace().getId());
                        Log.d(TAG, "onComplete() Name:" + place.getPlace().getName());
                        Log.d(TAG, "onComplete() Address:" + place.getPlace().getAddress());
                        Log.d(TAG, "onComplete() LatLng:" + place.getPlace().getLatLng());
                        Log.d(TAG, "onComplete: ================================");
                    }
                    showNearbyPlacesList(placeLikelihoods);

                } else {
                    Log.e(TAG, "findNearByPlaces: nearbyPlaces response is null");
                }
            }).addOnFailureListener(e -> {
                Log.e(TAG, "onFailure: not able to fetch nearby locations");
                e.printStackTrace();
            });
        }
    }

    private String getCurrentLocationString(double lat, double lng) {
        String result;
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        List<Address> addressList = null;
        try {
            addressList = geocoder.getFromLocation(lat, lng, 1);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (addressList != null)
            result = addressList.get(0).getAddressLine(0);
        else
            result = lat + ", " + lng;

        return result;
    }

    private void updateCameraViewOnMap(double latitude, double longitude) {
        if (isGoogleMapReady) {
            Log.d(TAG, "updateCameraOnMap: updating status on map");
            LatLng latlng = new LatLng(latitude, longitude);
//            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(current, GOOGLE_MAP_ZOOM_LEVEL));
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng, GOOGLE_MAP_ZOOM_LEVEL));
//            isFixAtCurrent = false;
        } else {
            Log.e(TAG, "updateCameraOnMap: Map not ready yet!!");
        }
    }

    private void updateCurrentLocationText(String s) {
        currentLocationTextView.setText(s);
    }


    @Override
    protected void onPause() {
        super.onPause();
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    @Override
    protected void onResume() {
        super.onResume();
        requestLocationUpdates();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d(TAG, "onMapReady: called");
        this.googleMap = googleMap;
        isGoogleMapReady = true;
        googleMap.getUiSettings().setAllGesturesEnabled(false);
        requestLocationUpdates();
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Log.d(TAG, "onItemClick: " + position);
        NearbyItem item = nearbyListAdapter.getItem(position);
        if (item == null) {
            Log.e(TAG, "onItemClick: item at position " + position + "is null");
            return;
        }
        setLocationResult(new LocationToBeSent(item.getName(),
                item.getAddress(),
                new LatLng(item.getLatlng().latitude, item.getLatlng().longitude)));

        view.setSelected(true);

        currentLocationTextView.setSelected(false);
    }

    private void setLocationResult(LocationToBeSent locationResult) {
        locationToBeSent = locationResult;
        updateCameraViewOnMap(locationResult.latlng.latitude, locationResult.latlng.longitude);
        sendLocationBtn.setEnabled(true);
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.current_location_string:
                setLocationResult(new LocationToBeSent("CURRENT",
                        currentLocationTextView.getText().toString(),
                        currentLocation));
                isFixAtCurrent = true;
                currentLocationTextView.setSelected(true);

                nearbyPlacesListView.clearChoices();
                nearbyPlacesListView.requestLayout();
                break;
            case R.id.sendLocationBtn:
                if (locationToBeSent != null) {
                    Intent i = new Intent();
                    i.putExtra(EXTRA_NAME, locationToBeSent.name);
                    i.putExtra(EXTRA_ADDRESS, locationToBeSent.add);
                    i.putExtra(EXTRA_LAT, locationToBeSent.latlng.latitude);
                    i.putExtra(EXTRA_LONG, locationToBeSent.latlng.longitude);
                    setResult(RESULT_OK, i);
                } else
                    Log.e(TAG, "onClick: locationString is null");
                finish();
                break;
        }
    }

    private static class LocationToBeSent {
        String name;
        String add;
        LatLng latlng;

        LocationToBeSent(String name, String add, LatLng latlng) {
            this.name = name;
            this.add = add;
            this.latlng = latlng;

        }
    }
}