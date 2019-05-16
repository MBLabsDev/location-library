package br.com.mblabs.service;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.*;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import br.com.mblabs.Notification;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Responsible for the services related to the location of the driver. Your responsibilities are:<br>
 * <li>Retrieve user location (lat/lng)<li/>
 * <li>Retrieve user speed from locations<li/>
 */
public abstract class LocationService extends Service {

    private static final String TAG = LocationService.class.getSimpleName();

    private PowerManager powerManager;
    private PowerManager.WakeLock wakeLock;

    private final IBinder iBinder = new LocalBinder();

    private final List<LocationListener> locationListeners = Arrays.asList(
            new LocationListener(LocationManager.GPS_PROVIDER),
            new LocationListener(LocationManager.NETWORK_PROVIDER));
    private LocationManager locationManager;
    private List<LocationServiceListener> locationServiceListenerList = new ArrayList<>();

    private final Handler handlerScheduler = new Handler(message -> false);

    private Runnable stopTrackingEventRunnable = this::sendOnTrackingStopEvents;

    protected abstract int getForegroundId();

    protected abstract float getBestAccuracy();

    protected abstract String getNotificationTitle();

    protected abstract String getNotificationText();

    protected abstract int getNotificationColor();

    protected abstract int getNotificationSmallIcon();

    protected abstract String getActionTracking();

    protected abstract int getMinTime();

    protected abstract int getMinDistance();

    protected abstract LocationServiceListener getLocationServiceListener();

    protected abstract LocationServiceListener removeLocationServiceListener();

    private void addTrackingServiceListener() {
        if (!LocationService.this.locationServiceListenerList.contains(getLocationServiceListener())) {
            LocationService.this.locationServiceListenerList.add(getLocationServiceListener());
        }
    }

    private void removeTrackingServiceListener() {
        if (LocationService.this.locationServiceListenerList.contains(removeLocationServiceListener())) {
            LocationService.this.locationServiceListenerList.remove(removeLocationServiceListener());
        }
    }

    private void sendOnLocationChangedEvents(final Location location) {
        for (LocationServiceListener listener : locationServiceListenerList) {
            if (location.getAccuracy() <= getBestAccuracy()) {
                listener.onLocationChanged(location);
            }
        }
    }

    private void sendOnTrackingStartedEvents() {
        for (LocationServiceListener listener : locationServiceListenerList) {
            listener.onTrackingStarted();
        }
    }

    private void sendOnTrackingStopEvents() {
        for (LocationServiceListener listener : locationServiceListenerList) {
            listener.onTrackingStop();
        }
    }

    private void sendOnGpsProviderDisabled() {
        for (LocationServiceListener listener : locationServiceListenerList) {
            listener.onGpsProviderDisabled("We need your location so we can follow up on your travels");
        }
    }

    private void sendOnGpsProviderEnabled() {
        for (LocationServiceListener listener : locationServiceListenerList) {
            listener.onGpsProviderEnabled();
        }
    }

    @Override
    public final int onStartCommand(final Intent intent, final int flags, final int startId) {
        super.onStartCommand(intent, flags, startId);
        logService(" onStartCommand");
        startForeground();
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        logService(" onCreate");
        powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getTag());
        handleSleepMode();
        LocationService.this.locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        requestLocationUpdates(getMinTime(), getMinDistance());
        addTrackingServiceListener();
    }

    @Override
    public void onDestroy() {
        logService(" onDestroy");
        LocationService.this.handlerScheduler.removeCallbacks(LocationService.this.stopTrackingEventRunnable);
        removeLocationUpdates();
        removeTrackingServiceListener();
        stopForeground(true);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(final Intent intent) {
        return iBinder;
    }

    public final class LocalBinder extends Binder {
        public LocationService getServiceInstance() {
            return LocationService.this;
        }
    }

    protected String getTag() {
        return TAG;
    }

    private void startForeground() {
        final ForegroundNotification notification = getForegroundNotificationSpec();
        final NotificationCompat.Builder builder = Notification.getBuilder(this, Notification.CHANNEL_DEFAULT, Notification.IMPORTANCE_DEFAULT);

        builder.setOngoing(notification.isOngoing())
                .setContentTitle(notification.getTitle())
                .setContentText(notification.getText())
                .setColor(notification.getColor())
                .setSmallIcon(notification.getResourceId());

        if (notification.getIntent() != null) {
            builder.setContentIntent(notification.getIntent());
        }

        startForeground(notification.getId(), builder.build());
    }

    protected ForegroundNotification getForegroundNotificationSpec() {
        ForegroundNotification notification = new ForegroundNotification();
        notification.setId(getForegroundId());
        notification.setText(getNotificationText());
        notification.setTitle(getNotificationTitle());
        notification.setResourceId(getNotificationSmallIcon());
        notification.setColor(ContextCompat.getColor(this, getNotificationColor()));

        final Intent intent = new Intent();
        intent.setAction(getApplicationContext().getPackageName() + getActionTracking());
        final PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0,
                intent, PendingIntent.FLAG_CANCEL_CURRENT);

        notification.setIntent(pendingIntent);
        notification.setOngoing(true);
        return notification;
    }

    protected final void handleSleepMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            if (!powerManager.isInteractive() && !wakeLock.isHeld()) {
                Log.i(getTag(), "Acquire lock");
                wakeLock.acquire();
            } else if (powerManager.isInteractive() && wakeLock.isHeld()) {
                Log.i(getTag(), "Release lock");
                wakeLock.release();
            }
        } else {
            if (!powerManager.isScreenOn() && !wakeLock.isHeld()) {
                Log.i(getTag(), "Acquire lock");
                wakeLock.acquire();
            } else if (powerManager.isScreenOn() && wakeLock.isHeld()) {
                Log.i(getTag(), "Release lock");
                wakeLock.release();
            }
        }
    }

    private void logService(final String eventLifecycle) {
        Log.i(getTag(), getTag() + eventLifecycle);
    }

    private void requestLocationUpdates(final long minTime, final long minDistance) {
        for (LocationListener locationListener : locationListeners) {
            try {
                LocationService.this.locationManager.requestLocationUpdates(
                        locationListener.getProvider(), minTime, minDistance,
                        locationListener);
            } catch (SecurityException ex) {
                Log.e(TAG, "Fail to request location update, ignore", ex);
            } catch (Exception ex) {
                Log.e(TAG, "Fail to request location update, ignore", ex);
            }
        }
    }

    private void removeLocationUpdates() {
        for (LocationListener locationListener : locationListeners) {
            try {
                LocationService.this.locationManager.removeUpdates(locationListener);
            } catch (Exception ex) {
                Log.e(TAG, "Fail to remove location provider, ignore", ex);
            }
        }
    }

    private final class LocationListener implements android.location.LocationListener {

        private final String provider;

        LocationListener(final String provider) {
            this.provider = provider;
        }

        String getProvider() {
            return provider;
        }

        @Override
        public void onLocationChanged( final Location location) {
            Log.d(TAG, location.getLatitude() + "/" + location.getLongitude());
            sendOnLocationChangedEvents(location);
        }

        @Override
        public void onProviderDisabled(final String provider) {
            Log.d(TAG, provider);
            if (provider.equals(LocationManager.GPS_PROVIDER)) {
                sendOnGpsProviderDisabled();
            }
        }

        @Override
        public void onProviderEnabled(final String provider) {
            Log.d(TAG, provider);
            if (provider.equals(LocationManager.GPS_PROVIDER)) {
                sendOnGpsProviderEnabled();
            }
        }

        @Override
        public void onStatusChanged(final String provider, final int status, final Bundle extras) {
            Log.d(TAG, provider + " " + status + " " + extras.toString());
            Log.i(TAG, "onStatusChanged: " + provider + " " + convertProviderStatus(status));
        }

        private String convertProviderStatus(final int status) {
            switch (status) {
                case 2:
                    return "AVAILABLE";
                case 1:
                    return "TEMPORARILY_UNAVAILABLE";
                case 0:
                    return "OUT_OF_SERVICE";
                default:
                    return "";
            }
        }
    }
}
