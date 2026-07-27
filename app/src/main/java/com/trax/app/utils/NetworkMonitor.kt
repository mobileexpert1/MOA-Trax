package com.trax.app.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network

object NetworkMonitor {

    //==============================================================================
    // Variables
    //==============================================================================

    private var registered = false

    //==============================================================================
    // Monitor Initialization Functions
    //==============================================================================

    //--------------------------------------------------
    // Registers default network callback notifications to capture connectivity changes.
    //--------------------------------------------------
    fun startMonitoring(context: Context) {
        if (registered) return
        val appContext = context.applicationContext
        val connectivityManager =
            appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                OfflineTrackManager.syncOfflineTracks(appContext)
            }
        }

        try {
            connectivityManager.registerDefaultNetworkCallback(callback)
            registered = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
