package org.sofwerx.torgi.ogc.sos;

import org.json.JSONException;
import org.json.JSONObject;

public class SensorResultDouble  extends AbstractSensorResult {
    private double value;

    public SensorResultDouble(String label, double value) {
        super(label);
        this.value = value;
    }

    public double getValue() { return value; }

    @Override
    public void addPropertyToObject(JSONObject obj) throws JSONException {
        if (obj == null)
            return;
        obj.put(label,value);
    }
}