package org.sofwerx.torgi.service;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.SensorEvent;
import android.location.GnssMeasurementsEvent;
import android.location.GnssStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import org.sofwerx.torgi.gnss.DataPoint;
import org.sofwerx.torgi.listener.GnssMeasurementListener;
import org.sofwerx.torgi.R;
import org.sofwerx.torgi.listener.SensorListener;

import static java.time.Instant.now;

/**
 * Torgi service handles getting information from the GPS receiver (and eventually accepting data
 * from other sensors as well) and then storing that data in the GeoPackage as well as making
 * this info available to any listening UI element.
 */
public class TorgiService extends Service {
    private final static String TAG = "TORGISvc";
    private final static int TORGI_NOTIFICATION_ID = 1;
    private final static String NOTIFICATION_CHANNEL = "torgi_report";
    public final static String ACTION_STOP = "STOP";
    public final static String PREFS_BIG_DATA = "bigdata";
    private GNSSMeasurementService gnssMeasurementService = null;
    private GeoPackageRecorder geoPackageRecorder = null;
    private SensorService sensorService = null;
    private Location currentLocation = null;

    public void setListener(GnssMeasurementListener listener) {
        this.listener = listener;
    }

    private LocationManager locMgr = null;
    private GnssMeasurementListener listener = null;

    private final IBinder mBinder = new TorgiBinder();

    public GeoPackageRecorder getGeoPackageRecorder() {
        return geoPackageRecorder;
    }

    public void start() {
        boolean permissionsPassed = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        permissionsPassed = permissionsPassed && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;

        if (permissionsPassed) {
            if (geoPackageRecorder == null) {
                geoPackageRecorder = new GeoPackageRecorder(this);
                geoPackageRecorder.start();
            }
            if (gnssMeasurementService == null) {
                gnssMeasurementService = new GNSSMeasurementService(this);
                gnssMeasurementService.start();
            }
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            if (prefs.getBoolean(PREFS_BIG_DATA,true))
                startSensorService();
            if (locMgr == null) {
                locMgr = getSystemService(LocationManager.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    locMgr.registerGnssMeasurementsCallback(measurementListener);
                    locMgr.registerGnssStatusCallback(statusListener);
                }

                locMgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, locListener);
                currentLocation = locMgr.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                setForeground();
            }
        }
    }

    public void startSensorService() {
        if (sensorService == null) {
            sensorService = new SensorService(this, sensorListener);
            Log.d(TAG,"sensor service started");
        }
    }

    public void stopSensorService() {
        if (sensorService != null) {
            sensorService.shutdown();
            sensorService = null;
            Log.d(TAG,"sensor service shutdown");
        }
    }

    private final SensorListener sensorListener = new SensorListener() {
        @Override
        public void onSensorUpdated(SensorEvent event) {
            if (geoPackageRecorder != null)
                geoPackageRecorder.onSensorUpdated(event);
        }
    };

    @RequiresApi(26)
    private final GnssStatus.Callback statusListener = new GnssStatus.Callback() {
        public void onSatelliteStatusChanged(final GnssStatus status) {
            if (geoPackageRecorder != null)
                geoPackageRecorder.onSatelliteStatusChanged(status);
            if (listener != null)
                listener.onSatStatusUpdated(status);
        }
    };

    @RequiresApi(26)
    private final GnssMeasurementsEvent.Callback measurementListener = new GnssMeasurementsEvent.Callback() {
        public void onGnssMeasurementsReceived(GnssMeasurementsEvent event) {
            if (geoPackageRecorder != null)
                geoPackageRecorder.onGnssMeasurementsReceived(event);
            if (gnssMeasurementService != null)
                gnssMeasurementService.onGnssMeasurementsReceived(currentLocation,event);
            //if (listener != null)
            //    listener.onGnssMeasurementReceived(event);
        }
    };

    private LocationListener locListener = new LocationListener() {
        public void onProviderEnabled(String provider) {
            if (listener != null)
                listener.onProviderChanged(provider,true);
        }

        public void onProviderDisabled(String provider) {
            if (listener != null)
                listener.onProviderChanged(provider,false);
        }

        public void onStatusChanged(final String provider, int status, Bundle extras) {}

        public void onLocationChanged(final Location loc) {
            currentLocation = loc;
            if (geoPackageRecorder != null)
                geoPackageRecorder.onLocationChanged(loc);
            if (listener != null)
                listener.onLocationChanged(loc);
        }
    };

    /**
     * Clean-up TORGI and then stop this service
     */
    public void shutdown() {
        stopSelf();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        start();
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public void onDestroy() {
        if (locMgr != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                locMgr.unregisterGnssMeasurementsCallback(measurementListener);
                locMgr.unregisterGnssStatusCallback(statusListener);
            }
            if (locListener != null)
                locMgr.removeUpdates(locListener);
        }
        stopSensorService();
        if (gnssMeasurementService != null)
            gnssMeasurementService.shutdown();
        if (geoPackageRecorder != null)
            geoPackageRecorder.shutdown();
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        synchronized (this) {
            if (intent != null) {
                String action = intent.getAction();
                if (ACTION_STOP.equalsIgnoreCase(action)) {
                    Log.d(TAG,"Shutting down TorgiService");
                    stopSelf();
                    return START_NOT_STICKY;
                }
            }
            return START_STICKY;
        }
    }

    public class TorgiBinder extends Binder {
        public TorgiService getService() {
            return TorgiService.this;
        }
    }

    /**
     * Running TORGI as a foreground service allows TORGI to stay active on API level 26+ devices. Depending
     * on desired collection rates, could also consider migrating to a JobScheduler
     */
    private void setForeground() {
        PendingIntent pendingIntent = null;
        try {
            Intent notificationIntent = new Intent(this, Class.forName("org.sofwerx.torgi.ui.MainActivity"));
            pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        } catch (ClassNotFoundException ignore) {
        }

        Notification.Builder builder;
        builder = new Notification.Builder(this);
        builder.setContentIntent(pendingIntent);
        builder.setSmallIcon(R.drawable.ic_notification_torgi);
        builder.setContentTitle(getResources().getString(R.string.app_name));
        builder.setTicker(getResources().getString(R.string.notification));
        builder.setContentText(getResources().getString(R.string.notification));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            builder.setChannelId(NOTIFICATION_CHANNEL);

        Intent intentStop = new Intent(this,TorgiService.class);
        intentStop.setAction(ACTION_STOP);
        PendingIntent pIntentShutdown = PendingIntent.getService(this,0,intentStop,PendingIntent.FLAG_UPDATE_CURRENT);
        builder.addAction(android.R.drawable.ic_lock_power_off, "Stop TORGI", pIntentShutdown);

        startForeground(TORGI_NOTIFICATION_ID, builder.build());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "TORGI";
            String description = "GNSS Recording data";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public void onEWDataProcessed(DataPoint dp) {
        if ((dp != null) && (listener != null))
            listener.onEWDataProcessed(dp);
    }
}
