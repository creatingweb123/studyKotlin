package com.andy.v2pproject

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity(), WifiAutoConnector.WifiInfoCallback {

    private val PERMISSIONS_REQUEST_CODE = 100
    private val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.WRITE_SETTINGS
    )

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Check if permissions are already granted
        if (hasPermissions()) {
            // If permissions are granted, proceed with WiFi operations
            connectAndRetrieveWifiInfo()
        } else {
            // Request permissions
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSIONS_REQUEST_CODE)
        }
    }

    private fun hasPermissions(): Boolean {
        for (permission in REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun connectAndRetrieveWifiInfo() {
        // Assuming you have a button or some trigger to start the process
        // For demonstration, we'll call these methods directly in onCreate
        WifiAutoConnector.connect(this,"Nice to meet you","andyann05080927@!")
        WifiAutoConnector.getConnectedWifiBssid(this, this)
        WifiAutoConnector.getConnectedWifiIpAddress(this, this)

        // Check if the permission is still needed after performing the operation
        if (!isPermissionStillNeeded()) {
            // If not, revoke the permission
            revokePermission()
        }
    }

    private fun isPermissionStillNeeded(): Boolean {
        // Implement your logic to determine if the permission is still needed
        // For demonstration, let's assume it's not needed
        return false
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun revokePermission() {
        // Revoke the permission
        val permission = Manifest.permission.ACCESS_FINE_LOCATION
        revokeSelfPermissionOnKill(permission)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        connectAndRetrieveWifiInfo()
    }

    override fun onWifiInfoRetrieved(info: String) {
        // Log the information
        Log.i("WifiInfo", info)
    }
}
