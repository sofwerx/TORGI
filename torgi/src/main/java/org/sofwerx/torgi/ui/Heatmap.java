package org.sofwerx.torgi.ui;

import org.osmdroid.util.BoundingBox;
import org.osmdroid.views.overlay.Polygon;
import org.sofwerx.torgi.gnss.DataPoint;
import org.sofwerx.torgi.gnss.EWIndicators;
import org.sofwerx.torgi.gnss.LatLng;

import java.util.ArrayList;

public class Heatmap {
    private final static double METERS_PER_LATITUDE = 111130d; //approximation; this is just for heatmap display so its a rough estimate
    private final static double BOX_SIDE_LENGTH = 50d; //meters
    private final static double latSize = BOX_SIDE_LENGTH/METERS_PER_LATITUDE;
    private static double lngSize = Double.NaN;
    private static LatLng origin = null;
    private static ArrayList<Heatmap> heatmap = null;
    private BoundingBox box;
    private long timeOfInformation;
    private int rfiRisk = -1; //0 to 100
    private final static int MAX_HEATMAP_LENGTH = 500;
    private Polygon polygon = null;
    private static HeatmapChangeListener listener = null;

    public static void clear() {
        listener = null;
        heatmap = null;
        origin = null;
    }

    public static ArrayList<Heatmap> getHeatmap() {
        return heatmap;
    }

    public void setPolygon(Polygon polygon) {
        this.polygon = polygon;
    }

    public static void setListener(HeatmapChangeListener heatmapChangeListener) {
        Heatmap.listener = heatmapChangeListener;
    }

    public static Heatmap put(double lat, double lng, int rfiRisk, long time) {
        if ((Double.isNaN(lat) || Double.isNaN(lng) || (rfiRisk < 0) || (time < 0l)))
            return null;

        Heatmap existing = findHeatmap(lat,lng);

        if (existing == null)
            existing = new Heatmap(lat, lng, rfiRisk, time);
        else
            existing.update(rfiRisk,time);
        return existing;
    }

    public static Heatmap put(double lat, double lng, int rfiRisk) {
        return put(lat,lng,rfiRisk,System.currentTimeMillis());
    }

    public static Heatmap put(DataPoint dp, EWIndicators indicators) {
        if ((dp == null) || !dp.isValid() && (indicators != null))
            return null;
        float fusedRisk = indicators.getFusedEWRisk();
        if (Float.isNaN(fusedRisk))
            return null;
        return put(dp.getSpaceTime().getLatitude(),dp.getSpaceTime().getLongitude(),Math.round(fusedRisk*100f),dp.getSpaceTime().getTime());
    }

    public Heatmap(double lat, double lng, int rfiRisk, long time) {
        if (heatmap == null)
            heatmap = new ArrayList<>();
        heatmap.add(this);
        if (heatmap.size() > MAX_HEATMAP_LENGTH)
            heatmap.remove(0);
        box = findBounds(lat,lng);
        update(rfiRisk,time);
    }

    private BoundingBox findBounds(double lat, double lng) {
        if (Double.isNaN(lat) || Double.isNaN(lng))
            return null;
        if (origin == null) { //this is the anchor bounding box
            origin = new LatLng(lat,lng);
            lngSize = BOX_SIDE_LENGTH/(Math.cos(Math.toRadians(lat))*METERS_PER_LATITUDE); //ignores oblate spheroid issues, but this is close enough for just a visual reference
            return new BoundingBox(lat+latSize/2d,lng+lngSize/2d,lat-latSize/2d,lng-lngSize/2d);
        }
        double centerLat = Math.round((lat - origin.latitude)/latSize)*latSize+origin.latitude;
        double centerLng = Math.round((lng - origin.longitude)/lngSize)*lngSize+origin.longitude;
        return new BoundingBox(centerLat+latSize/2d,centerLng+lngSize/2d,centerLat-latSize/2d,centerLng-lngSize/2d);
    }

    public static Heatmap findHeatmap(double lat, double lng) {
        if ((heatmap != null) && !Double.isNaN(lat) && !Double.isNaN(lng)) {
            for (int i = heatmap.size() - 1; i >= 0; i--) { //start with the most recent point since that's probably where the device is located
                if (heatmap.get(i).contains(lat, lng))
                    return heatmap.get(i);
            }
        }
        return null;
    }

    public Polygon getPolygon() {
        return polygon;
    }

    public void update(int rfiRisk, long time) {
        this.timeOfInformation = time;
        if (this.rfiRisk != rfiRisk) {
            this.rfiRisk = rfiRisk;
            if ((listener != null) && (rfiRisk > -1))
                listener.onHeatmapChange(this);
        //} else {
        //    if ((polygon == null) && (listener != null))
        //        listener.onHeatmapChange(this);
        }
    }

    public BoundingBox getBox() {
        return box;
    }

    public void setBox(BoundingBox box) {
        this.box = box;
    }

    public boolean contains(double lat, double lng) {
        if (box == null)
            return false;
        return box.contains(lat,lng);
    }

    public long getTimeOfInformation() {
        return timeOfInformation;
    }

    public void setTimeOfInformation(long timeOfInformation) {
        this.timeOfInformation = timeOfInformation;
    }

    public int getRfiRisk() {
        return rfiRisk;
    }

    public void setRfiRisk(int rfiRisk) {
        this.rfiRisk = rfiRisk;
    }

    public interface HeatmapChangeListener {
        void onHeatmapChange(Heatmap heatmap);
    }
}
