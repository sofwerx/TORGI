package org.sofwerx.torgi.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.sofwerx.torgi.R;

public class GNSSStatusView extends RelativeLayout {
    private final static long DELAY_TO_REDUCE_WARNING = 1000l * 60l; //provides a waiting period (milliseconds) before the warning level is allowed to be reduced
    private boolean showText = false;
    public final static int STATUS_DISABLED = -1;
    private int warnPercent = STATUS_DISABLED;
    private View blackBar;
    private TextView label;
    private final static int LOW_RISK_CAP = 15;
    private final static int MEDIUM_RISK_CAP = 45;
    private long nextDowngradeEligibleTime = Long.MIN_VALUE;
    private final Context context;

    public GNSSStatusView(Context context) {
        super(context);
        this.context = context;
        init(null, 0);
    }

    public GNSSStatusView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        init(attrs, 0);
    }

    public GNSSStatusView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.context = context;
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {
        // Load attributes
        final TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.GNSSStatusView, defStyle, 0);

        showText = a.getBoolean(R.styleable.GNSSStatusView_showGNSSText,true);
        a.recycle();

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.gnss_status_view, this);
        blackBar = findViewById(R.id.gnssViewBlackBar);
        blackBar.setVisibility(View.GONE);
        label = findViewById(R.id.gnssViewLabel);
        if (showText)
            label.setVisibility(View.VISIBLE);
        else
            label.setVisibility(View.GONE);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

    }

    public int getWarnPercent() {
        return warnPercent;
    }

    public void setWarnPercent(int warnPercent) {
        if (this.warnPercent != warnPercent) {
            if (warnPercent < this.warnPercent) {
                //ignore this update if still in the warning period for the last elevated warning level
                if (System.currentTimeMillis() < nextDowngradeEligibleTime)
                    return;
            } else
                nextDowngradeEligibleTime = System.currentTimeMillis() + DELAY_TO_REDUCE_WARNING;
            if (this.warnPercent < 0)
                blackBar.setVisibility(View.VISIBLE);
            else if (warnPercent < 0) {
                label.setText(context.getString(R.string.risk_unknown));
                blackBar.setVisibility(View.GONE);
            }
            this.warnPercent = warnPercent;
            if (warnPercent >= 0) {
                int width = this.getWidth();
                int desiredWidth = (100 - warnPercent) * width / 100;
                if (desiredWidth > width)
                    desiredWidth = width;
                else if (desiredWidth < 0)
                    desiredWidth = 0;
                blackBar.getLayoutParams().width = desiredWidth;
                if (showText) {
                    if (warnPercent < LOW_RISK_CAP)
                        label.setText(context.getString(R.string.risk_low));
                    else if (warnPercent < MEDIUM_RISK_CAP)
                        label.setText(context.getString(R.string.risk_medium));
                    else
                        label.setText(context.getString(R.string.risk_high));
                    label.setVisibility(View.VISIBLE);
                } else
                    label.setVisibility(View.GONE);
            }
            invalidate();
        }
    }

    public void clear() {
        setWarnPercent(-1);
    }
}
