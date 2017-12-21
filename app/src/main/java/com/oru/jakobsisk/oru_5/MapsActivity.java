package com.oru.jakobsisk.oru_5;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import java.util.TreeMap;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    public static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    public static final int DEFAULT_ZOOM = 7;

    private GoogleMap mMap;
    private Boolean mLocationPermissionGranted;
    private Location mLastKnownLocation;
    private Marker mUserMarker = null;
    private TreeMap<String, Marker> mPlayerMarkers = new TreeMap<>();

    private SharedPreferences mSharedPref;
    private String mPlayerName;
    private MapsActivity mActivity = this;

    private ServerConn mServerConn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        initServerConn();
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

        getLocationPermission();
    }

    private void getLocationPermission() {
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
            initLocationListener();
        }
        else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        mLocationPermissionGranted = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true;
                    initLocationListener();
                }
            }
        }
    }

    public void initLocationListener() {
        if (mLocationPermissionGranted) {
            Log.d("map", "Initializing location listener.");
            // Acquire a reference to the system Location Manager
            LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

            // Define a listener that responds to location updates
            LocationListener locationListener = new LocationListener() {
                public void onLocationChanged(Location location) {
                    // Called when a new location is found by the GPS location provider.
                    Log.d("map", "New location received.");
                    Log.d("map", "  Latitude    - " + location.getLatitude());
                    Log.d("map", "  Longitude   - " + location.getLongitude());
                    mLastKnownLocation = location;

                    // Send location to server
                    syncLocation();

                    // If this is the first time location is recorded
                    if (mUserMarker == null) {
                        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                        String title = "You";
                        mUserMarker = createMarker("DEFAULT", latLng, title);
                        mUserMarker.showInfoWindow();

                        getAllPlayers();
                    }

                    LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                    // Update camera to new position
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM));
                    // Update marker to new position
                    updateMarkerLocation(latLng, mUserMarker);
                }

                public void onStatusChanged(String provider, int status, Bundle extras) {}

                public void onProviderEnabled(String provider) {}

                public void onProviderDisabled(String provider) {}
            };

            try {
                // Register the listener with the Location Manager to receive location updates
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                Log.d("map", "Location listener registered with manager.");
            }
            catch (SecurityException e) {
                Log.d("map", "Error - Failed to register listener with location manager.");
                // TODO: 2017-12-20 Error handling
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.map_menu, menu);
        Log.d("log", "Loading menu.");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.logout:
                logout();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public Marker createMarker(String status, LatLng latLng, String t) {
        float hue = BitmapDescriptorFactory.HUE_AZURE;
        String title = t;
        switch (status) {
            case "HUMAN":
                title = title + " (H)";
                hue = BitmapDescriptorFactory.HUE_GREEN;
                break;
            case "ZOMBIE":
                title = title + " (Z)";
                hue = BitmapDescriptorFactory.HUE_RED;

                break;
        }

        Marker marker = buildMarker(latLng, title, hue);

        return marker;
    }

    public Marker buildMarker(final LatLng latLng, final String title, final float hue) {
        Marker marker = mMap.addMarker(new MarkerOptions()
                        .position(latLng)
                        .icon(BitmapDescriptorFactory.defaultMarker(hue))
                        .title(title));

        return marker;
    }

    public void updateMarkerLocation(LatLng latLng, Marker marker) {
        Log.d("map", "Updating marker.");

        marker.setPosition(latLng);
    }

    public void getAllPlayers() {
        String[] params = {};
        int commandNr = mServerConn.prepCommand("getVisiblePlayers", params);
        mServerConn.sendCommand(commandNr);
    }

    public void createPlayer(final String name, final String status, String lat, String lng) {
        // Server logged in user as well as other players
        // We don't want to add the logged in user again
        if (name != mPlayerName) {
            Log.d("map", "Received player " + name);
            Log.d("log", "  Adding player to map...");
            final LatLng latLng = new LatLng(Double.parseDouble(lat), Double.parseDouble(lng));

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Marker marker = createMarker(status, latLng, name);

                    mPlayerMarkers.put(name, marker);
                }
            });
        }
    }

    // <<------ SERVER ------>> //

    public void initServerConn() {
        Log.d("log", "Connecting to server.");
        mServerConn = new ServerConn(MapsActivity.this);

        mSharedPref = getSharedPreferences("prefs", Context.MODE_PRIVATE);
        mPlayerName = mSharedPref.getString("name", "");

        // If user data is not found in shared preferences
        if (mPlayerName == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setCancelable(false);
            builder.setTitle("Login failed");
            builder.setMessage("Redirect back to login page.");
            builder.setPositiveButton("Confirm",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent;
                            intent = new Intent(mActivity, LoginActivity.class);
                            startActivity(intent);
                        }
                    });

            AlertDialog dialog = builder.create();
            dialog.show();
        }
        else {
            String playerPassword = mSharedPref.getString("password", "");

            // Login
            String[] params = { mPlayerName, playerPassword };
            int commandNr = mServerConn.prepCommand("login", params);
            mServerConn.sendCommand(commandNr);
        }
    }

    public void getStatus() {
        // Refresh marker of user
        String[] params = {};
        int commandNr = mServerConn.prepCommand("getStatus", params);
        mServerConn.sendCommand(commandNr);
    }

    public void getStatusSuccess(String s) {
        final String status = s;

        Boolean waitForMap = true;

        // Wait for a marker with the users location to be created, then proceed to modify marker with new status
        while(waitForMap) {
            if (mUserMarker != null) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        LatLng latLng = new LatLng(mLastKnownLocation.getLatitude(), mLastKnownLocation.getLongitude());
                        String title = "You";
                        mUserMarker.remove();
                        mUserMarker = createMarker(status, latLng, title);

                        mUserMarker.showInfoWindow();
                    }
                });

                waitForMap = false;
            }
        }
    }

    public void syncLocation() {
        String[] params = { String.valueOf(mLastKnownLocation.getLatitude()), String.valueOf(mLastKnownLocation.getLongitude()) };
        int commandNr = mServerConn.prepCommand("setLocation", params);
        mServerConn.sendCommand(commandNr);
    }

    public void logout() {
        int commandNr = mServerConn.prepCommand("logout", null);
        mServerConn.sendCommand(commandNr);
    }

    public void logoutSuccess() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
    }

    public void handleError(String errorMsg) {
        switch (errorMsg) {
            // Client errors
            case "client_no_origin":
                // TODO: 2017-12-18 Error handling
                break;
            case "client_socket":
                // TODO: 2017-12-18 Error handling
                break;
            //Server errors
            case "THAT-PLAYER-ALREADY-LOGGED-IN":
                // TODO: 2017-12-18 Error handling
                break;
            case "BAD-ARGUMENTS":
                // TODO: 2017-12-18 Command failed error
                break;
            case "NOT-LOGGED-IN":
                // TODO: 2017-12-18 Redirect to login page
        }
    }

    private void showError(String msg) {

    }
}
