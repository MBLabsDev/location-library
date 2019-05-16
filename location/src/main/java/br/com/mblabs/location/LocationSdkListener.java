package br.com.mblabs.location;

import android.location.LocationListener;

public interface LocationSdkListener extends LocationListener {

    long getMinTime();

    float getMinDistance();

    String getProvider();
}
