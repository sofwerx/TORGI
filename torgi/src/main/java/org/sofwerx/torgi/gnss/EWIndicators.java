package org.sofwerx.torgi.gnss;

import android.util.Log;

import org.apache.commons.math3.stat.descriptive.moment.Skewness;
import org.apache.commons.math3.stat.inference.ChiSquareTest;

import java.util.ArrayList;

/**
 * Initial attempt to quantify real-time risk. This class holds the current ctest calculations
 * to try to quantify RFI and spoofing likelihoods
 */
public class EWIndicators {
    private final static String TAG = "TORGI.EW";
    private final static float CN0_SKEWNESS_BIAS = 1f; //weight to carry CN0 skewness vs other constellation disparities
    private final static float GLONASS_MISSING_BIAS = 1f; //weight to carry missing Glonass vs other constellation disparities
    private final static float CHISQUARE_BIAS = 1f; //weight to carry Chi Square test vs other constellation disparities
    private final static float CONSTELLATION_AVG_CN0_BIAS = 0.5f; //weight to carry Constellation average C/N0 differences vs other constellation disparities
    private final static float GPS_SPEC_FOR_EARTH_SURFACE_CN0 = 44; //dB-Hz for ideal condition C/N0 at Earth's surface
    private float likelihoodRFI = Float.NaN;
    private float likelihoodRSpoofCN0vsAGC = Float.NaN;
    private float likelihoodRSpoofConstellationDisparity = Float.NaN;
    private float weightOfConstellationDisparity = 2f;
    private float weightOfSpoofingRisk = 2f;

    /**
     * Gets the likelihood of RFI
     * @return 0.0 to 1.0 or NaN if unknown
     */
    public float getLikelihoodRFI() {
        return likelihoodRFI;
    }

    /**
     * Gets the fused (RFI and weighted Spoofing) likelihood of EW activity
     * @return 0.0 to 1.0 or UNKNOWN
     */
    public float getFusedEWRisk() {
        float fusedSpoofing = getFusedLikelihoodOfSpoofing();
        if (!Float.isNaN(fusedSpoofing)) {
            if (Float.isNaN(likelihoodRFI))
                return fusedSpoofing;
            else {
                float total = (likelihoodRFI + fusedSpoofing * weightOfSpoofingRisk)/(1f+weightOfSpoofingRisk);
                if (total > 1f)
                    total = 1f;
                return total;
            }
        } else
            return likelihoodRFI;
    }

    /**
     * Gets the fused (C/N0 and AGC comparison plus weighted ConstellationDisparity) likelihood of spoofing
     * @return 0.0 to 1.0 or NaN if unknown
     */
    public float getFusedLikelihoodOfSpoofing() {
        if (!Float.isNaN(likelihoodRSpoofCN0vsAGC)) {
            if (Float.isNaN(likelihoodRSpoofConstellationDisparity))
                return likelihoodRSpoofCN0vsAGC;
            else
                return (likelihoodRSpoofCN0vsAGC + likelihoodRSpoofConstellationDisparity * weightOfConstellationDisparity)/(1f+weightOfConstellationDisparity);
        } else
            return likelihoodRSpoofConstellationDisparity;
    }

    /**
     * Gets the likelihood of spoofing based on comparing C/N0 and AGC
     * @return 0.0 to 1.0 or NaN if unknown
     */
    public float getLikelihoodRSpoofCN0vsAGC() {
        return likelihoodRSpoofCN0vsAGC;
    }

    /**
     * Gets the likelihood that spoofing is occurring based on Constellation disparity (i.e.
     * variation from baseline compared between constellations)
     * @return 0.0 to 1.0 or NaN if unknown
     */
    public float getLikelihoodRSpoofConstellationDisparity() {
        return likelihoodRSpoofConstellationDisparity;
    }

    /**
     * Gets how heavily Constellation disparity is weighed vs C/N0 and AGC comparison when determining
     * overall spoofing likelihood
     * @return 1.0 == weighed evenly; 2.0 == Constellation disparity is weighed twice as much as C/N) and AGC comparison
     */
    public float getWeightOfConstellationDisparity() {
        return weightOfConstellationDisparity;
    }

    /**
     * Sets how heavily Constellation disparity is weighed vs C/N0 and AGC comparison when determining
     * overall spoofing likelihood
     * @param weightOfConstellationDisparity 1.0 == weighed evenly; 2.0 == Constellation disparity is weighed twice as much as C/N0 and AGC comparison
     */
    public void setWeightOfConstellationDisparity(float weightOfConstellationDisparity) {
        this.weightOfConstellationDisparity = weightOfConstellationDisparity;
    }

    /**
     * Gets the weight of spoofing when considering overall EW risk
     * @return 1.0 == weighed evenly; 2.0 == Spoofing is weighed twice as much as RFI
     */
    public float getWeightOfSpoofingRisk() {
        return weightOfSpoofingRisk;
    }

    /**
     * Sets the weight of spoofing when considering overall EW risk
     * @param weightOfSpoofingRisk 1.0 == weighed evenly; 2.0 == Spoofing is weighed twice as much as RFI
     */
    public void setWeightOfSpoofingRisk(float weightOfSpoofingRisk) {
        this.weightOfSpoofingRisk = weightOfSpoofingRisk;
    }

    /**
     * Equation to determine the likelihood of RFI based on the GNSSEWValues difference from baseline
     * @param diff
     * @return
     */
    private static float getRFILikelihood(GNSSEWValues diff) {
        if ((diff != null) && !Float.isNaN(diff.getCn0())) {
            if (diff.getCn0() < 0f) {
                float percent = diff.getCn0()/(-GPS_SPEC_FOR_EARTH_SURFACE_CN0);
                if (percent > 1f)
                    percent = 1f;
                if (percent < 0f)
                    percent = 0f;
                return percent;
            }
        }
        return 0f;
    }

    /**
     * Unused attempt to quantify likely jamming based on C/N0 and AGC values
     * @param diff
     * @return
     */
    private static float getRFILikelihoodCN0AGC(GNSSEWValues diff) {
        if ((diff != null) && !Float.isNaN(diff.getCn0())) {
            if (!Double.isNaN(diff.getAgc())) {
                if ((diff.getAgc() < 0d) && (diff.getCn0() < 0f)) {
                    //probability based on CN0 deviation from baseline
                    float prob = (diff.getAgc() < -1d)?(float)(diff.getAgc()/25d):0f;

                    //increase probability based on AGC/CN0 slope approximating 1
                    float slope = ((float)diff.getAgc())/diff.getCn0();
                    float distanceFromOne = 1f-Math.abs(slope);
                    prob += 1f-Math.abs(0.5f*distanceFromOne/2f);
                    prob = prob * (diff.getCn0() + 6f)/-25f;
                    if (prob > 1f)
                        prob = 1f;
                    if (prob < 0f)
                        prob = 0f;
                    return prob;
                }
            } else {
                if (diff.getCn0() < -6f)
                    return Math.abs((diff.getCn0() + 6f)/25f);
            }
        }
        return 0f;
    }

    private final static float NOMINAL_START_OF_SPOOFING_CN0 = 2f;
    private final static float NOMINAL_START_OF_SPOOFING_AGC = -5f;
    private static float getSpoofingLikelihoodCN0AGC(GNSSEWValues diff) {
        if ((diff != null) && !Float.isNaN(diff.getCn0()) && !Double.isNaN(diff.getAgc())) {
            if ((diff.getCn0() > NOMINAL_START_OF_SPOOFING_CN0) && (diff.getAgc() < NOMINAL_START_OF_SPOOFING_AGC)) {
                float prob = (float)Math.abs(diff.getAgc()/10d);
                if (prob > 1f)
                    prob = 1f;
                return prob;
            }
        }
        return 0f;
    }

    /**
     * Gets the standard deviation
     * @param values
     * @return
     */
    private static double getSD(double[] values) {
        if ((values == null) || (values.length == 0))
            return 0d;
        double powerSum1 = 0d;
        double powerSum2 = 0d;
        double stdev = 0d;

        for (int i = 0;i < values.length;i++) {
            powerSum1 += values[i];
            powerSum2 += Math.pow(values[i], 2);
            stdev = Math.sqrt(i*powerSum2 - Math.pow(powerSum1, 2))/i;
        }
        return stdev;
    }

    private static double getMean(double[] values) {
        if ((values == null) || (values.length == 0))
            return 0d;
        double sum = 0d;
        for (double value:values)
            sum += value;
        return sum/values.length;
    }

    private static float getMean(float[] values) {
        if ((values == null) || (values.length == 0))
            return 0f;
        float sum = 0f;
        for (float value:values)
            sum += value;
        return sum/values.length;
    }

    /**
     * Get the greatest difference between each of the differences between constellations and their baselines
     * @param values
     * @return
     */
    private static float getMaxAvgCN0Difference(GNSSEWValues[] values) {
        if ((values != null) && (values.length > 1)) {
            float maxDiff = 0f;
            for (int a=0;a<values.length-1;a++) {
                for (int b=a+1;b<values.length;b++) {
                    if ((values[a] != null) && (values[b] != null) && !Float.isNaN(values[a].getCn0()) && !Float.isNaN(values[b].getCn0())) {
                        float diff = Math.abs(values[b].getCn0()-values[a].getCn0());
                        if (Math.abs(diff) > maxDiff)
                            maxDiff = Math.abs(diff);
                    }
                }
            }
            Log.d(TAG,"Greatest C/N0 deviation between constellations == "+Float.toString(maxDiff));
            if (maxDiff > 0f)
                return maxDiff;
        }
        return Float.NaN;
    }

    private static float getCn0Skewness(ArrayList<GNSSEWValues> values) {
        double[] cn0values = null;
        if ((values != null) && !values.isEmpty()) {
            int count = 0;
            for (GNSSEWValues value:values) {
                if (!Float.isNaN(value.getCn0()))
                    count++;
            }
            if (count > 0) {
                cn0values = new double[count];
                int index = 0;
                for (GNSSEWValues value:values) {
                    if (!Float.isNaN(value.getCn0()) && (index < count)) {
                        cn0values[index] = value.getCn0();
                        index++;
                    }
                }
            }
        }

        if ((cn0values != null) && (cn0values.length > 2)) {
            Skewness skewness = new Skewness();
            return (float)skewness.evaluate(cn0values,0,cn0values.length);
        }
        return Float.NaN;
    }
    private final static float SKEWNESS_SCALE = 10f; //10 is arbitrarily chosen to set the scale for the amount of skewness

    private static float getSpoofingLikelihoodConstellationDisparity(DataPoint dp) {
        if (dp == null)
            return 0f;
        ArrayList<Constellation> constellations = dp.getConstellationsRepresented();
        if ((constellations == null) || constellations.isEmpty()) //this should never happen - if it does, it's probably pretty bad
            return 1f;
        if (!DataPoint.hasConstellation(constellations,Constellation.GPS)) //if the GPS constellation is missing, that is a very high indicator of jamming
            return 1f;

        //build the average of all methods for constellation differences
        float measureCount = 0f;
        float measureSum = 0f;

        //if the Glonass constellation is missing, that could be an indicator of jamming
        if (!DataPoint.hasConstellation(constellations,Constellation.Glonass)) {
            measureCount += GLONASS_MISSING_BIAS;
            measureSum += GLONASS_MISSING_BIAS;
        }

        //ArrayList<GNSSEWValues> values = dp.getAllGNSSEWValues();
        ArrayList<GNSSEWValues> values = new ArrayList<>();
        GNSSEWValues[] valueArray = dp.getAverageDifferenceFromBaselineByConstellation();
        if (valueArray != null) {
            for (GNSSEWValues value:valueArray) {
                if (value != null)
                    values.add(value);
            }
        }

        //Skewness testing for all satellite C/N0 values
        float skewness = getCn0Skewness(values);
        if (!Float.isNaN(skewness)) {
            Log.d(TAG,"Skewness = "+Float.toString(skewness));
            if (Math.abs(skewness) > 2f) { //within +/-2 is an indicator of normal distribution
                //float p_Skewness = Math.abs(skewness)/SKEWNESS_SCALE;
                float p_Skewness = Math.abs(skewness)/(GPS_SPEC_FOR_EARTH_SURFACE_CN0/2f);
                if (p_Skewness > 1f)
                    p_Skewness = 1f;
                measureCount += CN0_SKEWNESS_BIAS;
                measureSum += p_Skewness * CN0_SKEWNESS_BIAS;
            }
        }

        //Greatest difference between mean differences within constellations from constellation baseline
        float maxConstellationAvgCN0diff = getMaxAvgCN0Difference(dp.getAverageMeasurementsByConstellation());
        if (!Float.isNaN(maxConstellationAvgCN0diff)) {
            float sd = dp.getAllBaselinesCN0SD();
            float p_ConstellationAvgCN0diff;
            //GNSSEWValues avg = dp.getAverageMeasurements();
            //if (!Float.isNaN(avg.getCn0()) && (avg.getCn0() > 0f))
            //    p_ConstellationAvgCN0diff = maxConstellationAvgCN0diff/avg.getCn0();
            //else
                p_ConstellationAvgCN0diff = (maxConstellationAvgCN0diff-sd)/GPS_SPEC_FOR_EARTH_SURFACE_CN0;
            if (p_ConstellationAvgCN0diff > 1f)
                p_ConstellationAvgCN0diff = 1f;
            if (p_ConstellationAvgCN0diff > 0f) {
                measureCount += CONSTELLATION_AVG_CN0_BIAS;
                measureSum += p_ConstellationAvgCN0diff * CONSTELLATION_AVG_CN0_BIAS;
            }
        }

        //TODO maybe add a kurtosis value check?

        //Chi Square comparison across constellations
        //TODO Chi Square comparison return a lot of false positives so being dropped for now
        /*ArrayList<long[]> conValues = new ArrayList<>();
        for (Constellation constellation:Constellation.values()) {
            long[] longV = SatMeasurement.getCn0LongArray(dp.getMeasurementsByConstellation(constellation));
            if (longV != null)
                conValues.add(longV);
        }
        if (conValues.size() > 1) {
            float p_ChiSquare = Float.NaN;
            if (conValues.size() == 2) {
                long[][] comps = SatMeasurement.prepValuesForComparison(conValues.get(0),conValues.get(1));
                if ((comps != null) && (comps.length == 2)) {
                    try {
                        ChiSquareTest test = new ChiSquareTest();
                        p_ChiSquare = (float) test.chiSquareTestDataSetsComparison(comps[0], comps[1]);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } else {
                ChiSquareTest test = new ChiSquareTest();
                float temp_p_ChiSquare = 0f;
                for (int a=0;a<conValues.size()-1;a++) {
                    for (int b=a+1;b<conValues.size();b++) {
                        long[][] comps = SatMeasurement.prepValuesForComparison(conValues.get(a), conValues.get(b));
                        if ((comps != null) && (comps.length == 2)) {
                            try {
                                temp_p_ChiSquare = (float) test.chiSquareTestDataSetsComparison(comps[0], comps[1]);
                                Log.d(TAG,"ChiSquareTest constellation "+a+" vs "+b+", p = "+Float.toString(temp_p_ChiSquare));
                                if (temp_p_ChiSquare > p_ChiSquare)
                                    p_ChiSquare = temp_p_ChiSquare;
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
            Log.d(TAG,"highest p from ChiSquareTest across "+conValues.size()+" constellations == "+Float.toString(p_ChiSquare));
            if (!Float.isNaN(p_ChiSquare)) {
                measureCount += CHISQUARE_BIAS;
                measureSum += p_ChiSquare * CHISQUARE_BIAS;
            }
        } else
            Log.d(TAG,"Insufficient number of satellites in multiple constellations for ChiSquare testing");
            */



        if (measureCount > 0f)
            return measureSum/measureCount;
        else
            return 0f;
    }

    public static EWIndicators getEWIndicators(DataPoint dp) {
        if (dp == null)
            return null;
        EWIndicators iaw = new EWIndicators();
        GNSSEWValues diff = dp.getAverageDifference();
        if (diff != null) {
            Log.d(TAG,"EWIndicators: (diff = "+(Float.isNaN(diff.getCn0())?"unk":Float.toString(diff.getCn0()))+" C/N0, "+(Double.isNaN(diff.getAgc())?"unk":Double.toString(diff.getAgc()))+" AGC)");
            if (!Float.isNaN(diff.getCn0()))
                iaw.likelihoodRFI = getRFILikelihood(diff);
            iaw.likelihoodRSpoofCN0vsAGC = getSpoofingLikelihoodCN0AGC(diff);
            iaw.likelihoodRSpoofConstellationDisparity = getSpoofingLikelihoodConstellationDisparity(dp);
        }
        return iaw;
    }
}