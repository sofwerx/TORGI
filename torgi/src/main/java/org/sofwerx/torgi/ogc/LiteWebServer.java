package org.sofwerx.torgi.ogc;

import android.util.Log;

import org.apache.http.conn.util.InetAddressUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.sofwerx.torgi.gnss.helper.GeoPackageGPSPtHelper;
import org.sofwerx.torgi.gnss.helper.GeoPackageSatDataHelper;
import org.sofwerx.torgi.ogc.sos.AbstractOperation;
import org.sofwerx.torgi.ogc.sos.DescribeSensor;
import org.sofwerx.torgi.ogc.sos.GetCapabilities;
import org.sofwerx.torgi.ogc.sos.GetObservations;
import org.sofwerx.torgi.ogc.sos.UnsupportedOperationException;
import org.sofwerx.torgi.service.TorgiService;

import java.io.IOException;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

public class LiteWebServer {
    private final static String TAG = "TORGI.WS";
    private WebServer server;
    private final TorgiService torgiService;
    private final static int PORT = 8080;

    public LiteWebServer(TorgiService torgiService) {
        this.torgiService = torgiService;

        server = new WebServer();
        try {
            server.start();
        } catch(IOException ioe) {
            Log.w(TAG, "SOS web server could not start.");
        }
        torgiService.notifyOfWebServer(getLocalIpAddress()+":"+ PORT,null,null);
    }

    public void stop() {
        if (server != null)
            server.stop();
    }

    private class WebServer extends NanoHTTPD {
        public WebServer() {
            super(PORT);
        }

        public Response serve(IHTTPSession session) {
            final Map<String, String> map = new HashMap();
            Map<String, List<String>> qparams = new HashMap();
            Method method = session.getMethod();
            Log.d(TAG,"Method: "+method.name());
            if (Method.PUT.equals(method) || Method.POST.equals(method)) {
                try {
                    session.parseBody(map);
                    // get the POST body
                    if (map.size() == 0) {
                        qparams = session.getParameters();
                    }
                    JSONObject obj = null;
                    String data = null;
                    if (map.containsKey("postData")) {
                        data = map.get("postData");
                    } else if (qparams.size() > 0) {
                        data = qparams.keySet().toString();
                    }

                    Log.d(TAG,"Data = "+data);
                    if (data != null)
                        obj = getJSONAnywhereInHere(data);
                    else
                        return newFixedLengthResponse("TORGI received a POST or PUT but wasn't able to handle this data; it just received: " + session.getParameters().toString());

                    if (obj == null) //handle the case where the JSON isnt mapped to postData but is instead the first key in the map
                        obj = getJSONInMap(map);

                    if (obj != null) {
                        try {
                            AbstractOperation receivedOperation = AbstractOperation.getOperation(obj);
                            if (receivedOperation != null) {
                                if (receivedOperation instanceof GetObservations) {
                                    GetObservations getObservations = (GetObservations) receivedOperation;
                                    long start = getObservations.getStartTime();
                                    long stop = getObservations.getStopTime();
                                    StringWriter out = new StringWriter();
                                    ArrayList<GeoPackageSatDataHelper> measurements =
                                            torgiService.getGeoPackageRecorder().getGnssMeasurementsSatDataBlocking(start, stop);
                                    ArrayList<GeoPackageGPSPtHelper> gpsMeasurements =
                                            torgiService.getGeoPackageRecorder().getGPSObservationPointsBlocking(start, stop);
                                    out.append(SOSHelper.getObservationResult(measurements,gpsMeasurements));
                                    torgiService.notifyOfWebServer(getLocalIpAddress()+":"+ PORT,session.getRemoteIpAddress(),null);
                                    return newFixedLengthResponse(out.toString());
                                } else if (receivedOperation instanceof GetCapabilities) {
                                    Response response = newFixedLengthResponse(SOSHelper.getCapabilities());
                                    return response;
                                } else if (receivedOperation instanceof DescribeSensor) {
                                    return newFixedLengthResponse(SOSHelper.getDescribeSensor(torgiService, torgiService.getCurrentLocation()));
                                }
                            }
                        } catch (UnsupportedOperationException e) {
                            Log.e(TAG, e.getMessage());
                            torgiService.notifyOfWebServer(getLocalIpAddress()+":"+ PORT,null,e.getMessage());
                            return newFixedLengthResponse(e.getMessage());
                        }
                    }
                } catch (IOException ioe) {
                    torgiService.notifyOfWebServer(getLocalIpAddress()+":"+ PORT,null,ioe.getMessage());
                    return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "SERVER INTERNAL ERROR: IOException: " + ioe.getMessage());
                } catch (ResponseException re) {
                    torgiService.notifyOfWebServer(getLocalIpAddress()+":"+ PORT,null,re.getMessage());
                    return newFixedLengthResponse(re.getStatus(), MIME_PLAINTEXT, re.getMessage());
                }
            } else if (Method.OPTIONS.equals(method)) {
                Response response = newFixedLengthResponse("");
                response.addHeader("Allow","POST, OPTIONS");
                //response.addHeader("Access-Control-Allow-Origin","*");
                //response.addHeader("Access-Control-Allow-Methods","POST, OPTIONS");
                //response.addHeader("Access-Control-Max-Age","86400");
                return response;
            }

            Map<String, List<String>> decodedQueryParameters = decodeParameters(session.getQueryParameterString());

            StringBuilder sb = new StringBuilder();
            sb.append("<html><head><title>TORGI Debug</title></head><body>");
            sb.append("<h1>TORGI did not understand what it received</h1>");
            sb.append("<p><blockquote><b>URI</b> = ").append(String.valueOf(session.getUri())).append("<br />");
            sb.append("<b>Method</b> = ").append(String.valueOf(session.getMethod())).append("</blockquote></p>");
            sb.append("<h3>Headers</h3><p><blockquote>").append(toString(session.getHeaders())).append("</blockquote></p>");
            sb.append("<h3>Parms</h3><p><blockquote>").append(toString(session.getParms())).append("</blockquote></p>");
            sb.append("<h3>Parms (multi values?)</h3><p><blockquote>").append(toString(decodedQueryParameters)).append("</blockquote></p>");
            try {
                Map<String, String> files = new HashMap<String, String>();
                session.parseBody(files);
                sb.append("<h3>Files</h3><p><blockquote>").append(toString(files)).append("</blockquote></p>");
            } catch (Exception e) {
                e.printStackTrace();
            }
            sb.append("</body></html>");
            return newFixedLengthResponse(sb.toString());
        }

        private String toString(Map<String, ? extends Object> map) {
            if (map.size() == 0) {
                return "";
            }
            return unsortedList(map);
        }

        private String unsortedList(Map<String, ? extends Object> map) {
            StringBuilder sb = new StringBuilder();
            sb.append("<ul>");
            for (Map.Entry<String, ? extends Object> entry : map.entrySet()) {
                listItem(sb, entry);
            }
            sb.append("</ul>");
            return sb.toString();
        }
    }

    public static JSONObject getJSONAnywhereInHere(String data) {
        if (data == null)
            return null;
        JSONObject obj = null;
        try {
            obj = new JSONObject(data);
        } catch (JSONException e) {
            if (data.indexOf('{') > -1) {
                try {
                    obj = new JSONObject(data.substring(data.indexOf('{')));
                } catch (JSONException ignore) {}
            }
        }
        return obj;
    }

    private JSONObject getJSONInMap(Map<String, String> map) {//handles case where the JSON data does not get mapped properly but winds up somewhere else
        if ((map == null) || map.isEmpty())
            return null;
        JSONObject obj = null;
        for (String key:map.keySet()) {
            obj = getJSONAnywhereInHere(key);
            if (obj == null)
                obj = getJSONAnywhereInHere(map.get(key));
            if (obj != null)
                return obj;
        }
        return obj;
    }

    private void listItem(StringBuilder sb, Map.Entry<String, ? extends Object> entry) {
        sb.append("<li><code><b>").append(entry.getKey()).append("</b> = ").append(entry.getValue()).append("</code></li>");
    }

    private String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && InetAddressUtils.isIPv4Address(inetAddress.getHostAddress())) {
                        String ip = inetAddress.getHostAddress().toString();
                        Log.d(TAG,"Using IP address "+ip);
                        return ip;
                    }
                }
            }
        } catch (Exception ex) {
            Log.e(TAG, ex.toString());
        }
        return null;
    }
}