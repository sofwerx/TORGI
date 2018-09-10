package org.sofwerx.torgi.listener;

import android.hardware.SensorEvent;

public interface SensorListener {
    void onSensorUpdated(SensorEvent event);
}
