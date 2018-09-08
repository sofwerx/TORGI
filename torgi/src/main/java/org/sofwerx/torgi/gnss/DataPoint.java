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

    public GNSSEWValues getAverageMeasurements() {
        return SatMeasurement.getAverage(measurements);
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
