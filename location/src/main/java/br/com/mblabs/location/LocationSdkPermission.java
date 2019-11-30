package br.com.mblabs.location;

public interface LocationSdkPermission {

    void onPermissionsGranted();

    void onPermissionDenied(String permission);

    void onShouldRequestPermissionRationale(String permission);

    void onNeverAskAgain(String permission);
}
