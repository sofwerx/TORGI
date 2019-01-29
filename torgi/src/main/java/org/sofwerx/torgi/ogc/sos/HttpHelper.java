package org.sofwerx.torgi.ogc.sos;

import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class HttpHelper {
    public static String post(String serverURL, String body) throws IOException {
        if (serverURL == null)
            throw new IOException("Cannot connect to a null server URL");
        if (body == null)
            throw new IOException("Cannot send an empty body");

        StringWriter payload = new StringWriter();
        //payload.append(SOAP_HEADER);
        payload.append(body);
        //payload.append(SOAP_FOOTER);

        URL url;
        String response = "";
        url = new URL(serverURL);

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setReadTimeout(15000);
        conn.setConnectTimeout(15000);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type","application/soap+xml");
        conn.setDoInput(true);
        //conn.setDoOutput(true);
        conn.setInstanceFollowRedirects(false);

        OutputStream os = conn.getOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
        writer.write(payload.toString());

        writer.flush();
        writer.close();
        os.close();
        int responseCode=conn.getResponseCode();

        if (responseCode == HttpsURLConnection.HTTP_OK) {
            String line;
            BufferedReader br=new BufferedReader(new InputStreamReader(conn.getInputStream()));
            while ((line=br.readLine()) != null) {
                response+=line;
            }
        } else {
            Log.e(SosIpcTransceiver.TAG,"Http connection attempt failed: "+responseCode);
            response = null;
        }

        conn.disconnect();

        if ((response != null) && (response.length() < 1))
            response = null;
        return response;
    }

    //Not pretty, but efficient
    private final static String SOAP_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><env:Envelope xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.w3.org/2003/05/soap-envelope http://www.w3.org/2003/05/soap-envelope/soap-envelope.xsd\"> <env:Body>";
    private final static String SOAP_FOOTER = "</env:Body></env:Envelope>";
}
