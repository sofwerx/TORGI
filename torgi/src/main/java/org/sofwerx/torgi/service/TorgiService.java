package org.sofwerx.torgi.service;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
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
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import android.util.Log;

import org.sofwerx.torgi.Config;
import org.sofwerx.torgi.gnss.DataPoint;
import org.sofwerx.torgi.gnss.EWIndicators;
import org.sofwerx.torgi.gnss.LatLng;
import org.sofwerx.torgi.gnss.Satellite;
import org.sofwerx.torgi.gnss.helper.GeoPackageGPSPtHelper;
import org.sofwerx.torgi.gnss.helper.GeoPackageSatDataHelper;
import org.sofwerx.torgi.listener.GeoPackageRetrievalListener;
import org.sofwerx.torgi.listener.GnssMeasurementListener;
import org.sofwerx.torgi.R;
import org.sofwerx.torgi.listener.SensorListener;
import org.sofwerx.torgi.ogc.AbstractSOSBroadcastTransceiver;
import org.sofwerx.torgi.ogc.LiteSOSClient;
import org.sofwerx.torgi.ogc.LiteWebServer;
import org.sofwerx.torgi.ogc.SOSHelper;
import org.sofwerx.torgi.ogc.TorgiSOSBroadcastTransceiver;
import org.sofwerx.torgi.ogc.sos.AbstractOperation;
import org.sofwerx.torgi.ogc.sos.DescribeSensor;
import org.sofwerx.torgi.ogc.sos.GetCapabilities;
import org.sofwerx.torgi.ogc.sos.GetObservations;
import org.sofwerx.torgi.ui.Heatmap;

import java.io.File;
import java.util.ArrayList;

import static org.sofwerx.torgi.service.TorgiService.InputSourceType.LOCAL;
import static org.sofwerx.torgi.service.TorgiService.InputSourceType.LOCAL_FILE;
import static org.sofwerx.torgi.service.TorgiService.InputSourceType.NETWORK;

/**
 * Torgi service handles getting information from the GPS receiver (and eventually accepting data
 * from other sensors as well) and then storing that data in the GeoPackage as well as making
 * this info available to any listening UI element.
 */
public class TorgiService extends Service {
    private final static long MAX_SOS_RESPONSE_BLOCK = 1000l * 60l * 15l; //the maximum size (in milliseconds) of a block of time that the SOS service should return
    public enum InputSourceType {LOCAL,NETWORK,LOCAL_FILE};
    private final static String TAG = "TORGISvc";
    private final static int TORGI_NOTIFICATION_ID = 1;
    private final static int TORGI_WEB_SERVER_NOTIFICATION_ID = 2;
    private final static String NOTIFICATION_CHANNEL = "torgi_report";
    public final static String ACTION_STOP = "STOP";
    public final static String PREFS_BIG_DATA = "bigdata";
    public final static String PREFS_CURRENT_INPUT_MODE = "inputmode";
    private GNSSMeasurementService gnssMeasurementService = null;
    private GeoPackageRecorder geoPackageRecorder = null;
    private SensorService sensorService = null;
    private Location currentLocation = null;
    private ArrayList<LatLng> history = null;
    public final static int MAX_HISTORY_LENGTH = 50;
    private TorgiSOSBroadcastTransceiver sosBroadcastTransceiver = null;
    private LiteSOSClient sosClient = null;
    private LiteWebServer sosServer = null;
    private InputSourceType inputSourceType = LOCAL;
    private boolean didRemoteConnectionNotification = false;

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
        start(inputSourceType);
    }

    public void start(InputSourceType inputSourceType) {
        if (this.inputSourceType != inputSourceType) {
            this.inputSourceType = inputSourceType;
            Log.d(TAG, "TORGI mode changed to " + inputSourceType.name());
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            prefs.edit().putInt(PREFS_CURRENT_INPUT_MODE, inputSourceType.ordinal()).commit();
        }
        boolean permissionsPassed = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        permissionsPassed = permissionsPassed && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;

        if (permissionsPassed) {
            history = null;
            currentLocation = null;
            Satellite.clear();
            if (locMgr == null) {
                locMgr = getSystemService(LocationManager.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    locMgr.registerGnssMeasurementsCallback(measurementListener);
                    locMgr.registerGnssStatusCallback(statusListener);
                }

                locMgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, locListener);
                if (inputSourceType == LOCAL)
                    currentLocation = locMgr.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            } else {
                if (inputSourceType == LOCAL) {
                    currentLocation = locMgr.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if (listener != null)
                        listener.onLocationChanged(currentLocation);
                }
            }
            if (inputSourceType == LOCAL_FILE) {
                if (geoPackageRecorder != null)
                    geoPackageRecorder.shutdown();
            } else {
                if (geoPackageRecorder == null) {
                    geoPackageRecorder = new GeoPackageRecorder(this);
                    geoPackageRecorder.start();
                }
            }
            if (gnssMeasurementService == null) {
                gnssMeasurementService = new GNSSMeasurementService(this);
                gnssMeasurementService.start();
            } else
                gnssMeasurementService.clear();
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            if (inputSourceType == LOCAL) {
                if (prefs.getBoolean(PREFS_BIG_DATA, true))
                    startSensorService();
                if (sosServer == null)
                    sosServer = new LiteWebServer(this);
                if (sosClient != null) {
                    sosClient.shutdown();
                    sosClient = null;
                }
                setForeground();
            } else {
                if (sosServer != null) {
                    sosServer.stop();
                    sosServer = null;
                    NotificationManager notificationManager = getSystemService(NotificationManager.class);
                    notificationManager.cancel(TORGI_WEB_SERVER_NOTIFICATION_ID);
                }
                unregisterLocalSensors();
                if (inputSourceType == NETWORK) {
                    if (sosClient == null)
                        sosClient = new LiteSOSClient(this);
                    String serverIP = prefs.getString(Config.PREFS_LAST_SOS_SERVER_IP,null);
                    //TODO if (serverIP != null) {
                    //    sosClient.setIP(serverIP);
                        sosClient.start();
                    //}
                    setForeground();
                }
            }
            Heatmap.clear();
        }
    }

    public LiteSOSClient getSosClient() {
        return sosClient;
    }

    public InputSourceType getInputType() {
        return inputSourceType;
    }

    public void startSensorService() {
        if (sensorService == null) {
            sensorService = new SensorService(this, sensorListener);
            Log.d(TAG, "sensor service started");
        }
    }

    public void stopSensorService() {
        if (sensorService != null) {
            sensorService.shutdown();
            sensorService = null;
            Log.d(TAG, "sensor service shutdown");
        }
    }

    private final SensorListener sensorListener = event -> {
        if (geoPackageRecorder != null)
            geoPackageRecorder.onSensorUpdated(event);
    };

    private final GnssStatus.Callback statusListener = new GnssStatus.Callback() {
        public void onSatelliteStatusChanged(final GnssStatus status) {
            if (inputSourceType == LOCAL) {
                if (geoPackageRecorder != null)
                    geoPackageRecorder.onSatelliteStatusChanged(status);
                if (listener != null)
                    listener.onSatStatusUpdated(status);
            }
        }
    };

    private final GnssMeasurementsEvent.Callback measurementListener = new GnssMeasurementsEvent.Callback() {
        public void onGnssMeasurementsReceived(GnssMeasurementsEvent event) {
            if (inputSourceType == LOCAL) {
                if (geoPackageRecorder != null)
                    geoPackageRecorder.onGnssMeasurementsReceived(event);
                if (gnssMeasurementService != null)
                    gnssMeasurementService.onGnssMeasurementsReceived(currentLocation, event);
            }
        }
    };

    /**
     * When GeoPackageSatDataHelper data is received from an external source
     * @param data
     */
    public void onGeoPackageSatDataHelperReceived(ArrayList<GeoPackageSatDataHelper> data) {
        if (geoPackageRecorder != null)
            geoPackageRecorder.onGeoPackageSatDataHelperReceived(data);
        if (gnssMeasurementService != null)
            gnssMeasurementService.onGeoPackageSatDataHelperReceived(currentLocation,data);
        //TODO also contribute to the gnssMe
    }

    public ArrayList<LatLng> getLocationHistory() {
        return history;
    }

    public void retrieveHistoryFromGeoPackage() {
        if (geoPackageRecorder != null) {
            GeoPackageRetrievalListener listener = new GeoPackageRetrievalListener() {
                @Override
                public void onGnssSatDataRetrieved(ArrayList<GeoPackageSatDataHelper> measurements) {
                    Log.d(TAG, "onGnssSatDataRetrieved()");
                }

                @Override
                public void onGnssGeoPtRetrieved(ArrayList<GeoPackageGPSPtHelper> measurements) {
                    Log.d(TAG, "onGnssSatDataRetrieved()");
                }
            };
            geoPackageRecorder.getGnssMeasurements(System.currentTimeMillis() - 1000l * 10l, System.currentTimeMillis(), listener);
            geoPackageRecorder.getGPSObservationPoints(System.currentTimeMillis() - 1000l * 10l, System.currentTimeMillis(), listener);
        }
    }

    public void updateLocation(final Location loc) {
        currentLocation = loc;
        if (history == null)
            history = new ArrayList<>();
        history.add(new LatLng(loc.getLatitude(), loc.getLongitude()));
        if (history.size() > 1) {
            if (history.size() > MAX_HISTORY_LENGTH)
                history.remove(0);
        }
        if (geoPackageRecorder != null)
            geoPackageRecorder.onLocationChanged(loc);
        if (listener != null)
            listener.onLocationChanged(loc);

    }

    private LocationListener locListener = new LocationListener() {
        public void onProviderEnabled(String provider) {
            if ((listener != null) && (inputSourceType == LOCAL))
                listener.onProviderChanged(provider, true);
        }

        public void onProviderDisabled(String provider) {
            if ((listener != null) && (inputSourceType == LOCAL))
                listener.onProviderChanged(provider, false);
        }

        public void onStatusChanged(final String provider, int status, Bundle extras) {}

        public void onLocationChanged(final Location loc) {
            if (inputSourceType == LOCAL)
                updateLocation(loc);
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
        start(inputSourceType);
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        int inputModeIndex = prefs.getInt(PREFS_CURRENT_INPUT_MODE,0);
        InputSourceType[] sources = InputSourceType.values();
        if ((inputModeIndex >= 0) && (inputModeIndex < sources.length))
            inputSourceType = sources[inputModeIndex];
        sosBroadcastTransceiver = new TorgiSOSBroadcastTransceiver(this);
        IntentFilter intentFilter = new IntentFilter(AbstractSOSBroadcastTransceiver.ACTION_SOS);
        registerReceiver(sosBroadcastTransceiver, intentFilter);
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
            locMgr = null;
        }
        if (sosServer != null)
            sosServer.stop();
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.cancel(TORGI_WEB_SERVER_NOTIFICATION_ID);
        if (sosBroadcastTransceiver != null) {
            try {
                unregisterReceiver(sosBroadcastTransceiver);
            } catch (Exception e) {
                e.printStackTrace();
            }
            sosBroadcastTransceiver = null;
        }
        unregisterLocalSensors();
        if (gnssMeasurementService != null) {
            gnssMeasurementService.shutdown();
            gnssMeasurementService = null;
        }
        String dbFile = null;
        if (geoPackageRecorder != null)
            dbFile = geoPackageRecorder.shutdown();
        if ((dbFile != null) && Config.getInstance(this).isAutoShareEnabled()) {
            File file = new File(dbFile);
            if (file.exists()) {
                Intent intentShareFile = new Intent(Intent.ACTION_SEND);
                intentShareFile.setType("application/octet-stream");
                intentShareFile.putExtra(Intent.EXTRA_STREAM,
                        FileProvider.getUriForFile(this, this.getApplicationContext().getPackageName() + ".geopackage.provider", file));
                intentShareFile.putExtra(Intent.EXTRA_SUBJECT, file.getName());
                intentShareFile.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    startActivity(intentShareFile);
                } catch (ActivityNotFoundException ignore) {
                }
            }

        }
        super.onDestroy();
    }

    private void unregisterLocalSensors() {
        stopSensorService();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        synchronized (this) {
            if (intent != null) {
                String action = intent.getAction();
                if (ACTION_STOP.equalsIgnoreCase(action)) {
                    Log.d(TAG, "Shutting down TorgiService");
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
        switch (inputSourceType) {
            case NETWORK:
                builder.setTicker(getResources().getString(R.string.notification_remote));
                builder.setContentText(getResources().getString(R.string.notification_remote));
                break;

            case LOCAL_FILE:
                builder.setTicker(getResources().getString(R.string.notification_historical));
                builder.setContentText(getResources().getString(R.string.notification_historical));
                break;

            default:
                builder.setTicker(getResources().getString(R.string.notification));
                builder.setContentText(getResources().getString(R.string.notification));
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            builder.setChannelId(NOTIFICATION_CHANNEL);

        Intent intentStop = new Intent(this, TorgiService.class);
        intentStop.setAction(ACTION_STOP);
        PendingIntent pIntentShutdown = PendingIntent.getService(this, 0, intentStop, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.addAction(android.R.drawable.ic_lock_power_off, "Stop TORGI", pIntentShutdown);

        startForeground(TORGI_NOTIFICATION_ID, builder.build());
    }

    public void notifyOfWebServer(String host,String remote, String error) {
        if ((error != null) || (remote == null) || !didRemoteConnectionNotification) {
            PendingIntent pendingIntent = null;
            try {
                Intent notificationIntent = new Intent(this, Class.forName("org.sofwerx.torgi.ui.MainActivity"));
                pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
            } catch (ClassNotFoundException ignore) {
            }

            Notification.Builder builder;
            builder = new Notification.Builder(this);
            builder.setContentIntent(pendingIntent);
            String torgiHost;
            if (error == null) {
                if (remote == null) {
                    builder.setSmallIcon(R.drawable.ic_notification_torgi);
                    torgiHost = "TORGI is hosting SOS at http://" + host;
                } else {
                    builder.setSmallIcon(R.drawable.ic_notification_connected);
                    torgiHost = host + " connected to " + remote;
                    didRemoteConnectionNotification = true;
                }
            } else {
                torgiHost = error;
                builder.setSmallIcon(R.drawable.ic_notification_warning);
            }
            builder.setContentTitle("TORGI SOS");
            builder.setTicker(torgiHost);
            builder.setContentText(torgiHost);
            builder.setAutoCancel(true);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                builder.setChannelId(NOTIFICATION_CHANNEL);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.notify(TORGI_WEB_SERVER_NOTIFICATION_ID, builder.build());
        }
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

    public void onEWDataProcessed(final DataPoint dp) {
        if (dp != null) {
            if (Config.getInstance(this).processEWOnboard()) {
                final EWIndicators indicators = EWIndicators.getEWIndicators(dp);
                Heatmap.put(dp, indicators);
                if (listener != null)
                    listener.onEWDataProcessed(dp, indicators);
                if (geoPackageRecorder != null)
                    geoPackageRecorder.onEWDataProcessed(dp,indicators);
            }
        }
    }

    public void onSOSRequestReceived(AbstractOperation operation) {
        if (operation != null) {
            if (operation instanceof GetObservations) {
                GetObservations getObservations = (GetObservations) operation;
                long tempStart = getObservations.getStartTime();
                long tempStop = getObservations.getStopTime();
                if (tempStart < 0l)
                    tempStart = System.currentTimeMillis() - 1000l * 10;
                if (tempStop < tempStart)
                    tempStop = System.currentTimeMillis();
                if (tempStop - tempStart > MAX_SOS_RESPONSE_BLOCK)
                    tempStop = tempStart + MAX_SOS_RESPONSE_BLOCK;
                final long start = tempStart;
                final long stop = tempStop;
                if (geoPackageRecorder != null) {
                    geoPackageRecorder.getGnssMeasurements(start, stop, new GeoPackageRetrievalListener() {
                        @Override
                        public void onGnssSatDataRetrieved(ArrayList<GeoPackageSatDataHelper> points) {
                            Log.d(TAG, "onGnssSatDataRetrieved()");
                            TorgiSOSBroadcastTransceiver.broadcast(TorgiService.this,
                                    SOSHelper.getObservationResult(points,geoPackageRecorder.getGPSObservationPointsBlocking(start,stop)));
                        }

                        @Override
                        public void onGnssGeoPtRetrieved(ArrayList<GeoPackageGPSPtHelper> measurements) {
                            Log.d(TAG, "onGnssSatDataRetrieved() - should not be called in this case");
                        }
                    });
                }
            } else if (operation instanceof GetCapabilities) {
                TorgiSOSBroadcastTransceiver.broadcast(this,SOSHelper.getCapabilities());
            } else if (operation instanceof DescribeSensor) {
                TorgiSOSBroadcastTransceiver.broadcast(this,SOSHelper.getDescribeSensor(this,currentLocation));
            }
        }
    }

    public Location getCurrentLocation() {
        return currentLocation;
    }
}
