package org.sofwerx.torgi.ogc;

import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.sofwerx.torgi.BuildConfig;
import org.sofwerx.torgi.gnss.helper.GeoPackageSatDataHelper;
import org.sofwerx.torgi.ogc.sos.AbstractOperation;
import org.sofwerx.torgi.ogc.sos.DescribeSensor;
import org.sofwerx.torgi.ogc.sos.GetCapabilities;
import org.sofwerx.torgi.ogc.sos.GetObservations;
import org.sofwerx.torgi.ogc.sos.UnsupportedOperationException;
import org.sofwerx.torgi.service.TorgiService;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class TorgiSOSBroadcastTransceiver extends AbstractSOSBroadcastTransceiver {
    private final TorgiService torgiService;

    public TorgiSOSBroadcastTransceiver(TorgiService torgiService) {
        super();
        this.torgiService = torgiService;
    }

    @Override
    public void onMessageReceived(Context context, String source, String input) {
        super.onMessageReceived(context,source,input);
        if (BuildConfig.APPLICATION_ID.equalsIgnoreCase(source))
            return; //messages are ignored by their sender
        if (input == null)
            Log.e(TAG,"Emtpy SOS message received");
        else {
            Log.d(TAG,"Broadcast received: "+input);
            parse(input);
        }
    }

    private void parse(String input) {
        if (input != null) {
            try {
                AbstractOperation operation = AbstractOperation.getOperation(new JSONObject(input));
                torgiService.onSOSRequestReceived(operation);
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (UnsupportedOperationException e) {
                e.printStackTrace();
            }
        }
    }

    /*private void parse(String input) {
        if (input != null) {
            InputStream stream = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
            parse(stream);
            try {
                stream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void parse(InputStream stream) {
        if (stream != null) {
            XmlPullParserFactory parserFactory;
            try {
                parserFactory = XmlPullParserFactory.newInstance();
                XmlPullParser parser = parserFactory.newPullParser();
                parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
                parser.setInput(stream, null);
                processParsing(parser);
            } catch (XmlPullParserException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void processParsing(XmlPullParser parser) throws IOException, XmlPullParserException{
        int eventType = parser.getEventType();

        while (eventType != XmlPullParser.END_DOCUMENT) {
            String eltName;

            switch (eventType) {
                case XmlPullParser.START_TAG:
                    eltName = parser.getName();

                    if (SOSHelper.REQUEST_GET_OBSERVATION.equals(eltName)) {
                        torgiService.onSOSRequestReceived(eltName);
                    } else if (SOSHelper.REQUEST_DESCRIBE_SENSOR.equalsIgnoreCase(eltName)) {
                        torgiService.onSOSRequestReceived(eltName);
                    } else if (SOSHelper.REQUEST_GET_CAPABILITIES.equalsIgnoreCase(eltName)) {
                        torgiService.onSOSRequestReceived(eltName);
                    }
                    break;
            }
            eventType = parser.next();
        }
    }*/
}
