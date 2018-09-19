package org.sofwerx.torgi.gnss.helper;

public class GeoPackageGPSPtHelper {
    private long id;
    private double lat;
    private double lng;
    private double alt;
    private long time;

    public double getLat() {
        return lat;
    }

    public void setLat(Object lat) {
        this.lat = (double)lat;
    }

    public long getId() {
        return id;
    }

    public void setId(Object id) {
        this.id = (long)id;
    }

    public double getLng() {
        return lng;
    }

    public void setLng(Object lng) {
        this.lng = (double)lng;
    }

    public double getAlt() {
        return alt;
    }

    public void setAlt(Object alt) {
        this.alt = (double)alt;
    }

    public long getTime() {
        return time;
    }

    public void setTime(Object time) {
        this.time = (long)time;
    }
}