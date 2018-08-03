package org.sofwerx.signalmonitor;

import android.Manifest;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.GnssClock;
import android.os.Bundle;
import android.os.Environment;
import android.widget.TextView;

import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import android.content.pm.PackageManager;
import android.telephony.SmsManager;


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
import android.app.AlertDialog;

import javax.crypto.spec.GCMParameterSpec;

import mil.nga.geopackage.BoundingBox;
import mil.nga.geopackage.GeoPackage;
import mil.nga.geopackage.GeoPackageManager;
import mil.nga.geopackage.attributes.AttributesDao;
import mil.nga.geopackage.attributes.AttributesRow;
import mil.nga.geopackage.db.GeoPackageCoreConnection;
import mil.nga.geopackage.db.GeoPackageDataType;
import mil.nga.geopackage.extension.related.ExtendedRelation;
import mil.nga.geopackage.extension.related.ExtendedRelationsDao;
import mil.nga.geopackage.factory.GeoPackageFactory;
import mil.nga.geopackage.features.columns.GeometryColumns;
import mil.nga.geopackage.features.user.FeatureColumn;
import mil.nga.geopackage.features.user.FeatureDao;
import mil.nga.geopackage.features.user.FeatureRow;
import mil.nga.geopackage.geom.GeoPackageGeometryData;
import mil.nga.geopackage.schema.TableColumnKey;
import mil.nga.sf.GeometryType;
import mil.nga.sf.Point;

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
    final long WGS84_SRS = 4326;

    String GpkgFilename = "SignalMonitor";
    String GpkgFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();

    final String PtsTable = "gps_observation_points";
    final String extTblName = "sat_data";
    final String clkTblName = "rcvr_clock";
    final String mapTblName = PtsTable + "_" + extTblName;
    final String clkmapTblName = extTblName + "_" + clkTblName;

    HashMap<String, SatStatus> SatStatus = new HashMap<>();
    HashMap<String, GnssMeasurement> SatInfo = new HashMap<>();
    HashMap<String, Integer> SatRowsToMap = new HashMap<>();

    GeoPackageManager GpkgManager = null;
    GeoPackage GPSgpkg = null;


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
                thisSat.has_carrier_freq = status.hasCarrierFrequencyHz(i);

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

    private final GnssMeasurementsEvent.Callback MeasurementListener = new GnssMeasurementsEvent.Callback() {

        public void onGnssMeasurementsReceived(GnssMeasurementsEvent event) {
            Collection<GnssMeasurement> gm = event.getMeasurements();
            String displayTxt = "";

            TextView meas_tv = findViewById(R.id.measurement_text);

            GnssClock clk = event.getClock();

            AttributesDao clkDao = GPSgpkg.getAttributesDao(clkTblName);
            AttributesRow clkrow = clkDao.newRow();

            SatRowsToMap.clear();

            clkrow.setValue("time_nanos", clk.getTimeNanos());
            if (clk.hasTimeUncertaintyNanos()) {
                clkrow.setValue("time_uncertainty_nanos", clk.getTimeUncertaintyNanos());
            }
            if (clk.hasBiasNanos()) {
                clkrow.setValue("bias_nanos", clk.getBiasNanos());
            }
            if (clk.hasFullBiasNanos()) {
                clkrow.setValue("full_bias_nanos", clk.getFullBiasNanos());
            }
            if (clk.hasBiasUncertaintyNanos()) {
                clkrow.setValue("bias_uncertainty_nanos", clk.getTimeUncertaintyNanos());
            }
            if (clk.hasDriftNanosPerSecond()) {
                clkrow.setValue("drift_nanos_per_sec", clk.getDriftNanosPerSecond());
            }
            if (clk.hasDriftUncertaintyNanosPerSecond()) {
                clkrow.setValue("drift_uncertainty_nps", clk.getDriftUncertaintyNanosPerSecond());
            }
            if (clk.hasLeapSecond()) {
                clkrow.setValue("leap_second", clk.getLeapSecond());
            }
            clkrow.setValue("hw_clock_discontinuity_count", clk.getHardwareClockDiscontinuityCount());

            clkrow.setValue("data_dump", clk.toString());
            clkDao.insert(clkrow);

            GeoPackageCoreConnection db = GPSgpkg.getDatabase();
            String[] dummy = {};
            int clkid = db.max(clkTblName, "id", "id > 0", dummy);

            for(final GnssMeasurement g : gm) {

                String con = SatType.get(g.getConstellationType());
                String hashkey = con + g.getSvid();

                HashMap<String, String> thisSat = new HashMap<String, String>();

                AttributesDao satDao = GPSgpkg.getAttributesDao(extTblName);
                AttributesRow satrow = satDao.newRow();

                satrow.setValue("svid", g.getSvid());
                satrow.setValue("constellation", con);
                satrow.setValue("cn0", g.getCn0DbHz());
                if (g.hasAutomaticGainControlLevelDb()) {
                    satrow.setValue("agc", g.getAutomaticGainControlLevelDb());
                }
                satrow.setValue("sync_state_flags", g.getState());
                satrow.setValue("sat_time_nanos", g.getReceivedSvTimeNanos());
                satrow.setValue("sat_time_1sigma_nanos", g.getReceivedSvTimeUncertaintyNanos());
                satrow.setValue("rcvr_time_offset_nanos", g.getTimeOffsetNanos());
                satrow.setValue("multipath", g.getMultipathIndicator());
                if (g.hasCarrierFrequencyHz()) {
                    satrow.setValue("carrier_freq_hz", g.getCarrierFrequencyHz());
                }
                satrow.setValue("accum_delta_range", g.getAccumulatedDeltaRangeMeters());
                satrow.setValue("accum_delta_range_1sigma", g.getAccumulatedDeltaRangeUncertaintyMeters());
                satrow.setValue("accum_delta_range_state_flags", g.getAccumulatedDeltaRangeState());
                satrow.setValue("pseudorange_rate_mps", g.getPseudorangeRateMetersPerSecond());
                satrow.setValue("pseudorange_rate_1sigma", g.getPseudorangeRateUncertaintyMetersPerSecond());

                if (SatStatus.containsKey(hashkey)) {
                    satrow.setValue("in_fix", SatStatus.get(hashkey).in_fix);

                    satrow.setValue("has_almanac", SatStatus.get(hashkey).has_almanac);
                    satrow.setValue("has_ephemeris", SatStatus.get(hashkey).has_ephemeris);
                    satrow.setValue("has_carrier_freq", SatStatus.get(hashkey).has_carrier_freq);

                    satrow.setValue("elevation_deg", SatStatus.get(hashkey).elevation_deg);
                    satrow.setValue("azimuth_deg", SatStatus.get(hashkey).azimuth_deg);
                }
                displayTxt = displayTxt + g.toString() + "\n";

                satrow.setValue("data_dump", g.toString());
                satDao.insert(satrow);

                SatInfo.put(hashkey, g);
                meas_tv.setText(g.toString());

                // add last measurement for each sat to hash
                int satid = db.max(extTblName, "id", "id > 0", dummy);
                SatRowsToMap.put(hashkey, satid);

                // link sat records to associated rcvr clock record
                AttributesDao mapDao = GPSgpkg.getAttributesDao(clkmapTblName);
                AttributesRow maprow = mapDao.newRow();

                maprow.setValue("base_id", clkid);
                maprow.setValue("related_id", satid);

                mapDao.insert(maprow);
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

        public void onLocationChanged(final Location loc) {
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
                    put("HasVerticalAccuracy", String.valueOf(loc.hasVerticalAccuracy()));
                    put("RadialAccuracy", String.valueOf(loc.getAccuracy()));
                    put("VerticalAccuracy", String.valueOf(loc.getVerticalAccuracyMeters()));
                }
            };

            String txt = locData.toString() + "\n\n" + loc.toString() + "\n\n" + GpkgFilename;
            cur_tv.setText(txt);

            FeatureDao featDao = GPSgpkg.getFeatureDao(PtsTable);

            FeatureRow frow = featDao.newRow();

            Point fix = new Point(loc.getLongitude(), loc.getLatitude(), loc.getAltitude());

            GeoPackageGeometryData geomData = new GeoPackageGeometryData(WGS84_SRS);
            geomData.setGeometry(fix);

            frow.setGeometry(geomData);

            frow.setValue("SysTime", now().toString());
            frow.setValue("Lat", (float) loc.getLatitude());
            frow.setValue("Lon", (float) loc.getLongitude());
            frow.setValue("Alt", (float) loc.getAltitude());
            frow.setValue("Provider", loc.getProvider());
            frow.setValue("GPSTime", loc.getTime());
            frow.setValue("FixSatCount", loc.getExtras().getInt("satellites"));
            frow.setValue("HasRadialAccuracy", loc.hasAccuracy());
            frow.setValue("HasVerticalAccuracy", loc.hasVerticalAccuracy());
            frow.setValue("RadialAccuracy", loc.getAccuracy());
            frow.setValue("VerticalAccuracy", loc.getVerticalAccuracyMeters());
            frow.setValue("data_dump", loc.toString());

            featDao.insert(frow);


            // link location geometry row to associated sat records
            GeoPackageCoreConnection db = GPSgpkg.getDatabase();
            String[] dummy = {};
            int locid = db.max(extTblName, "id", "id > 0", dummy);

            // link point records to associated rsat records
            AttributesDao mapDao = GPSgpkg.getAttributesDao(mapTblName);

            HashMap<String, Integer> rows = (HashMap<String, Integer>) SatRowsToMap.clone();
            SatRowsToMap.clear();

            for (String skey : rows.keySet()) {
                int sid = rows.get(skey);
                AttributesRow maprow = mapDao.newRow();
                maprow.setValue("base_id", locid);
                maprow.setValue("related_id", sid);
                mapDao.insert(maprow);
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
                        " WHERE table_name = '" + PtsTable + "';";
                GPSgpkg.execSQL(bbsql);
            }

//            String phoneNumber = "";
//
//            SmsManager smsMgr = SmsManager.getDefault();
//            smsMgr.sendTextMessage(phoneNumber, null, txt, null, null);
        }
    };



//////////////////////////////////////////////////////////////////////////////////////
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
                            Manifest.permission.SEND_SMS
        };
        ActivityCompat.requestPermissions(this, perms, 1);

        String fileTime = now().toString();
        fileTime = fileTime.replace('/', '-');
        fileTime = fileTime.replace(':', '-');
        fileTime = fileTime.replace("-", "");


        GpkgFilename = GpkgFolder + "/" + GpkgFilename + fileTime + ".gpkg";
//        File dbFile = new File(this.getFilesDir(), GpkgFilename);

        GpkgManager = GeoPackageFactory.getManager(this);
        if (! GpkgManager.exists(GpkgFilename)) {
            GpkgManager.create(GpkgFilename);

        }

        GPSgpkg = GpkgManager.open(GpkgFilename, true);


        if (GPSgpkg == null) {
            AlertDialog.Builder dlgBuilder = new AlertDialog.Builder(this);

            // TODO: handle this for real
            dlgBuilder.setMessage("GPSgpkg is null")
                    .setTitle("Oops!");

            dlgBuilder.show();
        } else {
            GPSgpkg.createGeometryColumnsTable();
            GPSgpkg.createExtensionsTable();
            GPSgpkg.createExtendedRelationsTable();

            GeometryColumns gcol = new GeometryColumns();
            gcol.setId(new TableColumnKey(PtsTable, "geom"));
            gcol.setGeometryType(GeometryType.POINT);
            gcol.setZ((byte) 0);
            gcol.setM((byte) 0);

            List<FeatureColumn> tblcols = new LinkedList<>();
//            tblcols.add(FeatureColumn.createPrimaryKeyColumn(0,"id"));
            tblcols.add(FeatureColumn.createColumn(2, "SysTime", GeoPackageDataType.DATETIME, false, null));
            tblcols.add(FeatureColumn.createColumn(3, "Lat", GeoPackageDataType.FLOAT, false, null));
            tblcols.add(FeatureColumn.createColumn(4, "Lon", GeoPackageDataType.FLOAT, false, null));
            tblcols.add(FeatureColumn.createColumn(5, "Alt", GeoPackageDataType.FLOAT, false, null));
            tblcols.add(FeatureColumn.createColumn(6, "Provider", GeoPackageDataType.TEXT, false, null));
            tblcols.add(FeatureColumn.createColumn(7, "GPSTime", GeoPackageDataType.INTEGER, false, null));
            tblcols.add(FeatureColumn.createColumn(8, "FixSatCount", GeoPackageDataType.INTEGER, false, null));
            tblcols.add(FeatureColumn.createColumn(9, "HasRadialAccuracy", GeoPackageDataType.BOOLEAN, false, null));
            tblcols.add(FeatureColumn.createColumn(10, "HasVerticalAccuracy", GeoPackageDataType.BOOLEAN, false, null));
            tblcols.add(FeatureColumn.createColumn(11, "RadialAccuracy", GeoPackageDataType.FLOAT, false, null));
            tblcols.add(FeatureColumn.createColumn(12, "VerticalAccuracy", GeoPackageDataType.FLOAT, false, null));
            tblcols.add(FeatureColumn.createColumn(13, "data_dump", GeoPackageDataType.TEXT, false, null));

//            tblcols.add(FeatureColumn.createGeometryColumn(13, "geom", GeometryType.POINT, false, null));


            GPSgpkg.createFeatureTableWithMetadata(gcol, tblcols,
                    new BoundingBox(180.0, 90.0, -180.0, -90.0),
                    WGS84_SRS);

            ExtendedRelationsDao extrelDao = GPSgpkg.getExtendedRelationsDao();
            ExtendedRelation extrel = new ExtendedRelation();
            extrel.setBaseTableName(PtsTable);
            extrel.setBasePrimaryColumn("id");
            extrel.setMappingTableName(mapTblName);
            extrel.setRelatedTableName(extTblName);
            extrel.setRelatedPrimaryColumn("id");
            extrel.setRelationName("simple_attributes");

            extrel = new ExtendedRelation();
            extrel.setBaseTableName(extTblName);
            extrel.setBasePrimaryColumn("id");
            extrel.setMappingTableName(clkmapTblName);
            extrel.setRelatedTableName(clkTblName);
            extrel.setRelatedPrimaryColumn("id");
            extrel.setRelationName("simple_attributes");

            GeoPackageCoreConnection db = GPSgpkg.getDatabase();

            // Related Tables Extension setup
            String sql = "INSERT INTO gpkg_contents " +
                    "(table_name, data_type, identifier) " +
                    "VALUES ('" + extTblName + "', 'attributes', '" + extTblName + "');";
            db.execSQL(sql);


            sql = "INSERT INTO gpkg_extensions " +
                    "(table_name, extension_name, definition, scope) " +
                    "VALUES ('gpkgext_relations', 'related_tables', 'TBD', 'read-write');";
            db.execSQL(sql);


            sql = "CREATE TABLE " + extTblName + " (" +
                    " id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    " svid INTEGER, " +
                    " constellation TEXT, " +
                    " cn0 REAL, " +
                    " agc REAL, " +
                    " in_fix BOOLEAN, " +

                    " sync_state_flags INTEGER, " +
                    " sync_state_txt TEXT, " +
                    " sat_time_nanos REAL, " +
                    " sat_time_1sigma_nanos REAL, " +
                    " rcvr_time_offset_nanos REAL, " +
                    " multipath INTEGER, " +

                    " has_carrier_freq BOOLEAN, " +
                    " carrier_freq_hz REAL, " +
                    " accum_delta_range REAL, " +
                    " accum_delta_range_1sigma REAL, " +
                    " accum_delta_range_state_flags INTEGER, " +
                    " accum_delta_range_state_txt TEXT, " +
                    " pseudorange_rate_mps REAL, " +
                    " pseudorange_rate_1sigma REAL, " +

                    " has_ephemeris BOOLEAN, " +
                    " has_almanac BOOLEAN, " +
                    " azimuth_deg REAL, " +
                    " elevation_deg REAL, " +

                    " data_dump TEXT " +
                    ");";
            db.execSQL(sql);

            sql = "INSERT INTO gpkg_contents " +
                    "(table_name, data_type, identifier) " +
                    "VALUES ('" + clkTblName + "', 'attributes', '" + clkTblName + "');";
            db.execSQL(sql);

            sql = "CREATE TABLE " + clkTblName + " (" +
                    " id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    " time_nanos INTEGER," +
                    " time_uncertainty_nanos REAL, " +
                    " bias_nanos REAL, " +
                    " bias_uncertainty_nanos REAL," +
                    " full_bias_nanos REAL," +
                    " drift_nanos_per_sec REAL," +
                    " drift_uncertainty_nps REAL," +
                    " hw_clock_discontinuity_count INTEGER," +
                    " leap_second INTEGER," +
                    " has_bias_nanos BOOLEAN," +
                    " has_bias_uncertainty BOOLEAN," +
                    " has_full_bias_nanos BOOLEAN," +
                    " has_leap_second BOOLEAN," +
                    " has_time_uncertainty BOOLEAN," +
                    " data_dump TEXT " +
                    ");";
            db.execSQL(sql);


            sql = "CREATE TABLE " + mapTblName + " (" +
                    " base_id INTEGER NOT NULL, " +
                    " related_id INTEGER NOT NULL " +
                    ");";
            db.execSQL(sql);

            sql = "CREATE TABLE " + clkmapTblName + " (" +
                    " base_id INTEGER NOT NULL, " +
                    " related_id INTEGER NOT NULL " +
                    ");";
            db.execSQL(sql);

            sql = "INSERT INTO gpkg_contents " +
                    "(table_name, data_type, identifier) " +
                    "VALUES ('" + mapTblName + "', 'attributes', '" + mapTblName + "');";
            db.execSQL(sql);

            sql = "INSERT INTO gpkgext_relations " +
                    "(base_table_name, base_primary_column, related_table_name, related_primary_column, relation_name, mapping_table_name) " +
                    "VALUES ('" +
                    PtsTable + "', 'id', '" + extTblName + "', 'id', " +
                    "'simple_attributes', '" + mapTblName + "');";
            db.execSQL(sql);

            sql = "INSERT INTO gpkg_contents " +
                    "(table_name, data_type, identifier) " +
                    "VALUES ('" + clkmapTblName + "', 'attributes', '" + clkmapTblName + "');";
            db.execSQL(sql);

            sql = "INSERT INTO gpkgext_relations " +
                    "(base_table_name, base_primary_column, related_table_name, related_primary_column, relation_name, mapping_table_name) " +
                    "VALUES ('" +
                    extTblName + "', 'id', '" + clkTblName + "', 'id', " +
                    "'simple_attributes', '" + clkmapTblName + "');";
            db.execSQL(sql);

            List<String> tbls = GPSgpkg.getTables();
            tbls.add(GPSgpkg.getApplicationId());

            String dlgMsg = "SignalMonitor: " + String.join(" - ", tbls);

            Toolbar main_toolbar = findViewById(R.id.main_toolbar);
            main_toolbar.setTitle(dlgMsg);

        }
        GPSgpkg.close();

        GPSgpkg = GpkgManager.open(GpkgFilename, true);

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            stat_tv.setText("Location access denied.");
        } else {
            LocationManager locMgr = getSystemService(LocationManager.class);
            locMgr.registerGnssMeasurementsCallback(MeasurementListener);
            locMgr.registerGnssStatusCallback(StatusListener);
            Criteria crit = new Criteria();
            locMgr.requestLocationUpdates(locMgr.getBestProvider(crit, true), (long) 1000, (float) 0.0, locListener);
        }

    }
//////////////////////////////////////////////////////////////////////////////////////





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
