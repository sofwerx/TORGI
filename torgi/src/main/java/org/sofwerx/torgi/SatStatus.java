package org.sofwerx.torgi;

public class SatStatus {
    public String constellation;
    public int svid;

    public double cn0;
    public boolean in_fix;

    public boolean has_almanac;
    public boolean has_ephemeris;
    public boolean has_carrier_freq;

    public double elevation_deg;
    public double azimuth_deg;
    public double carrier_freq_hz;

    private boolean usedInFix = false;

    public boolean equals(SatStatus status) {
        if (status == null)
            return false;
        if (svid == status.svid) {
            if ((constellation == null) && (status.constellation == null))
                return true;
            else {
                if (constellation == null)
                    return false;
                return constellation.equalsIgnoreCase(status.constellation);
            }
        }
        return false;
    }

    public void update(SatStatus status) {
        if (status != null) {
            cn0 = status.cn0;
            in_fix = status.in_fix;
            has_almanac = status.has_almanac;
            has_ephemeris = status.has_ephemeris;
            has_carrier_freq = status.has_carrier_freq;
            elevation_deg = status.elevation_deg;
            azimuth_deg = status.azimuth_deg;
            carrier_freq_hz = status.carrier_freq_hz;
            usedInFix = status.usedInFix;
        }
    }

    public boolean isUsedInFix() {
        return usedInFix;
    }

    public void setUsedInFix(boolean usedInFix) {
        this.usedInFix = usedInFix;
    }
}
