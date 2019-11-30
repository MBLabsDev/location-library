package br.com.mblabs.service;

import android.location.Location;

public interface LocationServiceListener {

    void onGpsProviderDisabled(String message);

    void onGpsProviderEnabled();

    void onLocationChanged(Location location);

    void onTrackingStarted();

    void onTrackingStop();
}
