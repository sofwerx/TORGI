package org.sofwerx.torgi;

import android.location.GnssMeasurement;
import android.location.Location;

import java.util.ArrayList;
import java.util.Collection;

/**
 * This listener is used primarily to report changes detected by TorgiService to any display activity
 */
public interface GnssMeasurementListener {
    void onSatStatusUpdated(ArrayList<SatStatus> sats);
    void onLocationChanged(Location loc);
    void onGnssMeasurementReceived(Collection<GnssMeasurement> measurement);
    void onProviderChanged(String provider, boolean enabled);
}
