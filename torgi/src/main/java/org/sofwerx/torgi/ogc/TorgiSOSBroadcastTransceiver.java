package org.sofwerx.torgi.ogc;

import android.content.Context;
import android.util.Log;

import org.sofwerx.torgi.BuildConfig;
import org.sofwerx.torgi.service.TorgiService;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

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
        //TODO start parsing the choices
        /*ArrayList<Player> players = new ArrayList<>();
        int eventType = parser.getEventType();
        Player currentPlayer = null;

        while (eventType != XmlPullParser.END_DOCUMENT) {
            String eltName = null;

            switch (eventType) {
                case XmlPullParser.START_TAG:
                    eltName = parser.getName();

                    if ("player".equals(eltName)) {
                        currentPlayer = new Player();
                        players.add(currentPlayer);
                    } else if (currentPlayer != null) {
                        if ("name".equals(eltName)) {
                            currentPlayer.name = parser.nextText();
                        } else if ("age".equals(eltName)) {
                            currentPlayer.age = parser.nextText();
                        } else if ("position".equals(eltName)) {
                            currentPlayer.position = parser.nextText();
                        }
                    }
                    break;
            }

            eventType = parser.next();
        }

        printPlayers(players);*/
    }
}
