package tech.plugs.torgilistener;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.StringWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Implements the OGC Sensor Observation Service and provides a means for other processes
 * to communicate with the service.  To use, extend this class then add:
 *
 *     <receiver android:name=".YourBroadcastTransceiver">
 *         <intent-filter>
 *             <action android:name="org.sofwerx.torgi.ogc.ACTION_SOS"/>
 *     </intent-filter>
 *     </receiver>
 *
 * to <application> in your manifest. You can send requests to the SOS by using the
 * broadcast message and you will receive responses back through the onMessageReceived method.
 * See the TorgiSOSBroadcastTransceiver for an example of how to use this
 *
 */
public abstract class AbstractSOSBroadcastTransceiver extends BroadcastReceiver {
    private final static SimpleDateFormat dateFormatISO8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
    protected final static String TAG = "OGC.SOS";
    public static final String ACTION_SOS = "org.sofwerx.torgi.ogc.ACTION_SOS";
    private static final String EXTRA_PAYLOAD = "SOS";
    private static final String EXTRA_ORIGIN = "src";

    @Override
    public void onReceive(Context context, Intent intent) {
        if ((context != null) && (intent != null)) {
            if (ACTION_SOS.equals(intent.getAction()))
                onMessageReceived(context,intent.getStringExtra(EXTRA_ORIGIN),intent.getStringExtra(EXTRA_PAYLOAD));
            else
                Log.e(TAG, "Unexpected action message received: " + intent.getAction());
        }
    }

    /**
     * Broadcast an SOS Operation
     * @param context
     * @param sosOperation
     */
    public static void broadcast(Context context, String sosOperation) {
        if (context == null) {
            Log.d(TAG,"Context needed to broadcast an SOS Operation");
            return;
        }
        if (sosOperation == null) {
            Log.e(TAG, "Cannot broadcast an empty SOS Operation");
            return;
        }
        Intent intent = new Intent(ACTION_SOS);
        intent.putExtra(EXTRA_ORIGIN, BuildConfig.APPLICATION_ID);
        intent.putExtra(EXTRA_PAYLOAD,sosOperation);

        context.sendBroadcast(intent);
        Log.d(TAG,"Broadcast: "+sosOperation);
    }

    /**
     * Handles the message received from the SOS Broadcast; extend this method to handle the input
     * @param source the application that sent the request; mostly to prevent circular reporting
     * @param input the SOS operation received
     */
    public void onMessageReceived(Context context,String source,String input) {
        if (source == null) {
            Log.e(TAG,"SOS broadcasts from anonymous senders is not supported");
            return;
        }
    }

    public final static String getOperationDescribeSensor() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("request","DescribeSensor");
            obj.put("service","SOS");
            obj.put("version","2.0.0");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return obj.toString();
    }

    public final static String getOperationGetCapabilities() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("request","GetCapabilities");
            obj.put("service","SOS");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return obj.toString();
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

    /**
     * Gets the recent observations
     * @return
     */
    public static String getOperationGetObservations() {
        return getOperationGetObservations(Long.MIN_VALUE, Long.MAX_VALUE);
    }

    /**
     * Gets a range of observations
     * @param start start time
     * @param end end time
     * @return
     */
    public static String getOperationGetObservations(long start, long end) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("request","GetObservation");
            obj.put("service","SOS");
            obj.put("version","2.0.0");
            if ((start != Long.MIN_VALUE) && (end != Long.MAX_VALUE)) {
                JSONObject temporalFilter = new JSONObject();
                JSONObject during = new JSONObject();
                during.put("ref","om:phenomenonTime");
                JSONArray values = new JSONArray();
                values.put(formatTime(start));
                values.put(formatTime(end));
                during.put("value",values);
                temporalFilter.put("during",during);
                obj.put("temporalFilter",temporalFilter);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return obj.toString();
    }
}
