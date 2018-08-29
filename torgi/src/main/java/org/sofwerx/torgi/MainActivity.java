package org.sofwerx.torgi;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.location.GnssClock;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.text.TextUtils;
import android.util.Base64;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.TextView;

import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import android.content.pm.PackageManager;
import android.telephony.SmsManager;


import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;
import android.support.v4.app.ActivityCompat;
import android.location.Location;
import android.location.Criteria;
import android.location.LocationManager;
import android.location.LocationListener;
import android.location.GnssStatus;
import android.location.GnssMeasurement;
import android.location.GnssMeasurementsEvent;

import static java.time.Instant.now;
import static mil.nga.geopackage.db.GeoPackageDataType.INT;
import static mil.nga.geopackage.db.GeoPackageDataType.INTEGER;
import static mil.nga.geopackage.db.GeoPackageDataType.TEXT;
import static mil.nga.geopackage.db.GeoPackageDataType.REAL;
import static mil.nga.geopackage.db.GeoPackageDataType.BOOLEAN;
import static mil.nga.geopackage.db.GeoPackageDataType.DATETIME;



import android.app.AlertDialog;

import mil.nga.geopackage.BoundingBox;
import mil.nga.geopackage.GeoPackage;
import mil.nga.geopackage.GeoPackageException;
import mil.nga.geopackage.GeoPackageManager;
import mil.nga.geopackage.attributes.AttributesCursor;
import mil.nga.geopackage.attributes.AttributesDao;
import mil.nga.geopackage.attributes.AttributesRow;
import mil.nga.geopackage.core.contents.Contents;
import mil.nga.geopackage.core.contents.ContentsDao;
import mil.nga.geopackage.core.contents.ContentsDataType;
import mil.nga.geopackage.core.srs.SpatialReferenceSystem;
import mil.nga.geopackage.core.srs.SpatialReferenceSystemDao;
import mil.nga.geopackage.db.GeoPackageCoreConnection;
import mil.nga.geopackage.db.GeoPackageDataType;
import mil.nga.geopackage.extension.related.ExtendedRelation;
import mil.nga.geopackage.extension.related.ExtendedRelationsDao;
import mil.nga.geopackage.extension.related.RelatedTablesExtension;
import mil.nga.geopackage.extension.related.UserMappingDao;
import mil.nga.geopackage.extension.related.UserMappingRow;
import mil.nga.geopackage.extension.related.UserMappingTable;
import mil.nga.geopackage.extension.related.dublin.DublinCoreType;
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
import mil.nga.geopackage.schema.TableColumnKey;
import mil.nga.geopackage.user.UserTable;
import mil.nga.geopackage.user.custom.UserCustomColumn;
import mil.nga.geopackage.user.custom.UserCustomTable;
import mil.nga.sf.Geometry;
import mil.nga.sf.GeometryEnvelope;
import mil.nga.sf.GeometryType;
import mil.nga.sf.Point;
import mil.nga.sf.proj.ProjectionConstants;
import mil.nga.sf.util.GeometryEnvelopeBuilder;

class SatStatus {
String constellation;
int svid;

double cn0;
boolean in_fix;

boolean has_almanac;
boolean has_ephemeris;
boolean has_carrier_freq;

double elevation_deg;
double azimuth_deg;
double carrier_freq_hz;

}

public class MainActivity extends AppCompatActivity {

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

    final static long WGS84_SRS = 4326;
    final static int MIN_SDK_GNSS = 26;    // min. SDK level for certain GNSS measurement methods

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


    @RequiresApi(26)
    private final GnssStatus.Callback StatusListener = new GnssStatus.Callback() {
        public void onSatelliteStatusChanged(final GnssStatus status) {

            String displayTxt = "";
            int numSats = status.getSatelliteCount();

            for (int i = 0; i < numSats; ++i) {
                SatStatus thisSat = new SatStatus();
                thisSat.constellation = SatType.get(status.getConstellationType(i));
                thisSat.svid = status.getSvid(i);
                thisSat.cn0 = status.getCn0DbHz(i);

                thisSat.has_almanac = status.hasAlmanacData(i);
                thisSat.has_ephemeris = status.hasEphemerisData(i);

                if (Build.VERSION.SDK_INT >= MIN_SDK_GNSS) {
                    thisSat.has_carrier_freq = status.hasCarrierFrequencyHz(i);
                } else {
                    thisSat.has_carrier_freq = false;
                }

                thisSat.azimuth_deg = status.getAzimuthDegrees(i);
                thisSat.elevation_deg = status.getElevationDegrees(i);
                thisSat.cn0 = status.getCn0DbHz(i);

                String hashkey = thisSat.constellation + status.getSvid(i);
                SatStatus.put(hashkey, thisSat);

                if (status.usedInFix(i)) {
                    displayTxt = displayTxt + thisSat.constellation + thisSat.svid + "\n";
                }
            }

            TextView stat_tv = findViewById(R.id.status_text);
            stat_tv.setText(displayTxt);
        }
    };

    @RequiresApi(26)
    private final GnssMeasurementsEvent.Callback MeasurementListener = new GnssMeasurementsEvent.Callback() {

        public void onGnssMeasurementsReceived(GnssMeasurementsEvent event) {
            Collection<GnssMeasurement> gm = event.getMeasurements();
            String displayTxt = "";

            TextView meas_tv = findViewById(R.id.measurement_text);

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

                if (Build.VERSION.SDK_INT >= MIN_SDK_GNSS) {
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
                displayTxt = displayTxt + g.toString() + "\n";

                satrow.setValue("data_dump", g.toString());
                satDao.insert(satrow);

                UserMappingRow clkmaprow = clkMapDAO.newRow();
                clkmaprow.setBaseId(satrow.getId());
                clkmaprow.setRelatedId(clkrow.getId());
                clkMapDAO.create(clkmaprow);

                SatInfo.put(hashkey, g);
                SatRowsToMap.put(hashkey, satrow.getId());
                meas_tv.setText(g.toString());
            }
        }
    };

    LocationListener locListener = new LocationListener() {
        public void onProviderEnabled(String provider) {
            TextView cur_tv = findViewById(R.id.current_text);
            cur_tv.setText(provider + " enabled");
        }

        public void onProviderDisabled(String provider) {
            TextView cur_tv = findViewById(R.id.current_text);
            cur_tv.setText(provider + " disabled");
        }

        public void onStatusChanged(final String provider, int status, Bundle extras) {
            TextView cur_tv = findViewById(R.id.current_text);
    //            cur_tv.setText(provider + " status changed");
        }


        @TargetApi(26)
        public void onLocationChanged(final Location loc) {
            HashMap<String, Long> maprows = (HashMap)SatRowsToMap.clone();
            SatRowsToMap.clear();
            TextView cur_tv = findViewById(R.id.current_text);

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
                    if (Build.VERSION.SDK_INT >= MIN_SDK_GNSS) {
                        put("HasVerticalAccuracy", String.valueOf(loc.hasVerticalAccuracy()));
                        put("VerticalAccuracy", String.valueOf(loc.getVerticalAccuracyMeters()));
                    }
                }
            };

            String txt = locData.toString() + "\n\n" + loc.toString() + "\n\n" + GpkgFilename + "       SDK v" + Build.VERSION.SDK_INT;
            cur_tv.setText(txt);

            FeatureDao featDao = GPSgpkg.getFeatureDao(PtsTableName);
            UserMappingDao satMapDAO = RTE.getMappingDao(SatExtRel);

            FeatureRow frow = featDao.newRow();

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

            if (Build.VERSION.SDK_INT >= MIN_SDK_GNSS) {
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

//
            //
            // communicate new observation
            //
//            String phoneNumber = "";
//
//            SmsManager smsMgr = SmsManager.getDefault();
//            smsMgr.sendTextMessage(phoneNumber, null, txt, null, null);
        }
    };


    //////////////////////////////////////////////////////////////////////////////////////
    @RequiresApi(26)
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView cur_tv = findViewById(R.id.current_text);

        TextView meas_tv = findViewById(R.id.measurement_text);
        TextView stat_tv = findViewById(R.id.status_text);
        meas_tv.setText("Acquiring satellites...\n", TextView.BufferType.EDITABLE);
        stat_tv.setText("Acquiring satellites...\n", TextView.BufferType.EDITABLE);
        cur_tv.setText("Acquiring satellites...\n", TextView.BufferType.EDITABLE);

        Toolbar toolbar = findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);

        String[] perms = {Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
//                Manifest.permission.SEND_SMS,
                Manifest.permission.WAKE_LOCK
        };
        ActivityCompat.requestPermissions(this, perms, 1);

        if (GPSgpkg == null) {
            try {
                GPSgpkg = setupGpkgDB(this, GpkgFolder, GpkgFilename);
            } catch (SQLException e) {
                // TODO: handle this
            }
        }

        List<String> tbls = GPSgpkg.getTables();
        tbls.add(GPSgpkg.getApplicationId());

        String dlgMsg = "SignalMonitor: " + TextUtils.join(" - ", tbls);

        toolbar.setTitle(dlgMsg);

//        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (Build.VERSION.SDK_INT < MIN_SDK_GNSS) { // won't need extra text areas
            int meas_h = meas_tv.getHeight();
            int stat_h = meas_tv.getHeight();

            int new_meas = meas_h / 4;
            int new_stat = stat_h / 4;
            meas_tv.setHeight(new_meas);
            stat_tv.setHeight(new_stat);
            stat_tv.setTop(stat_tv.getTop() - (meas_h - new_meas));

            cur_tv.setHeight(cur_tv.getHeight() + (meas_h - new_meas) + (stat_h - new_stat));

            meas_tv.setText("(individual sat measurements unavailable on this platform)\n", TextView.BufferType.EDITABLE);
            stat_tv.setText("(rcvr clock measurements unavailable on this platform)\n", TextView.BufferType.EDITABLE);
        }

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            stat_tv.setText("Location access denied.");
        } else {
            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    "MyApp::MyWakelockTag");
            wakeLock.acquire();

            LocationManager locMgr = getSystemService(LocationManager.class);
            if (Build.VERSION.SDK_INT >= MIN_SDK_GNSS) {
                locMgr.registerGnssMeasurementsCallback(MeasurementListener);
                locMgr.registerGnssStatusCallback(StatusListener);
            }
            Criteria crit = new Criteria();
            locMgr.requestLocationUpdates(locMgr.getBestProvider(crit, true), (long) 1000, (float) 0.0, locListener);
        }

//        WebView map = findViewById(R.id.map_view);
//        WebSettings webSettings = map.getSettings();
//        webSettings.setJavaScriptEnabled(true);
//
//        String unencodedHtml =
//                "<html><body>'%23' is the percent code for ‘#‘ </body></html>";
//        String encodedHtml = Base64.encodeToString(unencodedHtml.getBytes(), Base64.NO_PADDING);
//        map.loadData(encodedHtml, "text/html", "base64");

//        WebView map = new WebView(this);
//        setContentView(map);
    };
//////////////////////////////////////////////////////////////////////////////////////

    @RequiresApi(26)
    private GeoPackage setupGpkgDB(Context context, String folder, String file) throws GeoPackageException, SQLException {

        // Create database file
        String fileTime = "";
        if (Build.VERSION.SDK_INT >= MIN_SDK_GNSS) {
            fileTime = now().toString();
        } else {
            Date currentTime = Calendar.getInstance().getTime();
            fileTime = currentTime.toString();
        }
        fileTime = fileTime.replace('/', '-');
        fileTime = fileTime.replace(':', '-');
        fileTime = fileTime.replace(' ', '-');

        fileTime = fileTime.replace("-", "");

        GpkgFilename = folder + "/" + file + "-" + fileTime + ".gpkg";

        GeoPackageManager gpkgMgr = GeoPackageFactory.getManager(context);
        if (!gpkgMgr.exists(GpkgFilename)) {
            gpkgMgr.create(GpkgFilename);

        }
        GeoPackage gpkg = gpkgMgr.open(GpkgFilename, true);
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

        // android intertial sensor measurements
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}