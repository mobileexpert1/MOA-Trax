package com.trax.app.firebase

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.trax.app.utils.AppConstant
import com.trax.app.utils.PrefManager

class MyFirebaseMessagingService : FirebaseMessagingService() {

    //==============================================================================
    // Notification Messaging Handler
    //==============================================================================

    //--------------------------------------------------
    // Receives and caches new Firebase Cloud Messaging registration tokens.
    //--------------------------------------------------
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", token)
        PrefManager.putString(AppConstant.DEVICE_TOKEN, token)
    }

    //--------------------------------------------------
    // Captures incoming remote pushes when the application is running.
    //--------------------------------------------------
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
    }
}