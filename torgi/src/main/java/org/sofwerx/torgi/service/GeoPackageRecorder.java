package org.sofwerx.torgi.service;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.location.GnssClock;
import android.location.GnssMeasurement;
import android.location.GnssMeasurementsEvent;
import android.location.GnssStatus;
import android.location.Location;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.widget.Toast;

import org.sofwerx.torgi.Config;
import org.sofwerx.torgi.R;
import org.sofwerx.torgi.SatStatus;
import org.sofwerx.torgi.gnss.DataPoint;
import org.sofwerx.torgi.gnss.helper.GeoPackageGPSPtHelper;
import org.sofwerx.torgi.gnss.helper.GeoPackageSatDataHelper;
import org.sofwerx.torgi.listener.GeoPackageRetrievalListener;

import java.io.File;
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
import java.util.concurrent.atomic.AtomicBoolean;

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
import mil.nga.geopackage.extension.related.UserRelatedTable;
import mil.nga.geopackage.extension.related.simple.SimpleAttributesDao;
import mil.nga.geopackage.extension.related.simple.SimpleAttributesRow;
import mil.nga.geopackage.extension.related.simple.SimpleAttributesTable;
import mil.nga.geopackage.factory.GeoPackageFactory;
import mil.nga.geopackage.features.columns.GeometryColumns;
import mil.nga.geopackage.features.columns.GeometryColumnsDao;
import mil.nga.geopackage.features.user.FeatureColumn;
import mil.nga.geopackage.features.user.FeatureCursor;
import mil.nga.geopackage.features.user.FeatureDao;
import mil.nga.geopackage.features.user.FeatureRow;
import mil.nga.geopackage.features.user.FeatureTable;
import mil.nga.geopackage.geom.GeoPackageGeometryData;
import mil.nga.geopackage.user.UserCoreResult;
import mil.nga.geopackage.user.UserCoreRow;
import mil.nga.geopackage.user.UserTable;
import mil.nga.geopackage.user.custom.UserCustomColumn;
import mil.nga.geopackage.user.custom.UserCustomDao;
import mil.nga.geopackage.user.custom.UserCustomRow;
import mil.nga.sf.Geometry;
import mil.nga.sf.GeometryType;
import mil.nga.sf.Point;
import mil.nga.sf.proj.ProjectionConstants;

import static java.time.Instant.now;
import static mil.nga.geopackage.db.GeoPackageDataType.DATETIME;
import static mil.nga.geopackage.db.GeoPackageDataType.INTEGER;
import static mil.nga.geopackage.db.GeoPackageDataType.REAL;
import static mil.nga.geopackage.db.GeoPackageDataType.TEXT;

/**
 * Saves GNSS data into a GeoPackage
 */
public class GeoPackageRecorder extends HandlerThread {
    private final static String TAG = "TORGI.GpkgRec";
    private Handler handler;
    private final Context context;
    private AtomicBoolean ready = new AtomicBoolean(false);

    private final static String SENSOR_TIME = "time";
    private final static String SENSOR_ACCEL_X = "accel_x";
    private final static String SENSOR_ACCEL_Y = "accel_y";
    private final static String SENSOR_ACCEL_Z = "accel_z";
    private final static String SENSOR_LINEAR_ACCEL_X = "linear_accel_x";
    private final static String SENSOR_LINEAR_ACCEL_Y = "linear_accel_y";
    private final static String SENSOR_LINEAR_ACCEL_Z = "linear_accel_z";
    private final static String SENSOR_MAG_X = "mag_x";
    private final static String SENSOR_MAG_Y = "mag_y";
    private final static String SENSOR_MAG_Z = "mag_z";
    private final static String SENSOR_GYRO_X = "gyro_x";
    private final static String SENSOR_GYRO_Y = "gyro_y";
    private final static String SENSOR_GYRO_Z = "gyro_z";
    private final static String SENSOR_GRAVITY_X = "gravity_x";
    private final static String SENSOR_GRAVITY_Y = "gravity_y";
    private final static String SENSOR_GRAVITY_Z = "gravity_z";
    private final static String SENSOR_ROT_VEC_X = "rot_vec_x";
    private final static String SENSOR_ROT_VEC_Y = "rot_vec_y";
    private final static String SENSOR_ROT_VEC_Z = "rot_vec_z";
    private final static String SENSOR_ROT_VEC_COS = "rot_vec_cos";
    private final static String SENSOR_ROT_VEC_HDG_ACC = "rot_vec_hdg_acc";
    private final static String SENSOR_BARO = "baro";
    private final static String SENSOR_HUMIDITY = "humidity";
    private final static String SENSOR_TEMP = "temp";
    private final static String SENSOR_LUX = "lux";
    private final static String SENSOR_PROX = "prox";
    private final static String SENSOR_STATIONARY = "stationary";
    private final static String SENSOR_MOTION = "motion";

    private final static String SAT_DATA_MEASSURED_TIME = "local_time";
    private final static String SAT_DATA_SVID = "svid";
    private final static String SAT_DATA_CONSTELLATION = "constellation";
    private final static String SAT_DATA_CN0 = "cn0";
    private final static String SAT_DATA_AGC = "agc";
    //private final static String SAT_DATA_SAT_TIME_NANOS = "sat_time_nanos";

    private final static String GPS_OBS_PT_LAT = "Lat";
    private final static String GPS_OBS_PT_LNG = "Lon";
    private final static String GPS_OBS_PT_ALT = "Alt";
    private final static String GPS_OBS_PT_GPS_TIME = "GPSTime";

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

    public GeoPackageRecorder(Context context) {
        super("GeoPkgRcdr");
        this.context = context;
    }

    @Override
    protected void onLooperPrepared() {
        handler = new Handler();
        if (GPSgpkg == null) {
            String fileFolder = Config.getInstance(context).getSavedDir();
            if (fileFolder != null) {
                File file = new File(fileFolder);
                if (!"TORGI".equalsIgnoreCase(file.getName())) {
                    File torgi = new File(file,"TORGI");
                    torgi.mkdirs();
                    torgi.setReadable(true);
                    GpkgFolder = torgi.getPath();
                } else {
                    if (file.exists())
                        GpkgFolder = fileFolder;
                }
            }
            if (GpkgFolder == null) {
                Log.e(TAG,"Unable to find TORGI storage location");
                GpkgFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
            }
            try {
                GPSgpkg = setupGpkgDB(context, GpkgFolder, GpkgFilename);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        List<String> tbls = GPSgpkg.getTables();
        tbls.add(GPSgpkg.getApplicationId());
        ready.set(true);
    }

    final static long WGS84_SRS = 4326;

    private static final String ID_COLUMN = "id";
    private static final String GEOMETRY_COLUMN = "geom";

    private String GpkgFilename = "TORGI-GNSS";
    private String GpkgFolder = null;

    private static final String PtsTableName = "gps_observation_points";
    private static final String satTblName = "sat_data";
    private static final String clkTblName = "rcvr_clock";
    private static final String motionTblName = "motion";
    private static final String satmapTblName = PtsTableName + "_" + satTblName;
    private static final String clkmapTblName = satTblName + "_" + clkTblName;
    private static final String motionmapTblName = PtsTableName + "_" + motionTblName;

    private GeoPackage gpkg = null;

    HashMap<String, org.sofwerx.torgi.SatStatus> SatStatus = new HashMap<>();
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

    private void removeTempFiles() {
        try {
            String dirPath = Config.getInstance(context).getSavedDir();
            if (dirPath != null) {
                File dir = new File(dirPath);
                if (dir.exists()) {
                    File[] files = dir.listFiles();
                    if ((files != null) && (files.length > 0)) {
                        for (File file : files) {
                            String fname = file.getName();
                            if ((fname != null) && fname.endsWith("-journal"))
                                file.delete();
                        }
                    }
                }
            }
        } catch (Exception ignore) {}
    }

    /**
     * Shutsdown the recorder and provides the file path for the database
     * @return
     */
    public String shutdown() {
        Toast.makeText(context, context.getString(R.string.data_saved_location)+Config.getInstance(context).getSavedDir(), Toast.LENGTH_LONG).show();
        Log.d(TAG,"GeoPackageRecorder.shutdown()");
        ready.set(false);
        getLooper().quit();
        if (GPSgpkg != null) {
            GPSgpkg.close();
            GPSgpkg = null;
        }
        removeTempFiles();
        return GpkgFilename;
    }

    public void onSatelliteStatusChanged(final GnssStatus status) {
        if (ready.get() && (handler != null)) {
            handler.post(() -> {
                try {
                    int numSats = status.getSatelliteCount();

                    for (int i = 0; i < numSats; ++i) {
                        SatStatus thisSat = new SatStatus();
                        thisSat.constellation = SatType.get(status.getConstellationType(i));
                        thisSat.svid = status.getSvid(i);
                        thisSat.cn0 = status.getCn0DbHz(i);

                        thisSat.has_almanac = status.hasAlmanacData(i);
                        thisSat.has_ephemeris = status.hasEphemerisData(i);

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                            thisSat.has_carrier_freq = status.hasCarrierFrequencyHz(i);
                        else
                            thisSat.has_carrier_freq = false;

                        thisSat.azimuth_deg = status.getAzimuthDegrees(i);
                        thisSat.elevation_deg = status.getElevationDegrees(i);
                        thisSat.cn0 = status.getCn0DbHz(i);

                        String hashkey = thisSat.constellation + status.getSvid(i);
                        SatStatus.put(hashkey, thisSat);

                        thisSat.setUsedInFix(status.usedInFix(i));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }

    public void getGPSObservationPoints(long startTime, long stopTime, final GeoPackageRetrievalListener listener) {
        if (ready.get() && (handler != null) && (listener != null)) {
            final String MAX_ROWS = Integer.toString(Integer.MAX_VALUE);
            final String[] range = {Long.toString(startTime),Long.toString(stopTime)};
            handler.post(() -> {
                ArrayList<GeoPackageGPSPtHelper> measurements = new ArrayList<>();
                FeatureDao featDao = GPSgpkg.getFeatureDao(PtsTableName);
                FeatureCursor resultCursor = featDao.query(GPS_OBS_PT_GPS_TIME+" >= ? AND "+GPS_OBS_PT_GPS_TIME+" < ?",range,null,null,null, MAX_ROWS);
                if (resultCursor != null) {
                    final int rowID = resultCursor.getColumnIndex(ID_COLUMN);
                    final int rowLat = resultCursor.getColumnIndex(GPS_OBS_PT_LAT);
                    final int rowLng = resultCursor.getColumnIndex(GPS_OBS_PT_LNG);
                    final int rowAlt = resultCursor.getColumnIndex(GPS_OBS_PT_ALT);
                    final int rowTime = resultCursor.getColumnIndex(GPS_OBS_PT_GPS_TIME);
                    try{
                        //FIXME this is a simplified portion of the full table; additional fields may need to be added later
                        FeatureRow row;
                        while(resultCursor.moveToNext()){
                            row = resultCursor.getRow();
                            GeoPackageGPSPtHelper helper = new GeoPackageGPSPtHelper();
                            helper.setId(row.getValue(rowID));
                            helper.setLat(row.getValue(rowLat));
                            helper.setLng(row.getValue(rowLng));
                            helper.setAlt(row.getValue(rowAlt));
                            helper.setTime(row.getValue(rowTime));
                            measurements.add(helper);
                        }
                    } finally {
                        resultCursor.close();
                    }
                }
                if (!measurements.isEmpty())
                    listener.onGnssGeoPtRetrieved(measurements);
            });
        }
    }

    /**
     * Retrieves the GNSS Measurements between two times
     * @param startTime unix time
     * @param stopTime unix time
     * @param listener
     */
    public void getGnssMeasurements(long startTime, long stopTime, final GeoPackageRetrievalListener listener) {
        if (ready.get() && (handler != null) && (listener != null)) {
            final String MAX_ROWS = Integer.toString(Integer.MAX_VALUE);
            final String[] range = {Long.toString(startTime),Long.toString(stopTime)};
            handler.post(() -> {
                ArrayList<GeoPackageSatDataHelper> measurements = new ArrayList<>();
                SimpleAttributesDao gnssDao = RTE.getSimpleAttributesDao(satTblName);
                UserCoreResult resultCursor = gnssDao.query(SAT_DATA_MEASSURED_TIME+" >= ? AND "+SAT_DATA_MEASSURED_TIME+" < ?",range,null,null,null, MAX_ROWS);
                if (resultCursor != null) {
                    final int rowID = resultCursor.getColumnIndex(ID_COLUMN);
                    final int rowSVID = resultCursor.getColumnIndex(SAT_DATA_SVID);
                    final int rowAGC = resultCursor.getColumnIndex(SAT_DATA_AGC);
                    final int rowCN0 = resultCursor.getColumnIndex(SAT_DATA_CN0);
                    final int rowConstellation = resultCursor.getColumnIndex(SAT_DATA_CONSTELLATION);
                    final int rowMeassuredTime = resultCursor.getColumnIndex(SAT_DATA_MEASSURED_TIME);
                    try{
                        //FIXME this is a simplified portion of the full table; additional fields may need to be added later
                        UserCoreRow row;
                        while(resultCursor.moveToNext()){
                            row = resultCursor.getRow();
                            GeoPackageSatDataHelper helper = new GeoPackageSatDataHelper();
                            helper.setId(row.getValue(rowID));
                            helper.setAgc(row.getValue(rowAGC));
                            helper.setCn0(row.getValue(rowCN0));
                            helper.setConstellation(row.getValue(rowConstellation));
                            helper.setSvid(row.getValue(rowSVID));
                            helper.setMeassuredTime(row.getValue(rowMeassuredTime));
                            measurements.add(helper);
                        }
                    } finally {
                        resultCursor.close();
                    }
                }
                if (!measurements.isEmpty())
                    listener.onGnssSatDataRetrieved(measurements);
            });
        }
    }

    public void onGnssMeasurementsReceived(final GnssMeasurementsEvent event) {
        if (ready.get() && (handler != null) && (event != null)) {
            handler.post(() -> {
                try {
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

                    final long time = System.currentTimeMillis();
                    for (final GnssMeasurement g : gm) {
                        String con = SatType.get(g.getConstellationType());
                        String hashkey = con + g.getSvid();

                        SimpleAttributesDao satDao = RTE.getSimpleAttributesDao(satTblName);
                        SimpleAttributesRow satrow = satDao.newRow();

                        satrow.setValue(SAT_DATA_MEASSURED_TIME, time);
                        satrow.setValue(SAT_DATA_SVID, g.getSvid());
                        satrow.setValue(SAT_DATA_CONSTELLATION, con);
                        satrow.setValue(SAT_DATA_CN0, g.getCn0DbHz());

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            if (g.hasAutomaticGainControlLevelDb()) {
                                satrow.setValue("agc", g.getAutomaticGainControlLevelDb());
                                satrow.setValue("has_agc", 1);
                            } else {
                                satrow.setValue("agc", 0);
                                satrow.setValue("has_agc", 0);
                            }
                        } else {
                            satrow.setValue(SAT_DATA_AGC, (double) 0.0);
                            satrow.setValue("has_agc", 0);
                        }
                        satrow.setValue("sync_state_flags", g.getState());
                        satrow.setValue("sync_state_txt", " ");
                        satrow.setValue("sat_time_nanos", (double) g.getReceivedSvTimeNanos());
                        satrow.setValue("sat_time_1sigma_nanos", (double) g.getReceivedSvTimeUncertaintyNanos());
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
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }

    public void onSensorUpdated(final SensorEvent event) {
        if (ready.get() && (handler != null) && (event != null)) {
            handler.post(() -> {
                float[] values = event.values;
                if ((values != null) && (values.length > 0)) {
                    UserCustomDao sensorDao = RTE.getUserDao(motionTblName);
                    UserCustomRow sensorRow = sensorDao.newRow();
                    //SimpleAttributesDao sensorDao = RTE.getSimpleAttributesDao(motionTblName);
                    //SimpleAttributesRow sensorRow = sensorDao.newRow();
                    for (int i=1;i<sensorRow.columnCount();i++) {
                        sensorRow.setValue(i,0d);
                    }
                    sensorRow.setValue(SENSOR_STATIONARY,0);
                    sensorRow.setValue(SENSOR_MOTION,0);
                    sensorRow.setValue("data_dump","");
                    sensorRow.setValue(SENSOR_TIME,event.timestamp);
                    switch (event.sensor.getType()) {
                        case Sensor.TYPE_MAGNETIC_FIELD:
                            if (values.length >= 3) {
                                sensorRow.setValue(SENSOR_MAG_X, (double)values[0]);
                                sensorRow.setValue(SENSOR_MAG_Y, (double)values[1]);
                                sensorRow.setValue(SENSOR_MAG_Z, (double)values[2]);
                            }
                            break;

                        case Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED:
                            if (values.length >= 3) {
                                sensorRow.setValue(SENSOR_MAG_X, (double)values[0]);
                                sensorRow.setValue(SENSOR_MAG_Y, (double)values[1]);
                                sensorRow.setValue(SENSOR_MAG_Z, (double)values[2]);
                            }
                            break;

                        case Sensor.TYPE_ACCELEROMETER:
                            if (values.length >= 3) {
                                sensorRow.setValue(SENSOR_ACCEL_X, (double)values[0]);
                                sensorRow.setValue(SENSOR_ACCEL_Y, (double)values[1]);
                                sensorRow.setValue(SENSOR_ACCEL_Z, (double)values[2]);
                            }
                            break;

                        case Sensor.TYPE_ACCELEROMETER_UNCALIBRATED:
                            if (values.length >= 3) {
                                sensorRow.setValue(SENSOR_ACCEL_X, (double)values[0]);
                                sensorRow.setValue(SENSOR_ACCEL_Y, (double)values[1]);
                                sensorRow.setValue(SENSOR_ACCEL_Z, (double)values[2]);
                            }
                            break;

                        case Sensor.TYPE_LINEAR_ACCELERATION:
                            if (values.length >= 3) {
                                sensorRow.setValue(SENSOR_LINEAR_ACCEL_X, (double)values[0]);
                                sensorRow.setValue(SENSOR_LINEAR_ACCEL_Y, (double)values[1]);
                                sensorRow.setValue(SENSOR_LINEAR_ACCEL_Z, (double)values[2]);
                            }
                            break;

                        case Sensor.TYPE_GYROSCOPE:
                            if (values.length >= 3) {
                                sensorRow.setValue(SENSOR_GYRO_X, (double)values[0]);
                                sensorRow.setValue(SENSOR_GYRO_Y, (double)values[1]);
                                sensorRow.setValue(SENSOR_GYRO_Z, (double)values[2]);
                            }
                            break;

                        case Sensor.TYPE_GYROSCOPE_UNCALIBRATED:
                            if (values.length >= 3) {
                                sensorRow.setValue(SENSOR_GYRO_X, (double)values[0]);
                                sensorRow.setValue(SENSOR_GYRO_Y, (double)values[1]);
                                sensorRow.setValue(SENSOR_GYRO_Z, (double)values[2]);
                            }
                            break;

                        case Sensor.TYPE_GRAVITY:
                            if (values.length >= 3) {
                                sensorRow.setValue(SENSOR_GRAVITY_X, (double)values[0]);
                                sensorRow.setValue(SENSOR_GRAVITY_Y, (double)values[1]);
                                sensorRow.setValue(SENSOR_GRAVITY_Z, (double)values[2]);
                            }
                            break;

                        case Sensor.TYPE_ROTATION_VECTOR:
                            if (values.length >= 5) {
                                sensorRow.setValue(SENSOR_ROT_VEC_X, (double)values[0]);
                                sensorRow.setValue(SENSOR_ROT_VEC_Y, (double)values[1]);
                                sensorRow.setValue(SENSOR_ROT_VEC_Z, (double)values[2]);
                                sensorRow.setValue(SENSOR_ROT_VEC_COS, (double)values[3]);
                                sensorRow.setValue(SENSOR_ROT_VEC_HDG_ACC, (double)values[4]);
                            }
                            break;

                        case Sensor.TYPE_AMBIENT_TEMPERATURE:
                            sensorRow.setValue(SENSOR_TEMP,(double)values[0]);
                            break;

                        case Sensor.TYPE_RELATIVE_HUMIDITY:
                            sensorRow.setValue(SENSOR_HUMIDITY,(double)values[0]);
                            break;

                        case Sensor.TYPE_PROXIMITY:
                            sensorRow.setValue(SENSOR_PROX,(double)values[0]);
                            break;

                        case Sensor.TYPE_PRESSURE:
                            sensorRow.setValue(SENSOR_BARO,(double)values[0]);
                            break;

                        case Sensor.TYPE_LIGHT:
                            sensorRow.setValue(SENSOR_LUX,(double)values[0]);
                            break;

                        case Sensor.TYPE_STATIONARY_DETECT:
                            sensorRow.setValue(SENSOR_STATIONARY,1);
                            break;

                        case Sensor.TYPE_MOTION_DETECT:
                            sensorRow.setValue(SENSOR_MOTION,1);
                            break;

                        default:
                            sensorRow = null;
                    }
                    if (sensorRow != null)
                        sensorDao.insert(sensorRow);
                }
            });
        }
    }

    public void onLocationChanged(final Location loc) {
        if (ready.get() && (handler != null)) {
            handler.post(() -> {
                try {
                    HashMap<String, Long> maprows = (HashMap) SatRowsToMap.clone();
                    SatRowsToMap.clear();

                    /*HashMap<String, String> locData = new HashMap<String, String>() {
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
                    };*/

                    if (GPSgpkg != null) {
                        FeatureDao featDao = GPSgpkg.getFeatureDao(PtsTableName);
                        FeatureRow frow = featDao.newRow();
                        UserMappingDao satMapDAO = RTE.getMappingDao(SatExtRel);

                        Point fix = new Point(loc.getLongitude(), loc.getLatitude(), loc.getAltitude());

                        GeoPackageGeometryData geomData = new GeoPackageGeometryData(WGS84_SRS);
                        geomData.setGeometry(fix);

                        frow.setGeometry(geomData);

                        frow.setValue(GPS_OBS_PT_LAT, (double) loc.getLatitude());
                        frow.setValue(GPS_OBS_PT_LNG, (double) loc.getLongitude());
                        frow.setValue(GPS_OBS_PT_ALT, (double) loc.getAltitude());
                        frow.setValue("Provider", loc.getProvider());
                        frow.setValue(GPS_OBS_PT_GPS_TIME, loc.getTime());
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

                        if (loc.getLongitude() < bb.getMinLongitude())
                            bb.setMinLongitude(loc.getLongitude());
                        if (loc.getLongitude() > bb.getMaxLongitude())
                            bb.setMaxLongitude(loc.getLongitude());

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
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }

    private GeoPackage openDatabase(String database) {
        if (gpkg == null) {
            GeoPackageManager gpkgMgr = GeoPackageFactory.getManager(context);
            gpkg = gpkgMgr.open(database, true);
            if (gpkg == null)
                throw new GeoPackageException("Failed to open GeoPackage database " + database);
        }
        return gpkg;
    }

    private GeoPackage setupGpkgDB(Context context, String folder, String file) throws GeoPackageException, SQLException {
        String filename = file + "-" + fmtFilenameFriendlyTime.format(System.currentTimeMillis()) + ".gpkg";
        GeoPackageManager gpkgMgr = GeoPackageFactory.getManager(context);

        GpkgFilename = folder + "/" + filename;
        if (!gpkgMgr.exists(GpkgFilename)) {
            try {
                gpkgMgr.create(GpkgFilename);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        gpkg = openDatabase(GpkgFilename);

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
        tblcols.add(FeatureColumn.createColumn(colNum++, GPS_OBS_PT_LAT, REAL, false, null));
        tblcols.add(FeatureColumn.createColumn(colNum++, GPS_OBS_PT_LNG, REAL, false, null));
        tblcols.add(FeatureColumn.createColumn(colNum++, GPS_OBS_PT_ALT, REAL, false, null));
        tblcols.add(FeatureColumn.createColumn(colNum++, "Provider", TEXT, false, null));
        tblcols.add(FeatureColumn.createColumn(colNum++, GPS_OBS_PT_GPS_TIME, INTEGER, false, null));
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
        tblcols.add(UserCustomColumn.createColumn(colNum++, SAT_DATA_MEASSURED_TIME, INTEGER, true, null));
        tblcols.add(UserCustomColumn.createColumn(colNum++, SAT_DATA_SVID, INTEGER, true, null));
        tblcols.add(UserCustomColumn.createColumn(colNum++, SAT_DATA_CONSTELLATION, TEXT, true, null));
        tblcols.add(UserCustomColumn.createColumn(colNum++, SAT_DATA_CN0, REAL, true, null));
        tblcols.add(UserCustomColumn.createColumn(colNum++, SAT_DATA_AGC, REAL, true, null));
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
        tblcols.add(UserCustomColumn.createColumn(colNum++, SENSOR_TIME, INTEGER, true, 0l));

        tblcols.add(UserCustomColumn.createColumn(colNum++, SENSOR_ACCEL_X, REAL, true, 0d));
        tblcols.add(UserCustomColumn.createColumn(colNum++, SENSOR_ACCEL_Y, REAL, true, 0d));
        tblcols.add(UserCustomColumn.createColumn(colNum++, SENSOR_ACCEL_Z, REAL, true, 0d));

        tblcols.add(UserCustomColumn.createColumn(colNum++, SENSOR_LINEAR_ACCEL_X, REAL, true, 0d));
        tblcols.add(UserCustomColumn.createColumn(colNum++, SENSOR_LINEAR_ACCEL_Y, REAL, true, 0d));
        tblcols.add(UserCustomColumn.createColumn(colNum++, SENSOR_LINEAR_ACCEL_Z, REAL, true, 0d));

        tblcols.add(UserCustomColumn.createColumn(colNum++, SENSOR_MAG_X, REAL, true, 0d));
        tblcols.add(UserCustomColumn.createColumn(colNum++, SENSOR_MAG_Y, REAL, true, 0d));
        tblcols.add(UserCustomColumn.createColumn(colNum++, SENSOR_MAG_Z, REAL, true, 0d));

        tblcols.add(UserCustomColumn.createColumn(colNum++, SENSOR_GYRO_X, REAL, true, 0d));
        tblcols.add(UserCustomColumn.createColumn(colNum++, SENSOR_GYRO_Y, REAL, true, 0d));
        tblcols.add(UserCustomColumn.createColumn(colNum++, SENSOR_GYRO_Z, REAL, true, 0d));

        tblcols.add(UserCustomColumn.createColumn(colNum++, SENSOR_GRAVITY_X, REAL, true, 0d));
        tblcols.add(UserCustomColumn.createColumn(colNum++, SENSOR_GRAVITY_Y, REAL, true, 0d));
        tblcols.add(UserCustomColumn.createColumn(colNum++, SENSOR_GRAVITY_Z, REAL, true, 0d));

        tblcols.add(UserCustomColumn.createColumn(colNum++, SENSOR_ROT_VEC_X, REAL, true, 0d));
        tblcols.add(UserCustomColumn.createColumn(colNum++, SENSOR_ROT_VEC_Y, REAL, true, 0d));
        tblcols.add(UserCustomColumn.createColumn(colNum++, SENSOR_ROT_VEC_Z, REAL, true, 0d));
        tblcols.add(UserCustomColumn.createColumn(colNum++, SENSOR_ROT_VEC_COS, REAL, true, 0d));
        tblcols.add(UserCustomColumn.createColumn(colNum++, SENSOR_ROT_VEC_HDG_ACC, REAL, true, 0d));

        tblcols.add(UserCustomColumn.createColumn(colNum++, SENSOR_BARO, REAL, true, 0d));
        tblcols.add(UserCustomColumn.createColumn(colNum++, SENSOR_HUMIDITY, REAL, true, 0d));
        tblcols.add(UserCustomColumn.createColumn(colNum++, SENSOR_TEMP, REAL, true, 0d));

        tblcols.add(UserCustomColumn.createColumn(colNum++, SENSOR_LUX, REAL, true, 0d));
        tblcols.add(UserCustomColumn.createColumn(colNum++, SENSOR_PROX, REAL, true, 0d));

        tblcols.add(UserCustomColumn.createColumn(colNum++, SENSOR_STATIONARY, INTEGER, true, 0));
        tblcols.add(UserCustomColumn.createColumn(colNum++, SENSOR_MOTION, INTEGER, true, 0));

        tblcols.add(UserCustomColumn.createColumn(colNum++, "data_dump", TEXT, true, ""));

        SimpleAttributesTable table = SimpleAttributesTable.create(tableName, tblcols);

        UserMappingTable mapTbl = UserMappingTable.create(mapTblName);
        ClkExtRel = rte.addSimpleAttributesRelationship(baseTblName, table, mapTbl);

        return (table);
    }
}
