package org.sofwerx.torgi.gnss;

import java.util.ArrayList;

/**
 * Collects EW indicator levels and provides estimates on likelihood of EW activity. This is a parallel
 * effort to the GeoPackage recording and contains duplicate information optimized for eventual real-time
 * detection of RFI.
 */
public class EWDetection {
    private final static int MAX_POINT_STORAGE = 50;
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
            if (points.size() > MAX_POINT_STORAGE)
                points.remove(0);
        }
    }

    /**
     * Clears out the old data points, but not the stored satellite information; helpful as a way
     * to refresh totals after recalculating a baseline
     */
    public void emptyDataPoints() {
        if (points != null)
            points = new ArrayList<>();
    }

    /**
     * Gets all the measurements for one satellite
     * @param sat
     * @param discardSignificantDifference true == ignore any measurement that is significantly different from that satellite's baseline
     * @return vlues or null if none found
     */
    public ArrayList<GNSSEWValues> getAllSatMeasurementsForOneSat(Satellite sat, boolean discardSignificantDifference) {
        ArrayList<GNSSEWValues> values = null;
        if ((sat != null) && (points != null) && !points.isEmpty()) {
            values = new ArrayList<>();

            ArrayList<SatMeasurement> measurements;
            boolean hasBaseline = sat.getBaseline() != null;
            for (DataPoint pt:points) {
                measurements = pt.getMeasurements();
                if ((measurements != null) && !measurements.isEmpty()) {
                    for (SatMeasurement measurement:measurements) {
                        if ((measurement.getValues() != null) && sat.equals(measurement.getSat())) {
                            if (discardSignificantDifference && hasBaseline) {
                                if (!measurement.getValues().isDeviationSignificant(sat.getBaseline()))
                                    values.add(measurement.getValues());
                            } else
                                values.add(measurement.getValues());
                        }
                    }
                }
            }

            if (values.isEmpty())
                values = null;
        }
        return values;
    }

    /**
     * Updates the baseline for all satellites
     * @param discardSignificantDifference true = ignore any measurement that is significantly deviated from the baseline
     */
    public void updateBaseline(boolean discardSignificantDifference) {
        if ((points != null) && !points.isEmpty() && (satellites != null) && !satellites.isEmpty()) {
            for (Satellite sat:satellites) {
                ArrayList<GNSSEWValues> values;
                if (sat.getBaseline() == null)
                    values = getAllSatMeasurementsForOneSat(sat,false);
                else
                    values = getAllSatMeasurementsForOneSat(sat,discardSignificantDifference);
                if ((values != null) && !values.isEmpty())
                    sat.setBaseline(GNSSEWValues.getAverage(values));
            }
        }
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
