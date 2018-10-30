package org.sofwerx.torgi.gnss;

import org.sofwerx.torgi.Config;

import java.util.ArrayList;

/**
 * A single instance of the EW relevant data for one sat
 */
public class SatMeasurement {
    private Satellite sat = null;
    private GNSSEWValues values = null;

    public SatMeasurement(Satellite sat, GNSSEWValues values) {
        this.sat = sat;
        this.values = values;
    }

    public Satellite getSat() {
        return sat;
    }

    /**
     * Gets the average GNSSEWValues
     * @param satMeasurements
     * @return average values
     */
    public static GNSSEWValues getAverage(ArrayList<SatMeasurement> satMeasurements) {
        if ((satMeasurements == null) || satMeasurements.isEmpty())
            return null;
        ArrayList<GNSSEWValues> values = new ArrayList<>();
        for (SatMeasurement sat:satMeasurements) {
            if (sat.values != null)
                values.add(sat.values);
        }
        if (values.isEmpty())
            return null;
        else
            return GNSSEWValues.getAverage(values);
    }

    /**
     * Gets the average difference between values and the baseline
     * @param satMeasurements
     * @return average difference from baseline
     */
    public static GNSSEWValues getAverageDifference(ArrayList<SatMeasurement> satMeasurements) {
        if ((satMeasurements == null) || satMeasurements.isEmpty())
            return null;
        ArrayList<GNSSEWValues> values = new ArrayList<>();
        for (SatMeasurement sat:satMeasurements) {
            if ((sat.values != null) && (sat.getSat() != null) && (sat.getSat().getBaseline() != null)) {
                if (!Config.isGpsOnly() || (sat.getSat().getConstellation() == Constellation.GPS)) //filter out non-GPS constellation satellites if user selected GPS only
                    values.add(GNSSEWValues.getDifference(sat.values, sat.getSat().getBaseline()));
            }
        }
        if (values.isEmpty())
            return null;
        else
            return GNSSEWValues.getAverage(values);
    }

    public void setSat(Satellite sat) {
        this.sat = sat;
    }

    public GNSSEWValues getValues() {
        return values;
    }

    public void setValues(GNSSEWValues values) {
        this.values = values;
    }

    public boolean isValid() {
        return ((sat != null) && (values != null) && values.isValid());
    }

    /**
     * Long array of the rounded C/N0 measurements used for statistical analysis
     * @param measurements
     * @return array of values or null if fewer than 3 valid C/N0 values are present
     */
    public static long[] getCn0LongArray(ArrayList<SatMeasurement> measurements) {
        if ((measurements != null) && (measurements.size() > 2)) {
            int count = 0;

            for (SatMeasurement measurement : measurements) {
                if ((measurement.getValues() != null) && !Float.isNaN(measurement.getValues().getCn0()))
                    count++;
            }

            if (count > 2) {
                long[] values = new long[count];
                int index = 0;
                for (SatMeasurement measurement : measurements) {
                    if ((measurement.getValues() != null) && !Float.isNaN(measurement.getValues().getCn0()) && (index < count)) {
                        values[index] = (long)measurement.getValues().getCn0();
                        index++;
                    }
                }
                return values;
            }
        }

        return null;
    }

    /**
     * Provides two arrays of equal length for comparison; when the arrays are not of equal length,
     * the longer array is sorted into descending size then truncated to match the smaller array
     * @param a
     * @param b
     * @return
     */
    public static long[][] prepValuesForComparison(long[] a, long[] b) {
        if (((a == null) || (a.length < 3) || (b == null) || (b.length < 3)))
            return null;
        long[][] out = null;
        long[] shorter;
        long[] longer;
        if (a.length > b.length) {
            shorter = b;
            longer = a;
        } else {
            shorter = a;
            longer = b;
        }
        longer = sort(longer);
        out = new long[2][shorter.length];
        for (int i=0;i<shorter.length;i++) {
            out[0][i] = shorter[i];
            out[1][i] = longer[i];
        }

        return out;
    }

    /**
     * Sorts the array based on greatest distance from zero to least distance from zero
     * @param values
     * @return
     */
    private static long[] sort(long[] values) {
        if (values == null)
            return values;
        if (values.length < 2)
            return values;
        boolean sorted = false;
        int i=0;
        while (!sorted) {
            sorted = true;
            i = 0;
            while (i < values.length-1) {
                if (Math.abs(values[i]) < Math.abs(values[i + 1])) {
                    long temp = values[i];
                    values[i] = values[i+1];
                    values[i+1] = temp;
                    sorted = false;
                }
                i++;
            }
        }
        return values;
    }
}
