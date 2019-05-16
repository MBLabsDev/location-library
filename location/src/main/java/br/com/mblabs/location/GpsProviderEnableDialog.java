package br.com.mblabs.location;

import android.app.Activity;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.*;

public class GpsProviderEnableDialog {

    private static GoogleApiClient googleApiClient;
    private static LocationRequest locationRequest;
    public static int REQUEST_CHECK_SETTINGS = 9010;

    public static void enableGpsProvider(@NonNull final Activity activity) {
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(30 * 1000);
        locationRequest.setFastestInterval(5 * 1000);

        googleApiClient = new GoogleApiClient.Builder(activity)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(@Nullable Bundle bundle) {
                        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                                .addLocationRequest(locationRequest);
                        builder.setAlwaysShow(true);
                        PendingResult<LocationSettingsResult> result =
                                LocationServices.SettingsApi.checkLocationSettings(
                                        googleApiClient,
                                        builder.build()
                                );

                        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
                            @Override
                            public void onResult(@NonNull LocationSettingsResult locationSettingsResult) {
                                final Status status = locationSettingsResult.getStatus();
                                switch (status.getStatusCode()) {
                                    case LocationSettingsStatusCodes.SUCCESS:
                                        // NO need to show the dialog;
                                        break;

                                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                        //  GPS turned off, Show the user a dialog
                                        try {
                                            // Show the dialog by calling startResolutionForResult(), and check the result
                                            // in onActivityResult().
                                            status.startResolutionForResult(activity, REQUEST_CHECK_SETTINGS);
                                        } catch (IntentSender.SendIntentException e) {
                                            //failed to show dialog
                                        }
                                        break;

                                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                        // Location settings are unavailable so not possible to show any dialog now
                                        break;
                                }
                            }
                        });
                    }

                    @Override
                    public void onConnectionSuspended(int i) {

                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

                    }
                }).build();

        googleApiClient.connect();
    }

    public static void disconnect() {
        if (googleApiClient != null && googleApiClient.isConnected()) {
            googleApiClient.disconnect();
        }
    }
}
