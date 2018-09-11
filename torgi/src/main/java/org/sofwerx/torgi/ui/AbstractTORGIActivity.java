package org.sofwerx.torgi.ui;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import org.sofwerx.torgi.R;
import org.sofwerx.torgi.gnss.LatLng;
import org.sofwerx.torgi.listener.GnssMeasurementListener;
import org.sofwerx.torgi.service.TorgiService;

import java.util.ArrayList;

public abstract class AbstractTORGIActivity extends Activity {
    protected static final int REQUEST_DISABLE_BATTERY_OPTIMIZATION = 401;
    protected final static String TAG = "TORGI.monitor";
    private final static String PREF_BATTERY_OPT_IGNORE = "nvroptbat";
    private final static int PERM_REQUEST_CODE = 1;
    private boolean serviceBound = false;
    private TorgiService torgiService = null;
    protected boolean permissionsPassed = false;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayout());
        permissionsPassed = checkPermissions();
        if (permissionsPassed)
            startService();
        openBatteryOptimizationDialogIfNeeded();
    };

    protected abstract int getLayout();

    @Override
    public void onResume() {
        super.onResume();
        if (this instanceof GnssMeasurementListener) {
            if (serviceBound && (torgiService != null))
                torgiService.setListener((GnssMeasurementListener)this);
        }
    }

    @Override
    public void onPause() {
        if (torgiService != null)
            torgiService.setListener(null);
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (serviceBound && (torgiService != null)) {
            try {
                unbindService(mConnection);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    protected ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.d(TAG,"MdxService bound to this activity");
            TorgiService.TorgiBinder binder = (TorgiService.TorgiBinder) service;
            torgiService = binder.getService();
            serviceBound = true;
            onTorgiServiceConnected();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            serviceBound = false;
        }
    };

    private void onTorgiServiceConnected() {
        if (this instanceof GnssMeasurementListener)
            torgiService.setListener((GnssMeasurementListener)this);
    }

    private void startService() {
        if (serviceBound)
            torgiService.start();
        else {
            startService(new Intent(this, TorgiService.class));
            Intent intent = new Intent(this, TorgiService.class);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        }
    }

    /**
     * Request battery optimization exception so that the system doesn't throttle back our app
     */
    private void openBatteryOptimizationDialogIfNeeded() {
        if (isOptimizingBattery() && isAllowAskAboutBattery()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.enable_battery_optimization);
            builder.setMessage(R.string.battery_optimizations_narrative);
            builder.setPositiveButton(R.string.battery_optimize_yes, (dialog, which) -> {
                Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                Uri uri = Uri.parse("package:" + getPackageName());
                intent.setData(uri);
                try {
                    startActivityForResult(intent, REQUEST_DISABLE_BATTERY_OPTIMIZATION);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(this, R.string.does_not_support_battery_optimization, Toast.LENGTH_SHORT).show();
                }
            });
            builder.setOnDismissListener(dialog -> setNeverAskBatteryOptimize());
            final AlertDialog dialog = builder.create();
            dialog.setCanceledOnTouchOutside(false);
            dialog.show();
        }
    }

    protected boolean isOptimizingBattery() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            final PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            return pm != null && !pm.isIgnoringBatteryOptimizations(getPackageName());
        } else
            return false;
    }

    private boolean isAllowAskAboutBattery() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        return !prefs.getBoolean(PREF_BATTERY_OPT_IGNORE,false);
    }

    private void setNeverAskBatteryOptimize() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor edit = prefs.edit();
        edit.putBoolean(PREF_BATTERY_OPT_IGNORE,true);
        edit.apply();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_DISABLE_BATTERY_OPTIMIZATION:
                setNeverAskBatteryOptimize();
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (requestCode == PERM_REQUEST_CODE) {
            if (checkPermissions()) {
                permissionsPassed = true;
                startService();
            } else {
                Toast.makeText(this, "Both Location and Storage permissions are needed", Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Returns true once all required permissions are granted, otherwise returns false and requests the
     * required permission
     * @return
     */
    private boolean checkPermissions() {
        ArrayList<String> needed = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            needed.add(Manifest.permission.ACCESS_FINE_LOCATION);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            needed.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (needed.isEmpty())
            return true;
        else {
            String[] perms = new String[needed.size()];
            perms = new String[perms.length];
            for (int i=0;i<perms.length;i++) {
                perms[i] = needed.get(i);
            }
            ActivityCompat.requestPermissions(this, perms, PERM_REQUEST_CODE);
            return false;
        }
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.quit_torgi)
                .setMessage(R.string.quit_torgi_narrative)
                .setNegativeButton(R.string.quit_yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (serviceBound && (torgiService != null))
                            torgiService.shutdown();
                        AbstractTORGIActivity.this.finish();
                    }
                })
                .setPositiveButton(R.string.quit_run_in_background, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface arg0, int arg1) {
                        AbstractTORGIActivity.this.finish();
                    }
                }).create().show();
    }
}
