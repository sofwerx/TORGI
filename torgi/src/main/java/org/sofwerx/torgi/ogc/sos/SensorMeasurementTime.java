package org.sofwerx.torgi.ogc.sos;

import android.util.Log;

/**
 * Holds a measurement location
 */
public class SensorMeasurementTime extends SensorMeasurement {
    public SensorMeasurementTime() {
        super(new SensorTimeResultTemplateField());
    }

    @Override
    public String toString() {
        if (value instanceof Long)
            return SosIpcTransceiver.formatTime((Long)value);
        Log.e(SosIpcTransceiver.TAG,"Value for this time measurement is not of type Long");
        return null;
    }

    @Override
    public void parseLong(String in) throws NumberFormatException {
        if (in != null)
            value = SosIpcTransceiver.parseTime(in); //consume ISO 8601 format
        if (!(value instanceof Long) || ((Long)value == Long.MIN_VALUE))
            throw new NumberFormatException("String was not ISO 8601 formatted time");
    }
}
