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

import java.util.Collection;

public class GNSSMeasurementService extends Thread {
    private final static String TAG = "TORGI.MsrSrc";
    private Handler handler;
    private long HELPER_INTERVAL = 1000l;
    private long BASELINE_UPDATE_INTERVAL = 1000l * 60l;
    private long lastBaselineUpdate = Long.MIN_VALUE;
    private TorgiService torgiService;
    private EWDetection ewDetection = null;
    private Looper looper = null;

    public GNSSMeasurementService(TorgiService torgiService) {
        this.torgiService = torgiService;
        ewDetection = new EWDetection();
    }

    private final Runnable periodicHelper = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG,"GNSSMeasurementService - periodicHelper");
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

    public void onGnssMeasurementsReceived(final Location loc, GnssMeasurementsEvent event) {
        final Collection<GnssMeasurement> measurements = event.getMeasurements();
        if ((loc != null) && (measurements != null) && !measurements.isEmpty()) {
            handler.post(() -> {
                synchronized (ewDetection) {
                    Log.d(TAG,"EW Measurement recorded");
                    DataPoint dp = new DataPoint(new SpaceTime(loc));
                    for (GnssMeasurement measurement : measurements) {
                        Satellite sat = new Satellite(Constellation.get(measurement.getConstellationType()),measurement.getSvid());
                        SatMeasurement satMeasurement;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                            satMeasurement = new SatMeasurement(sat, new GNSSEWValues((float)measurement.getCn0DbHz(),measurement.getAutomaticGainControlLevelDb()));
                        else
                            satMeasurement = new SatMeasurement(sat, new GNSSEWValues((float)measurement.getCn0DbHz(),GNSSEWValues.NA));
                        dp.add(satMeasurement);
                    }
                    ewDetection.add(dp);
                }
            });
        }
    }
}
