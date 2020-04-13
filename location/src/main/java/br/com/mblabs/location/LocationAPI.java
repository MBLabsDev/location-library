package br.com.mblabs.location;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@SuppressLint("MissingPermission")
public class LocationAPI {

    private static final String TAG = LocationAPI.class.getSimpleName();

    private static final String PREFERENCES = "br.com.mblabs.location.PREFERENCES";
    private static final String KEY_FIRST_TIME_ASK_PERMISSION = "br.com.mblabs.location.KEY_FIRST_TIME_ASK_PERMISSION";

    public static final String START_FOREGROUND_ACTION = "br.com.mblabs.location.action.START_FOREGROUND";
    public static final String UPDATE_LOCATION_UPDATES = "br.com.mblabs.location.action.UPDATE_LOCATION_UPDATES";
    public static final String STOP_FOREGROUND_ACTION = "br.com.mblabs.location.action.STOP_FOREGROUND";
    public static final int REQUEST_CODE_ACCESS_LOCATION = 9990;

    private final LocationManager locationManager;
    private static List<LocationSdkListener> locationSdkListeners = new ArrayList<>();

    private LocationAPI(final Context context) throws LocationException {
        if (context == null) {
            throw new LocationException(LocationException.LocationErrorCode.CONTEXT_NULL);
        }
        this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }

    public void init() {
        for (LocationSdkListener locationSdkListener : locationSdkListeners) {
            // Register the listener with the Location Manager to receive location updates
            requestLocationUpdates(locationSdkListener);
        }
    }

    public static void startService(Context context, Class<?> cls) {
        Intent startIntent = new Intent(context, cls);
        startIntent.setAction(START_FOREGROUND_ACTION);
        ContextCompat.startForegroundService(context, startIntent);
    }

    public static void stopService(Context context, Class<?> cls) {
        Intent stopIntent = new Intent(context, cls);
        context.stopService(stopIntent);
    }

    public static void updateService(Context context, Class<?> cls) {
        Intent updateIntent = new Intent(context, cls);
        updateIntent.setAction(UPDATE_LOCATION_UPDATES);
        ContextCompat.startForegroundService(context, updateIntent);
    }

    private void setListeners(final List<LocationSdkListener> locationSdkListeners) {
        LocationAPI.locationSdkListeners = locationSdkListeners;
    }

    public static boolean hasNecessaryPermissions(final Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    public static void requestLocationPermissions(final Activity activity, String[] permissions) {
        ActivityCompat.requestPermissions(activity, permissions, LocationAPI.REQUEST_CODE_ACCESS_LOCATION);
    }

    public static void checkLocationNecessarySettings(final Activity activity,
                                                      final LocationSettingsListener listener,
                                                      final String title,
                                                      final String message,
                                                      final String positiveButtonText) {

        if (!hasNecessaryPermissions(activity.getBaseContext())) {
            requestLocationPermissions(activity,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION});

        } else if (!hasGpsDevice(activity.getBaseContext()) && listener != null) {
            listener.onDeviceNotSupportGps();

        } else if (!isProviderEnabled(activity.getBaseContext(), LocationManager.GPS_PROVIDER)) {
            GpsProviderEnableDialog.enableGpsProvider(activity);

        } else if (!isIgnoringBatteryOptimizations(activity.getBaseContext())) {
            checkIgnoringBatteryOptimizations(activity, title, message, positiveButtonText);

        } else if(listener != null) {
            listener.onDeviceSettingsReady();
        }
    }

    public static boolean hasGpsDevice(final Context context) {
        final LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) {
            return false;
        }

        final List<String> providers = locationManager.getAllProviders();
        return providers != null && providers.contains(LocationManager.GPS_PROVIDER);
    }

    public static boolean isProviderEnabled(final Context context, final String provider) {
        return ((LocationManager) Objects.requireNonNull(context.getSystemService(Context.LOCATION_SERVICE)))
                .isProviderEnabled(provider);
    }

    public static boolean isIgnoringBatteryOptimizations(final Context context) {
        if (Build.VERSION.SDK_INT >= 23) {
            final String packageName = context.getPackageName();
            final PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            return pm.isIgnoringBatteryOptimizations(packageName);
        } else {
            return true;
        }
    }

    public static void checkIgnoringBatteryOptimizations(final Activity activity,
                                                         final String title,
                                                         final String message,
                                                         final String positiveButtonText) {
        // Necessary to some devices do not know how to treat Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
        try {
            final Intent intent = new Intent();
            intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + activity.getPackageName()));
            activity.startActivity(intent);

        } catch (final Exception ex) {
            final AlertDialog dialogIgnoreBatteryOptimizations = new AlertDialog.Builder(activity)
                    .setTitle(title)
                    .setMessage(message)
                    .setPositiveButton(positiveButtonText, (dialogInterface, i) -> {
                        dialogInterface.dismiss();
                    })
                    .setCancelable(false)
                    .create();
            dialogIgnoreBatteryOptimizations.show();
        }
    }

    public static void onRequestPermissionsResult(final Activity activity, int requestCode, String permissions[], int[] grantResults, final LocationSdkPermission locationSdkPermission) {
        if (requestCode == REQUEST_CODE_ACCESS_LOCATION) {
            for (int index = 0; index < permissions.length; index++) {
                if (grantResults[index] != PackageManager.PERMISSION_GRANTED) {
                    boolean showRationale = ActivityCompat.shouldShowRequestPermissionRationale(activity, permissions[index]);
                    if (!showRationale && !permissionFirstTimeAsking(activity)) {
                        setPermissionFirstTimeAsking(activity, false);
                        locationSdkPermission.onNeverAskAgain(permissions[index]);
                        return;
                    } else if (!permissionFirstTimeAsking(activity)) {
                        setPermissionFirstTimeAsking(activity, false);
                        locationSdkPermission.onShouldRequestPermissionRationale(permissions[index]);
                        return;
                    } else {
                        setPermissionFirstTimeAsking(activity, false);
                        locationSdkPermission.onPermissionDenied(permissions[index]);
                        return;
                    }
                }
            }

            locationSdkPermission.onPermissionsGranted();
        }
    }

    private static boolean permissionFirstTimeAsking(final Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFERENCES,
                Context.MODE_PRIVATE);
        return sharedPreferences.getBoolean(KEY_FIRST_TIME_ASK_PERMISSION, true);
    }

    private static void setPermissionFirstTimeAsking(final Context context, final boolean firstTimeAsking) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFERENCES,
                Context.MODE_PRIVATE);
        final SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(KEY_FIRST_TIME_ASK_PERMISSION, firstTimeAsking);
        editor.apply();
    }

    private void requestLocationUpdates(final LocationSdkListener locationSdkListener) {
        try {
            locationManager.requestLocationUpdates(
                    locationSdkListener.getProvider(), locationSdkListener.getMinTime(),
                    locationSdkListener.getMinDistance(), locationSdkListener);

        } catch (Exception ex) {
            Log.e(TAG, "Fail to request location update, ignore", ex);
        }
    }

    public void removeLocationUpdates() {
        for (LocationSdkListener locationSdkListener : locationSdkListeners) {
            try {
                locationManager.removeUpdates(locationSdkListener);
            } catch (Exception ex) {
                Log.e(TAG, "Fail to remove location provider, ignore", ex);
            }
        }
    }

    public static void lastKnowLocation(final Activity activity, final LocationSdkListener locationSdkListener) {
        FusedLocationProviderClient FusedLocationClient = LocationServices.getFusedLocationProviderClient(activity);
        FusedLocationClient.getLastLocation().addOnSuccessListener(activity, location -> {
            if (location != null) {
                locationSdkListener.onLocationChanged(location);
            }
        });
    }

    public static class Builder {

        private final Context context;
        private final List<LocationSdkListener> listeners;

        public Builder(final Context context) {
            this.context = context;
            this.listeners = new ArrayList<>();
        }

        public Builder addListener(final LocationSdkListener locationSdkListener) {
            listeners.add(locationSdkListener);
            return this;
        }

        public LocationAPI build() throws LocationException {
            LocationAPI locationAPI = new LocationAPI(context);
            locationAPI.setListeners(listeners);
            return locationAPI;
        }

        public void init() throws LocationException {
            LocationAPI locationAPI = new LocationAPI(context);
            locationAPI.setListeners(listeners);
            locationAPI.init();
        }
    }
}
