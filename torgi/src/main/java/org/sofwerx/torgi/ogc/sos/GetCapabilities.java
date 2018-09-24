package org.sofwerx.torgi.ogc.sos;

import org.json.JSONException;
import org.json.JSONObject;

public class GetCapabilities extends AbstractOperation {
    public GetCapabilities() {
        super();
    }

    public GetCapabilities(JSONObject obj) {
        super(obj);
    }

    @Override
    public JSONObject toJSON() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("request","GetCapabilities");
            obj.put("service","SOS");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return obj;
    }
}
