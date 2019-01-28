package org.sofwerx.torgi.ogc.sosv2;

import java.lang.reflect.Array;
import java.util.ArrayList;

/**
 * Holds a measurement location
 */
public class SensorMeasurementLocation extends SensorMeasurement {
    public final static int FIELD_LATITUDE = 0;
    public final static int FIELD_LONGITUDE = 1;
    public final static int FIELD_ALTITUDE = 2;
    private double lat,lng,alt = Double.NaN;

    public SensorMeasurementLocation() {
        super(new SensorLocationResultTemplateField(null,null,null));
    }

    /**
     * Gets the latitude (WGS-84)
     * @return
     */
    public double getLatitude() { return lat; }

    /**
     * Sets the latitude (WGS-84)
     * @param lat
     */
    public void setLatitude(double lat) { this.lat = lat; }

    /**
     * Gets the longitude (WGS-84)
     * @return
     */
    public double getLongitude() { return lng; }

    /**
     * Sets the longitude (WGS-84)
     * @param lng
     */
    public void setLng(double lng) { this.lng = lng; }

    /**
     * Gets the altitude
     * @return m HAE
     */
    public double getAltitude() { return alt; }

    /**
     * Gets the altitude
     * @param alt m HAE
     */
    public void setAltitude(double alt) { this.alt = alt; }


    public double[] getValues() {
        double[] values = new double[3];
        values[FIELD_LATITUDE] = lat;
        values[FIELD_LONGITUDE] = lng;
        values[FIELD_ALTITUDE] = alt;
        return values;
    }

    /**
     * Sets the location
     * @param latitude WGS-84
     * @param longitude WGS-84
     * @param altitude m HAE
     */
    public void setLocation(double latitude, double longitude, double altitude) {
        this.lat = latitude;
        this.lng = longitude;
        this.alt = altitude;
    }
}
