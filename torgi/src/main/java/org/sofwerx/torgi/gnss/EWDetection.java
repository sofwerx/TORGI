package org.sofwerx.torgi.gnss;

import java.util.ArrayList;

/**
 * Collects EW indicator levels and provides estimates on likelihood of EW activity
 */
public class EWDetection {
    private ArrayList<DataPoint> points = null;
    private ArrayList<Satellite> satellites = null;

    public void add(DataPoint point) {
        if (point != null) {
            if (points == null)
                points = new ArrayList<>();
            points.add(point);
            ArrayList<SatMeasurement> sats = point.getMeasurements();
            if ((sats != null) && !sats.isEmpty()) {
                for (SatMeasurement meas:sats) {
                    update(meas.getSat());
                }
            }
        }
    }

    public void updateBaseline() {
        //TODO update the baseline discarding any significantly different values
    }

    public Satellite find(Satellite satellite) {
        if ((satellite != null) && (satellites != null) && !satellites.isEmpty()) {
            for (Satellite sat : satellites) {
                if (satellite.equals(sat))
                    return sat;
            }
        }
        return null;
    }

    public void update(Satellite satellite) {
        if (satellite != null) {
            if (satellites == null) {
                satellites = new ArrayList<>();
                satellites.add(satellite);
            } else {
                if (find(satellite) == null)
                    satellites.add(satellite);
            }
        }
    }
}
