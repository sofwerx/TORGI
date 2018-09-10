package org.sofwerx.torgi.gnss;

import android.location.GnssNavigationMessage;

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
        return ((sat != null) && (values != null) && values.isComplete());
    }
}
