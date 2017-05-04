package buw.maps_and_polygons;

import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.gson.Gson;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.OnConnectionFailedListener,
        GoogleMap.OnMapLongClickListener, ConnectionCallbacks, View.OnClickListener {

    private final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1306;
    private final int DEFAULT_ZOOM = 15;
    private final String MARKER_LIST = "MARKER_LIST";
    private final FragmentActivity ACTIVITY = this;
    private final String START_POLYGON = "Start Polygon";
    private final String END_POLYGON = "End Polygon";
    private final double LATLNG_TO_METER_RATE = 111000000000.0;
    private final double MAX_SQUARE_METER = 1000000;
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
                objPolygonOptions = new PolygonOptions();
            } else {
                clickPolygonButton(v);
            }
        } catch (Exception ex) {
            showToast("Error: " + ex.getMessage());
            Log.e("onClick", "Exception", ex);
        }
    }

    private void clickPolygonButton(View v) {
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
                showPolygon();
            } else {
                showToast("Not enough points to form a polygon");
            }

            objPolygonOptions = null;
        }
    }

    private void showPolygon() {
        Polygon objPolyline = objGoogleMap.addPolygon(objPolygonOptions);
        objPolyline.setFillColor(0x7F00FF00);
        double dblArea = getPolygonArea();
        double dblCentroidLat = getCentroidLat(dblArea);
        double dblCentroidLng = getCentroidLng(dblArea);
        dblArea *= LATLNG_TO_METER_RATE;
        dblArea = Math.abs(Math.round(dblArea));
        boolean bolIsKM = false;

        if (dblArea >= MAX_SQUARE_METER) {
            bolIsKM = true;
            dblArea /= 1000000;
        }

        String strTitle = getCentroidTitle(dblArea, bolIsKM);
        MarkerOptions objMarker = new MarkerOptions()
                .position(new LatLng(dblCentroidLat, dblCentroidLng))
                .title(strTitle)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
        objGoogleMap.addMarker(objMarker);
    }

    private String getCentroidTitle(double dblArea, boolean bolIsKM) {
        DecimalFormatSymbols objFormatSymbols = new DecimalFormatSymbols(Locale.GERMANY);
        objFormatSymbols.setDecimalSeparator(',');
        objFormatSymbols.setGroupingSeparator('.');
        DecimalFormat decimalFormat = new DecimalFormat("#,##0.0", objFormatSymbols);
        String strTitle = "Area is: " + decimalFormat.format(dblArea) + " square ";

        if (bolIsKM) {
            strTitle += "kilo";
        }

        strTitle += "meters";
        return strTitle;
    }

    private double getPolygonArea() {
        double dblArea = 0;

        for (int i = 0; i < objPolygonOptions.getPoints().size(); ++i) {
            LatLng objCurrentPoint = objPolygonOptions.getPoints().get(i);
            LatLng objNextPoint;

            if (i == objPolygonOptions.getPoints().size() - 1) {
                objNextPoint = objPolygonOptions.getPoints().get(0);
            } else {
                objNextPoint = objPolygonOptions.getPoints().get(i + 1);
            }

            dblArea += objCurrentPoint.latitude * objNextPoint.longitude - objNextPoint.latitude * objCurrentPoint.longitude;
        }

        return dblArea / 2;
    }

    private double getCentroidLat(double dblArea) {
        double dblLat = 0;

        for (int i = 0; i < objPolygonOptions.getPoints().size(); ++i) {
            LatLng objCurrentPoint = objPolygonOptions.getPoints().get(i);
            LatLng objNextPoint;

            if (i == objPolygonOptions.getPoints().size() - 1) {
                objNextPoint = objPolygonOptions.getPoints().get(0);
            } else {
                objNextPoint = objPolygonOptions.getPoints().get(i + 1);
            }

            dblLat += (objCurrentPoint.latitude + objNextPoint.latitude) *
                    (objCurrentPoint.latitude * objNextPoint.longitude - objNextPoint.latitude * objCurrentPoint.longitude);
        }

        return dblLat / (6 * dblArea);
    }

    private double getCentroidLng(double dblArea) {
        double dblLng = 0;

        for (int i = 0; i < objPolygonOptions.getPoints().size(); ++i) {
            LatLng objCurrentPoint = objPolygonOptions.getPoints().get(i);
            LatLng objNextPoint;

            if (i == objPolygonOptions.getPoints().size() - 1) {
                objNextPoint = objPolygonOptions.getPoints().get(0);
            } else {
                objNextPoint = objPolygonOptions.getPoints().get(i + 1);
            }

            dblLng += (objCurrentPoint.longitude + objNextPoint.longitude) *
                    (objCurrentPoint.latitude * objNextPoint.longitude - objNextPoint.latitude * objCurrentPoint.longitude);
        }

        return dblLng / (6 * dblArea);
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