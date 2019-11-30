package br.com.example;

import android.location.Location;
import android.util.Log;

import br.com.mblabs.service.LocationService;
import br.com.mblabs.service.LocationServiceListener;

public class AppTestLocationService extends LocationService {

    private final static String TAG = AppTestLocationService.class.getSimpleName();

    LocationServiceListener locationServiceListener = new LocationServiceListener() {
        @Override
        public void onGpsProviderDisabled(String message) {
            Log.d(TAG, "onGpsProviderDisabled - " + message);
        }

        @Override
        public void onGpsProviderEnabled() {
            Log.d(TAG, "onGpsProviderDisabled");
        }

        @Override
        public void onLocationChanged(Location location) {
            Log.d(TAG, "onLocationChanged - " + location.getLatitude() + " / " + location.getLongitude());
        }

        @Override
        public void onTrackingStarted() {
            Log.d(TAG, "onTrackingStarted");
        }

        @Override
        public void onTrackingStop() {
            Log.d(TAG, "onTrackingStop");
        }
    };

    @Override
    protected boolean isDebugMode() {
        return false;
    }

    @Override
    protected int getForegroundId() {
        return 12345678;
    }

    @Override
    protected float getBestAccuracy() {
        return 50;
    }


    @Override
    protected String getNotificationTitle() {
        return "Notification Title test";
    }


    @Override
    protected String getNotificationText() {
        return "Notification Text test";
    }

    @Override
    protected int getNotificationColor() {
        return R.color.colorAccent;
    }

    @Override
    protected int getNotificationSmallIcon() {
        return R.drawable.ic_launcher_foreground;
    }


    @Override
    protected String getActionTracking() {
        return "ACTION_TRACKING";
    }

    @Override
    protected int getMinTime() {
        return 0;
    }

    @Override
    protected int getMinDistance() {
        return 0;
    }

    @Override
    protected LocationServiceListener getLocationServiceListener() {
        return locationServiceListener;
    }

    @Override
    protected LocationServiceListener removeLocationServiceListener() {
        return locationServiceListener;
    }
}
