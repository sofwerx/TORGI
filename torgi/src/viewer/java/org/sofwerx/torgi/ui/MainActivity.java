package org.sofwerx.torgi.ui;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.GnssMeasurement;
import android.location.GnssMeasurementsEvent;
import android.location.GnssStatus;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.CombinedChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.CombinedData;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay;
import org.sofwerx.torgi.R;
import org.sofwerx.torgi.gnss.Constellation;
import org.sofwerx.torgi.gnss.DataPoint;
import org.sofwerx.torgi.gnss.GNSSEWValues;
import org.sofwerx.torgi.gnss.LatLng;
import org.sofwerx.torgi.gnss.SatMeasurement;
import org.sofwerx.torgi.gnss.Satellite;
import org.sofwerx.torgi.gnss.SpaceTime;
import org.sofwerx.torgi.listener.GnssMeasurementListener;
import org.sofwerx.torgi.service.TorgiService;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;

public class MainActivity extends AbstractTORGIActivity implements GnssMeasurementListener {
    private final static long MAX_CHART_UPDATE_RATE = 500l;
    private long lastChartUpdate = Long.MIN_VALUE;
    private float chartIndex = 0f;
    private double CENTER_US_LAT = 39.181071d;
    private double CENTER_US_LNG =  -99.938295d;
    private final static String PREF_LAT = "lat";
    private final static String PREF_LNG = "lng";
    private final static int MAX_HISTORY_LENGTH = 50;
    private final static SimpleDateFormat fmtTime = new SimpleDateFormat("HH:mm:ss");
    private final static DecimalFormat fmtAccuracy = new DecimalFormat("#.##");
    private org.osmdroid.views.MapView osmMap = null;
    private org.osmdroid.views.overlay.Marker currentOSM = null;
    private org.osmdroid.views.overlay.Polyline historyPolylineOSM = null;
    private ArrayList<LatLng> history = null;
    private LatLng current = null;
    private CombinedChart chartEW = null;
    private CombinedData chartData = null;
    private TextView textOverview;

    private LineDataSet setCN0 = null;
    private BarDataSet setAGC = null;
    private Location currentLoc = null;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        textOverview = findViewById(R.id.monitorTextOverview);
        osmMapSetup();
    }

    @Override
    protected int getLayout() {
        return R.layout.activity_monitor;
    }

    private void setupEWchart(Entry entryCNO, BarEntry entryAGC) {
        if ((chartEW == null) && (entryCNO != null) && (entryAGC != null)) {
            ArrayList<Entry> entriesCNO = new ArrayList<>();
            ArrayList<BarEntry> entriesAGC = new ArrayList<>();
            entriesCNO.add(entryCNO);
            entriesAGC.add(entryAGC);

            chartEW = findViewById(R.id.chartEW);
            chartEW.getDescription().setEnabled(false);
            chartEW.setBackgroundColor(Color.BLACK);
            chartEW.setDrawGridBackground(false);
            chartEW.setDrawBarShadow(false);
            chartEW.setHighlightFullBarEnabled(false);
            chartEW.setPinchZoom(false);
            // draw bars behind lines
            //chartEW.setDrawOrder(new CombinedChart.DrawOrder[]{CombinedChart.DrawOrder.BAR, CombinedChart.DrawOrder.BUBBLE, CombinedChart.DrawOrder.CANDLE, CombinedChart.DrawOrder.LINE, CombinedChart.DrawOrder.SCATTER});
            chartEW.setDrawOrder(new CombinedChart.DrawOrder[]{CombinedChart.DrawOrder.BAR, CombinedChart.DrawOrder.LINE});

            Legend l = chartEW.getLegend();
            l.setWordWrapEnabled(true);
            l.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
            l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
            l.setOrientation(Legend.LegendOrientation.HORIZONTAL);
            l.setDrawInside(false);
            l.setTextColor(Color.WHITE);

            YAxis rightAxis = chartEW.getAxisRight(); //AGC
            rightAxis.setDrawGridLines(false);
            rightAxis.setAxisMinimum(-1f);
            rightAxis.setAxisMaximum(5f);
            rightAxis.setTextColor(getColor(R.color.agc));
            rightAxis.setDrawLabels(true);
            rightAxis.setValueFormatter((value, axis) -> (int) value + "dB");

            YAxis leftAxis = chartEW.getAxisLeft(); //CNO
            leftAxis.setDrawGridLines(false);
            leftAxis.setAxisMinimum(10f);
            leftAxis.setAxisMaximum(40f);
            leftAxis.setTextColor(Color.rgb(255, 150, 150));
            leftAxis.setValueFormatter((value, axis) -> (int) value + "dB-Hz");

            XAxis xAxis = chartEW.getXAxis();
            xAxis.setPosition(XAxis.XAxisPosition.BOTH_SIDED);
            //xAxis.setAxisMinimum(0f);
            //xAxis.setGranularity(1f);

            chartData = new CombinedData();

            chartData.setData(generateLineData(entriesCNO));
            chartData.setData(generateBarData(entriesAGC));
            //data.setValueTypeface(mTfLight);

            //xAxis.setAxisMaximum(data.getXMax() + 0.25f);

            chartEW.setData(chartData);
            //chartEW.invalidate();
        }
    }

    private LineData generateLineData(ArrayList<Entry> entriesCNO) {
        LineData d = new LineData();

        setCN0 = new LineDataSet(entriesCNO, "Avg C/N₀");
        setCN0.setColor(getColor(R.color.cn0));
        setCN0.setLineWidth(2.5f);
        setCN0.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        setCN0.setDrawValues(false);
        setCN0.setDrawCircles(false);
        //setCN0.setValueTextSize(10f);
        //setCN0.setValueTextColor(getColor(R.color.cn0));
        setCN0.setAxisDependency(YAxis.AxisDependency.LEFT);
        d.addDataSet(setCN0);

        return d;
    }

    private BarData generateBarData(ArrayList<BarEntry> entriesAGC) {
        BarData d = new BarData();

        setAGC = new BarDataSet(entriesAGC, "Avg AGC");
        setAGC.setColor(getColor(R.color.agc));
        setAGC.setDrawIcons(false);
        setAGC.setDrawValues(false);
        //setAGC.setValueTextColor(getColor(R.color.agc));
        //setAGC.setValueTextSize(10f);
        setAGC.setAxisDependency(YAxis.AxisDependency.RIGHT);

        d.addDataSet(setAGC);
        return d;
    }

    private void osmMapSetup() {
        osmMap = findViewById(R.id.maposm);

        RotationGestureOverlay mRotationGestureOverlay = new RotationGestureOverlay(osmMap);
        mRotationGestureOverlay.setEnabled(true);
        osmMap.getOverlays().add(mRotationGestureOverlay);
        osmMap.setBuiltInZoomControls(false);
        osmMap.setMultiTouchControls(true); //needed for pinch zooms
        osmMap.setTilesScaledToDpi(true); //scales tiles to the current screen's DPI, helps with readability of labels
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_settings:
                //TODO
                return true;
            case R.id.action_about:
                startActivity(new Intent(this,AboutActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        saveLastLocation();
    }

    private void drawMarker(LatLng pos, String info) {
        if (pos != null) {
            if (history == null)
                history = new ArrayList<>();
            history.add(pos);
            if (history.size() > 1) {
                if (history.size() > MAX_HISTORY_LENGTH)
                    history.remove(0);

                if (historyPolylineOSM == null) {
                    historyPolylineOSM = new org.osmdroid.views.overlay.Polyline();
                    ArrayList<GeoPoint> list = new ArrayList<>();
                    for (LatLng pt:history) {
                        list.add(new GeoPoint(pt.latitude,pt.longitude));
                    }
                    historyPolylineOSM.setPoints(list);
                    historyPolylineOSM.setColor(Color.YELLOW);
                    osmMap.getOverlays().add(historyPolylineOSM);
                } else {
                    historyPolylineOSM.addPoint(new GeoPoint(pos.latitude, pos.longitude));
                    if (historyPolylineOSM.getPoints().size() > MAX_HISTORY_LENGTH)
                        historyPolylineOSM.getPoints().remove(0);
                }
            }

            if (currentOSM == null) {
                currentOSM = new org.osmdroid.views.overlay.Marker(osmMap);
                currentOSM.setPosition(new GeoPoint(pos.latitude,pos.longitude));
                currentOSM.setAnchor(org.osmdroid.views.overlay.Marker.ANCHOR_CENTER,org.osmdroid.views.overlay.Marker.ANCHOR_CENTER);
                currentOSM.setIcon(getResources().getDrawable(R.drawable.map_icon));
                currentOSM.setTitle("GPS");
                osmMap.getOverlays().add(currentOSM);
                if (osmMap != null) {
                    osmMap.getController().setZoom(18d);
                    osmMap.setExpectedCenter(new GeoPoint(pos.latitude, pos.longitude));
                }
            } else {
                currentOSM.setPosition(new GeoPoint(pos.latitude,pos.longitude));
                osmMap.invalidate();
            }
        }
    }

    private void saveLastLocation() {
        if (current != null) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            SharedPreferences.Editor edit = prefs.edit();
            edit.putFloat(PREF_LAT,(float)current.latitude);
            edit.putFloat(PREF_LNG,(float)current.longitude);
            edit.apply();
        }
    }

    private LatLng getLastLatLng() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        float lat = prefs.getFloat(PREF_LAT,Float.NaN);
        float lng = prefs.getFloat(PREF_LNG,Float.NaN);
        if (!Float.isNaN(lat) && !Float.isNaN(lng))
            return new LatLng(lat,lng);
        else
            return null;
    }

    @Override
    public void onSatStatusUpdated(final GnssStatus status) {
        if (status != null) {
            runOnUiThread(() -> {
                //TODO
            });
        }
    }

    @Override
    public void onGnssMeasurementReceived(GnssMeasurementsEvent event) {
        if (System.currentTimeMillis() > lastChartUpdate + MAX_CHART_UPDATE_RATE) {
            if (event != null) {
                final Collection<GnssMeasurement> measurements = event.getMeasurements();
                if (measurements != null) {
                    lastChartUpdate = System.currentTimeMillis();
                    DataPoint dp = new DataPoint(new SpaceTime(currentLoc));
                    for (GnssMeasurement measurement : measurements) {
                        Satellite sat = new Satellite(Constellation.get(measurement.getConstellationType()), measurement.getSvid());
                        SatMeasurement satMeasurement;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                            satMeasurement = new SatMeasurement(sat, new GNSSEWValues((float) measurement.getCn0DbHz(), measurement.getAutomaticGainControlLevelDb()));
                        else
                            satMeasurement = new SatMeasurement(sat, new GNSSEWValues((float) measurement.getCn0DbHz(), GNSSEWValues.NA));
                        dp.add(satMeasurement);
                    }
                    GNSSEWValues avg = dp.getAverageMeasurements();
                    if (avg != null)
                        addChartEntry(dp.getSpaceTime().getTime(), avg);
                }
            }
        }
    }

    private void addChartEntry(final long time, final GNSSEWValues values) {
        if ((time > 0l) && (values != null)) {
            runOnUiThread(() -> {
                Log.d(TAG,"Chart update #"+(int)chartIndex);
                Entry entryCN0 = null;
                BarEntry entryAGC = null;
                boolean updatedAGC = false;
                boolean updatedCN0 = false;
                if (!Double.isNaN(values.getAgc())) {
                    //entryAGC = new BarEntry((float) time, (float) values.getAgc());
                    entryAGC = new BarEntry(chartIndex, (float) values.getAgc());
                    if (setAGC != null) {
                        setAGC.addEntry(entryAGC);
                        if (setAGC.getValues().size() > MAX_HISTORY_LENGTH)
                            setAGC.removeFirst();
                        setAGC.notifyDataSetChanged();
                    }
                    updatedAGC = true;
                }
                if (!Float.isNaN(values.getCn0())) {
                    //entryCN0 = new Entry((float)time,values.getCn0());
                    entryCN0 = new Entry(chartIndex,values.getCn0());
                    if (setCN0 != null) {
                        setCN0.addEntry(entryCN0);
                        if (setCN0.getValues().size() > MAX_HISTORY_LENGTH)
                            setCN0.removeFirst();
                        setCN0.notifyDataSetChanged();
                    }
                    updatedCN0 = true;
                }
                if ((chartEW == null) && updatedAGC && updatedCN0) {
                    setupEWchart(entryCN0,entryAGC);
                }
                if (updatedAGC || updatedCN0) {
                    chartData.notifyDataChanged();
                    chartEW.notifyDataSetChanged();
                    chartEW.invalidate();
                    chartIndex += 1f;
                }
            });
        }
    }

    @Override
    public void onLocationChanged(final Location loc) {
        runOnUiThread(() -> {
            currentLoc = loc;
            drawMarker(new LatLng(loc.getLatitude(), loc.getLongitude()),fmtTime.format(loc.getTime())+", ±"+(loc.hasAccuracy()?fmtAccuracy.format(loc.getAccuracy()):"")+"m");
            int sats = loc.getExtras().getInt("satellites");
            StringBuffer label = new StringBuffer();
            if (sats > 0)
                label.append(sats+" satellites in fix");
            if (loc.hasAccuracy()) {
                if (sats > 0)
                    label.append(", ");
                label.append("±" + (int)loc.getAccuracy() + "m");
            }
            textOverview.setText(label.toString());
        });
    }

    @Override
    public void onProviderChanged(final String provider, final boolean enabled) {
        runOnUiThread(() -> {
            //TODO
        });
    }
}