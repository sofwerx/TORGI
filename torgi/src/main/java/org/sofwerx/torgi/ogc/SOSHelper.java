package org.sofwerx.torgi.ogc;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.params.HttpParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sofwerx.torgi.gnss.Constellation;
import org.sofwerx.torgi.gnss.helper.GeoPackageGPSPtHelper;
import org.sofwerx.torgi.gnss.helper.GeoPackageSatDataHelper;
import org.sofwerx.torgi.ogc.sos.AbstractOperation;
import org.sofwerx.torgi.ogc.sos.UnsupportedOperationException;
import org.sofwerx.torgi.service.TorgiService;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

//Built to comply with OGC SOS v2.0, see http://cite.opengeospatial.org/pub/cite/files/edu/sos/text/main.html
public class SOSHelper {
    private final static String TAG = "TORGI.SOS";
    private final static String PROCEEDURE_GNSS_LOCATION = "GNSS reported location";
    private final static String PROCEEDURE_AGC = "AGC";
    private final static String PROCEEDURE_CN0 = "C/N0";

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

    public static String getObservations(long start,long stop) {
        JSONObject obj = new JSONObject();

        try {
            obj.put("request","GetObservation");
            obj.put("version","2.0.0");
            obj.put("service","SOS");
            JSONObject temporalFilter = new JSONObject();
            JSONObject during = new JSONObject();
            during.put("ref","om:phenomenonTime");
            JSONArray value = new JSONArray();
            value.put(formatTime(start));
            value.put(formatTime(stop));
            during.put("value",value);
            temporalFilter.put("during",during);
            obj.put("temporalFilter",temporalFilter);
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

    /**
     * Use
     * @param points
     * @return
     */
    @Deprecated
    public static String getObservationResultGPSPts(ArrayList<GeoPackageGPSPtHelper> points) {
        return getObservationResultGPSPts(points,Integer.MAX_VALUE);
    }

    @Deprecated
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
                    obs.put(getObservationResult(points.get(i), Double.NaN));
                }
                obj.put("observations", obs);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return obj.toString();
    }

    public static String getObservationResult(ArrayList<GeoPackageSatDataHelper> points,ArrayList<GeoPackageGPSPtHelper> gpsMeasurements) {
        return getObservationResult(points, gpsMeasurements,Integer.MAX_VALUE, Double.NaN);
    }

    public static String getObservationResult(ArrayList<GeoPackageSatDataHelper> points,ArrayList<GeoPackageGPSPtHelper> gpsMeasurements, double ewRisk) {
        return getObservationResult(points, gpsMeasurements,Integer.MAX_VALUE, ewRisk);
    }

    public static String getObservationResult(ArrayList<GeoPackageSatDataHelper> points, ArrayList<GeoPackageGPSPtHelper> gpsMeasurements, int max, double ewRisk) {
        //TODO this needs to be changed to better conform with OGC standards
        JSONObject obj = new JSONObject();

        try {
            obj.put("request","GetObservation");
            obj.put("version","2.0.0");
            obj.put("service","SOS");
            if ((points != null) && !points.isEmpty()) {
                JSONArray obs = new JSONArray();
                int tempMax = gpsMeasurements.size();
                if (max < tempMax)
                    tempMax = max;
                for (int i=0;i<tempMax;i++) {
                    obs.put(getObservationResult(gpsMeasurements.get(i),ewRisk));
                }
                tempMax = points.size();
                if (max < tempMax)
                    tempMax = max;
                for (int i=0;i<tempMax;i++) {
                    obs.put(getObservationResultCN0(points.get(i)));
                    obs.put(getObservationResultAGC(points.get(i)));
                }
                obj.put("observations", obs);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return obj.toString();
    }

    private static JSONObject getObservationResultCN0(GeoPackageSatDataHelper satData) {
        JSONObject obj = new JSONObject();

        try {
            obj.put("type","http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_Measurement");
            obj.put("procedure",PROCEEDURE_CN0);
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
            obj.put("procedure",PROCEEDURE_AGC);
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

    private static JSONObject getObservationResult(GeoPackageGPSPtHelper satData, double ewRisk) {
        JSONObject obj = new JSONObject();

        try {
            obj.put("type","http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_Measurement");
            obj.put("procedure",PROCEEDURE_GNSS_LOCATION);
            String time = formatTime(satData.getTime());
            obj.put("phenomenonTime",time);
            //temp reporting of assessed EW risk
            if (!Double.isNaN(ewRisk)) {
                JSONObject objResult = new JSONObject();
                objResult.put("value",ewRisk);
                obj.put("result",objResult);
            }
            obj.put("resultTime",time);
            JSONObject foi = new JSONObject();
            JSONObject foiName = new JSONObject();
            foiName.put("codespace","http://www.opengis.net/def/nil/OGC/0/unknown");
            foiName.put("value", satData.getId());
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

    public static void parseObservation(TorgiService torgiService, String response) {
        if ((torgiService == null) || (response == null))
            return;
        JSONObject obj = LiteWebServer.getJSONAnywhereInHere(response);
        if ((obj != null) && "GetObservation".equalsIgnoreCase(obj.optString("request",null))) {
            JSONArray obs = obj.optJSONArray("observations");
            if (obs != null) {
                Log.d(TAG,obs.length()+" observations received");
                ArrayList<GeoPackageSatDataHelper> satDatas = null;
                ArrayList<GeoPackageGPSPtHelper> pts = null;
                for (int i=0;i<obs.length();i++) {
                    JSONObject obsResult = obs.optJSONObject(i);
                    if (obsResult != null) {
                        String procedure = obsResult.optString("procedure",null);
                        if (procedure != null) {
                            long time = parseTime(obsResult.optString("phenomenonTime",null));
                            JSONObject foi = obsResult.optJSONObject("featureOfInterest");
                            if (foi == null)
                                continue;
                            JSONObject foiName = foi.optJSONObject("name");
                            if (foiName == null)
                                continue;
                            String id = foiName.optString("value",null);
                            if (id == null)
                                continue;
                            if (PROCEEDURE_GNSS_LOCATION.equalsIgnoreCase(procedure)) {
                                JSONObject geometry = foi.optJSONObject("geometry");
                                if (geometry == null)
                                    continue;
                                JSONArray coordinates = geometry.optJSONArray("coordinates");
                                if ((coordinates == null) || (coordinates.length() < 2))
                                    continue;
                                try {
                                    GeoPackageGPSPtHelper pt = new GeoPackageGPSPtHelper();
                                    pt.setLat(coordinates.getDouble(0));
                                    pt.setLng(coordinates.getDouble(1));
                                    if (coordinates.length() > 2)
                                        pt.setAlt(coordinates.getDouble(2));
                                    pt.setTime(time);
                                    pt.setId(Long.parseLong(id));
                                    if (pts == null)
                                        pts = new ArrayList<>();
                                    pts.add(pt);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                } catch (NumberFormatException e) {
                                    e.printStackTrace();
                                }
                            } else {
                                JSONObject result = obsResult.optJSONObject("result");
                                if (result == null)
                                    continue;
                                try {
                                    double value = Double.parseDouble(result.optString("value",""));
                                    String[] idParts = id.split("-");
                                    if ((idParts == null) || (idParts.length != 2))
                                        continue;
                                    Constellation constellation = Constellation.valueOf(idParts[0]);
                                    long svid = Long.parseLong(idParts[1]);
                                    GeoPackageSatDataHelper thisSatData = new GeoPackageSatDataHelper();
                                    thisSatData.setConstellation(constellation);
                                    thisSatData.setSvid(svid);
                                    thisSatData.setMeassuredTime(time);
                                    if (PROCEEDURE_AGC.equalsIgnoreCase(procedure))
                                        thisSatData.setAgc(value);
                                    else if (PROCEEDURE_CN0.equalsIgnoreCase(procedure))
                                        thisSatData.setCn0(value);
                                    if (satDatas == null) {
                                        satDatas = new ArrayList<>();
                                        satDatas.add(thisSatData);
                                    } else {
                                        boolean found = false;
                                        for (GeoPackageSatDataHelper satData:satDatas) {
                                            if (satData.isSame(thisSatData)) {
                                                found = true;
                                                satData.update(thisSatData);
                                                break;
                                            }
                                        }
                                        if (!found)
                                            satDatas.add(thisSatData);
                                    }
                                } catch (NumberFormatException e) {
                                    e.printStackTrace();
                                } catch (IllegalArgumentException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                }
                if (pts != null) {
                    for (GeoPackageGPSPtHelper pt:pts) {
                        Location location = new Location("TORGI");
                        location.setLatitude(pt.getLat());
                        location.setLongitude(pt.getLng());
                        if (!Double.isNaN(pt.getAlt()))
                            location.setAltitude(pt.getAlt());
                        location.setTime(pt.getTime());
                        torgiService.updateLocation(location);
                    }
                }
                if (satDatas != null)
                    torgiService.onGeoPackageSatDataHelperReceived(satDatas);
            }
        }
    }
}
