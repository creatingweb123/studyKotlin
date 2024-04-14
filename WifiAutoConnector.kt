package com.andy.v2pproject

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.ScanResult
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.net.wifi.WifiNetworkSuggestion
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class WifiAutoConnector {
    interface WifiInfoCallback {
        fun onWifiInfoRetrieved(info: String)
    }

    companion object {
        private const val NETWORK_PASSWORD = "ssid password"
        private val SSIDS = arrayOf("SSID NAME", "SSID NAME")

        var isConnected = false
        var connectedWifiName = ""
        fun scanWifiNetworks(context: Context, callback: (List<ScanResult>) -> Unit) {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val permission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)

            if (permission == PackageManager.PERMISSION_GRANTED) {
                wifiManager?.startScan()
                val results = wifiManager?.scanResults
                callback(results ?: emptyList())
            } else {
                // Request location permission if not already granted
                ActivityCompat.requestPermissions(context as Activity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
            }
        }

        @SuppressLint("ServiceCast")
        fun connect(context: Context, ssid: String, password: String) {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager


            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // For Android Q and above, use Wi-Fi suggestion API
                val suggestion = WifiNetworkSuggestion.Builder()
                    .setSsid(ssid)
                    .setWpa2Passphrase(password)
                    .build()

                val suggestionsList = listOf(suggestion)
                val status = wifiManager?.addNetworkSuggestions(suggestionsList)
                if (status == 0) {
                    // Network suggestion added successfully
                    val networkRequest = NetworkRequest.Builder()
                        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                        .setNetworkSpecifier(WifiNetworkSpecifier.Builder().setSsid(ssid).build())
                        .build()

                    val networkCallback = object : ConnectivityManager.NetworkCallback() {
                        override fun onAvailable(network: Network) {
                            isConnected = true
                            Log.i("WifiAutoConnected", "Connected to network")
                            val wifiInfo = connectivityManager?.getNetworkCapabilities(network)?.transportInfo as? WifiInfo
                            connectedWifiName = wifiInfo?.ssid?.removeSurrounding("\"") ?: ""
                            Log.i("WifiAutoConnected", "Connected to WiFi: $connectedWifiName")
                        }

                        override fun onUnavailable() {
                            isConnected = false
                            Log.i("WifiAutoConnected", "Failed to connect to network")
                        }
                    }

                    connectivityManager?.requestNetwork(networkRequest, networkCallback)
                } else {
                    // Network suggestion not added
                }
            } else {
                // For Android versions below Q, use the old method
                val conf = WifiConfiguration().apply {
                    SSID = "\"$ssid\""
                    preSharedKey = "\"$password\""
                    allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK)
                }

                val netId = wifiManager?.addNetwork(conf)
                if (netId != null) {
                    wifiManager.disconnect()
                    wifiManager.enableNetwork(netId, true)
                    wifiManager.reconnect()
                }
            }
        }

        fun getConnectedWifiBssid(context: Context, callback: WifiInfoCallback) {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkRequest = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build()

            val networkCallback = object : ConnectivityManager.NetworkCallback() {
                @RequiresApi(Build.VERSION_CODES.Q)
                override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                    super.onCapabilitiesChanged(network, networkCapabilities)
                    val wifiInfo = networkCapabilities.transportInfo as? WifiInfo
                    wifiInfo?.let {
                        Log.i("WifiInfoRetriever", "BSSID: ${it.bssid}")
                        callback.onWifiInfoRetrieved(it.bssid)
                    }
                }
            }

            connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
        }

        fun getConnectedWifiIpAddress(context: Context, callback: WifiInfoCallback) {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkRequest = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build()

            val networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                    super.onCapabilitiesChanged(network, networkCapabilities)
                    val linkProperties = connectivityManager.getLinkProperties(network)
                    linkProperties?.let {
                        val ipAddress = it.linkAddresses.firstOrNull { address ->
                            address.address.hostAddress?.contains('.') ?: false
                        }?.address?.hostAddress
                        ipAddress?.let { ip ->
                            Log.i("WifiIPAddress", "Connected WiFi IP Address: $ip")
                            callback.onWifiInfoRetrieved(ip)
                        }
                    }
                }
            }


            connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
        }
    }
}
