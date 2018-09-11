package org.sofwerx.torgi.gnss;

import java.util.ArrayList;

/**
 * Single measurement of significant values for determining EW effects for one measurement from one sat
 */
public class GNSSEWValues {
    public final static double NA = Double.NaN;
    private static int significantDifferencePercentCN0 = 5;
    private static int significantDifferencePercentAGC = 5;
    private float cn0 = Float.NaN;
    private double agc = NA;

    private GNSSEWValues() {}

    public GNSSEWValues(float cn0, double agc) {
        this.cn0 = cn0;
        this.agc = agc;
    }

    /**
     * Sets the percentage of difference that indicates this is a significant variation in C/N0
     * @param percent (i.e. 100 == 100%)
     */
    public static void setSignificantDifferencePercentCN0(int percent) {
        GNSSEWValues.significantDifferencePercentCN0 = Math.abs(percent);
    }

    /**
     * Sets the percentage of difference that indicates this is a significant variation in AGC
     * @param percent (i.e. 100 == 100%)
     */
    public static void setSignificantDifferencePercentAGC(int percent) {
        GNSSEWValues.significantDifferencePercentAGC = Math.abs(percent);
    }

    /**
     * Gets the Carrier-to-Noise density
     * @return in dB-Hz
     */
    public float getCn0() {
        return cn0;
    }

    /**
     * Sets the Carrier-to-Noise density
     * @param cn0 in dB-Hz
     */
    public void setCn0(float cn0) {
        this.cn0 = cn0;
    }

    /**
     * Gets the Automatic Gain Control level
     * @return in dB
     */
    public double getAgc() {
        return agc;
    }

    /**
     * Sets the Automatic Gain Control level
     * @param agc in dB
     */
    public void setAgc(double agc) {
        this.agc = agc;
    }

    /**
     * Does this measurement have all required values
     * @return
     */
    public boolean isComplete() {
        return !Float.isNaN(cn0) && !Double.isNaN(agc);
    }

    /**
     * Is this C/N0 value different enough from the reference to be significant
     * @param referenceValue
     * @return
     */
    public boolean isCNODeviationSignificant(GNSSEWValues referenceValue) {
        return Math.abs(getCN0PercentDeviation(referenceValue))>significantDifferencePercentCN0;
    }

    /**
     * Is this AGC value different enough from the reference to be significant
     * @param referenceValue
     * @return
     */
    public boolean isAGCDeviationSignificant(GNSSEWValues referenceValue) {
        return Math.abs(getAGCPercentDeviation(referenceValue))>significantDifferencePercentAGC;
    }

    /**
     * Is any value in this GNSSEWValues significantly different that the reference
     * @param referenceValue
     * @return
     */
    public boolean isDeviationSignificant(GNSSEWValues referenceValue) {
        return isCNODeviationSignificant(referenceValue) || isAGCDeviationSignificant(referenceValue);
    }

    /**
     * Gets the percentage C/N0 deviation from the reference value
     * @param referenceValue
     * @return the percentage deviation (or 0 if unable to compare)
     */
    public int getCN0PercentDeviation(GNSSEWValues referenceValue) {
        if ((referenceValue == null) || Float.isNaN(cn0) || Float.isNaN(referenceValue.cn0) || (referenceValue.cn0 == 0f))
            return 0;
        return Math.round((cn0-referenceValue.cn0)*100f/referenceValue.cn0);
    }

    /**
     * Gets the percentage AGC deviation from the reference value
     * @param referenceValue
     * @return the percentage deviation (or 0 if unable to compare)
     */
    public int getAGCPercentDeviation(GNSSEWValues referenceValue) {
        if ((referenceValue == null) || Double.isNaN(agc) || Double.isNaN(referenceValue.agc) || (Double.compare(0d,referenceValue.agc) == 0))
            return 0;
        return (int)Math.round((agc-referenceValue.agc)*100d/referenceValue.agc);
    }

    public static GNSSEWValues getAverage(ArrayList<GNSSEWValues> values) {
        if ((values == null) || values.isEmpty())
            return null;
        float sumCN0 = 0f;
        int numCN0 = 0;
        double sumAGC = 0d;
        int numAGC = 0;
        for (GNSSEWValues value:values) {
            if (!Float.isNaN(value.cn0)) {
                sumCN0 += value.cn0;
                numCN0++;
            }
            if (!Double.isNaN(value.agc)) {
                sumAGC += value.agc;
                numAGC++;
            }
        }
        if ((numCN0 == 0) || (numAGC == 0))
            return null;
        else
            return new GNSSEWValues(sumCN0/numCN0,sumAGC/numAGC);
    }
}
