package org.sofwerx.torgi.service;

import android.location.GnssMeasurement;
import android.location.GnssMeasurementsEvent;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.sofwerx.torgi.gnss.Constellation;
import org.sofwerx.torgi.gnss.DataPoint;
import org.sofwerx.torgi.gnss.EWDetection;
import org.sofwerx.torgi.gnss.GNSSEWValues;
import org.sofwerx.torgi.gnss.SatMeasurement;
import org.sofwerx.torgi.gnss.Satellite;
import org.sofwerx.torgi.gnss.SpaceTime;
import org.sofwerx.torgi.gnss.helper.GeoPackageSatDataHelper;

import java.util.ArrayList;
import java.util.Collection;

public class GNSSMeasurementService extends Thread {
    private final static String TAG = "TORGI.EW";
    private Handler handler;
    private long HELPER_INTERVAL = 1000l;
    private long SAME_OBSERVATION_WINDOW_TIME = 100l; //milliseconds to consider an observation within the same time window as other observations
    private long BASELINE_UPDATE_INTERVAL = 1000l * 60l;
    private long lastBaselineUpdate = Long.MIN_VALUE;
    private TorgiService torgiService;
    private EWDetection ewDetection = null;
    private Looper looper = null;

    public GNSSMeasurementService(TorgiService torgiService) {
        this.torgiService = torgiService;
        ewDetection = new EWDetection();
    }

    public void clear() {
        ewDetection = new EWDetection();
    }

    private final Runnable periodicHelper = new Runnable() {
        @Override
        public void run() {
            //Log.d(TAG,"GNSSMeasurementService - periodicHelper");
            if (System.currentTimeMillis() > lastBaselineUpdate + BASELINE_UPDATE_INTERVAL) {
                ewDetection.updateBaseline(true);
                ewDetection.emptyDataPoints();
                lastBaselineUpdate = System.currentTimeMillis();
                Log.d(TAG,"GNSS measurement baseline updated");
            }
            if (handler != null)
                handler.postDelayed(this, HELPER_INTERVAL);
        }
    };

    @Override
    public void run() {
        Looper.prepare();
        looper = Looper.myLooper();
        handler = new Handler();
        handler.postDelayed(periodicHelper,HELPER_INTERVAL);
        Looper.loop();
    }

    public void shutdown() {
        if (handler != null)
            handler.removeCallbacks(periodicHelper);
        if (looper != null)
            looper.quit();
    }

    /**
     * Method that helps digest data received over OGC SOS GetObservation
     * @param data
     */
    public void onGeoPackageSatDataHelperReceived(final Location location,final ArrayList<GeoPackageSatDataHelper> data) {
        if ((data == null) || data.isEmpty())
            return;
        handler.post(() -> {
            synchronized (ewDetection) {
                DataPoint dp = null;
                int dpsReceived = 0;
                long currentObservationTime = Long.MIN_VALUE;
                for (GeoPackageSatDataHelper satData:data) {
                    Satellite sat = Satellite.get(satData.getConstellation(),satData.getSvid());
                    SatMeasurement satMeasurement = new SatMeasurement(sat, new GNSSEWValues((float)satData.getCn0(),satData.getAgc()));
                    if (currentObservationTime == Long.MIN_VALUE) {
                        currentObservationTime = satData.getMeassuredTime();
                    } else if ((Math.abs(satData.getMeassuredTime() - currentObservationTime)) > SAME_OBSERVATION_WINDOW_TIME) {
                        ewDetection.add(dp);
                        torgiService.onEWDataProcessed(dp);
                        dp = null;
                        currentObservationTime = satData.getMeassuredTime();
                    }
                    if (dp == null) {
                        dpsReceived++;
                        if (location == null)
                            dp = new DataPoint(new SpaceTime(satData.getMeassuredTime()));
                        else {
                            //TODO this is a slight bit of assumption, but basically, we are assuming that the GeoPackageSatDataHelper data was collected at the most recent location (should at least be very nearby
                            if (location.hasAltitude())
                                dp = new DataPoint(new SpaceTime(location.getLatitude(), location.getLongitude(), location.getAltitude(), satData.getMeassuredTime()));
                            else
                                dp = new DataPoint(new SpaceTime(location.getLatitude(), location.getLongitude(), Double.NaN, satData.getMeassuredTime()));
                        }
                        currentObservationTime = satData.getMeassuredTime();
                    }
                    dp.add(satMeasurement);
                }
                ewDetection.add(dp);
                torgiService.onEWDataProcessed(dp);
                Log.d(TAG,dpsReceived+" DataPoints received from SOS");
            }
        });
    }

    public void onGnssMeasurementsReceived(final Location loc, GnssMeasurementsEvent event) {
        final Collection<GnssMeasurement> measurements = event.getMeasurements();
        if ((loc != null) && (measurements != null) && !measurements.isEmpty()) {
            handler.post(() -> {
                synchronized (ewDetection) {
                    DataPoint dp = new DataPoint(new SpaceTime(loc));
                    for (GnssMeasurement measurement : measurements) {
                        Satellite sat = Satellite.get(Constellation.get(measurement.getConstellationType()),measurement.getSvid());
                        SatMeasurement satMeasurement;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                            satMeasurement = new SatMeasurement(sat, new GNSSEWValues((float)measurement.getCn0DbHz(),measurement.getAutomaticGainControlLevelDb()));
                        else
                            satMeasurement = new SatMeasurement(sat, new GNSSEWValues((float)measurement.getCn0DbHz(),GNSSEWValues.NA));
                        dp.add(satMeasurement);
                    }
                    ewDetection.add(dp);
                    torgiService.onEWDataProcessed(dp);
                }
            });
        }
    }
}
