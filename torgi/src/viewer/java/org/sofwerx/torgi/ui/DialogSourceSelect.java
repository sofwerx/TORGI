package org.sofwerx.torgi.ui;

import android.app.AlertDialog;

import androidx.annotation.NonNull;
import com.google.android.material.textfield.TextInputLayout;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import org.sofwerx.torgi.Config;
import org.sofwerx.torgi.R;
import org.sofwerx.torgi.service.TorgiService;

import java.net.MalformedURLException;
import java.net.URL;

@Deprecated
public class DialogSourceSelect {
    //private static boolean ipChanged = false;
    private static boolean sourceSwitched = false;

    public static void show(@NonNull final MainActivity activity, @NonNull final TorgiService torgiService) {
        //ipChanged = false;
        sourceSwitched = false;
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        LayoutInflater inflater = activity.getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_source, null);
        builder.setView(view).setPositiveButton(android.R.string.ok, (dialog, id) -> {
                    dialog.dismiss();
                });
        //final EditText editIp = view.findViewById(R.id.sourceNetworkIP);
        //String ip = Config.getInstance(activity).getRemoteIp();
        //editIp.setText(ip);
        View viewSourceLocal = view.findViewById(R.id.sourceLocal);
        //View viewSourceNetwork = view.findViewById(R.id.sourceNetwork);
        View imageSourceLocal = view.findViewById(R.id.sourceImageLocal);
        //View imageSourceNetwork = view.findViewById(R.id.sourceImageNetwork);
        //final TextInputLayout editIpLayout = view.findViewById(R.id.sourceNetworkIPLayout);
        //if (ip == null)
        //    editIpLayout.setError(null);
        //try {
        //    new URL(ip);
        //} catch (MalformedURLException ignore) {
        //    editIpLayout.setError(activity.getString(R.string.invalid_url));
        //}
        if (torgiService.getInputType() == TorgiService.InputSourceType.LOCAL) {
            imageSourceLocal.setVisibility(View.VISIBLE);
        //    imageSourceNetwork.setVisibility(View.INVISIBLE);
        //} else if (torgiService.getInputType() == TorgiService.InputSourceType.NETWORK) {
        //    imageSourceLocal.setVisibility(View.INVISIBLE);
        //    imageSourceNetwork.setVisibility(View.VISIBLE);
        }
        viewSourceLocal.setOnClickListener(v -> {
            if (imageSourceLocal.getVisibility() == View.INVISIBLE) {
                sourceSwitched = true;
                imageSourceLocal.setVisibility(View.VISIBLE);
        //        imageSourceNetwork.setVisibility(View.INVISIBLE);
            }
        });
        /*viewSourceNetwork.setOnClickListener(v -> {
            if (imageSourceNetwork.getVisibility() == View.INVISIBLE) {
                sourceSwitched = true;
                imageSourceLocal.setVisibility(View.INVISIBLE);
                imageSourceNetwork.setVisibility(View.VISIBLE);
            }
        });
        editIp.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                ipChanged = true;
                String value = editIp.getText().toString();
                if (value == null)
                    editIpLayout.setError(null);
                try {
                    new URL(value);
                } catch (MalformedURLException ignore) {
                    editIpLayout.setError(activity.getString(R.string.invalid_url));
                }
            }
        });*/
        builder.setTitle(R.string.action_switch_input);
        builder.setOnCancelListener(dialog -> {
            //if (ipChanged)
            //    Config.getInstance(activity).setRemoteIp(editIp.getText().toString());
            if (sourceSwitched) {
                final TorgiService.InputSourceType type;
                if (imageSourceLocal.getVisibility() == View.VISIBLE)
                    type = TorgiService.InputSourceType.LOCAL;
                //else if (imageSourceNetwork.getVisibility() == View.VISIBLE)
                //    type = TorgiService.InputSourceType.NETWORK;
                else
                    type = null;
                if (type != null) {
                    torgiService.start(type);
                    activity.runOnUiThread(() -> {
                        activity.clear();
                        activity.onSourceUpdated(type);
                    });
                }
            }
        });
        builder.setOnDismissListener(dialog -> {
            //if (ipChanged)
            //    Config.getInstance(activity).setRemoteIp(editIp.getText().toString());
            if (sourceSwitched) {
                final TorgiService.InputSourceType type;
                if (imageSourceLocal.getVisibility() == View.VISIBLE)
                    type = TorgiService.InputSourceType.LOCAL;
                //else if (imageSourceNetwork.getVisibility() == View.VISIBLE)
                //    type = TorgiService.InputSourceType.NETWORK;
                else
                    type = null;
                if (type != null) {
                    torgiService.start(type);
                    activity.runOnUiThread(() -> {
                        activity.clear();
                        activity.onSourceUpdated(type);
                    });
                }
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }
}
