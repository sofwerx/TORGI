package org.sofwerx.torgi.ogc.sosv2;

import org.json.JSONException;
import org.json.JSONObject;

public class SensorResultString  extends AbstractSensorResult {
    private String value;

    public SensorResultString(String label, String value) {
        super(label);
        this.value = value;
    }

    public String getValue() { return value; }

    @Override
    public void addPropertyToObject(JSONObject obj) throws JSONException {
        if (obj == null)
            return;
        obj.put(label,value);
    }
}
