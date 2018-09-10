package org.sofwerx.torgi.ui;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.GnssMeasurement;
import android.location.GnssMeasurementsEvent;
import android.location.GnssStatus;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
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

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay;
import org.sofwerx.torgi.gnss.Constellation;
import org.sofwerx.torgi.gnss.LatLng;
import org.sofwerx.torgi.listener.GnssMeasurementListener;
import org.sofwerx.torgi.R;
import org.sofwerx.torgi.service.TorgiService;

public class MainActivity extends FragmentActivity implements GnssMeasurementListener {
    protected static final int REQUEST_DISABLE_BATTERY_OPTIMIZATION = 401;
    private double CENTER_US_LAT = 39.181071d;
    private double CENTER_US_LNG =  -99.938295d;
    private final static String TAG = "TORGIact";
    private final static String PREF_LAT = "lat";
    private final static String PREF_LNG = "lng";
    private final static String PREF_BATTERY_OPT_IGNORE = "nvroptbat";
    private final static int MAX_HISTORY_LENGTH = 50;
    private final static int PERM_REQUEST_CODE = 1;
    private final static SimpleDateFormat fmtTime = new SimpleDateFormat("HH:mm:ss");
    private final static DecimalFormat fmtAccuracy = new DecimalFormat("#.##");
    private TextView stat_tv;
    private TextView cur_tv;
    private TextView meas_tv;
    private boolean serviceBound = false;
    private TorgiService torgiService = null;
    private org.osmdroid.views.MapView osmMap = null;
    private org.osmdroid.views.overlay.Marker currentOSM = null;
    private org.osmdroid.views.overlay.Polyline historyPolylineOSM = null;
    private ArrayList<LatLng> history = null;
    private LatLng current = null;

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

        osmMapSetup();

        checkPermissions();
        startService();
        openBatteryOptimizationDialogIfNeeded();
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
        if (torgiService != null)
            torgiService.setListener(null);
        super.onPause();
        saveLastLocation();
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
            torgiService.start();
        else {
            startService(new Intent(this, TorgiService.class));
            Intent intent = new Intent(this, TorgiService.class);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        }
    }

    private void drawMarker(LatLng pos, String info) {
        if (pos != null) {
            if (history == null)
                history = new ArrayList<>();
            history.add(pos);
            if (history.size() > 1) {
                if (history.size() > MAX_HISTORY_LENGTH)
                    history.remove(0);

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
        }
    }

    /**
     * Request battery optimization exception so that the system doesn't throttle back our app
     */
    private void openBatteryOptimizationDialogIfNeeded() {
        if (isOptimizingBattery() && isAllowAskAboutBattery()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.enable_battery_optimization);
            builder.setMessage(R.string.battery_optimizations_narrative);
            builder.setPositiveButton(R.string.battery_optimize_yes, (dialog, which) -> {
                Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                Uri uri = Uri.parse("package:" + getPackageName());
                intent.setData(uri);
                try {
                    startActivityForResult(intent, REQUEST_DISABLE_BATTERY_OPTIMIZATION);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(this, R.string.does_not_support_battery_optimization, Toast.LENGTH_SHORT).show();
                }
            });
            builder.setOnDismissListener(dialog -> setNeverAskBatteryOptimize());
            final AlertDialog dialog = builder.create();
            dialog.setCanceledOnTouchOutside(false);
            dialog.show();
        }
    }

    protected boolean isOptimizingBattery() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            final PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            return pm != null && !pm.isIgnoringBatteryOptimizations(getPackageName());
        } else
            return false;
    }

    private boolean isAllowAskAboutBattery() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        return !prefs.getBoolean(PREF_BATTERY_OPT_IGNORE,false);
    }

    private void setNeverAskBatteryOptimize() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor edit = prefs.edit();
        edit.putBoolean(PREF_BATTERY_OPT_IGNORE,true);
        edit.apply();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_DISABLE_BATTERY_OPTIMIZATION:
                setNeverAskBatteryOptimize();
                break;
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
        if (current != null) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            SharedPreferences.Editor edit = prefs.edit();
            edit.putFloat(PREF_LAT,(float)current.latitude);
            edit.putFloat(PREF_LNG,(float)current.longitude);
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
    public void onSatStatusUpdated(final GnssStatus status) {
        if (status != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    int numSats = status.getSatelliteCount();

                    if (numSats == 0)
                        stat_tv.setVisibility(View.GONE);
                    else {
                        StringBuffer out = new StringBuffer();

                        boolean first = true;
                        for (int i = 0; i < numSats; ++i) {
                            if (status.usedInFix(i)) {
                                if (first)
                                    first = false;
                                else
                                    out.append('\n');
                                out.append(Constellation.get(status.getConstellationType(i)).name() + status.getSvid(i));
                            }
                        }

                        stat_tv.setText(out.toString());
                        stat_tv.setVisibility(View.VISIBLE);
                    }
                }
            });
        }
    }

    @Override
    public void onGnssMeasurementReceived(GnssMeasurementsEvent event) {
        if (event != null) {
            Collection<GnssMeasurement> measurements = event.getMeasurements();
            final String values;
            if ((measurements == null) || measurements.isEmpty())
                values = null;
            else {
                StringBuffer out = new StringBuffer();
                boolean first = true;
                for (GnssMeasurement measurement : measurements) {
                    if (first)
                        first = false;
                    else
                        out.append('\n');
                    out.append(measurement.toString());
                }
                values = out.toString();
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (values == null)
                        meas_tv.setVisibility(View.GONE);
                    else {
                        meas_tv.setText(values);
                        meas_tv.setVisibility(View.VISIBLE);
                    }
                }
            });
        }
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

                String txt = out.toString() + "\n" + (serviceBound?torgiService.getGeoPackageRecorder().getGpkgFilename():"") + "       SDK v" + Build.VERSION.SDK_INT;
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