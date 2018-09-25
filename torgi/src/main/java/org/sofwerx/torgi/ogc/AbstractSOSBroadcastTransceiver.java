package org.sofwerx.torgi.ogc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.sofwerx.torgi.BuildConfig;
import org.sofwerx.torgi.ogc.sos.DescribeSensor;
import org.sofwerx.torgi.ogc.sos.GetCapabilities;
import org.sofwerx.torgi.ogc.sos.GetObservations;

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
        if (BuildConfig.APPLICATION_ID.equalsIgnoreCase(source))
            return; //messages are ignored by their sender
    }

    public final static String getOperationDescribeSensor() {
        return new DescribeSensor().toJSON().toString();
    }

    public final static String getOperationGetCapabilities() {
        return new GetCapabilities().toJSON().toString();
    }

    public static String getOperationGetObservations() {
        return new GetObservations().toJSON().toString();
    }

    public static String getOperationGetObservations(long start, long stop) {
        GetObservations getObservations = new GetObservations();
        getObservations.setStartTime(start);
        getObservations.setStopTime(stop);
        return getObservations.toJSON().toString();
    }
}
