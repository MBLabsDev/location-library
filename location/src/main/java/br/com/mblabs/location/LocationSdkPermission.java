package br.com.mblabs.location;

import android.support.annotation.NonNull;

public interface LocationSdkPermission {
    void onPermissionsGranted();

    void onPermissionDenied(@NonNull String permission);

    void onShouldRequestPermissionRationale(@NonNull String permission);

    void onNeverAskAgain(@NonNull String permission);
}
