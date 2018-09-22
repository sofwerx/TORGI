package org.sofwerx.torgi.ogc;

import android.util.Log;

import org.apache.http.conn.util.InetAddressUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.sofwerx.torgi.gnss.helper.GeoPackageSatDataHelper;
import org.sofwerx.torgi.service.TorgiService;

import java.io.IOException;
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
        torgiService.notifyOfWebServer(getLocalIpAddress()+":"+ PORT);
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
            final Map<String, String> map = new HashMap<String, String>();
            Method method = session.getMethod();
            Log.d(TAG,"Method: "+method.name());
            if (Method.PUT.equals(method) || Method.POST.equals(method)) {
                try {
                    session.parseBody(map);
                    // get the POST body
                    if (map.containsKey("postData")) {
                        String data = map.get("postData");
                        Log.d(TAG,"Data = "+data);
                        if (data != null) {
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
                            if (obj != null) {
                                String operation = obj.optString("request",null);
                                if (operation != null) {
                                    if ("GetObservation".equalsIgnoreCase(operation)) {
                                        ArrayList<GeoPackageSatDataHelper> measurements =
                                                torgiService.getGeoPackageRecorder().getGnssMeasurementsSatDataBlocking(System.currentTimeMillis()-1000l*10l,System.currentTimeMillis());
                                        if (measurements == null)
                                            return newFixedLengthResponse("{}"); //TODO send back a better empty description
                                        else {
                                            String out = SOSHelper.getObservation(measurements);
                                            return newFixedLengthResponse(out);
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (IOException ioe) {
                    return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "SERVER INTERNAL ERROR: IOException: " + ioe.getMessage());
                } catch (ResponseException re) {
                    return newFixedLengthResponse(re.getStatus(), MIME_PLAINTEXT, re.getMessage());
                }
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