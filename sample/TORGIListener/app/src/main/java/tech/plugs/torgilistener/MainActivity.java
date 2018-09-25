package tech.plugs.torgilistener;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements SampleTransceiver.MessageReceiveListener {
    private SampleTransceiver transceiver = null;
    private TextView response;
    private Button bDescribeSensor, bGetCapabilities, bGetObservations;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        response = findViewById(R.id.messgage);
        response.setOnClickListener(view -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("SOS", response.getText().toString());
            clipboard.setPrimaryClip(clip);
            Toast.makeText(MainActivity.this,"Response copied to clipboard",Toast.LENGTH_SHORT).show();
        });
        bDescribeSensor = findViewById(R.id.sendDescribeSensor);
        bGetCapabilities = findViewById(R.id.sendGetCapabilities);
        bGetObservations = findViewById(R.id.sendGetObservations);
        bDescribeSensor.setOnClickListener(view -> broadcast(AbstractSOSBroadcastTransceiver.getOperationDescribeSensor()));
        bGetCapabilities.setOnClickListener(view -> broadcast(AbstractSOSBroadcastTransceiver.getOperationGetCapabilities()));
        bGetObservations.setOnClickListener(view -> broadcast(AbstractSOSBroadcastTransceiver.getOperationGetObservations()));
        transceiver = new SampleTransceiver();
        IntentFilter intentFilter = new IntentFilter(AbstractSOSBroadcastTransceiver.ACTION_SOS);
        registerReceiver(transceiver, intentFilter);

    }

    private void broadcast(String value) {
        response.setText(getString(R.string.waiting_for_message));
        AbstractSOSBroadcastTransceiver.broadcast(MainActivity.this,value);
    }

    @Override
    public void onResume() {
        super.onResume();
        transceiver.setListener(this);
    }

    @Override
    public void onPause() {
        transceiver.setListener(null);
        super.onPause();
    }

    @Override
    public void onDestroy() {
        if (transceiver != null) {
            try {
                unregisterReceiver(transceiver);
            } catch (Exception e) {
                e.printStackTrace();
            }
            transceiver = null;
        }
        super.onDestroy();
    }

    @Override
    public void onMessageReceived(String input) {
        response.setText(input);
    }
}
