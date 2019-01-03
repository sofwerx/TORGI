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
    private final static int[] SENSORS_OF_INTEREST = //Just change this list to change collected sensors
            {Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED,
                    Sensor.TYPE_GRAVITY,
                    Sensor.TYPE_ACCELEROMETER_UNCALIBRATED,
                    Sensor.TYPE_GYROSCOPE_UNCALIBRATED,
                    Sensor.TYPE_LINEAR_ACCELERATION,
                    Sensor.TYPE_PROXIMITY,
                    Sensor.TYPE_AMBIENT_TEMPERATURE,
                    Sensor.TYPE_PRESSURE,
                    Sensor.TYPE_RELATIVE_HUMIDITY,
                    Sensor.TYPE_LIGHT,
                    Sensor.TYPE_MOTION_DETECT,
                    Sensor.TYPE_STATIONARY_DETECT,
                    Sensor.TYPE_ROTATION_VECTOR
            };

    private SensorManager sensorManager = null;
    private SensorListener listener = null;
    private long[] lastUpdate = new long[SENSORS_OF_INTEREST.length];

    private final static int SAMPLING_PERIOD_US = 1000000 * 2; //desired sensor report rate in microseconds
    private final static int MAX_LATENCY_US = 1000000 * 4; //max delay between sensor reports in microseconds
    private final static long MAX_REPORT_RATE = 1000l; //fastest that the listener will be provided updates in seconds

    public SensorService(Context context, SensorListener listener) {
        if (context != null) {
            this.listener = listener;
            sensorManager = (SensorManager) context.getApplicationContext().getSystemService(SENSOR_SERVICE);
            if (SENSORS_OF_INTEREST != null) {
                for (int i=0;i<SENSORS_OF_INTEREST.length;i++) {
                    Sensor defaultSensor = sensorManager.getDefaultSensor(SENSORS_OF_INTEREST[i]);
                    if (defaultSensor != null)
                        sensorManager.registerListener(this,defaultSensor, SAMPLING_PERIOD_US, MAX_LATENCY_US);
                    lastUpdate[i] = Long.MIN_VALUE;
                }
            }
        }
    }

    public void setListener(SensorListener listener) {
        this.listener = listener;
    }

    public void shutdown() {
        listener = null;
        if (sensorManager != null) {
            if (SENSORS_OF_INTEREST != null) {
                for (int sensor : SENSORS_OF_INTEREST) {
                    Sensor defaultSensor = sensorManager.getDefaultSensor(sensor);
                    if (defaultSensor != null)
                        sensorManager.unregisterListener(this, defaultSensor);
                }
            }
        }
    }

    private int getSensorArrayIndex(int type) {
        for (int i = 0;i < SENSORS_OF_INTEREST.length; i++) {
            if (type == SENSORS_OF_INTEREST[i])
                return i;
        }
        return -1;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        int index = getSensorArrayIndex(event.sensor.getType());
        if ((listener != null) && (index > -1) && (System.currentTimeMillis() > lastUpdate[index] + MAX_REPORT_RATE)) {
            listener.onSensorUpdated(event);
            lastUpdate[index] = System.currentTimeMillis();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { /*ignored */ }
}
