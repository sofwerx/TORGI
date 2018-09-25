package org.sofwerx.torgi.ogc;

import android.content.Context;
import android.location.Location;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sofwerx.torgi.gnss.helper.GeoPackageGPSPtHelper;
import org.sofwerx.torgi.gnss.helper.GeoPackageSatDataHelper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

//Built to comply with OGC SOS v2.0, see http://cite.opengeospatial.org/pub/cite/files/edu/sos/text/main.html
public class SOSHelper {
    private final static SimpleDateFormat dateFormatISO8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

    public static String getCapabilities() {
        JSONObject obj = new JSONObject();

        try {
            obj.put("request","GetCapabilities");
            obj.put("version","2.0.0");
            obj.put("service","SOS");
            obj.put("procedureDescriptionFormat","http://www.opengis.net/sensorML/1.0.1");
            JSONObject procedureDescription  = new JSONObject();
            JSONArray validTime = new JSONArray();
            validTime.put(formatTime(System.currentTimeMillis()));
            procedureDescription.put("validTime",validTime);
            obj.put("procedureDescription",procedureDescription);

            //TODO add the GetCapabilities response
            obj.put("description","TODO"); //TODO
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return obj.toString();
    }
    public static String getDescribeSensor(Context context, Location last) {
        JSONObject obj = new JSONObject();

        try {
            obj.put("request","DescribeSensor");
            obj.put("version","2.0.0");
            obj.put("service","SOS");

            //TODO add the GetCapabilities response
            obj.put("serviceIdentification","TODO"); //TODO
            obj.put("serviceProvider","TODO"); //TODO
            obj.put("operationMetadata","TODO"); //TODO
            obj.put("operations","TODO"); //TODO
            obj.put("contents","TODO"); //TODO
            obj.put("filterCapabilities","TODO"); //TODO
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return obj.toString();
    }

    public static String getObservationResultGPSPts(ArrayList<GeoPackageGPSPtHelper> points) {
        return getObservationResultGPSPts(points,Integer.MAX_VALUE);
    }

    public static String getObservationResultGPSPts(ArrayList<GeoPackageGPSPtHelper> points, int max) {
        //TODO this needs to be changed to better conform with OGC standards
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
                    obs.put(getObservationResult(points.get(i)));
                }
                obj.put("observations", obs);
            }
            return obj.toString(2);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return obj.toString();
    }

    public static String getObservationResult(ArrayList<GeoPackageSatDataHelper> points) {
        return getObservationResult(points,Integer.MAX_VALUE);
    }

    public static String getObservationResult(ArrayList<GeoPackageSatDataHelper> points, int max) {
        //TODO this needs to be changed to better conform with OGC standards
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
                    obs.put(getObservationResultCN0(points.get(i)));
                    obs.put(getObservationResultAGC(points.get(i)));
                }
                obj.put("observations", obs);
            }
            return obj.toString(2);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return obj.toString();
    }

    private static JSONObject getObservationResultCN0(GeoPackageSatDataHelper satData) {
        JSONObject obj = new JSONObject();

        try {
            obj.put("type","http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_Measurement");
            obj.put("procedure","C/N0");
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

    private static JSONObject getObservationResultAGC(GeoPackageSatDataHelper satData) {
        JSONObject obj = new JSONObject();

        try {
            obj.put("type","http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_Measurement");
            obj.put("procedure","AGC");
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
            result.put("uom","dB");
            result.put("value",satData.getAgc());
            obj.put("result",result);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return obj;
    }

    private static JSONObject getObservationResult(GeoPackageGPSPtHelper satData) {
        JSONObject obj = new JSONObject();

        try {
            obj.put("type","http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_Measurement");
            obj.put("procedure","GNSS reported location");
            String time = formatTime(satData.getTime());
            obj.put("phenomenonTime",time);
            obj.put("resultTime",time);
            JSONObject foi = new JSONObject();
            JSONObject foiName = new JSONObject();
            foiName.put("codespace","http://www.opengis.net/def/nil/OGC/0/unknown");
            foiName.put("value", "Sensor");
            foi.put("name",foiName);
            JSONObject geometry = new JSONObject();
            geometry.put("type","Point");
            JSONArray coordinates = new JSONArray();
            coordinates.put(satData.getLat());
            coordinates.put(satData.getLng());
            coordinates.put(satData.getAlt());
            geometry.put("coordinates",coordinates);
            foi.put("geometry",geometry);
            obj.put("featureOfInterest",foi);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return obj;
    }

    public static long parseTime(String time) {
        if (time != null) {
            try {
                Date date = dateFormatISO8601.parse(time);
                return date.getTime();
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        return Long.MIN_VALUE;
    }

    public static String formatTime(long time) {
        return dateFormatISO8601.format(time);
    }
}
