package org.sofwerx.torgi.ui;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import org.sofwerx.torgi.R;
import org.sofwerx.torgi.util.PackageUtil;

public class FailureActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_failure);
        View buttonOk = findViewById(R.id.failureButton);
        if (buttonOk != null)
            buttonOk.setOnClickListener(v -> finish());
        View buttonUninstall = findViewById(R.id.failureUninstallButton);
        if (buttonUninstall != null)
            buttonUninstall.setOnClickListener(v -> {
                Uri packageUri = Uri.parse("package:"+ FailureActivity.this.getPackageName());
                try {
                    startActivity(new Intent(Intent.ACTION_UNINSTALL_PACKAGE, packageUri));
                } catch (ActivityNotFoundException ignore) {
                }

            });
    }
}
