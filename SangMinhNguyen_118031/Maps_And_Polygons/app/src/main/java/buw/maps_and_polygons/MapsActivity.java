package buw.maps_and_polygons;

import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.*;
import android.support.v4.app.*;
import android.os.Bundle;
import android.support.v4.content.*;
import android.text.*;
import android.util.*;
import android.view.View;
import android.widget.*;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.*;
import com.google.android.gms.common.api.GoogleApiClient.*;
import com.google.android.gms.location.*;
import com.google.android.gms.location.places.*;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import com.google.gson.*;

import java.util.*;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.OnConnectionFailedListener,
        GoogleMap.OnMapLongClickListener, ConnectionCallbacks, View.OnClickListener {

    private final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1306;
    private final int DEFAULT_ZOOM = 15;
    private final String MARKER_LIST = "MARKER_LIST";
    private final FragmentActivity ACTIVITY = this;
    private final String START_POLYGON = "Start Polygon";
    private final String END_POLYGON = "End Polygon";
    private GoogleMap objGoogleMap;
    private boolean bolLocationPermissionGranted;
    private Location objLastKnownLocation;
    private GoogleApiClient objGoogleApiClient;
    private CameraPosition objCameraPosition;
    private List<MarkerOptions> lstMarker;
    private PolygonOptions objPolygonOptions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_maps);
            Button btnPolygon = (Button) findViewById(R.id.btnPolygon);
            btnPolygon.setText(START_POLYGON);
            btnPolygon.setBackground(ContextCompat.getDrawable(this, R.color.colorStartPolygon));
            SetMarkerList();

            objGoogleApiClient = new GoogleApiClient.Builder(this)
                    .enableAutoManage(this, this)
                    .addConnectionCallbacks(this)
                    .addApi(LocationServices.API)
                    .addApi(Places.GEO_DATA_API)
                    .addApi(Places.PLACE_DETECTION_API)
                    .build();
            objGoogleApiClient.connect();
        } catch (Exception ex) {
            showToast("Error: " + ex.getMessage());
            Log.e("onMapReady", "Exception", ex);
        }
    }

    // Toast wrapper
    private void showToast(final String strMessage) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast objToast = Toast.makeText(ACTIVITY, strMessage, Toast.LENGTH_SHORT);
                objToast.show();
            }
        });
    }

    private void SetMarkerList() {
        SharedPreferences objPreferences = getPreferences(MODE_PRIVATE);
        String strJSON = objPreferences.getString(MARKER_LIST, "");

        if (TextUtils.isEmpty(strJSON)) {
            lstMarker = new ArrayList<MarkerOptions>();
        } else {
            Gson objGson = new Gson();
            MarkerOptions[] arrMarker = objGson.fromJson(strJSON, MarkerOptions[].class);
            lstMarker = new ArrayList<>(Arrays.asList(arrMarker));
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        try {
            objGoogleMap = googleMap;
            updateLocationUI();
            getDeviceLocation();
            objGoogleMap.setOnMapLongClickListener(this);

            for (MarkerOptions objMarker : lstMarker) {
                objGoogleMap.addMarker(objMarker);
            }
        } catch (Exception ex) {
            showToast("Error: " + ex.getMessage());
            Log.e("onMapReady", "Exception", ex);
        }
    }

    private void getDeviceLocation() {
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            bolLocationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }

        if (bolLocationPermissionGranted) {
            objLastKnownLocation = LocationServices.FusedLocationApi
                    .getLastLocation(objGoogleApiClient);
        }

        if (objLastKnownLocation != null) {
            objGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(objLastKnownLocation.getLatitude(),
                            objLastKnownLocation.getLongitude()), DEFAULT_ZOOM));
        }
    }

    private void updateLocationUI() {
        if (objGoogleMap == null) {
            return;
        }

        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            bolLocationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }

        if (bolLocationPermissionGranted) {
            objGoogleMap.setMyLocationEnabled(true);
            objGoogleMap.getUiSettings().setMyLocationButtonEnabled(true);
        } else {
            objGoogleMap.setMyLocationEnabled(false);
            objGoogleMap.getUiSettings().setMyLocationButtonEnabled(false);
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        try {
            bolLocationPermissionGranted = false;

            switch (requestCode) {
                case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                    // If request is cancelled, the result arrays are empty.
                    if (grantResults.length > 0
                            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        bolLocationPermissionGranted = true;
                    }
                }
            }

            updateLocationUI();
        } catch (Exception ex) {
            showToast("Error: " + ex.getMessage());
            Log.e("onRequestPermissions", "Exception", ex);
        }
    }

    @Override
    public void onClick(View v) {
        try {
            int intID = v.getId();

            if (intID == R.id.btnClearMarkers) {
                objGoogleMap.clear();
                lstMarker = new ArrayList<MarkerOptions>();
                saveMarkers();
            } else {
                clickPolygon(v);
            }
        } catch (Exception ex) {
            showToast("Error: " + ex.getMessage());
            Log.e("onClick", "Exception", ex);
        }
    }

    private void clickPolygon(View v) {
        Button btnPolygon = (Button) v;

        // Start polygon
        if (btnPolygon.getText().equals(START_POLYGON)) {
            btnPolygon.setText(END_POLYGON);
            btnPolygon.setBackground(ContextCompat.getDrawable(this, R.color.colorEndPolygon));
            objPolygonOptions = new PolygonOptions();
        }
        // End polygon
        else {
            btnPolygon.setText(START_POLYGON);
            btnPolygon.setBackground(ContextCompat.getDrawable(this, R.color.colorStartPolygon));

            // Only draw polygon if there are at least 3 points
            if (objPolygonOptions.getPoints().size() > 2) {
                Polygon objPolyline = objGoogleMap.addPolygon(objPolygonOptions);
                objPolyline.setFillColor(0x7F00FF00);
            } else {
                showToast("Not enough points to form a polygon");
            }

            objPolygonOptions = null;
        }
    }

    @Override
    public void onMapLongClick(LatLng objLatLng) {
        try {
            Button btnPolygon = (Button) findViewById(R.id.btnPolygon);
            EditText txtMessage = (EditText) findViewById(R.id.txtMessage);
            String strMessage = txtMessage.getText().toString();
            MarkerOptions objMarker = new MarkerOptions()
                    .position(objLatLng)
                    .title(strMessage);
            objGoogleMap.addMarker(objMarker);

            if (btnPolygon.getText().equals(START_POLYGON)) {
                lstMarker.add(objMarker);
                saveMarkers();
            } else {
                objPolygonOptions.add(objLatLng);
            }
        } catch (Exception ex) {
            showToast("Error: " + ex.getMessage());
            Log.e("onMapLongClick", "Exception", ex);
        }
    }

    private void saveMarkers() {
        Gson objGson = new Gson();
        String json = objGson.toJson(lstMarker);
        SharedPreferences objPreferences = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor objEditor = objPreferences.edit();
        objEditor.putString(MARKER_LIST, json);
        objEditor.commit();
    }
}