package br.com.mblabs.service;

import android.location.Location;
import android.support.annotation.NonNull;

public interface LocationServiceListener {

    void onGpsProviderDisabled(@NonNull String message);

    void onGpsProviderEnabled();

    void onLocationChanged(@NonNull Location location);

    void onTrackingStarted();

    void onTrackingStop();
}
