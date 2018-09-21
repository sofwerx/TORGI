package tech.plugs.torgilistener;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

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
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><DescribeSensor version=\"1.0.0\" service=\"SOS\" mobileEnabled=\"true\" xmlns=\"http://www.opengis.net/sos/1.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.opengis.net/sos/1.0 http://schemas.opengis.net/sos/1.0.0/sosDescribeSensor.xsd\" outputFormat=\"text/xml;subtype=&quot;sensorML/1.0.1&quot;\"><procedure>urn:ogc:object:feature:Sensor:IFGI:ifgi-sensor-1</procedure></DescribeSensor>";
    }

    public final static String getOperationGetCapabilities() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><GetCapabilities xmlns=\"http://www.opengis.net/sos/1.0\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.opengis.net/sos/1.0http://schemas.opengis.net/sos/1.0.0/sosGetCapabilities.xsd\" service=\"SOS\" updateSequence=\"\"><ows:AcceptVersions><ows:Version>1.0.0</ows:Version></ows:AcceptVersions><ows:Sections><ows:Section>OperationsMetadata</ows:Section><ows:Section>ServiceIdentification</ows:Section><ows:Section>Filter_Capabilities</ows:Section><ows:Section>Contents</ows:Section></ows:Sections></GetCapabilities>";
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
        return getOperationGetObservations(Long.MIN_VALUE, Long.MIN_VALUE);
    }

    /**
     * Gets a range of observations
     * @param start start time
     * @param end end time
     * @return
     */
    public static String getOperationGetObservations(long start, long end) {
        StringWriter writer = new StringWriter();
        writer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<GetObservation xmlns=\"http://www.opengis.net/sos/1.0\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" xmlns:gml=\"http://www.opengis.net/gml\" xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:om=\"http://www.opengis.net/om/1.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.opengis.net/sos/1.0\n" +
                "http://schemas.opengis.net/sos/1.0.0/sosGetObservation.xsd\" service=\"SOS\" version=\"1.0.0\" srsName=\"urn:ogc:def:crs:EPSG:4326\">\n" +
                "   <offering>TORGI</offering>\n"); //TODO change this type of offering
        if (end > Long.MIN_VALUE) {
            writer.append("   <eventTime>\n" +
                    "      <ogc:TM_During>\n" +
                    "         <ogc:PropertyName>urn:ogc:data:time:iso8601</ogc:PropertyName>\n" +
                    "         <gml:TimePeriod>\n" +
                    "            <gml:beginPosition>");
            writer.append(formatTime(start));
            writer.append("</gml:beginPosition>\n" +
                    "            <gml:endPosition>");
            writer.append(formatTime(end));
            writer.append("</gml:endPosition>\n" +
                    "         </gml:TimePeriod>\n" +
                    "      </ogc:TM_During>\n" +
                    "   </eventTime>\n");
        }
        //TODO need to implement "   <procedure>urn:ogc:object:feature:Sensor:IFGI:ifgi-sensor-1</procedure>\n" +
        //TODO need to implement "   <observedProperty>urn:ogc:def:phenomenon:OGC:1.0.30:waterlevel</observedProperty>\n" +
        writer.append("   <responseFormat>text/xml;subtype=&quot;om/1.0.0&quot;</responseFormat>\n" +
                "</GetObservation>");

        return writer.toString();
    }
}
