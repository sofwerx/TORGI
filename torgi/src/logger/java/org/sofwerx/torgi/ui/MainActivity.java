package org.sofwerx.torgi.ui;

import android.os.Bundle;
import android.widget.TextView;

import org.sofwerx.torgi.R;

public class MainActivity extends AbstractTORGIActivity {
    private TextView status;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        status = findViewById(R.id.textView);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (permissionsPassed)
            status.setText(getString(R.string.notification));
        else
            status.setText(getString(R.string.torgi_not_running));
    }

    @Override
    protected int getLayout() {
        return R.layout.activity_logger;
    }
}