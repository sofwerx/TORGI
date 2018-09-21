package org.osmdroid.util;

/**
 * Minimal implementation to allow Logger flavor to use some features without a full import
 */
public class BoundingBox {
    private double mLatNorth;
    private double mLatSouth;
    private double mLonEast;
    private double mLonWest;

    public BoundingBox(final double north, final double east, final double south, final double west) {
        set(north, east, south, west);
    }

    /**
     * @since 6.0.2
     * In order to avoid longitude and latitude checks that will crash
     * in TileSystem configurations with a bounding box that doesn't include [0,0]
     */
    public BoundingBox() {}

    /**
     * @since 6.0.0
     */
    public void set(final double north, final double east, final double south, final double west) {
        mLatNorth = north;
        mLonEast = east;
        mLatSouth = south;
        mLonWest = west;
    }

    public boolean contains(final double aLatitude, final double aLongitude) {
        return ((aLatitude < this.mLatNorth) && (aLatitude > this.mLatSouth))
                && ((aLongitude < this.mLonEast) && (aLongitude > this.mLonWest));
    }
}