package org.sofwerx.torgi.ui;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import org.sofwerx.torgi.Config;
import org.sofwerx.torgi.R;

import java.io.File;

public class SettingsActivity extends Activity {
    private final static int PICKFILE_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new PrefsFragment()).commit();
    }

    public static class PrefsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.prefs_app);
            Preference filePicker = findPreference("savedir");
            String dest = Config.getInstance(getActivity()).getSavedDir();
            filePicker.setSummary(dest);
            filePicker.setOnPreferenceClickListener(preference -> {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse(dest), "resource/folder");
                if (intent.resolveActivityInfo(getContext().getPackageManager(), 0) != null)
                    startActivity(intent);
                return true;
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == PICKFILE_REQUEST_CODE) {
                Uri pathUri = data.getData();
                if (pathUri != null) {
                    File file = new File(pathUri.getPath());
                    if ((file != null) && file.exists()) {
                        String abs = file.getAbsolutePath();
                        Config.getInstance(this).setSavedDir(abs);
                    }
                }
            }
        }
    }
}
