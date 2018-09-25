package org.sofwerx.torgi.ogc.sos;

import org.json.JSONException;
import org.json.JSONObject;

public class DescribeSensor extends AbstractOperation {
    public DescribeSensor(){
        super();
    }

    public DescribeSensor(JSONObject obj) {
        super(obj);
    }

    @Override
    public JSONObject toJSON() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("request","DescribeSensor");
            obj.put("service","SOS");
            obj.put("version","2.0.0");
            //TODO add procedure
            //TODO add procedure description format
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return obj;
    }
}
