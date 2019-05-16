package br.com.mblabs.location;

public interface LocationSettingsListener {

    void onDeviceNotSupportGps();

    void onDeviceSettingsReady();
}
