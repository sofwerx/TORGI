package org.sofwerx.torgi.gnss;

public class Satellite {
    private Constellation constellation;
    private int svid;
    private GNSSEWValues baseline = null;

    public Satellite(Constellation constellation, int svid) {
        this.constellation = constellation;
        this.svid = svid;
    }

    public boolean equals(Satellite sat) {
        if (sat == null)
            return false;
        return (constellation == sat.constellation) && (svid == sat.svid);
    }

    @Override
    public String toString() {
        return constellation.name()+svid;
    }

    public GNSSEWValues getBaseline() {
        return baseline;
    }

    public void setBaseline(GNSSEWValues baseline) {
        this.baseline = baseline;
    }
}
