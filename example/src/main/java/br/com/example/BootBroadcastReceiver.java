package br.com.example;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import br.com.mblabs.location.LocationAPI;

public class BootBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = BootBroadcastReceiver.class.toString();

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "BootBroadcastReceiver onReceive");
        if (LocationAPI.hasGpsDevice(context)) {
            ContextCompat.startForegroundService(context, new Intent(context, AppTestLocationService.class));
        }
    }
}