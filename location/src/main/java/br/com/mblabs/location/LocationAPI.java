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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
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

    public static final int REQUEST_CODE_ACCESS_LOCATION = 9990;

    private final LocationManager locationManager;
    private static List<LocationSdkListener> locationSdkListeners = new ArrayList<>();

    private LocationAPI(@Nullable final Context context) throws LocationException {
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

    private void setListeners(@NonNull final List<LocationSdkListener> locationSdkListeners) {
        LocationAPI.locationSdkListeners = locationSdkListeners;
    }

    public static boolean hasNecessaryPermissions(@NonNull final Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    public static void requestLocationPermissions(@NonNull final Activity activity, @NonNull String[] permissions) {
        ActivityCompat.requestPermissions(activity, permissions, LocationAPI.REQUEST_CODE_ACCESS_LOCATION);
    }

    public static void checkLocationNecessarySettings(@NonNull final Activity activity,
                                                      @NonNull final LocationSettingsListener listener,
                                                      @NonNull final String title,
                                                      @NonNull final String message,
                                                      @NonNull final String positiveButtonText) {
        if (!hasNecessaryPermissions(activity.getBaseContext())) {
            requestLocationPermissions(activity,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION});
        } else if (!hasGpsDevice(activity.getBaseContext())) {
            listener.onDeviceNotSupportGps();
        } else if (!isProviderEnabled(activity.getBaseContext(), LocationManager.GPS_PROVIDER)) {
            GpsProviderEnableDialog.enableGpsProvider(activity);
        } else if (!isIgnoringBatteryOptimizations(activity.getBaseContext())) {
            checkIgnoringBatteryOptimizations(activity, title, message, positiveButtonText);
        } else {
            listener.onDeviceSettingsReady();
        }
    }

    public static boolean hasGpsDevice(@NonNull final Context context) {
        final LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) {
            return false;
        }

        final List<String> providers = locationManager.getAllProviders();
        return providers != null && providers.contains(LocationManager.GPS_PROVIDER);
    }

    public static boolean isProviderEnabled(@NonNull final Context context, @NonNull final String provider) {
        return ((LocationManager) Objects.requireNonNull(context.getSystemService(Context.LOCATION_SERVICE)))
                .isProviderEnabled(provider);
    }

    public static boolean isIgnoringBatteryOptimizations(@NonNull final Context context) {
        if (Build.VERSION.SDK_INT >= 23) {
            final String packageName = context.getPackageName();
            final PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            return pm.isIgnoringBatteryOptimizations(packageName);
        } else {
            return true;
        }
    }

    public static void checkIgnoringBatteryOptimizations(@NonNull final Activity activity,
                                                         @NonNull final String title,
                                                         @NonNull final String message,
                                                         @NonNull final String positiveButtonText) {
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

    public static void onRequestPermissionsResult(@NonNull final Activity activity, int requestCode, String permissions[], int[] grantResults, @NonNull final LocationSdkPermission locationSdkPermission) {
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

    private static boolean permissionFirstTimeAsking(@NonNull final Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFERENCES,
                Context.MODE_PRIVATE);
        return sharedPreferences.getBoolean(KEY_FIRST_TIME_ASK_PERMISSION, true);
    }

    private static void setPermissionFirstTimeAsking(@NonNull final Context context, final boolean firstTimeAsking) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFERENCES,
                Context.MODE_PRIVATE);
        final SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(KEY_FIRST_TIME_ASK_PERMISSION, firstTimeAsking);
        editor.apply();
    }

    private void requestLocationUpdates(@NonNull final LocationSdkListener locationSdkListener) {
        try {
            locationManager.requestLocationUpdates(
                    locationSdkListener.getProvider(), locationSdkListener.getMinTime(),
                    locationSdkListener.getMinDistance(), locationSdkListener);
        } catch (SecurityException ex) {
            Log.e(TAG, "Fail to request location update, ignore", ex);
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

    public static void lastKnowLocation(@NonNull final Activity activity, @NonNull final LocationSdkListener locationSdkListener) {
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

        public Builder(@Nullable final Context context) {
            this.context = context;
            this.listeners = new ArrayList<>();
        }

        public Builder addListener(@NonNull final LocationSdkListener locationSdkListener) {
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