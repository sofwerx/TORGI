package org.sofwerx.torgi.gnss;

import android.location.Location;
import android.os.Build;

/**
 * One point in space and time
 */
public class SpaceTime {
    private double longitude = Double.NaN;
    private double latitude = Double.NaN;
    private double altitude = Double.NaN; //m MSL
    private float horzUncertainty = Float.NaN; //m
    private float vertUncertainty = Float.NaN; //m
    private long time = Long.MIN_VALUE;

    public SpaceTime(double latitude, double longitude, double altitude, long time) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
        this.time = time;
    }

    public SpaceTime(long time) {
        this.time = time;
    }

    public SpaceTime(Location loc) {
        if (loc == null) {
            this.time = System.currentTimeMillis();
        } else {
            this.latitude = loc.getLatitude();
            this.longitude = loc.getLongitude();
            if (loc.hasAltitude())
                this.altitude = loc.getAltitude();
            if (loc.hasAccuracy())
                this.horzUncertainty = loc.getAccuracy();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (loc.hasVerticalAccuracy())
                    this.vertUncertainty = loc.getVerticalAccuracyMeters();
            }
            this.time = loc.getTime();
        }
    }

    public SpaceTime(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
        time = System.currentTimeMillis();
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getAltitude() {
        return altitude;
    }

    public void setAltitude(double altitude) {
        this.altitude = altitude;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public boolean isValid() {
        return !Double.isNaN(latitude) && !Double.isNaN(longitude) && (time > 0l);
    }
}
