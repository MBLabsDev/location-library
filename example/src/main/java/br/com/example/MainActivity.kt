package br.com.example

import android.content.Intent
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import br.com.mblabs.location.LocationAPI
import br.com.mblabs.location.LocationSdkPermission
import br.com.mblabs.location.LocationSettingsListener

class MainActivity : AppCompatActivity() {

    companion object {
        val TAG: String = MainActivity::class.java.simpleName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        checkLocationNecessarySettings()
    }

    fun checkLocationNecessarySettings() {
        LocationAPI.checkLocationNecessarySettings(this, object : LocationSettingsListener {
            override fun onDeviceNotSupportGps() {
                Log.d(TAG, "onDeviceNotSupportGps")
            }

            override fun onDeviceSettingsReady() {
                ContextCompat.startForegroundService(
                    this@MainActivity,
                    Intent(this@MainActivity, AppTestLocationService::class.java)
                )
            }

        }, "Your title here", "Your message here", "OK")
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        LocationAPI.onRequestPermissionsResult(this, requestCode, permissions, grantResults, object : LocationSdkPermission {
            override fun onPermissionsGranted() {
                checkLocationNecessarySettings()
            }

            override fun onPermissionDenied(permission: String) {
                Log.d(TAG, "onPermissionDenied - $permission")
            }

            override fun onShouldRequestPermissionRationale(permission: String) {
                Log.d(TAG, "onShouldRequestPermissionRationale - $permission")
            }

            override fun onNeverAskAgain(permission: String) {
                Log.d(TAG, "onPermissionDenied - $permission")
            }

        })
    }
}
