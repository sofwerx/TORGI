package org.sofwerx.torgi.gnss;

import android.util.Log;

import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;

import java.util.ArrayList;

/**
 * Represents relevant EW indicator values at one place in spacetime
 */
public class DataPoint {
    private final static String TAG="TORGI.EW";
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

    public ArrayList<GNSSEWValues> getAllGNSSEWValues() {
        if ((measurements == null) || measurements.isEmpty())
            return null;
        ArrayList<GNSSEWValues> valuesList = new ArrayList<>();
        for (SatMeasurement measurement:measurements) {
            if (measurement.getValues() != null)
                valuesList.add(measurement.getValues());
        }
        if (valuesList.isEmpty())
            valuesList = null;
        return valuesList;
    }

    /**
     * Gets the average difference between values and baseline
     * @return
     */
    public GNSSEWValues getAverageDifference() {
        return SatMeasurement.getAverageDifference(measurements);
    }

    public ArrayList<SatMeasurement> getMeasurementsByConstellation(Constellation con) {
        if ((measurements != null) && !measurements.isEmpty() && (con != null)) {
            ArrayList<SatMeasurement> conMeas = null;
            for (SatMeasurement measurement:measurements) {
                if ((measurement.getSat() != null) && (measurement.getSat().getConstellation() == con)) {
                    if (conMeas == null)
                        conMeas = new ArrayList<>();
                    conMeas.add(measurement);
                }
            }
            return conMeas;
        }
        return null;
    }

    /**
     * Gets the average GNSSEWValues for each constellation
     * @return array with the average value with index corresponding to the constellation number of the constellation
     */
    public GNSSEWValues[] getAverageMeasurementsByConstellation() {
        GNSSEWValues[] values = new GNSSEWValues[Constellation.size()];

        if ((measurements != null) && !measurements.isEmpty()) {
            Constellation con;
            ArrayList<SatMeasurement> conMeas;
            for (int i=0;i<values.length;i++) {
                con = Constellation.get(i);
                conMeas = getMeasurementsByConstellation(con);
                values[i] = SatMeasurement.getAverage(conMeas);
            }
        }

        return values;
    }

    public float getAllBaselinesCN0SD() {
        if ((measurements != null) && !measurements.isEmpty()) {
            ArrayList<GNSSEWValues> valuesWithCN0s = new ArrayList<>();
            for (SatMeasurement measurement:measurements) {
                if ((measurement.getValues() != null) && !Float.isNaN(measurement.getValues().getCn0()))
                    valuesWithCN0s.add(measurement.getValues());
            }
            if (!valuesWithCN0s.isEmpty()) {
                double[] values = new double[valuesWithCN0s.size()];
                for (int i=0;i<values.length;i++) {
                    values[i] = valuesWithCN0s.get(i).getCn0();
                }
                StandardDeviation devCal = new StandardDeviation();
                float stdDev = (float)devCal.evaluate(values);
                Log.d(TAG,"C/N0 stdDev across all satellites == "+Float.toString(stdDev));
                return stdDev;
            }
        }
        Log.d(TAG,"Could not compute C/N0 stdDev across all satellites");
        return Float.NaN;
    }

    /**
     * Gets the difference between baseline and current GNSSEWValues for each constellation
     * @return array with the average difference between value and baseline with index corresponding to the constellation number of the constellation
     */
    public GNSSEWValues[] getAverageDifferenceFromBaselineByConstellation() {
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
                values[i] = SatMeasurement.getAverageDifference(conMeas);
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
