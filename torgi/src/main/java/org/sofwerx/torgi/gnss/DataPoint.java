package org.sofwerx.torgi.gnss;

import java.util.ArrayList;

/**
 * Represents relevant EW indicator values at one place in spacetime
 */
public class DataPoint {
    private SpaceTime spaceTime;
    private ArrayList<SatMeasurement> measurements = null;

    public DataPoint(SpaceTime spaceTime, ArrayList<SatMeasurement> measurements) {
        this.spaceTime = spaceTime;
        this.measurements = measurements;
    }

    public DataPoint(SpaceTime spaceTime) {
        this.spaceTime = spaceTime;
    }

    /**
     * Gets the average GNSSEWValues for all satellites
     * @return
     */
    public GNSSEWValues getAverageMeasurements() {
        return SatMeasurement.getAverage(measurements);
    }

    //TODO hey dummy! maybe calculate the difference from baseline as well

    /**
     * Gets the average GNSSEWValues for each constellation
     * @return array with index corresponding to the constellation number of the constellation
     */
    public GNSSEWValues[] getAverageMeasurementsByConstellation() {
        GNSSEWValues[] values = new GNSSEWValues[Constellation.size()];

        if ((measurements != null) && !measurements.isEmpty()) {
            Constellation con;
            ArrayList<SatMeasurement> conMeas;
            for (int i=0;i<values.length;i++) {
                con = Constellation.get(i);
                conMeas = null;
                for (SatMeasurement measurement:measurements) {
                    if ((measurement.getSat() != null) && (measurement.getSat().getConstellation() == con)) {
                        if (conMeas == null)
                            conMeas = new ArrayList<>();
                        conMeas.add(measurement);
                    }
                }
                values[i] = SatMeasurement.getAverage(conMeas);
            }
        }

        return values;
    }

    public ArrayList<SatMeasurement> getMeasurements() {
        return measurements;
    }

    public void add(SatMeasurement measurement) {
        if (measurement != null) {
            if (measurements == null)
                measurements = new ArrayList<>();
            measurements.add(measurement);
        }
    }

    public SpaceTime getSpaceTime() {
        return spaceTime;
    }

    public boolean isValid() {
        return (spaceTime != null) && (measurements != null) && !measurements.isEmpty() && spaceTime.isValid();
    }
}
