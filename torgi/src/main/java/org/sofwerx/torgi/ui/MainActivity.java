package org.sofwerx.torgi.ui;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.GnssMeasurement;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;

import android.support.v4.app.ActivityCompat;
import android.location.Location;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay;
import org.sofwerx.torgi.listener.GnssMeasurementListener;
import org.sofwerx.torgi.R;
import org.sofwerx.torgi.SatStatus;
import org.sofwerx.torgi.service.TorgiService;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback, GnssMeasurementListener {
    private LatLng CENTER_US = new LatLng(39.181071, -99.938295);
    private final static String TAG = "TORGIact";
    private final static String PREF_LAT = "lat";
    private final static String PREF_LNG = "lng";
    private final static int MAX_HISTORY_LENGTH = 50;
    private final static int PERM_REQUEST_CODE = 1;
    private final static SimpleDateFormat fmtTime = new SimpleDateFormat("HH:mm:ss");
    private final static DecimalFormat fmtAccuracy = new DecimalFormat("#.##");
    private TextView stat_tv;
    private TextView cur_tv;
    private TextView meas_tv;
    private boolean serviceBound = false;
    private TorgiService torgiService = null;
    private GoogleMap mMap;
    private org.osmdroid.views.MapView osmMap = null;
    private boolean mapReady = false;
    private Marker current = null;
    private org.osmdroid.views.overlay.Marker currentOSM = null;
    private Polyline historyPolyline = null;
    private org.osmdroid.views.overlay.Polyline historyPolylineOSM = null;
    private ArrayList<LatLng> history = null;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        stat_tv = findViewById(R.id.status_text);
        cur_tv = findViewById(R.id.current_text);
        meas_tv = findViewById(R.id.measurement_text);

        meas_tv.setText("Acquiring satellites...\n", TextView.BufferType.EDITABLE);
        stat_tv.setText("Acquiring satellites...\n", TextView.BufferType.EDITABLE);
        cur_tv.setText("Acquiring satellites...\n", TextView.BufferType.EDITABLE);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            meas_tv.setText("(individual sat measurements unavailable on this platform)");
            stat_tv.setText("(rcvr clock measurements unavailable on this platform)");
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        osmMapSetup();

        checkPermissions();
        startService();
    };

    private void osmMapSetup() {
        osmMap = findViewById(R.id.maposm);

        RotationGestureOverlay mRotationGestureOverlay = new RotationGestureOverlay(osmMap);
        mRotationGestureOverlay.setEnabled(true);
        osmMap.getOverlays().add(mRotationGestureOverlay);
        osmMap.setBuiltInZoomControls(false);
        osmMap.setMultiTouchControls(true); //needed for pinch zooms
        osmMap.setTilesScaledToDpi(true); //scales tiles to the current screen's DPI, helps with readability of labels
    }

    @Override
    public void onResume() {
        super.onResume();
        if (serviceBound && (torgiService != null))
            torgiService.setListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        saveLastLocation();
        if (serviceBound && (torgiService != null))
            torgiService.setListener(null);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (serviceBound && (torgiService != null))
            unbindService(mConnection);
    }

    protected ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.d(TAG,"MdxService bound to this activity");
            TorgiService.TorgiBinder binder = (TorgiService.TorgiBinder) service;
            torgiService = binder.getService();
            serviceBound = true;
            onTorgiServiceConnected();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            serviceBound = false;
        }
    };

    private void onTorgiServiceConnected() {
        torgiService.setListener(this);
    }

    private void startService() {
        if (serviceBound)
            torgiService.startGnssRecorder();
        else {
            startService(new Intent(this, TorgiService.class));
            Intent intent = new Intent(this, TorgiService.class);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        }
    }

    private void drawMarker(LatLng pos, String info) {
        if ((pos != null) && mapReady) {
            if (history == null)
                history = new ArrayList<>();
            history.add(pos);
            if (history.size() > 1) {
                if (history.size() > MAX_HISTORY_LENGTH)
                    history.remove(0);
                if (historyPolyline == null) {
                    PolylineOptions opts = new PolylineOptions();
                    for (LatLng pt:history) {
                        opts.add(pt);
                    }
                    opts.width(5);
                    opts.color(Color.YELLOW);
                    historyPolyline = mMap.addPolyline(opts);
                } else
                    historyPolyline.setPoints(history);

                if (historyPolylineOSM == null) {
                    historyPolylineOSM = new org.osmdroid.views.overlay.Polyline();
                    ArrayList<GeoPoint> list = new ArrayList<>();
                    for (LatLng pt:history) {
                        list.add(new GeoPoint(pt.latitude,pt.longitude));
                    }
                    historyPolylineOSM.setPoints(list);
                    historyPolylineOSM.setColor(Color.YELLOW);
                    osmMap.getOverlays().add(historyPolylineOSM);
                } else
                    historyPolylineOSM.addPoint(new GeoPoint(pos.latitude,pos.longitude));
            }

            if (current == null) {
                current = mMap.addMarker(new MarkerOptions()
                        .position(pos)
                        .anchor(0.5f,0.5f)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.map_icon))
                        .title("GPS"));
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, 19));
            } else
                current.setPosition(pos);
            if (currentOSM == null) {
                currentOSM = new org.osmdroid.views.overlay.Marker(osmMap);
                currentOSM.setPosition(new GeoPoint(pos.latitude,pos.longitude));
                currentOSM.setAnchor(org.osmdroid.views.overlay.Marker.ANCHOR_CENTER,org.osmdroid.views.overlay.Marker.ANCHOR_CENTER);
                currentOSM.setIcon(getResources().getDrawable(R.drawable.map_icon));
                currentOSM.setTitle("GPS");
                osmMap.getOverlays().add(currentOSM);
                if (osmMap != null) {
                    osmMap.getController().setZoom(18d);
                    osmMap.setExpectedCenter(new GeoPoint(pos.latitude, pos.longitude));
                }
            } else {
                currentOSM.setPosition(new GeoPoint(pos.latitude,pos.longitude));
                osmMap.invalidate();
            }

            current.setSnippet(info);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (requestCode == PERM_REQUEST_CODE) {
            if (checkPermissions())
                startService();
            else {
                Toast.makeText(this, "Both Location and Storage permissions are needed", Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Returns true once all required permissions are granted, otherwise returns false and requests the
     * required permission
     * @return
     */
    private boolean checkPermissions() {
        ArrayList<String> needed = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            needed.add(Manifest.permission.ACCESS_FINE_LOCATION);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            needed.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (needed.isEmpty())
            return true;
        else {
            String[] perms = new String[needed.size()];
            perms = new String[perms.length];
            for (int i=0;i<perms.length;i++) {
                perms[i] = needed.get(i);
            }
            ActivityCompat.requestPermissions(this, perms, PERM_REQUEST_CODE);
            return false;
        }
    }

    private void saveLastLocation() {
        if ((current != null) && (current.getPosition() != null)) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            SharedPreferences.Editor edit = prefs.edit();
            edit.putFloat(PREF_LAT,(float)current.getPosition().latitude);
            edit.putFloat(PREF_LNG,(float)current.getPosition().longitude);
            edit.apply();
        }
    }

    private LatLng getLastLatLng() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        float lat = prefs.getFloat(PREF_LAT,Float.NaN);
        float lng = prefs.getFloat(PREF_LNG,Float.NaN);
        if (!Float.isNaN(lat) && !Float.isNaN(lng))
            return new LatLng(lat,lng);
        else
            return null;
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.quit_torgi)
                .setMessage(R.string.quit_torgi_narrative)
                .setNegativeButton(R.string.quit_yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (serviceBound && (torgiService != null))
                            torgiService.shutdown();
                        MainActivity.this.finish();
                    }
                })
                .setPositiveButton(R.string.quit_run_in_background, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface arg0, int arg1) {
                        MainActivity.this.finish();
                    }
                }).create().show();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        mMap.setBuildingsEnabled(false);
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                mMap.setMyLocationEnabled(false);
        } else
            mMap.setMyLocationEnabled(false);
        LatLng lastLatLng = getLastLatLng();
        if (lastLatLng != null) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastLatLng, 19));
            if (osmMap != null) {
                osmMap.getController().setZoom(19d);
                osmMap.setExpectedCenter(new GeoPoint(lastLatLng.latitude, lastLatLng.longitude));
            }
        } else {
            mMap.moveCamera(CameraUpdateFactory.newLatLng(CENTER_US));
            if (osmMap != null) {
                osmMap.getController().setZoom(1d);
                osmMap.setExpectedCenter(new GeoPoint(CENTER_US.latitude, CENTER_US.longitude));
            }
        }
        mapReady = true;
        mMap.getUiSettings().setMapToolbarEnabled(false);
    }

    @Override
    public void onSatStatusUpdated(final ArrayList<SatStatus> statuses) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if ((statuses == null) || statuses.isEmpty())
                    stat_tv.setVisibility(View.GONE);
                else {
                    StringBuffer out = new StringBuffer();

                    boolean first = true;
                    for (SatStatus status:statuses) {
                        if (status.isUsedInFix()) {
                            if (first)
                                first = false;
                            else
                                out.append('\n');
                            out.append(status.constellation + status.svid);
                        }
                    }

                    stat_tv.setText(out.toString());
                    stat_tv.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    @Override
    public void onGnssMeasurementReceived(final Collection<GnssMeasurement> measurements) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if ((measurements == null) || measurements.isEmpty())
                    meas_tv.setVisibility(View.GONE);
                else {
                    StringBuffer out = new StringBuffer();
                    boolean first = true;
                    for (GnssMeasurement measurement:measurements) {
                        if (first)
                            first = false;
                        else
                            out.append('\n');
                        out.append(measurement.toString());
                    }
                    meas_tv.setText(out.toString());
                    meas_tv.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    @Override
    public void onLocationChanged(final Location loc) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                StringBuffer out = new StringBuffer();

                out.append("Lat: "+ String.valueOf(loc.getLatitude()) + "\n");
                out.append("Lon: " + String.valueOf(loc.getLongitude()) + "\n");
                out.append("Alt: " + String.valueOf(loc.getAltitude()) + "\n");
                out.append("Provider: " + String.valueOf(loc.getProvider()) + "\n");
                out.append("Time: " + fmtTime.format(loc.getTime()) + "\n");
                int sats = loc.getExtras().getInt("satellites");
                if (sats > 0)
                    out.append("FixSatCount: " + String.valueOf(sats) + "\n");
                if (loc.hasAccuracy())
                    out.append("RadialAccuracy: " + String.valueOf(loc.getAccuracy()) + "\n");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if (loc.hasVerticalAccuracy())
                        out.append("VerticalAccuracy: " + String.valueOf(loc.getVerticalAccuracyMeters()) + "\n");
                }

                String txt = out.toString() + "\n" + (serviceBound?torgiService.getGpkgFilename():"") + "       SDK v" + Build.VERSION.SDK_INT;
                cur_tv.setText(txt);
                drawMarker(new LatLng(loc.getLatitude(), loc.getLongitude()),fmtTime.format(loc.getTime())+", ±"+(loc.hasAccuracy()?fmtAccuracy.format(loc.getAccuracy()):"")+"m");
            }
        });
    }

    @Override
    public void onProviderChanged(final String provider, final boolean enabled) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                cur_tv.setText(provider+": "+(enabled?"Enabled":"Disabled"));
            }
        });
    }
}