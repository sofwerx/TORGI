package org.sofwerx.torgi.ui;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.GnssMeasurementsEvent;
import android.location.GnssStatus;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

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
import org.osmdroid.views.overlay.Polygon;
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay;
import org.sofwerx.torgi.Config;
import org.sofwerx.torgi.R;
import org.sofwerx.torgi.gnss.Constellation;
import org.sofwerx.torgi.gnss.DataPoint;
import org.sofwerx.torgi.gnss.EWIndicators;
import org.sofwerx.torgi.gnss.GNSSEWValues;
import org.sofwerx.torgi.gnss.LatLng;
import org.sofwerx.torgi.gnss.SatMeasurement;
import org.sofwerx.torgi.listener.GnssMeasurementListener;
import org.sofwerx.torgi.ogc.SOSHelper;
import org.sofwerx.torgi.service.TorgiService;
import org.sofwerx.torgi.util.PackageUtil;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

public class MainActivity extends AbstractTORGIActivity implements GnssMeasurementListener {
    private final static long MAX_CHART_UPDATE_RATE = 500l;
    private long lastChartUpdate = Long.MIN_VALUE;
    private float chartIndex = 0f;
    private final static String PREF_LAT = "lat";
    private final static String PREF_LNG = "lng";
    private final static String PREF_DUAL_APP_ASK = "dual";
    private final static SimpleDateFormat fmtTime = new SimpleDateFormat("HH:mm:ss");
    private final static DecimalFormat fmtAccuracy = new DecimalFormat("#.##");
    private org.osmdroid.views.MapView osmMap = null;
    private org.osmdroid.views.overlay.Marker currentOSM = null;
    private org.osmdroid.views.overlay.Polyline historyPolylineOSM = null;
    private LatLng current = null;
    private CombinedChart chartEW = null;
    private CombinedData chartEWData = null;
    private CombinedChart chartIAW = null;
    private CombinedData chartIAWData = null;
    private TextView textOverview,textConstellations,textLive;
    private GNSSStatusView ewWarningView;
    private HeatmapOverlay overlayHeatmap = null;

    //Observed GNSS values
    private LineDataSet setCN0 = null;
    private BarDataSet setAGC = null;

    // likelihood of EW values
    private LineDataSet setRFI = null;
    private LineDataSet setCN0AGC = null;
    private LineDataSet setConstellation = null;
    private LineDataSet setFusedSpoof = null;
    private BarDataSet setFused = null;

    private boolean nagAboutDualInstalls = true;

    private boolean live = true;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Config.getInstance(this).setProcessEWonboard(true);
        Toolbar toolbar = findViewById(R.id.mainToolbar);
        setActionBar(toolbar);
        textOverview = findViewById(R.id.monitorTextOverview);
        textConstellations = findViewById(R.id.monitorConstellationCount);
        textLive = findViewById(R.id.mainLiveIndicator);
        textLive.setOnClickListener(v -> updateLive(!live));
        ewWarningView = findViewById(R.id.mainEWStatusView);
        ((CombinedChart)findViewById(R.id.chartIAW)).setNoDataText(getString(R.string.waiting_baseline));

        osmMapSetup();
    }

    private void updateLive(boolean live) {
        if (this.live != live) {
            //TODO this.live = live;
            live = true; //TODO
            if (live) {
                textLive.setText(getString(R.string.live));
                textLive.setTextColor(getColor(R.color.brightgreen));
                textLive.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_satellite,0,0,0);
                Toast.makeText(this, "This will eventually toggle between viewing realtime and recorded.", Toast.LENGTH_SHORT).show();

                //TODO temp for testing
                Log.d(TAG, SOSHelper.getCapabilities());
                //if (serviceBound)
                //    torgiService.getHistory();

            } else {
                textLive.setText(getString(R.string.recorded));
                textLive.setTextColor(getColor(R.color.brightred));
                textLive.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_history,0,0,0);
            }
        }
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
            rightAxis.setAxisMinimum(-5f);
            rightAxis.setAxisMaximum(5f);
            rightAxis.setTextColor(getColor(R.color.agc));
            rightAxis.setDrawLabels(true);
            rightAxis.setValueFormatter((value, axis) -> (int) value + " dB");

            YAxis leftAxis = chartEW.getAxisLeft(); //CNO
            leftAxis.setDrawGridLines(false);
            leftAxis.setAxisMinimum(10f);
            leftAxis.setAxisMaximum(40f);
            leftAxis.setTextColor(Color.rgb(255, 150, 150));
            leftAxis.setValueFormatter((value, axis) -> (int) value + " dB-Hz");

            XAxis xAxis = chartEW.getXAxis();
            xAxis.setPosition(XAxis.XAxisPosition.BOTH_SIDED);

            chartEWData = new CombinedData();

            chartEWData.setData(generateEWLineData(entriesCNO));
            chartEWData.setData(generateEWBarData(entriesAGC));

            chartEW.setData(chartEWData);
        }
    }

    private LineData generateEWLineData(ArrayList<Entry> entriesCNO) {
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

    private BarData generateEWBarData(ArrayList<BarEntry> entriesAGC) {
        BarData d = new BarData();

        setAGC = new BarDataSet(entriesAGC, "Avg AGC");
        setAGC.setColor(getColor(R.color.agc));
        setAGC.setDrawIcons(false);
        setAGC.setDrawValues(false);
        setAGC.setAxisDependency(YAxis.AxisDependency.RIGHT);

        d.addDataSet(setAGC);
        return d;
    }

    private void setupIAWchart(Entry entryRFI, Entry entryCN0AGC, Entry entryConstellation, Entry entryFusedSpoof, BarEntry entryFused) {
        if ((chartIAW == null) && (entryRFI != null) && (entryCN0AGC != null) && (entryConstellation != null) && (entryFusedSpoof != null)  && (entryFused != null)) {
            ArrayList<Entry> entriesRFI = new ArrayList<>();
            ArrayList<Entry> entriesCN0AGC = new ArrayList<>();
            ArrayList<Entry> entriesConstellation = new ArrayList<>();
            ArrayList<Entry> entriesFusedSpoof = new ArrayList<>();
            ArrayList<BarEntry> entriesFused = new ArrayList<>();
            entriesRFI.add(entryRFI);
            entriesCN0AGC.add(entryCN0AGC);
            entriesConstellation.add(entryConstellation);
            entriesFusedSpoof.add(entryFusedSpoof);
            entriesFused.add(entryFused);

            chartIAW = findViewById(R.id.chartIAW);
            chartIAW.getDescription().setEnabled(false);
            chartIAW.setBackgroundColor(Color.BLACK);
            chartIAW.setDrawGridBackground(false);
            chartIAW.setDrawBarShadow(false);
            chartIAW.setHighlightFullBarEnabled(false);
            chartIAW.setPinchZoom(false);
            chartIAW.setDrawOrder(new CombinedChart.DrawOrder[]{CombinedChart.DrawOrder.LINE,CombinedChart.DrawOrder.BAR});

            Legend l = chartIAW.getLegend();
            l.setWordWrapEnabled(true);
            l.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
            l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
            l.setOrientation(Legend.LegendOrientation.HORIZONTAL);
            l.setDrawInside(false);
            l.setTextColor(Color.WHITE);

            YAxis leftAxis = chartIAW.getAxisLeft();
            leftAxis.setDrawGridLines(false);
            leftAxis.setAxisMinimum(0f);
            leftAxis.setAxisMaximum(1f);
            leftAxis.setTextColor(Color.WHITE);
            leftAxis.setValueFormatter((value, axis) -> (int)(value*100f)+"%");

            XAxis xAxis = chartIAW.getXAxis();
            xAxis.setPosition(XAxis.XAxisPosition.BOTH_SIDED);

            chartIAWData = new CombinedData();
            generateIAWRFILineData(entriesRFI);
            generateIAWCN0AGCLineData(entriesCN0AGC);
            generateIAWConstellationLineData(entriesConstellation);
            generateIAWFusedSpoofLineData(entriesFusedSpoof);
            generateIAWBarData(entriesFused);
            LineData dataSets = new LineData();
            dataSets.addDataSet(setRFI);
            dataSets.addDataSet(setCN0AGC);
            dataSets.addDataSet(setConstellation);
            dataSets.addDataSet(setFusedSpoof);
            chartIAWData.setData(dataSets);
            chartIAWData.setData(generateIAWBarData(entriesFused));
            chartIAW.setData(chartIAWData);
        }
    }

    private void generateIAWCN0AGCLineData(ArrayList<Entry> entriesCN0AGC) {
        setCN0AGC = new LineDataSet(entriesCN0AGC, "C/N₀ vs AGC");
        setCN0AGC.setColor(getColor(R.color.cn0agc));
        setCN0AGC.setLineWidth(2.5f);
        setCN0AGC.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        setCN0AGC.setDrawValues(false);
        setCN0AGC.setDrawCircles(false);
        setCN0AGC.setAxisDependency(YAxis.AxisDependency.LEFT);
    }

    private void generateIAWConstellationLineData(ArrayList<Entry> entriesConstellation) {
        setConstellation = new LineDataSet(entriesConstellation, "Δ Constellation");
        setConstellation.setColor(getColor(R.color.constellation));
        setConstellation.setLineWidth(2.5f);
        setConstellation.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        setConstellation.setDrawValues(false);
        setConstellation.setDrawCircles(false);
        setConstellation.setAxisDependency(YAxis.AxisDependency.LEFT);
    }

    private void generateIAWFusedSpoofLineData(ArrayList<Entry> entriesFusedSpoof) {
        setFusedSpoof = new LineDataSet(entriesFusedSpoof, "Σ Spoof");
        setFusedSpoof.setColor(getColor(R.color.fusedSpoof));
        setFusedSpoof.setLineWidth(2.5f);
        setFusedSpoof.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        setFusedSpoof.setDrawValues(false);
        setFusedSpoof.setDrawCircles(false);
        setFusedSpoof.setAxisDependency(YAxis.AxisDependency.LEFT);
    }

    private void generateIAWRFILineData(ArrayList<Entry> entriesRFI) {
        setRFI = new LineDataSet(entriesRFI, "RFI");
        setRFI.setColor(getColor(R.color.rfi));
        setRFI.setLineWidth(2.5f);
        setRFI.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        setRFI.setDrawValues(false);
        setRFI.setDrawCircles(false);
        setRFI.setAxisDependency(YAxis.AxisDependency.LEFT);
    }

    private BarData generateIAWBarData(ArrayList<BarEntry> entriesFused) {
        BarData d = new BarData();

        setFused = new BarDataSet(entriesFused, "Total EW Risk");
        setFused.setColor(getColor(R.color.fused));
        setFused.setDrawIcons(false);
        setFused.setDrawValues(false);
        setFused.setAxisDependency(YAxis.AxisDependency.LEFT);

        d.addDataSet(setFused);
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
        overlayHeatmap = new HeatmapOverlay(osmMap);
        //osmMap.setTileSource(TileSourceFactory.USGS_SAT);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
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
        if (nagAboutDualInstalls && askAboutDualApps()) {
            nagAboutDualInstalls = false;
            if (PackageUtil.isSensorAppInstalled(this)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.alert_dual_install_title);
                builder.setMessage(R.string.alert_dual_install_narrative);
                builder.setPositiveButton(R.string.alert_dual_install_uninstall, (dialog, which) -> {
                    Uri packageUri = Uri.parse("package:" + PackageUtil.PACKAGE_LOGGER);
                    try {
                        startActivity(new Intent(Intent.ACTION_UNINSTALL_PACKAGE, packageUri));
                    } catch (ActivityNotFoundException ignore) {
                    }
                });
                builder.setOnDismissListener(dialog -> setDontAskAboutDualApps());
                final AlertDialog dialog = builder.create();
                dialog.setCanceledOnTouchOutside(false);
                dialog.show();
            }
        }
        overlayHeatmap.initOverlay();
    }

    @Override
    public void onPause() {
        super.onPause();
        saveLastLocation();
    }

    private void drawMarker(LatLng pos, String info) {
        if (pos != null) {
            ArrayList<LatLng> history = torgiService.getLocationHistory();
            if ((history != null) && (history.size() > 1)) {
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
                    if (historyPolylineOSM.getPoints().size() > TorgiService.MAX_HISTORY_LENGTH)
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

    private boolean askAboutDualApps() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        return prefs.getBoolean(PREF_DUAL_APP_ASK,true);
    }

    private void setDontAskAboutDualApps() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor edit = prefs.edit();
        edit.putBoolean(PREF_DUAL_APP_ASK,false);
        edit.apply();
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
        //ignore
    }

    @Override
    public void onEWDataProcessed(DataPoint dp,EWIndicators indicators) {
        if ((dp != null) && (System.currentTimeMillis() > lastChartUpdate + MAX_CHART_UPDATE_RATE)) {
            lastChartUpdate = System.currentTimeMillis();
            int constellations = 0;
            ArrayList<SatMeasurement> measurements = dp.getMeasurements();
            if (measurements != null) {
                boolean[] constPresent = new boolean[Constellation.size()];
                for (SatMeasurement measurement:measurements) {
                    if ((measurement.getSat() != null) && (measurement.getSat().getConstellation() != null)) {
                        constPresent[measurement.getSat().getConstellation().getValue()] = true;
                    }
                }
                for (boolean constellation : constPresent) {
                    if (constellation)
                        constellations++;
                }
            }
            final int constCount = constellations;
            runOnUiThread(() -> {
                if (constCount == 0)
                    textConstellations.setVisibility(View.INVISIBLE);
                else {
                    textConstellations.setText(constCount+" Constellation"+((constCount == 1)?"":"s"));
                    textConstellations.setVisibility(View.VISIBLE);
                }
            });
            GNSSEWValues avg = dp.getAverageMeasurements();
            if (avg != null)
                addEWChartEntry(dp.getSpaceTime().getTime(), avg);
            if (indicators != null) {
                addIAWChartEntry(dp.getSpaceTime().getTime(), indicators);
                final int risk = (int)(100f*indicators.getFusedEWRisk());
                runOnUiThread(() -> ewWarningView.setWarnPercent(risk));
            }
        }
    }

    @Override
    public void onGnssMeasurementReceived(GnssMeasurementsEvent event) {
        //ignoring the unprocessed data
    }

    private void addEWChartEntry(final long time, final GNSSEWValues values) {
        if ((time > 0l) && (values != null)) {
            runOnUiThread(() -> {
                Log.d(TAG,"Chart update #"+(int)chartIndex);
                Entry entryCN0 = null;
                BarEntry entryAGC = null;
                boolean updatedAGC = false;
                boolean updatedCN0 = false;
                if (!Double.isNaN(values.getAgc())) {
                    entryAGC = new BarEntry(chartIndex, (float) values.getAgc());
                    if (setAGC != null) {
                        setAGC.addEntry(entryAGC);
                        if (setAGC.getValues().size() > TorgiService.MAX_HISTORY_LENGTH)
                            setAGC.removeFirst();
                        setAGC.notifyDataSetChanged();
                    }
                    updatedAGC = true;
                }
                if (!Float.isNaN(values.getCn0())) {
                    entryCN0 = new Entry(chartIndex,values.getCn0());
                    if (setCN0 != null) {
                        setCN0.addEntry(entryCN0);
                        if (setCN0.getValues().size() > TorgiService.MAX_HISTORY_LENGTH)
                            setCN0.removeFirst();
                        setCN0.notifyDataSetChanged();
                    }
                    updatedCN0 = true;
                }
                if ((chartEW == null) && updatedAGC && updatedCN0)
                    setupEWchart(entryCN0,entryAGC);
                if (updatedAGC || updatedCN0) {
                    chartEWData.notifyDataChanged();
                    chartEW.notifyDataSetChanged();
                    chartEW.invalidate();
                    chartIndex += 1f;
                }
            });
        }
    }

    private void addIAWChartEntry(final long time, final EWIndicators indicators) {
        if ((time > 0l) && (indicators != null)) {
            runOnUiThread(() -> {
                Log.d(TAG,"Chart update #"+(int)chartIndex);
                Entry entryRFI = null;
                Entry entryCN0AGC = null;
                Entry entryConstellation = null;
                Entry entryFusedSpoof = null;
                BarEntry entryFused = null;
                boolean updatedRFI = false;
                boolean updatedCN0AGC = false;
                boolean updatedConstellation = false;
                boolean updatedFusedSpoof = false;
                boolean updatedFused = false;
                if (!Float.isNaN(indicators.getFusedEWRisk())) {
                    entryFused = new BarEntry(chartIndex, indicators.getFusedEWRisk());
                    if (setFused != null) {
                        setFused.addEntry(entryFused);
                        if (setFused.getValues().size() > TorgiService.MAX_HISTORY_LENGTH)
                            setFused.removeFirst();
                        setFused.notifyDataSetChanged();
                    }
                    updatedFused = true;
                }
                if (!Float.isNaN(indicators.getLikelihoodRFI())) {
                    entryRFI = new Entry(chartIndex,indicators.getLikelihoodRFI());
                    if (setRFI != null) {
                        setRFI.addEntry(entryRFI);
                        if (setRFI.getValues().size() > TorgiService.MAX_HISTORY_LENGTH)
                            setRFI.removeFirst();
                        setRFI.notifyDataSetChanged();
                    }
                    updatedRFI = true;
                }
                if (!Float.isNaN(indicators.getLikelihoodRSpoofCN0vsAGC())) {
                    entryCN0AGC = new Entry(chartIndex,indicators.getLikelihoodRSpoofCN0vsAGC());
                    if (setCN0AGC != null) {
                        setCN0AGC.addEntry(entryCN0AGC);
                        if (setCN0AGC.getValues().size() > TorgiService.MAX_HISTORY_LENGTH)
                            setCN0AGC.removeFirst();
                        setCN0AGC.notifyDataSetChanged();
                    }
                    updatedCN0AGC = true;
                }
                if (!Float.isNaN(indicators.getLikelihoodRSpoofConstellationDisparity())) {
                    entryConstellation = new Entry(chartIndex,indicators.getLikelihoodRSpoofConstellationDisparity());
                    if (setConstellation != null) {
                        setConstellation.addEntry(entryConstellation);
                        if (setConstellation.getValues().size() > TorgiService.MAX_HISTORY_LENGTH)
                            setConstellation.removeFirst();
                        setConstellation.notifyDataSetChanged();
                    }
                    updatedConstellation = true;
                }
                if (!Float.isNaN(indicators.getFusedLikelihoodOfSpoofing())) {
                    entryFusedSpoof = new Entry(chartIndex,indicators.getFusedLikelihoodOfSpoofing());
                    if (setFusedSpoof != null) {
                        setFusedSpoof.addEntry(entryFusedSpoof);
                        if (setFusedSpoof.getValues().size() > TorgiService.MAX_HISTORY_LENGTH)
                            setFusedSpoof.removeFirst();
                        setFusedSpoof.notifyDataSetChanged();
                    }
                    updatedFusedSpoof = true;
                }
                if ((chartIAW == null) && updatedRFI && updatedCN0AGC && updatedConstellation && updatedFusedSpoof && updatedFused)
                    setupIAWchart(entryRFI,entryCN0AGC,entryConstellation,entryFusedSpoof,entryFused);
                if (updatedRFI || updatedCN0AGC || updatedConstellation || updatedFusedSpoof || updatedFused) {
                    chartIAWData.notifyDataChanged();
                    chartIAW.notifyDataSetChanged();
                    chartIAW.invalidate();
                    chartIndex += 1f;
                }
            });
        }
    }

    @Override
    public void onLocationChanged(final Location loc) {
        runOnUiThread(() -> {
            drawMarker(new LatLng(loc.getLatitude(), loc.getLongitude()),fmtTime.format(loc.getTime())+", ±"+(loc.hasAccuracy()?fmtAccuracy.format(loc.getAccuracy()):"")+"m");
            int sats = loc.getExtras().getInt("satellites");
            StringBuilder label = new StringBuilder();
            if (sats > 0)
                label.append(sats+" satellites");
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
        //ignore
    }

    @Override
    public void onHeatmapChange(final Heatmap heatmap) {
        if (heatmap != null) {
            if (heatmap.getPolygon() != null) {
                runOnUiThread(() -> {
                    heatmap.getPolygon().setFillColor(HeatmapOverlay.getFillColor(heatmap.getRfiRisk()));
                    osmMap.invalidate();
                });
            } else {
                runOnUiThread(() -> {
                    overlayHeatmap.createPolygon(heatmap);
                    osmMap.invalidate();
                });
            }
        }
    }
}