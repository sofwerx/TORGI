package org.sofwerx.torgi.ui;

import android.app.Activity;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.TextView;

import org.sofwerx.torgi.R;
import org.sofwerx.torgi.util.Acknowledgements;

public class AboutActivity extends Activity {
    private TextView ackTitle;
    private TextView ack;
    private TextView licTitle;
    private TextView lic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about_activity);
        //Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        //setSupportActionBar(toolbar);
        ackTitle = findViewById(R.id.legalAckTitle);
        ack = findViewById(R.id.legalAck);
        licTitle = findViewById(R.id.legalLicenseTitle);
        lic = findViewById(R.id.legalLicense);
        ackTitle.setOnClickListener(v -> {
            if (ack.getVisibility() == View.VISIBLE) {
                ack.setVisibility(View.GONE);
                ackTitle.setCompoundDrawablesWithIntrinsicBounds(R.drawable.icon_expanded_white, 0, 0, 0);
            } else {
                ack.setVisibility(View.VISIBLE);
                ackTitle.setCompoundDrawablesWithIntrinsicBounds(R.drawable.icon_expandable_white, 0, 0, 0);
            }
        });
        licTitle.setOnClickListener(v -> {
            if (lic.getVisibility() == View.VISIBLE) {
                lic.setVisibility(View.GONE);
                licTitle.setCompoundDrawablesWithIntrinsicBounds(R.drawable.icon_expanded_white, 0, 0, 0);
            } else {
                lic.setVisibility(View.VISIBLE);
                licTitle.setCompoundDrawablesWithIntrinsicBounds(R.drawable.icon_expandable_white, 0, 0, 0);
            }
        });
        ack.setText(Html.fromHtml(Acknowledgements.getCredits()));
        ack.setMovementMethod(LinkMovementMethod.getInstance());
        lic.setText(Html.fromHtml(Acknowledgements.getLicenses()));
        lic.setMovementMethod(LinkMovementMethod.getInstance());
    }
}