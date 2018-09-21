package tech.plugs.torgilistener;

import android.content.Context;
import android.util.Log;

public class SampleTransceiver extends AbstractSOSBroadcastTransceiver {
    private MessageReceiveListener listener = null;

    public void setListener(MessageReceiveListener listener) {
        this.listener = listener;
    }

    @Override
    public void onMessageReceived(Context context, String source, String input) {
        super.onMessageReceived(context,source,input);
        if (BuildConfig.APPLICATION_ID.equalsIgnoreCase(source))
            return; //messages are ignored by their sender
        if (input == null)
            Log.e(TAG,"Emtpy SOS message received");
        else {
            if (listener != null)
                listener.onMessageReceived(input);
        }
    }

    public interface MessageReceiveListener {
        void onMessageReceived(String input);
    }
}
