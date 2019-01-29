package org.sofwerx.torgi.ogc.sos;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;

public abstract class AbstractSensorResult {
    protected String label;
    protected AbstractSensorResult(String label) { this.label = label; }
    public String getLabel() { return label; }
    public abstract void addPropertyToObject(JSONObject obj) throws JSONException;
    public static ArrayList<AbstractSensorResult> getProperties(JSONObject obj) throws JSONException {
        if (obj == null)
            return null;

        ArrayList<AbstractSensorResult> results = null;
        Iterator<String> keys = obj.keys();
        String key;
        while(keys.hasNext()) {
            key = keys.next();
            AbstractSensorResult result = null;
            Object value = obj.get(key);
            if (value instanceof Double)
                result = new SensorResultDouble(key,obj.getDouble(key));
            else if (value instanceof String)
                result = new SensorResultString(key,obj.getString(key));
            //TODO add other property data types if needed
            if (result != null) {
                if (results == null)
                    results = new ArrayList<>();
                results.add(result);
            }
        }
        return results;
    }
}
