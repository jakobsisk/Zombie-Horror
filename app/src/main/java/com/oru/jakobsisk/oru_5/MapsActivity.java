package com.oru.jakobsisk.oru_5;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    private SharedPreferences sharedPref;
    private String playerName;
    private MapsActivity activity = this;

    private ServerConn serverConn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        Log.d("log", "Connecting to server.");
        serverConn = new ServerConn(MapsActivity.this);

        sharedPref = getSharedPreferences("prefs", Context.MODE_PRIVATE);
        playerName = sharedPref.getString("name", "");

        // If user data is found in shared preferences
        if (playerName == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setCancelable(false);
            builder.setTitle("Login failed");
            builder.setMessage("Redirect back to login page.");
            builder.setPositiveButton("Confirm",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent;
                            intent = new Intent(activity, LoginActivity.class);
                            startActivity(intent);
                        }
                    });

            AlertDialog dialog = builder.create();
            dialog.show();
        }
        else {
            String playerPassword = sharedPref.getString("password", "");

            // Login
            String[] params = {playerName, playerPassword};
            int commandNr = serverConn.prepCommand("login", params);
            serverConn.sendCommand(commandNr);
        }
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

        // Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
    }

    public void handleError(String errorMsg) {
        switch (errorMsg) {
            // Client errors
            case "client_no_origin":
                break;
            case "client_socket":

                break;
            //Server errors
            case "THAT-PLAYER-ALREADY-LOGGED-IN":

                break;
        }
    }

    private void showError(String msg) {

    }
}
