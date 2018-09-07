package org.sofwerx.torgi;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteCantOpenDatabaseException;
import android.location.Criteria;
import android.location.GnssClock;
import android.location.GnssMeasurement;
import android.location.GnssMeasurementsEvent;
import android.location.GnssStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.TextView;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import mil.nga.geopackage.BoundingBox;
import mil.nga.geopackage.GeoPackage;
import mil.nga.geopackage.GeoPackageException;
import mil.nga.geopackage.GeoPackageManager;
import mil.nga.geopackage.core.contents.Contents;
import mil.nga.geopackage.core.contents.ContentsDao;
import mil.nga.geopackage.core.contents.ContentsDataType;
import mil.nga.geopackage.core.srs.SpatialReferenceSystem;
import mil.nga.geopackage.core.srs.SpatialReferenceSystemDao;
import mil.nga.geopackage.extension.related.ExtendedRelation;
import mil.nga.geopackage.extension.related.RelatedTablesExtension;
import mil.nga.geopackage.extension.related.UserMappingDao;
import mil.nga.geopackage.extension.related.UserMappingRow;
import mil.nga.geopackage.extension.related.UserMappingTable;
import mil.nga.geopackage.extension.related.simple.SimpleAttributesDao;
import mil.nga.geopackage.extension.related.simple.SimpleAttributesRow;
import mil.nga.geopackage.extension.related.simple.SimpleAttributesTable;
import mil.nga.geopackage.factory.GeoPackageFactory;
import mil.nga.geopackage.features.columns.GeometryColumns;
import mil.nga.geopackage.features.columns.GeometryColumnsDao;
import mil.nga.geopackage.features.user.FeatureColumn;
import mil.nga.geopackage.features.user.FeatureDao;
import mil.nga.geopackage.features.user.FeatureRow;
import mil.nga.geopackage.features.user.FeatureTable;
import mil.nga.geopackage.geom.GeoPackageGeometryData;
import mil.nga.geopackage.user.UserTable;
import mil.nga.geopackage.user.custom.UserCustomColumn;
import mil.nga.sf.GeometryType;
import mil.nga.sf.Point;
import mil.nga.sf.proj.ProjectionConstants;

import static java.time.Instant.now;
import static java.time.Instant.parse;
import static mil.nga.geopackage.db.GeoPackageDataType.DATETIME;
import static mil.nga.geopackage.db.GeoPackageDataType.INTEGER;
import static mil.nga.geopackage.db.GeoPackageDataType.REAL;
import static mil.nga.geopackage.db.GeoPackageDataType.TEXT;

/**
 * Torgi service handles getting information from the GPS receiver (and eventually accepting data
 * from other sensors as well) and then storing that data in the GeoPackage as well as making
 * this info available to any listenng UI element.
 *
 * TODO this is currently on the main thread - move to a separate thread to support more responsive UI
 */
public class TorgiService extends Service {
    private final static String TAG = "TORGISvc";
    private final static int TORGI_NOTIFICATION_ID = 1;
    private final static String NOTIFICATION_CHANNEL = "torgi_report";
    public final static String ACTION_STOP = "STOP";
    private final static SimpleDateFormat fmtFilenameFriendlyTime = new SimpleDateFormat("YYYYMMdd HHmmss");

    HashMap<Integer, String> SatType = new HashMap<Integer, String>() {
        {
            put(0, "Unknown");
            put(1, "GPS");
            put(2, "SBAS");
            put(3, "Glonass");
            put(4, "QZSS");
            put(5, "Beidou");
            put(6, "Galileo");

        }
    };

    public void setListener(GnssMeasurementListener listener) {
        this.listener = listener;
    }

    private LocationManager locMgr = null;
    private GnssMeasurementListener listener = null;

    private final IBinder mBinder = new TorgiBinder();
    final static long WGS84_SRS = 4326;

    private static final String ID_COLUMN = "id";
    private static final String GEOMETRY_COLUMN = "geom";

    String GpkgFilename = "TORGI-GNSS";
    String GpkgFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();

    private static final String PtsTableName = "gps_observation_points";
    private static final String satTblName = "sat_data";
    private static final String clkTblName = "rcvr_clock";
    private static final String motionTblName = "motion";
    private static final String satmapTblName = PtsTableName + "_" + satTblName;
    private static final String clkmapTblName = satTblName + "_" + clkTblName;
    private static final String motionmapTblName = PtsTableName + "_" + motionTblName;

    private ArrayList<SatStatus> sats = new ArrayList<>();
    private GeoPackage gpkg = null;

    HashMap<String, SatStatus> SatStatus = new HashMap<>();
    HashMap<String, GnssMeasurement> SatInfo = new HashMap<>();
    HashMap<String, Long> SatRowsToMap = new HashMap<>();

    GeoPackage GPSgpkg = null;
    RelatedTablesExtension RTE = null;
    ExtendedRelation SatExtRel = null;
    ExtendedRelation ClkExtRel = null;
    UserTable PtsTable = null;
    UserTable SatTable = null;
    UserTable ClkTable = null;
    UserTable MotionTable = null;

    public String getGpkgFilename() {
        return GpkgFilename;
    }

    @RequiresApi(26)
    private final GnssStatus.Callback StatusListener = new GnssStatus.Callback() {
        public void onSatelliteStatusChanged(final GnssStatus status) {
            int numSats = status.getSatelliteCount();

            for (int i = 0; i < numSats; ++i) {
                SatStatus thisSat = new SatStatus();
                thisSat.constellation = SatType.get(status.getConstellationType(i));
                thisSat.svid = status.getSvid(i);
                thisSat.cn0 = status.getCn0DbHz(i);

                thisSat.has_almanac = status.hasAlmanacData(i);
                thisSat.has_ephemeris = status.hasEphemerisData(i);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    thisSat.has_carrier_freq = status.hasCarrierFrequencyHz(i);
                } else {
                    thisSat.has_carrier_freq = false;
                }

                thisSat.azimuth_deg = status.getAzimuthDegrees(i);
                thisSat.elevation_deg = status.getElevationDegrees(i);
                thisSat.cn0 = status.getCn0DbHz(i);

                String hashkey = thisSat.constellation + status.getSvid(i);
                SatStatus.put(hashkey, thisSat);

                thisSat.setUsedInFix(status.usedInFix(i));
                update(thisSat);
            }

            if (listener != null) {
                //TODO this is where later dynamic calculations dealing with this data should be called from
                listener.onSatStatusUpdated(sats);
            }
        }
    };

    /**
     * Updates the current list of satellite statuses; currently a bit redundant but exists to
     * eventually help provide less jitter in GUI options like a ListView and to help in
     * dynamic computations rather than always pulling from the GeoPackage
     * @param sat
     */
    private void update(SatStatus sat) {
        if (sat != null) {
            boolean found = false;
            for (SatStatus status:sats) {
                if (status.equals(sat)) {
                    found = true;
                    status.update(sat);
                }
            }
            if (!found)
                sats.add(sat);
        }
    }

    /**
     * Starts the location listener and starts saving GNSS data to the GeoPackage
     */
    public void startGnssRecorder() {
        boolean permissionsPassed = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        permissionsPassed = permissionsPassed && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;

        if (permissionsPassed) {
            if (locMgr == null) {
                locMgr = getSystemService(LocationManager.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    locMgr.registerGnssMeasurementsCallback(MeasurementListener);
                    locMgr.registerGnssStatusCallback(StatusListener);
                }

                locMgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, locListener);
                setForeground();
            }

            if (GPSgpkg == null) {
                try {
                    GPSgpkg = setupGpkgDB(this, GpkgFolder, GpkgFilename);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

            List<String> tbls = GPSgpkg.getTables();
            tbls.add(GPSgpkg.getApplicationId());
        }
    }

    @RequiresApi(26)
    private final GnssMeasurementsEvent.Callback MeasurementListener = new GnssMeasurementsEvent.Callback() {
        public void onGnssMeasurementsReceived(GnssMeasurementsEvent event) {
            Collection<GnssMeasurement> gm = event.getMeasurements();

            GnssClock clk = event.getClock();

            SimpleAttributesDao clkDao = RTE.getSimpleAttributesDao(clkTblName);
            SimpleAttributesRow clkrow = clkDao.newRow();

            clkrow.setValue("time_nanos", (double) clk.getTimeNanos());
            if (clk.hasTimeUncertaintyNanos()) {
                clkrow.setValue("time_uncertainty_nanos", (double) clk.getTimeUncertaintyNanos());
                clkrow.setValue("has_time_uncertainty_nanos", 1);
            } else {
                clkrow.setValue("time_uncertainty_nanos", (double) 0.0);
                clkrow.setValue("has_time_uncertainty_nanos", 0);
            }

            if (clk.hasBiasNanos()) {
                clkrow.setValue("bias_nanos", (double) clk.getBiasNanos());
                clkrow.setValue("has_bias_nanos", 1);
            } else {
                clkrow.setValue("bias_nanos", (double) 0.0);
                clkrow.setValue("has_bias_nanos", 0);
            }
            if (clk.hasFullBiasNanos()) {
                clkrow.setValue("full_bias_nanos", clk.getFullBiasNanos());
                clkrow.setValue("has_full_bias_nanos", 1);
            } else {
                clkrow.setValue("full_bias_nanos", 0);
                clkrow.setValue("has_full_bias_nanos", 0);
            }
            if (clk.hasBiasUncertaintyNanos()) {
                clkrow.setValue("bias_uncertainty_nanos", (double) clk.getBiasUncertaintyNanos());
                clkrow.setValue("has_bias_uncertainty_nanos", 1);
            } else {
                clkrow.setValue("bias_uncertainty_nanos", (double) 0.0);
                clkrow.setValue("has_bias_uncertainty_nanos", 0);
            }
            if (clk.hasDriftNanosPerSecond()) {
                clkrow.setValue("drift_nanos_per_sec", (double) clk.getDriftNanosPerSecond());
                clkrow.setValue("has_drift_nanos_per_sec", 1);
            } else {
                clkrow.setValue("drift_nanos_per_sec", (double) 0.0);
                clkrow.setValue("has_drift_nanos_per_sec", 0);
            }
            if (clk.hasDriftUncertaintyNanosPerSecond()) {
                clkrow.setValue("drift_uncertainty_nps", (double) clk.getDriftUncertaintyNanosPerSecond());
                clkrow.setValue("has_drift_uncertainty_nps", 1);
            } else {
                clkrow.setValue("drift_uncertainty_nps", (double) 0.0);
                clkrow.setValue("has_drift_uncertainty_nps", 0);
            }
            if (clk.hasLeapSecond()) {
                clkrow.setValue("leap_second", clk.getLeapSecond());
                clkrow.setValue("has_leap_second", 1);
            } else {
                clkrow.setValue("leap_second", 0);
                clkrow.setValue("has_leap_second", 0);
            }
            clkrow.setValue("hw_clock_discontinuity_count", clk.getHardwareClockDiscontinuityCount());

            clkrow.setValue("data_dump", clk.toString());
            clkDao.insert(clkrow);

            UserMappingDao clkMapDAO = RTE.getMappingDao(ClkExtRel);

            SatRowsToMap.clear();

            for(final GnssMeasurement g : gm) {

                String con = SatType.get(g.getConstellationType());
                String hashkey = con + g.getSvid();

                HashMap<String, String> thisSat = new HashMap<String, String>();

                SimpleAttributesDao satDao = RTE.getSimpleAttributesDao(satTblName);
                SimpleAttributesRow satrow = satDao.newRow();

                satrow.setValue("svid", g.getSvid());
                satrow.setValue("constellation", con);
                satrow.setValue("cn0", (double) g.getCn0DbHz());

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if (g.hasAutomaticGainControlLevelDb()) {
                        satrow.setValue("agc", (double) g.getAutomaticGainControlLevelDb());
                        satrow.setValue("has_agc", 1);
                    } else {
                        satrow.setValue("agc", 0);
                        satrow.setValue("has_agc", 0);
                    }
                } else {
                    satrow.setValue("agc", (double) 0.0);
                    satrow.setValue("has_agc", 0);
                }
                satrow.setValue("sync_state_flags", g.getState());
                satrow.setValue("sync_state_txt", " ");
                satrow.setValue("sat_time_nanos", (double) g.getReceivedSvTimeNanos());
                satrow.setValue("sat_time_1sigma_nanos",(double)  g.getReceivedSvTimeUncertaintyNanos());
                satrow.setValue("rcvr_time_offset_nanos", (double) g.getTimeOffsetNanos());
                satrow.setValue("multipath", g.getMultipathIndicator());
                if (g.hasCarrierFrequencyHz()) {
                    satrow.setValue("carrier_freq_hz", (double) g.getCarrierFrequencyHz());
                    satrow.setValue("has_carrier_freq", 1);
                } else {
                    satrow.setValue("carrier_freq_hz", (double) 0.0);
                    satrow.setValue("has_carrier_freq", 0);
                }
                satrow.setValue("accum_delta_range", (double) g.getAccumulatedDeltaRangeMeters());
                satrow.setValue("accum_delta_range_1sigma", (double) g.getAccumulatedDeltaRangeUncertaintyMeters());
                satrow.setValue("accum_delta_range_state_flags", g.getAccumulatedDeltaRangeState());
                satrow.setValue("accum_delta_range_state_txt", " ");
                satrow.setValue("pseudorange_rate_mps", (double) g.getPseudorangeRateMetersPerSecond());
                satrow.setValue("pseudorange_rate_1sigma", (double) g.getPseudorangeRateUncertaintyMetersPerSecond());

                if (SatStatus.containsKey(hashkey)) {
                    satrow.setValue("in_fix", SatStatus.get(hashkey).in_fix ? 0 : 1);

                    satrow.setValue("has_almanac", SatStatus.get(hashkey).has_almanac ? 0 : 1);
                    satrow.setValue("has_ephemeris", SatStatus.get(hashkey).has_ephemeris ? 0 : 1);
                    satrow.setValue("has_carrier_freq", SatStatus.get(hashkey).has_carrier_freq ? 0 : 1);

                    satrow.setValue("elevation_deg", (double) SatStatus.get(hashkey).elevation_deg);
                    satrow.setValue("azimuth_deg", (double) SatStatus.get(hashkey).azimuth_deg);
                } else {
                    satrow.setValue("in_fix", 0);

                    satrow.setValue("has_almanac", 0);
                    satrow.setValue("has_ephemeris", 0);
                    satrow.setValue("has_carrier_freq", 0);

                    satrow.setValue("elevation_deg", 0.0);
                    satrow.setValue("azimuth_deg", 0.0);
                }

                satrow.setValue("data_dump", g.toString());
                satDao.insert(satrow);

                UserMappingRow clkmaprow = clkMapDAO.newRow();
                clkmaprow.setBaseId(satrow.getId());
                clkmaprow.setRelatedId(clkrow.getId());
                clkMapDAO.create(clkmaprow);

                SatInfo.put(hashkey, g);
                SatRowsToMap.put(hashkey, satrow.getId());
            }
            if (listener != null) {
                //TODO this is where later dynamic calculations for this data should be called from
                listener.onGnssMeasurementReceived(gm);
            }
        }
    };

    LocationListener locListener = new LocationListener() {
        public void onProviderEnabled(String provider) {
            if (listener != null)
                listener.onProviderChanged(provider,true);
        }

        public void onProviderDisabled(String provider) {
            if (listener != null)
                listener.onProviderChanged(provider,false);
        }

        public void onStatusChanged(final String provider, int status, Bundle extras) {}

        public void onLocationChanged(final Location loc) {
            HashMap<String, Long> maprows = (HashMap)SatRowsToMap.clone();
            SatRowsToMap.clear();

            HashMap<String, String> locData = new HashMap<String, String>() {
                {
                    put("Lat", String.valueOf(loc.getLatitude()));
                    put("Lon", String.valueOf(loc.getLongitude()));
                    put("Alt", String.valueOf(loc.getAltitude()));
                    put("Provider", String.valueOf(loc.getProvider()));
                    put("Time", String.valueOf(loc.getTime()));
                    put("FixSatCount", String.valueOf(loc.getExtras().getInt("satellites")));
                    put("HasRadialAccuracy", String.valueOf(loc.hasAccuracy()));
                    put("RadialAccuracy", String.valueOf(loc.getAccuracy()));
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        put("HasVerticalAccuracy", String.valueOf(loc.hasVerticalAccuracy()));
                        put("VerticalAccuracy", String.valueOf(loc.getVerticalAccuracyMeters()));
                    }
                }
            };

            if (listener != null) {
                //TODO this is where later dynamic calculations dealing with this data should be called from
                listener.onLocationChanged(loc);
            }

            if (GPSgpkg != null) {
                FeatureDao featDao = GPSgpkg.getFeatureDao(PtsTableName);
                FeatureRow frow = featDao.newRow();
                UserMappingDao satMapDAO = RTE.getMappingDao(SatExtRel);

                Point fix = new Point(loc.getLongitude(), loc.getLatitude(), loc.getAltitude());

                GeoPackageGeometryData geomData = new GeoPackageGeometryData(WGS84_SRS);
                geomData.setGeometry(fix);

                frow.setGeometry(geomData);

                frow.setValue("Lat", (double) loc.getLatitude());
                frow.setValue("Lon", (double) loc.getLongitude());
                frow.setValue("Alt", (double) loc.getAltitude());
                frow.setValue("Provider", loc.getProvider());
                frow.setValue("GPSTime", loc.getTime());
                frow.setValue("FixSatCount", loc.getExtras().getInt("satellites"));
                if (loc.hasAccuracy()) {
                    frow.setValue("RadialAccuracy", (double) loc.getAccuracy());
                    frow.setValue("HasRadialAccuracy", 1);
                } else {
                    frow.setValue("RadialAccuracy", (double) 0.0);
                    frow.setValue("HasRadialAccuracy", 0);
                }

                if (loc.hasSpeed()) {
                    frow.setValue("Speed", (double) loc.getAccuracy());
                    frow.setValue("HasSpeed", 1);
                } else {
                    frow.setValue("Speed", (double) 0.0);
                    frow.setValue("HasSpeed", 0);
                }

                if (loc.hasBearing()) {
                    frow.setValue("Bearing", (double) loc.getAccuracy());
                    frow.setValue("HasBearing", 1);
                } else {
                    frow.setValue("Bearing", (double) 0.0);
                    frow.setValue("HasBearing", 0);
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    frow.setValue("SysTime", now().toString());

                    if (loc.hasVerticalAccuracy()) {
                        frow.setValue("VerticalAccuracy", (double) loc.getVerticalAccuracyMeters());
                        frow.setValue("HasVerticalAccuracy", 1);
                    } else {
                        frow.setValue("VerticalAccuracy", (double) 0.0);
                        frow.setValue("HasVerticalAccuracy", 0);
                    }

                    if (loc.hasSpeedAccuracy()) {
                        frow.setValue("SpeedAccuracy", (double) loc.getAccuracy());
                        frow.setValue("HasSpeedAccuracy", 1);
                    } else {
                        frow.setValue("SpeedAccuracy", (double) 0.0);
                        frow.setValue("HasSpeedAccuracy", 0);
                    }

                    if (loc.hasBearingAccuracy()) {
                        frow.setValue("BearingAccuracy", (double) loc.getAccuracy());
                        frow.setValue("HasBearingAccuracy", 1);
                    } else {
                        frow.setValue("BearingAccuracy", (double) 0.0);
                        frow.setValue("HasBearingAccuracy", 0);
                    }
                } else {
                    Date currentTime = Calendar.getInstance().getTime();
                    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US);
                    frow.setValue("SysTime", df.format(currentTime));
                    frow.setValue("HasVerticalAccuracy", 0);
                    frow.setValue("VerticalAccuracy", (double) 0.0);
                }

                frow.setValue("data_dump", loc.toString() + " " + loc.describeContents());

                featDao.insert(frow);

                for (long id : maprows.values()) {
                    UserMappingRow satmaprow = satMapDAO.newRow();
                    satmaprow.setBaseId(frow.getId());
                    satmaprow.setRelatedId(id);
                    satMapDAO.create(satmaprow);
                }

                // update feature table bounding box if necessary
                boolean dirty = false;
                BoundingBox bb = featDao.getBoundingBox();
                if (loc.getLatitude() < bb.getMinLatitude()) {
                    bb.setMinLatitude(loc.getLatitude());
                    dirty = true;
                }
                if (loc.getLatitude() > bb.getMaxLatitude()) {
                    bb.setMaxLatitude(loc.getLatitude());
                    dirty = true;
                }

                if (loc.getLongitude() < bb.getMinLongitude()) {
                    bb.setMinLongitude(loc.getLongitude());
                }
                if (loc.getLongitude() > bb.getMaxLongitude()) {
                    bb.setMaxLongitude(loc.getLongitude());
                }

                if (dirty) {
                    String bbsql = "UPDATE gpkg_contents SET " +
                            " min_x = " + bb.getMinLongitude() +
                            ", max_x = " + bb.getMaxLongitude() +
                            ", min_y = " + bb.getMinLatitude() +
                            ", max_y = " + bb.getMaxLatitude() +
                            " WHERE table_name = '" + PtsTableName + "';";
                    GPSgpkg.execSQL(bbsql);
                }
            }
        }
    };

    private GeoPackage setupGpkgDB(Context context, String folder, String file) throws GeoPackageException, SQLException {
        GpkgFilename = folder + "/" + file + "-" + fmtFilenameFriendlyTime.format(System.currentTimeMillis()) + ".gpkg";

        GeoPackageManager gpkgMgr = GeoPackageFactory.getManager(context);
        if (!gpkgMgr.exists(GpkgFilename)) {
            try {
                gpkgMgr.create(GpkgFilename);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }

        }
        gpkg = gpkgMgr.open(GpkgFilename, true);
        if (gpkg == null) {
            throw new GeoPackageException("Failed to open GeoPackage database " + GpkgFilename);
        }

        // create SRS & feature tables
        SpatialReferenceSystemDao srsDao = gpkg.getSpatialReferenceSystemDao();

        SpatialReferenceSystem srs = srsDao.getOrCreateCode(ProjectionConstants.AUTHORITY_EPSG, (long) ProjectionConstants.EPSG_WORLD_GEODETIC_SYSTEM);

        gpkg.createGeometryColumnsTable();

        PtsTable = createObservationTable(gpkg, srs, PtsTableName, GeometryType.POINT);
        String bbsql = "UPDATE gpkg_contents SET min_x = 180.0, max_x = -180.0, min_y = 90.0, max_y = -90.0 WHERE table_name = '" + PtsTableName + "';";
        gpkg.execSQL(bbsql);

        Contents contents = new Contents();
        RTE = new RelatedTablesExtension(gpkg);

        SatTable = createSatelliteTable(contents, RTE, srs, satTblName, satmapTblName, PtsTableName);
        ClkTable = createClockTable(contents, RTE, srs, clkTblName, clkmapTblName, satTblName);

        MotionTable = createMotionTable(contents, RTE, srs, motionTblName, motionmapTblName, PtsTableName);

        return gpkg;
    }


    private UserTable createObservationTable(GeoPackage geoPackage, SpatialReferenceSystem srs, String tableName, GeometryType type) throws SQLException {
        ContentsDao contentsDao = geoPackage.getContentsDao();

        Contents contents = new Contents();
        contents.setTableName(tableName);
        contents.setDataType(ContentsDataType.FEATURES);
        contents.setIdentifier(tableName);
        contents.setDescription(tableName);
        contents.setSrs(srs);

        int colNum = 0;
        List<FeatureColumn> tblcols = new LinkedList<>();
        tblcols.add(FeatureColumn.createPrimaryKeyColumn(colNum++, ID_COLUMN));
        tblcols.add(FeatureColumn.createGeometryColumn(colNum++, GEOMETRY_COLUMN, GeometryType.POINT, false, null));
        tblcols.add(FeatureColumn.createColumn(colNum++, "SysTime", DATETIME, false, null));
        tblcols.add(FeatureColumn.createColumn(colNum++, "Lat", REAL, false, null));
        tblcols.add(FeatureColumn.createColumn(colNum++, "Lon", REAL, false, null));
        tblcols.add(FeatureColumn.createColumn(colNum++, "Alt", REAL, false, null));
        tblcols.add(FeatureColumn.createColumn(colNum++, "Provider", TEXT, false, null));
        tblcols.add(FeatureColumn.createColumn(colNum++, "GPSTime", INTEGER, false, null));
        tblcols.add(FeatureColumn.createColumn(colNum++, "FixSatCount", INTEGER, false, null));
        tblcols.add(FeatureColumn.createColumn(colNum++, "HasRadialAccuracy", INTEGER, false, null));
        tblcols.add(FeatureColumn.createColumn(colNum++, "HasVerticalAccuracy", INTEGER, false, null));
        tblcols.add(FeatureColumn.createColumn(colNum++, "RadialAccuracy", REAL, false, null));
        tblcols.add(FeatureColumn.createColumn(colNum++, "VerticalAccuracy", REAL, false, null));

        tblcols.add(FeatureColumn.createColumn(colNum++, "ElapsedRealtimeNanos", REAL, false, null));

        tblcols.add(FeatureColumn.createColumn(colNum++, "HasSpeed", INTEGER, false, null));
        tblcols.add(FeatureColumn.createColumn(colNum++, "HasSpeedAccuracy", INTEGER, false, null));
        tblcols.add(FeatureColumn.createColumn(colNum++, "Speed", REAL, false, null));
        tblcols.add(FeatureColumn.createColumn(colNum++, "SpeedAccuracy", REAL, false, null));

        tblcols.add(FeatureColumn.createColumn(colNum++, "HasBearing", INTEGER, false, null));
        tblcols.add(FeatureColumn.createColumn(colNum++, "HasBearingAccuracy", INTEGER, false, null));
        tblcols.add(FeatureColumn.createColumn(colNum++, "Bearing", REAL, false, null));
        tblcols.add(FeatureColumn.createColumn(colNum++, "BearingAccuracy", REAL, false, null));

        tblcols.add(FeatureColumn.createColumn(colNum++, "data_dump", TEXT, false, null));

        FeatureTable table = new FeatureTable(tableName, tblcols);
        geoPackage.createFeatureTable(table);

        contentsDao.create(contents);

        GeometryColumnsDao geometryColumnsDao = geoPackage.getGeometryColumnsDao();

        GeometryColumns geometryColumns = new GeometryColumns();
        geometryColumns.setContents(contents);
        geometryColumns.setColumnName(GEOMETRY_COLUMN);
        geometryColumns.setGeometryType(type);
        geometryColumns.setSrs(srs);
        geometryColumns.setZ((byte) 0);
        geometryColumns.setM((byte) 0);
        geometryColumnsDao.create(geometryColumns);

        return (table);
    }

    /**
     * Clean-up TORGI and then stop this service
     */
    public void shutdown() {
        stopSelf();
    }

    private UserTable createSatelliteTable(Contents contents, RelatedTablesExtension rte, SpatialReferenceSystem srs, String tableName, String mapTblName, String baseTblName) {
        contents.setTableName(tableName);
        contents.setDataType(ContentsDataType.FEATURES);
        contents.setIdentifier(tableName);
        contents.setDescription(tableName);
        contents.setSrs(srs);

        int colNum = 1;
        List<UserCustomColumn> tblcols = new LinkedList<>();
//        tblcols.add(UserCustomColumn.createPrimaryKeyColumn(colNum++, ID_COLUMN));
        // Dublin Core metadata descriptor profile
//        tblcols.add(UserCustomColumn.createColumn(colNum++, DublinCoreType.DATE.getName(), GeoPackageDataType.DATETIME, false, null));
//        tblcols.add(FeatureColumn.createColumn(colNum++, DublinCoreType.TITLE.getName(), GeoPackageDataType.TEXT, false, null));
//        tblcols.add(FeatureColumn.createColumn(colNum++, DublinCoreType.SOURCE.getName(), GeoPackageDataType.TEXT, false, null));
//        tblcols.add(FeatureColumn.createColumn(colNum++, DublinCoreType.DESCRIPTION.getName(), GeoPackageDataType.TEXT, false, null));

        // android GNSS measurements
        tblcols.add(UserCustomColumn.createColumn(colNum++, "svid", INTEGER, true, null));
        tblcols.add(UserCustomColumn.createColumn(colNum++, "constellation", TEXT, true, null));
        tblcols.add(UserCustomColumn.createColumn(colNum++, "cn0", REAL, true, null));
        tblcols.add(UserCustomColumn.createColumn(colNum++, "agc", REAL, true, null));
        tblcols.add(UserCustomColumn.createColumn(colNum++, "has_agc", INTEGER, true, null));
        tblcols.add(UserCustomColumn.createColumn(colNum++, "in_fix", INTEGER, true, null));

        tblcols.add(UserCustomColumn.createColumn(colNum++, "sync_state_flags", INTEGER, true, null));
        tblcols.add(UserCustomColumn.createColumn(colNum++, "sync_state_txt", TEXT, true, null));
        tblcols.add(UserCustomColumn.createColumn(colNum++, "sat_time_nanos", REAL, true, null));
        tblcols.add(UserCustomColumn.createColumn(colNum++, "sat_time_1sigma_nanos", REAL, true, null));
        tblcols.add(UserCustomColumn.createColumn(colNum++, "rcvr_time_offset_nanos", REAL, true, null));
        tblcols.add(UserCustomColumn.createColumn(colNum++, "multipath", INTEGER, true, null));

        tblcols.add(UserCustomColumn.createColumn(colNum++, "has_carrier_freq", INTEGER, true, null));
        tblcols.add(UserCustomColumn.createColumn(colNum++, "carrier_freq_hz", REAL, true, null));
        tblcols.add(UserCustomColumn.createColumn(colNum++, "accum_delta_range", REAL, true, null));
        tblcols.add(UserCustomColumn.createColumn(colNum++, "accum_delta_range_1sigma", REAL, true, null));
        tblcols.add(UserCustomColumn.createColumn(colNum++, "accum_delta_range_state_flags", INTEGER, true, null));
        tblcols.add(UserCustomColumn.createColumn(colNum++, "accum_delta_range_state_txt", TEXT, true, null));
        tblcols.add(UserCustomColumn.createColumn(colNum++, "pseudorange_rate_mps", REAL, true, null));
        tblcols.add(UserCustomColumn.createColumn(colNum++, "pseudorange_rate_1sigma", REAL, true, null));

        tblcols.add(UserCustomColumn.createColumn(colNum++, "has_ephemeris", INTEGER, true, null));
        tblcols.add(UserCustomColumn.createColumn(colNum++, "has_almanac", INTEGER, true, null));
        tblcols.add(UserCustomColumn.createColumn(colNum++, "azimuth_deg", REAL, true, null));
        tblcols.add(UserCustomColumn.createColumn(colNum++, "elevation_deg", REAL, true, null));

        tblcols.add(UserCustomColumn.createColumn(colNum++, "data_dump", TEXT, true, null));

        SimpleAttributesTable table = SimpleAttributesTable.create(tableName, tblcols);

        UserMappingTable mapTbl = UserMappingTable.create(mapTblName);
        SatExtRel = rte.addSimpleAttributesRelationship(baseTblName, table, mapTbl);

        return (table);
    }


    private UserTable createClockTable(Contents contents, RelatedTablesExtension rte, SpatialReferenceSystem srs, String tableName, String mapTblName, String baseTblName) {
        contents.setTableName(tableName);
        contents.setDataType(ContentsDataType.FEATURES);
        contents.setIdentifier(tableName);
        contents.setDescription(tableName);
        contents.setSrs(srs);

        int colNum = 1;
        List<UserCustomColumn> tblcols = new LinkedList<>();
//        tblcols.add(UserCustomColumn.createPrimaryKeyColumn(colNum++, ID_COLUMN));
        // Dublin Core metadata descriptor profile
//        tblcols.add(UserCustomColumn.createColumn(colNum++, DublinCoreType.DATE.getName(), DATETIME, false, null));
//        tblcols.add(FeatureColumn.createColumn(colNum++, DublinCoreType.TITLE.getName(), TEXT, false, null));
//        tblcols.add(FeatureColumn.createColumn(colNum++, DublinCoreType.SOURCE.getName(), TEXT, false, null));
//        tblcols.add(FeatureColumn.createColumn(colNum++, DublinCoreType.DESCRIPTION.getName(), TEXT, false, null));

        // android GNSS measurements
        tblcols.add(UserCustomColumn.createColumn(colNum++, "time_nanos", REAL, true, null));
        tblcols.add(UserCustomColumn.createColumn(colNum++, "time_uncertainty_nanos", REAL, true, null));
        tblcols.add(UserCustomColumn.createColumn(colNum++, "has_time_uncertainty_nanos", INTEGER, true, null));
        tblcols.add(UserCustomColumn.createColumn(colNum++, "bias_nanos", REAL, true, null));
        tblcols.add(UserCustomColumn.createColumn(colNum++, "has_bias_nanos", INTEGER, true, null));
        tblcols.add(UserCustomColumn.createColumn(colNum++, "bias_uncertainty_nanos", REAL, true, null));
        tblcols.add(UserCustomColumn.createColumn(colNum++, "has_bias_uncertainty_nanos", INTEGER, true, null));
        tblcols.add(UserCustomColumn.createColumn(colNum++, "full_bias_nanos", INTEGER, true, null));
        tblcols.add(UserCustomColumn.createColumn(colNum++, "has_full_bias_nanos", INTEGER, true, null));

        tblcols.add(UserCustomColumn.createColumn(colNum++, "drift_nanos_per_sec", REAL, true, null));
        tblcols.add(UserCustomColumn.createColumn(colNum++, "has_drift_nanos_per_sec", INTEGER, true, null));
        tblcols.add(UserCustomColumn.createColumn(colNum++, "drift_uncertainty_nps", REAL, true, null));
        tblcols.add(UserCustomColumn.createColumn(colNum++, "has_drift_uncertainty_nps", INTEGER, true, null));
        tblcols.add(UserCustomColumn.createColumn(colNum++, "hw_clock_discontinuity_count", INTEGER, true, null));
        tblcols.add(UserCustomColumn.createColumn(colNum++, "leap_second", INTEGER, true, null));
        tblcols.add(UserCustomColumn.createColumn(colNum++, "has_leap_second", INTEGER, true, null));

        tblcols.add(UserCustomColumn.createColumn(colNum++, "data_dump", TEXT, true, null));

        SimpleAttributesTable table = SimpleAttributesTable.create(tableName, tblcols);

        UserMappingTable mapTbl = UserMappingTable.create(mapTblName);
        ClkExtRel = rte.addSimpleAttributesRelationship(baseTblName, table, mapTbl);

        return (table);
    }

    private UserTable createMotionTable(Contents contents, RelatedTablesExtension rte, SpatialReferenceSystem srs, String tableName, String mapTblName, String baseTblName) {
        contents.setTableName(tableName);
        contents.setDataType(ContentsDataType.FEATURES);
        contents.setIdentifier(tableName);
        contents.setDescription(tableName);
        contents.setSrs(srs);

        int colNum = 1;
        List<UserCustomColumn> tblcols = new LinkedList<>();
//        tblcols.add(UserCustomColumn.createPrimaryKeyColumn(colNum++, ID_COLUMN));
        // Dublin Core metadata descriptor profile
//        tblcols.add(UserCustomColumn.createColumn(colNum++, DublinCoreType.DATE.getName(), DATETIME, false, null));
//        tblcols.add(FeatureColumn.createColumn(colNum++, DublinCoreType.TITLE.getName(), TEXT, false, null));
//        tblcols.add(FeatureColumn.createColumn(colNum++, DublinCoreType.SOURCE.getName(), TEXT, false, null));
//        tblcols.add(FeatureColumn.createColumn(colNum++, DublinCoreType.DESCRIPTION.getName(), TEXT, false, null));

        // android inertial sensor measurements
        tblcols.add(UserCustomColumn.createColumn(colNum++, "accel_x", REAL, true, null));
        tblcols.add(UserCustomColumn.createColumn(colNum++, "accel_y", REAL, true, null));
        tblcols.add(UserCustomColumn.createColumn(colNum++, "accel_z", REAL, true, null));

        tblcols.add(UserCustomColumn.createColumn(colNum++, "linear_accel_x", REAL, true, null));
        tblcols.add(UserCustomColumn.createColumn(colNum++, "linear_accel_y", REAL, true, null));
        tblcols.add(UserCustomColumn.createColumn(colNum++, "linear_accel_z", REAL, true, null));

        tblcols.add(UserCustomColumn.createColumn(colNum++, "mag_x", REAL, true, null));
        tblcols.add(UserCustomColumn.createColumn(colNum++, "mag_y", REAL, true, null));
        tblcols.add(UserCustomColumn.createColumn(colNum++, "mag_z", REAL, true, null));

        tblcols.add(UserCustomColumn.createColumn(colNum++, "gyro_x", REAL, true, null));
        tblcols.add(UserCustomColumn.createColumn(colNum++, "gyro_y", REAL, true, null));
        tblcols.add(UserCustomColumn.createColumn(colNum++, "gyro_z", REAL, true, null));

        tblcols.add(UserCustomColumn.createColumn(colNum++, "gravity_x", REAL, true, null));
        tblcols.add(UserCustomColumn.createColumn(colNum++, "gravity_y", REAL, true, null));
        tblcols.add(UserCustomColumn.createColumn(colNum++, "gravity_z", REAL, true, null));

        tblcols.add(UserCustomColumn.createColumn(colNum++, "rot_vec_x", REAL, true, null));
        tblcols.add(UserCustomColumn.createColumn(colNum++, "rot_vec_y", REAL, true, null));
        tblcols.add(UserCustomColumn.createColumn(colNum++, "rot_vec_z", REAL, true, null));
        tblcols.add(UserCustomColumn.createColumn(colNum++, "rot_vec_cos", REAL, true, null));
        tblcols.add(UserCustomColumn.createColumn(colNum++, "rot_vec_hdg_acc", REAL, true, null));

        tblcols.add(UserCustomColumn.createColumn(colNum++, "baro", REAL, true, null));
        tblcols.add(UserCustomColumn.createColumn(colNum++, "humidity", REAL, true, null));
        tblcols.add(UserCustomColumn.createColumn(colNum++, "temp", REAL, true, null));

        tblcols.add(UserCustomColumn.createColumn(colNum++, "lux", REAL, true, null));
        tblcols.add(UserCustomColumn.createColumn(colNum++, "prox", REAL, true, null));

        tblcols.add(UserCustomColumn.createColumn(colNum++, "stationary", INTEGER, true, null));
        tblcols.add(UserCustomColumn.createColumn(colNum++, "motion", INTEGER, true, null));

        tblcols.add(UserCustomColumn.createColumn(colNum++, "data_dump", TEXT, true, null));

        SimpleAttributesTable table = SimpleAttributesTable.create(tableName, tblcols);

        UserMappingTable mapTbl = UserMappingTable.create(mapTblName);
        ClkExtRel = rte.addSimpleAttributesRelationship(baseTblName, table, mapTbl);

        return (table);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        startGnssRecorder();
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public void onDestroy() {
        if (locMgr != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                locMgr.unregisterGnssMeasurementsCallback(MeasurementListener);
                locMgr.unregisterGnssStatusCallback(StatusListener);
            }
            if (locListener != null)
                locMgr.removeUpdates(locListener);
        }
        if (GPSgpkg != null)
            GPSgpkg.close();
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        synchronized (this) {
            if (intent != null) {
                String action = intent.getAction();
                if (ACTION_STOP.equalsIgnoreCase(action)) {
                    Log.d(TAG,"Shutting down GNSS recorder");
                    stopSelf();
                    return START_NOT_STICKY;
                }
            }
            return START_STICKY;
        }
    }

    public class TorgiBinder extends Binder {
        public TorgiService getService() {
            return TorgiService.this;
        }
    }

    /**
     * Running TORGI as a foreground service allows TORGI to stay active on API level 26+ devices. Depending
     * on desired collection rates, could also consider migrating to a JobScheduler
     */
    private void setForeground() {
        PendingIntent pendingIntent = null;
        try {
            Intent notificationIntent = new Intent(this, Class.forName("org.sofwerx.torgi.MainActivity"));
            pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        } catch (ClassNotFoundException ignore) {
        }

        Notification.Builder builder;
        builder = new Notification.Builder(this);
        builder.setContentIntent(pendingIntent);
        builder.setSmallIcon(R.drawable.ic_notification_torgi);
        builder.setContentTitle(getResources().getString(R.string.app_name));
        builder.setTicker(getResources().getString(R.string.notification));
        builder.setContentText(getResources().getString(R.string.notification));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            builder.setChannelId(NOTIFICATION_CHANNEL);

        Intent intentStop = new Intent(this,TorgiService.class);
        intentStop.setAction(ACTION_STOP);
        PendingIntent pIntentShutdown = PendingIntent.getService(this,0,intentStop,PendingIntent.FLAG_UPDATE_CURRENT);
        builder.addAction(android.R.drawable.ic_lock_power_off, "Stop TORGI", pIntentShutdown);

        startForeground(TORGI_NOTIFICATION_ID, builder.build());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "TORGI";
            String description = "GNSS Recording data";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}
