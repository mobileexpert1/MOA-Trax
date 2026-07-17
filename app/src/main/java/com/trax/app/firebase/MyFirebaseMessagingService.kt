package com.trax.app.firebase

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.trax.app.utils.AppConstant
import com.trax.app.utils.PrefManager

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)


        Log.d("FCM", token)

        PrefManager.putString(
            AppConstant.DEVICE_TOKEN,
            token
        )
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
    }
}