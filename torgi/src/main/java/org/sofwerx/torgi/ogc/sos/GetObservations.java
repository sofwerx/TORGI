package org.sofwerx.torgi.ogc.sos;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sofwerx.torgi.ogc.SOSHelper;

public class GetObservations extends AbstractOperation {
    protected long startTime = Long.MIN_VALUE;
    protected long stopTime = Long.MAX_VALUE;

    public GetObservations() {
        super();
    }

    public GetObservations(JSONObject obj) {
        super(obj);
        if (obj != null) {
            startTime = System.currentTimeMillis() - DEFAULT_TIMESPAN;
            stopTime = System.currentTimeMillis();
            JSONObject temporalFilter = obj.optJSONObject("temporalFilter");
            if (temporalFilter != null) {
                JSONObject during = obj.optJSONObject("during");
                if (during != null) {
                    if ("om:phenomenonTime".equalsIgnoreCase(obj.optString("ref",null))) {
                        JSONArray span = obj.optJSONArray("value");
                        if ((span != null) && (span.length() == 2)) {
                            try {
                                startTime = SOSHelper.parseTime(span.getString(0));
                                stopTime = SOSHelper.parseTime(span.getString(1));
                                if (stopTime < startTime) {
                                    long temp = startTime;
                                    startTime = stopTime;
                                    stopTime = temp;
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        }
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public void setStopTime(long stopTime) {
        this.stopTime = stopTime;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getStopTime() {
        return stopTime;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("request","GetObservation");
            obj.put("service","SOS");
            obj.put("version","2.0.0");
            //TODO add procedure
            //TODO add offering
            //TODO add observedProperty
            //TODO add featureOfInterest
            if ((startTime != Long.MIN_VALUE) && (stopTime != Long.MAX_VALUE)) {
                JSONObject temporalFilter = new JSONObject();
                JSONObject during = new JSONObject();
                during.put("ref","om:phenomenonTime");
                JSONArray values = new JSONArray();
                values.put(SOSHelper.formatTime(startTime));
                values.put(SOSHelper.formatTime(stopTime));
                during.put("value",values);
                temporalFilter.put("during",during);
                obj.put("temporalFilter",temporalFilter);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return obj;
    }
}
