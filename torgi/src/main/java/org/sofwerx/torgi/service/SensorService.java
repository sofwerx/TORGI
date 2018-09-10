package org.sofwerx.torgi.service;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import org.sofwerx.torgi.listener.SensorListener;

import static android.content.Context.SENSOR_SERVICE;

public class SensorService implements SensorEventListener {
    private final static String TAG = "TORGI.SensorSvc";

    private SensorManager sensorManager = null;
    private SensorListener listener = null;
    private long lastUpdate = Long.MIN_VALUE;

    private final static int SAMPLING_PERIOD_US = 1000000 * 2; //desired sensor report rate in microseconds
    private final static int MAX_LATENCY_US = 1000000 * 4; //max delay between sensor reports in microseconds
    private final static long MAX_REPORT_RATE = 1000l; //fastest that the listener will be provided updates in seconds

    public SensorService(Context context, SensorListener listener) {
        if (context != null) {
            this.listener = listener;
            sensorManager = (SensorManager) context.getApplicationContext().getSystemService(SENSOR_SERVICE);
            sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED), SAMPLING_PERIOD_US, MAX_LATENCY_US);
        }
    }

    public void setListener(SensorListener listener) {
        this.listener = listener;
    }

    public void shutdown() {
        listener = null;
        if (sensorManager != null)
            sensorManager.unregisterListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED));
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if ((listener != null) && (System.currentTimeMillis() > lastUpdate + MAX_REPORT_RATE)) {
            listener.onSensorUpdated(event);
            lastUpdate = System.currentTimeMillis();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        //ignore
    }
}
