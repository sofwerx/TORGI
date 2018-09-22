package org.sofwerx.torgi.ogc;

import android.content.Context;
import android.location.Location;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sofwerx.torgi.Config;
import org.sofwerx.torgi.gnss.helper.GeoPackageSatDataHelper;

import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

//Built to comply with OGC SOS v2.0, see http://cite.opengeospatial.org/pub/cite/files/edu/sos/text/main.html
public class SOSHelper {
    private final static SimpleDateFormat dateFormatISO8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
    public final static String REQUEST_GET_OBSERVATION = "GetObservation";
    public final static String REQUEST_DESCRIBE_SENSOR = "DescribeSensor";
    public final static String REQUEST_GET_CAPABILITIES = "GetCapabilities";

    public static String getCapabilities() {
        return null; //TODO
    }

    public static String getObservation(ArrayList<GeoPackageSatDataHelper> points) {
        return getObservation(points,Integer.MAX_VALUE);
    }

    public static String getObservation(ArrayList<GeoPackageSatDataHelper> points, int max) {
        JSONObject obj = new JSONObject();

        try {
            obj.put("request","GetObservation");
            obj.put("version","2.0.0");
            obj.put("service","SOS");
            if ((points != null) && !points.isEmpty()) {
                JSONArray obs = new JSONArray();
                int tempMax = points.size();
                if (max < tempMax)
                    tempMax = max;
                for (int i=0;i<tempMax;i++) {
                    obs.put(getObservation(points.get(i)));
                }
                obj.put("observations", obs);
            }
            return obj.toString(2);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return obj.toString();
    }

    private static JSONObject getObservation(GeoPackageSatDataHelper satData) {
        JSONObject obj = new JSONObject();

        try {
            obj.put("type","http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_Measurement");
            String time = formatTime(satData.getMeassuredTime());
            obj.put("phenomenonTime",time);
            obj.put("resultTime",time);
            JSONObject foi = new JSONObject();
            JSONObject foiName = new JSONObject();
            foiName.put("codespace","http://www.opengis.net/def/nil/OGC/0/unknown");
            foiName.put("value", satData.getConstellation().toString()+"-"+satData.getId());
            foi.put("name",foiName);
            obj.put("featureOfInterest",foi);
            JSONObject result = new JSONObject();
            result.put("uom","dB-Hz");
            result.put("value",satData.getCn0());
            obj.put("result",result);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return obj;
    }

    private static String formatTime(long time) {
        return dateFormatISO8601.format(time);
    }

    public static String getDescribeSensor(Context context, Location last) {
        return null; //TODO
    }
}
