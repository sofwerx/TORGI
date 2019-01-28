package org.sofwerx.torgi;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;

import org.sofwerx.torgi.util.CallsignUtil;

import java.io.File;
import java.util.UUID;

public class Config {
    public final static String PREFS_SAVE_DIR = "savedir";
    public final static String PREFS_AUTO_SHARE = "autoshare";
    public final static String PREFS_PROCESS_EW = "processew";
    public final static String PREFS_UUID = "callsign";
    public final static String PREFS_LAST_SOS_SERVER_IP = "sosip";
    public final static String PREFS_REMOTE_IP = "remoteip";
    public final static String PREFS_GPS_ONLY = "gpsonly";
    public final static String PREFS_BROADCAST = "broadcast";
    public final static String PREFS_SEND_TO_SOS = "sendtosos";
    public final static String PREFS_SOS_URL = "sosurl";

    private static Config instance = null;
    private String savedDir = null;
    private boolean processEWonboard = false;
    private static boolean broadcast = true;
    private SharedPreferences prefs = null;
    private String uuid = null;
    private Context context;
    private String remoteIP = null;
    private static boolean gpsOnly = false;

    private Config(Context context) {
        this.context = context;
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        processEWonboard = prefs.getBoolean(PREFS_PROCESS_EW,false);
        broadcast = prefs.getBoolean(PREFS_BROADCAST,true);
        if (prefs.getString(PREFS_UUID,null) == null) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(PREFS_UUID, CallsignUtil.getRandomCallsign());
            editor.apply();
        }
    }

    public void loadPrefs() {
        gpsOnly = prefs.getBoolean(PREFS_GPS_ONLY,false);
    }

    public String getRemoteIp() {
        if (remoteIP == null)
            remoteIP = prefs.getString(PREFS_REMOTE_IP,null);
        return remoteIP;
    }

    /**
     * Sets the ip (or ip and port like "172.16.117.191:8080" for the SOS that is serving up TORGI data)
     * @param ip
     */
    public void setRemoteIp(String ip) {
        SharedPreferences.Editor edit = prefs.edit();
        if (ip == null)
            edit.remove(PREFS_REMOTE_IP);
        else
            edit.putString(PREFS_REMOTE_IP, ip);
        edit.commit();
        remoteIP = ip;
    }

    public static Config getInstance(Context context) {
        if (instance == null)
            instance = new Config(context);
        return instance;
    }

    public void setProcessEWonboard(boolean processEWonboard) {
        this.processEWonboard = processEWonboard;
        SharedPreferences.Editor edit = prefs.edit();
        edit.putBoolean(PREFS_PROCESS_EW,processEWonboard);
        edit.commit();
    }

    public boolean isAutoShareEnabled() {
        return prefs.getBoolean(PREFS_AUTO_SHARE,true);
    }

    public static boolean isGpsOnly() {
        return gpsOnly;
    }

    public void setGpsOnly(boolean gpsOnly) {
        Config.gpsOnly = gpsOnly;
        prefs.edit().putBoolean(PREFS_GPS_ONLY,gpsOnly).commit();
    }

    public boolean processEWOnboard() {
        return processEWonboard;
    }

    public String getUuid() {
        if (uuid == null) {
            uuid = prefs.getString(PREFS_UUID,null);
            if (uuid == null) {
                uuid = UUID.randomUUID().toString();
                prefs.edit().putString(PREFS_UUID,uuid).apply();
            }
        }
        return uuid;
    }

    public String getSavedDir() {
        if (savedDir == null) {
            /*savedDir = prefs.getString(PREFS_SAVE_DIR, null);
            if (savedDir != null) {
                try {
                    Uri savedDirUri = Uri.parse(savedDir);
                    if (savedDirUri != null) {
                        context.getContentResolver().takePersistableUriPermission(savedDirUri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION |
                                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION); //Keep the permissions to access this location up to date across reboots
                    }
                } catch (NullPointerException ignore) {}
            }
            if (savedDir == null)
                savedDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();*/
            if (savedDir == null) {
                File folder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "TORGI");
                folder.mkdirs();
                savedDir = folder.getAbsolutePath();
            }
        }
        return savedDir;
    }

    public void setSavedDir(String savedDir) {
        SharedPreferences.Editor edit = prefs.edit();
            if (savedDir == null)
                edit.remove(PREFS_SAVE_DIR);
            else
                edit.putString(PREFS_SAVE_DIR,savedDir);
        edit.commit();
    }

    /**
     * Is the app allowed to respond back to requests from other apps via IPC for sensor data
     * @return
     */
    public static boolean isBroadcastSupported() {
        return broadcast;
    }
}
