package br.com.mblabs.service;

import android.annotation.TargetApi;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import br.com.mblabs.Notification;

import static br.com.mblabs.location.LocationAPI.START_FOREGROUND_ACTION;
import static br.com.mblabs.location.LocationAPI.STOP_FOREGROUND_ACTION;
import static br.com.mblabs.location.LocationAPI.UPDATE_LOCATION_UPDATES;

/**
 * Responsible for the services related to the location of the driver. Your responsibilities are:<br>
 * <li>Retrieve user location (lat/lng)<li/>
 * <li>Retrieve user speed from locations<li/>
 */
public abstract class LocationService extends Service {

    private static final String TAG = LocationService.class.getSimpleName();

    private static final String ANDROID_CHANNEL_SERVICE_ID = "location_service";
    private static final String ANDROID_CHANNEL_SERVICE = "Serviço de localização";

    private PowerManager powerManager;
    private PowerManager.WakeLock wakeLock;

    private final IBinder iBinder = new LocalBinder();

    private final List<LocationListener> locationListeners = Arrays.asList(
            new LocationListener(LocationManager.GPS_PROVIDER),
            new LocationListener(LocationManager.NETWORK_PROVIDER));

    private LocationManager locationManager;
    private List<LocationServiceListener> locationServiceListenerList = new ArrayList<>();

    protected abstract boolean isDebugMode();

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
        LocationService.this.locationServiceListenerList.remove(removeLocationServiceListener());
    }

    private void sendOnLocationChangedEvents(final Location location) {
        for (LocationServiceListener listener : locationServiceListenerList) {
            if (location.getAccuracy() <= getBestAccuracy()) {
                listener.onLocationChanged(location);
            }
        }
    }

    private void sendOnTrackingStarted() {
        for (LocationServiceListener listener : locationServiceListenerList) {
            listener.onTrackingStarted();
        }
    }

    private void sendOnTrackingStop() {
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
        logService("onStartCommand");

        if (intent != null && START_FOREGROUND_ACTION.equals(intent.getAction())) {
            Log.i(TAG, "Received Start Foreground Intent ");
            startForeground();

        } else if (intent != null && UPDATE_LOCATION_UPDATES.equals(intent.getAction())) {
            Log.i(TAG, "Update location settings");
            updateLocationSettings();
            startForeground();

        } else if (intent != null && STOP_FOREGROUND_ACTION.equals(intent.getAction())) {
            Log.i(TAG, "Received Stop Foreground Intent");
            stopForeground(true);
            stopSelf();
        }
        return START_STICKY;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        logService("onCreate");
        powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getTag());
        handleSleepMode();
        LocationService.this.locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        requestLocationUpdates(getMinTime(), getMinDistance());
        addTrackingServiceListener();
        sendOnTrackingStarted();
    }

    private void updateLocationSettings() {
        LocationService.this.locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        requestLocationUpdates(getMinTime(), getMinDistance());
    }

    @Override
    public void onDestroy() {
        logService("onDestroy");
        removeLocationUpdates();
        removeTrackingServiceListener();
        stopForeground(true);
        sendOnTrackingStop();
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
        final NotificationCompat.Builder builder = Notification.getBuilder(
                this, notification.getChannelName(), notification.getChannel());

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

    @TargetApi(Build.VERSION_CODES.O)
    public NotificationChannel getForegroundNotificationChannel() {
        NotificationChannel channelService = new NotificationChannel(
                ANDROID_CHANNEL_SERVICE_ID,
                ANDROID_CHANNEL_SERVICE,
                NotificationManager.IMPORTANCE_LOW);

        channelService.enableLights(true);
        channelService.enableVibration(false);
        channelService.setLightColor(Color.GREEN);
        channelService.setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC);
        return channelService;
    }

    protected ForegroundNotification getForegroundNotificationSpec() {
        ForegroundNotification notification = new ForegroundNotification();
        notification.setId(getForegroundId());
        notification.setText(getNotificationText());
        notification.setTitle(getNotificationTitle());
        notification.setResourceId(getNotificationSmallIcon());
        notification.setColor(ContextCompat.getColor(this, getNotificationColor()));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notification.setChannel(getForegroundNotificationChannel());
            notification.setChannelName(ANDROID_CHANNEL_SERVICE_ID);
        }

        Intent launchIntent = getPackageManager()
                .getLaunchIntentForPackage(getApplicationContext().getPackageName());
        final PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                launchIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        notification.setIntent(pendingIntent);
        notification.setOngoing(true);
        return notification;
    }

    protected final void handleSleepMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            if (!powerManager.isInteractive() && !wakeLock.isHeld()) {
                logService("Acquire lock");
                wakeLock.acquire();
            } else if (powerManager.isInteractive() && wakeLock.isHeld()) {
                logService("Release lock");
                wakeLock.release();
            }
        } else {
            if (!powerManager.isScreenOn() && !wakeLock.isHeld()) {
                logService("Acquire lock");
                wakeLock.acquire();
            } else if (powerManager.isScreenOn() && wakeLock.isHeld()) {
                logService("Release lock");
                wakeLock.release();
            }
        }
    }

    private void logService(final String message) {
        if (isDebugMode()) {
            Log.i(getTag(), message);
        }
    }

    private void requestLocationUpdates(final long minTime, final long minDistance) {
        for (LocationListener locationListener : locationListeners) {
            try {
                LocationService.this.locationManager.requestLocationUpdates(
                        locationListener.getProvider(), minTime, minDistance,
                        locationListener);
            } catch (SecurityException ex) {
                logService("Fail to request location update, ignore: " + ex.getMessage());
            } catch (Exception ex) {
                logService("Fail to request location update, ignore: " + ex.getMessage());
            }
        }
    }

    private void removeLocationUpdates() {
        for (LocationListener locationListener : locationListeners) {
            try {
                LocationService.this.locationManager.removeUpdates(locationListener);
            } catch (Exception ex) {
                logService("Fail to remove location provider, ignore: " + ex.getMessage());
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
        public void onLocationChanged(final Location location) {
            sendOnLocationChangedEvents(location);
        }

        @Override
        public void onProviderDisabled(final String provider) {
            logService(provider);
            if (provider.equals(LocationManager.GPS_PROVIDER)) {
                sendOnGpsProviderDisabled();
            }
        }

        @Override
        public void onProviderEnabled(final String provider) {
            logService(provider);
            if (provider.equals(LocationManager.GPS_PROVIDER)) {
                sendOnGpsProviderEnabled();
            }
        }

        @Override
        public void onStatusChanged(final String provider, final int status, final Bundle extras) {
            logService(provider + " " + status + " " + extras.toString());
            logService("onStatusChanged: " + provider + " " + convertProviderStatus(status));
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
