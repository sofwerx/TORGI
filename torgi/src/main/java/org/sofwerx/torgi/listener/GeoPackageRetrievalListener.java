package org.sofwerx.torgi.listener;

import android.location.GnssMeasurement;
import android.support.annotation.NonNull;

import org.sofwerx.torgi.gnss.helper.GeoPackageGPSPtHelper;
import org.sofwerx.torgi.gnss.helper.GeoPackageSatDataHelper;

import java.util.ArrayList;

public interface GeoPackageRetrievalListener {
    void onGnssSatDataRetrieved(ArrayList<GeoPackageSatDataHelper> measurements);
    void onGnssGeoPtRetrieved(ArrayList<GeoPackageGPSPtHelper> measurements);
}
