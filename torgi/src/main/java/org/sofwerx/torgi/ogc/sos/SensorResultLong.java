package org.sofwerx.torgi.ogc.sos;

import org.json.JSONException;
import org.json.JSONObject;

public class SensorResultLong extends AbstractSensorResult {
    private long value;

    public SensorResultLong(String label, long value) {
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