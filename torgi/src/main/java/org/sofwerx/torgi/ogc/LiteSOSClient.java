package org.sofwerx.torgi.ogc;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.sofwerx.torgi.service.TorgiService;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class LiteSOSClient extends Thread {
    private final static String TAG = "TORGI.WS";
    private final TorgiService torgiService;
    //private String ip = null; //TODO
    //private String ip = "http://sensorweb.demo.52north.org/52n-sos-webapp/service";
    //private String ip = "http://172.16.117.191:8080";
    private String ip = "http://172.16.117.191";
    private Handler handler;
    private long HELPER_INTERVAL = 1000l * 5l;
    private Looper looper = null;

    @Override
    public void run() {
        Looper.prepare();
        looper = Looper.myLooper();
        handler = new Handler();
        handler.post(periodicHelper);
        Looper.loop();
    }

    public void shutdown() {
        if (handler != null)
            handler.removeCallbacks(periodicHelper);
        if (looper != null)
            looper.quit();
    }

    public LiteSOSClient(TorgiService torgiService) {
        this.torgiService = torgiService;
    }

    /**
     * Sets the ip (or ip and port like "172.16.117.191:8080" for the SOS that is serving up TORGI data)
     * @param ip
     */
    public void setIP(String ip) {
        this.ip = ip;
    }

    private final Runnable periodicHelper = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG,"GNSS measurement baseline updated");
            if (ip == null) {
                Log.e(TAG,"Cannot connect to SOS server without an ip");
            } else {
                Log.d(TAG, "Trying to start connection with SOS on " + ip);
                //TODO String response = postData(SOSHelper.getCapabilities());
                String response = postData("{request: GetObservation, service: SOS, version: 2.0.0}");
                SOSHelper.parseObservation(torgiService,response);
            }
            //TODO if (handler != null)
            //    handler.postDelayed(this, HELPER_INTERVAL);
        }
    };

    //TODO this approach doesn't seem to be working
    /*public String postData(String data) {
        String response = null;

        if (data != null) {
            HttpURLConnection client = null;
            try {
                URL url = new URL(ip);
                client = (HttpURLConnection) url.openConnection();
                client.setReadTimeout(15000);
                client.setConnectTimeout(15000);
                client.setRequestMethod("POST");
                client.setRequestProperty("Content-Type", "application/json");
                client.setRequestProperty("postData",data);
                client.setDoOutput(true);
                client.setDoInput(true);
                response = client.getResponseMessage();
                //OutputStream outputPost = new BufferedOutputStream(client.getOutputStream());
                //writeStream(outputPost);
                //outputPost.flush();
                //outputPost.close();
                //client.setFixedLengthStreamingMode(outputPost.getBytes("UTF-8").length);
                //client.setChunkedStreamingMode(0);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (client != null) // Make sure the connection is not null.
                    client.disconnect();
            }
        }
        return response;
    }*/

    public String postData(String data) {
        String responseString = null;
        if (data != null) {
            URL url;
            try {
                HttpHost target = new HttpHost(ip, 8080);
                HttpClient client = new DefaultHttpClient();
                HttpPost post = new HttpPost();
                post.setHeader("postData",data);
                HttpEntity results = null;
                HttpResponse response=client.execute(target, post);
                results = response.getEntity();
                responseString = EntityUtils.toString(results);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return responseString;
    }

    //TODO this approach doesn't seem to be working
    /*public String postData(String data) {
        String response = null;
        if (data != null) {
            URL url;
            try {
                url = new URL(ip);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(7000);
                conn.setConnectTimeout(7000);
                conn.setRequestMethod("POST");
                //conn.setDoInput(true);
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json");
                //byte[] outputBytes = data.getBytes("UTF-8");
                //OutputStream os = conn.getOutputStream();
                //os.write(outputBytes);

                int responseCode = conn.getResponseCode();

                if (responseCode == HttpsURLConnection.HTTP_OK) {
                    StringWriter out = new StringWriter();
                    Log.d(TAG, "HTTP_OK from "+ip);
                    String line;
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    while ((line = br.readLine()) != null) {
                        out.append(line);
                    }
                    response = out.toString();
                    if (response.length() < 2)
                        response = null;
                } else {
                    Log.e(TAG, "Did not return HTTP_OK");
                    response = "";
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return response;
    }*/
}