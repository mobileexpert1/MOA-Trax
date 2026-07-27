package com.trax.app.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.trax.app.R

class TrackingForegroundService : Service() {

    //==============================================================================
    // Companion Object
    //==============================================================================

    companion object {
        private const val CHANNEL_ID = "tracking_foreground_channel"
        private const val CHANNEL_NAME = "Live Tracking Notification Channel"
        private const val NOTIFICATION_ID = 1001

        //--------------------------------------------------
        // Starts the service to keep tracking alive in the background.
        //--------------------------------------------------
        fun start(context: Context) {
            val intent = Intent(context, TrackingForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        //--------------------------------------------------
        // Stops the foreground tracking service.
        //--------------------------------------------------
        fun stop(context: Context) {
            val intent = Intent(context, TrackingForegroundService::class.java)
            context.stopService(intent)
        }
    }

    //==============================================================================
    // Lifecycle Methods
    //==============================================================================

    //--------------------------------------------------
    // Creates and registers system notifications channels.
    //--------------------------------------------------
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    //--------------------------------------------------
    // Binds the foreground notifications to prevent system shutdown.
    //--------------------------------------------------
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
            } catch (e: Exception) {
                startForeground(NOTIFICATION_ID, notification)
            }
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        return START_STICKY
    }

    //--------------------------------------------------
    // Binds the service interface channel.
    //--------------------------------------------------
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    //==============================================================================
    // Notification Management
    //==============================================================================

    //--------------------------------------------------
    // Registers the notification channel for Android Oreo and above.
    //--------------------------------------------------
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows ongoing live tracking notification"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    //--------------------------------------------------
    // Builds ongoing status bar notification details.
    //--------------------------------------------------
    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MOA-Trax Live Tracking")
            .setContentText("Live tracking is active in the background.")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
