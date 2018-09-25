package org.sofwerx.torgi.ui;

import android.graphics.Color;

import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.FolderOverlay;
import org.osmdroid.views.overlay.Polygon;

import java.util.ArrayList;
import java.util.List;

public class HeatmapOverlay {
    private FolderOverlay heatmapOverlay = null;
    private final MapView osmMap;
    private boolean rendering = false;
    private final static String[] GRADIENT = {
            "#4400FF00",
            "#5533FF00",
            "#5555FF00",
            "#5599FF00",
            "#55FFFF00",
            "#55FFBB00",
            "#55FF9900",
            "#55FF7700",
            "#55FF5500",
            "#66FF3300",
            "#66FF1100",
            "#77FF0000"};

    public HeatmapOverlay(MapView osmMap) {
        this.osmMap = osmMap;
        initOverlay();
    }

    public void initOverlay() {
        if (!rendering) {
            rendering = true;

            if (heatmapOverlay != null)
                osmMap.getOverlayManager().remove(heatmapOverlay);
            heatmapOverlay = new FolderOverlay();

            new Thread() {
                @Override
                public void run() {
                    ArrayList<Heatmap> heatmaps = Heatmap.getHeatmap();
                    if ((heatmaps != null) && !heatmaps.isEmpty()) {
                        for (Heatmap heatmap:heatmaps) {
                            Polygon poly = createPolygon(heatmap);
                            if (poly != null)
                                heatmap.setPolygon(poly);
                        }
                    }

                    osmMap.post(() -> {
                        osmMap.getOverlayManager().add(heatmapOverlay);
                        osmMap.invalidate();
                        rendering = false;
                    });
                }
            }.start();
        }
    }

    public static int getFillColor(int percent) {
        int index = GRADIENT.length*percent/100;
        //index = GRADIENT.length*percent/10; uncomment this for an exaggerated scale for testing
        if (index < 0)
            index = 0;
        if (index >= GRADIENT.length)
            index = GRADIENT.length - 1;
        return Color.parseColor(GRADIENT[index]);
    }

    public Polygon createPolygon(Heatmap heatmap) {
        Polygon polygon = new Polygon(osmMap);
        polygon.setFillColor(getFillColor(heatmap.getRfiRisk()));
        polygon.setStrokeColor(polygon.getFillColor());

        polygon.setStrokeWidth(0f);
        //polygon.setStrokeWidth(20f);
        List<GeoPoint> pts = new ArrayList<GeoPoint>();
        BoundingBox key = heatmap.getBox();
        pts.add(new GeoPoint(key.getLatNorth(), key.getLonWest()));
        pts.add(new GeoPoint(key.getLatNorth(), key.getLonEast()));
        pts.add(new GeoPoint(key.getLatSouth(), key.getLonEast()));
        pts.add(new GeoPoint(key.getLatSouth(), key.getLonWest()));
        polygon.setPoints(pts);
        heatmap.setPolygon(polygon);
        heatmapOverlay.add(polygon);
        return polygon;
    }
}
