package org.sofwerx.torgi.gnss.helper;

import org.sofwerx.torgi.gnss.Constellation;

public class GeoPackageSatDataHelper {
    private long meassuredTime;
    private long id;
    private int svid;
    private Constellation constellation;
    private double cn0;
    private double agc;

    public int getSvid() {
        return svid;
    }

    public void setSvid(Object svid) {
        this.svid = (int)((long)svid);
    }

    public Constellation getConstellation() {
        return constellation;
    }

    public void setConstellation(String constellation) {
        this.constellation = Constellation.valueOf((String) constellation);
    }

    public void setConstellation(Constellation constellation) {
        this.constellation = constellation;
    }

    public double getCn0() {
        return cn0;
    }

    public void setCn0(Object cn0) {
        this.cn0 = (double)cn0;
    }

    public double getAgc() {
        return agc;
    }

    public void setAgc(Object agc) {
        this.agc = (double)agc;
    }

    public long getId() {
        return id;
    }

    public void setId(Object id) {
        this.id = (long)id;
    }

    public long getMeassuredTime() {
        return meassuredTime;
    }

    public void setMeassuredTime(Object meassuredTime) {
        this.meassuredTime = (long) meassuredTime;
    }

    public boolean isSame(GeoPackageSatDataHelper other) {
        if (other == null)
            return false;
        return (constellation == other.constellation) && (svid == other.svid) && (meassuredTime == other.meassuredTime);
    }

    public void update(GeoPackageSatDataHelper other) {
        if (other == null)
            return;
        if (other.id != Long.MIN_VALUE)
            id = other.id;
        if ((!Double.isNaN(other.agc) && (Double.compare(0d,other.agc))!=0))
            agc = other.agc;
        if ((!Double.isNaN(other.cn0) && (Double.compare(0d,other.cn0))!=0))
            cn0 = other.cn0;
    }
}
