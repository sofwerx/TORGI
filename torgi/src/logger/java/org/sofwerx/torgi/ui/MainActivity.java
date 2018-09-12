package org.sofwerx.torgi.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import org.sofwerx.torgi.R;
import org.sofwerx.torgi.service.TorgiService;

public class MainActivity extends AbstractTORGIActivity {
    private TextView status, infoBigData;
    private Switch switchBigData;
    private boolean systemChangingSwitch = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        status = findViewById(R.id.textView);
        switchBigData = findViewById(R.id.switchBigData);
        infoBigData = findViewById(R.id.switchInfo);
        switchBigData.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!systemChangingSwitch) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                SharedPreferences.Editor edit = prefs.edit();
                edit.putBoolean(TorgiService.PREFS_BIG_DATA,isChecked);
                edit.apply();
                if (serviceBound) {
                    if (isChecked)
                        torgiService.startSensorService();
                    else
                        torgiService.stopSensorService();
                }
                showStatus();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        showStatus();
        systemChangingSwitch = true;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        switchBigData.setChecked(prefs.getBoolean(TorgiService.PREFS_BIG_DATA,false));
        systemChangingSwitch = false;
    }

    @Override
    protected void onTorgiServiceConnected() {
        super.onTorgiServiceConnected();
        showStatus();
    }

    private void showStatus() {
        if (permissionsPassed) {
            if (serviceBound) {
                status.setText(getString(R.string.notification));
                status.setVisibility(View.VISIBLE);
                switchBigData.setVisibility(View.VISIBLE);
                if (switchBigData.isChecked())
                    infoBigData.setVisibility(View.VISIBLE);
                else
                    infoBigData.setVisibility(View.INVISIBLE);
            } else {
                status.setVisibility(View.INVISIBLE);
                switchBigData.setVisibility(View.INVISIBLE);
                infoBigData.setVisibility(View.INVISIBLE);
            }
        } else {
            status.setText(getString(R.string.torgi_not_running));
            status.setVisibility(View.VISIBLE);
            switchBigData.setVisibility(View.INVISIBLE);
            infoBigData.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    protected int getLayout() {
        return R.layout.activity_logger;
    }
}