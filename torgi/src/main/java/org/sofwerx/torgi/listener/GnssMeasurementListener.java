package org.sofwerx.torgi.listener;

import android.location.GnssMeasurement;
import android.location.GnssMeasurementsEvent;
import android.location.GnssStatus;
import android.location.Location;

import org.sofwerx.torgi.SatStatus;
import org.sofwerx.torgi.gnss.DataPoint;

import java.util.ArrayList;
import java.util.Collection;

/**
 * This listener is used primarily to report changes detected by TorgiService to any display activity
 */
public interface GnssMeasurementListener {
    void onSatStatusUpdated(GnssStatus status);
    void onLocationChanged(Location loc);
    void onGnssMeasurementReceived(GnssMeasurementsEvent event);
    void onEWDataProcessed(DataPoint dp);
    void onProviderChanged(String provider, boolean enabled);
}
