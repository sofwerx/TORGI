package org.sofwerx.torgi.gnss;

import android.util.Log;

import java.util.ArrayList;

public class Satellite {
    private final static String TAG = "TORGI.EW";
    private Constellation constellation;
    private int svid;
    private GNSSEWValues baseline = null;
    private static ArrayList<Satellite> satellites = new ArrayList<>();

    private Satellite(Constellation constellation, int svid) {
        this.constellation = constellation;
        this.svid = svid;
        satellites.add(this);
    }

    public static Satellite get(Constellation constellation, int svid) {
        Satellite sat = null;
        if (!satellites.isEmpty()) {
            for (Satellite old:satellites) {
                if (old.equals(constellation,svid)) {
                    sat = old;
                    break;
                }
            }
        }
        if (sat == null)
            return new Satellite(constellation, svid);
        else
            return sat;
    }

    public Constellation getConstellation() {
        return constellation;
    }

    public boolean equals(Satellite sat) {
        if (sat == null)
            return false;
        return (constellation == sat.constellation) && (svid == sat.svid);
    }

    public boolean equals(Constellation constellation, int svid) {
        return (this.constellation == constellation) && (this.svid == svid);
    }

    @Override
    public String toString() {
        return constellation.name()+svid;
    }

    public GNSSEWValues getBaseline() {
        return baseline;
    }

    public void setBaseline(GNSSEWValues baseline) {
        if (baseline != null)
            Log.d(TAG,toString()+" baseline updated to "+baseline.toString());
        else
            Log.d(TAG,toString()+" baseline erased");
        this.baseline = baseline;
    }
}
