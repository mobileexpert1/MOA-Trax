package com.trax.app.utils

import android.app.Activity
import android.content.Intent
import android.view.KeyEvent
import androidx.appcompat.app.AlertDialog
import com.trax.app.activities.LoginActivity

object SessionManager {

    //==============================================================================
    // Variables
    //==============================================================================

    private var isDialogShowing = false

    //==============================================================================
    // Alert Dialog Functions
    //==============================================================================

    //--------------------------------------------------
    // Creates and renders a non-cancelable Session Expired alert dialog.
    //--------------------------------------------------
    fun showSessionExpiredDialog(activity: Activity) {
        if (isDialogShowing || activity.isFinishing || activity.isDestroyed) {
            return
        }
        isDialogShowing = true

        // Clear session data
        PrefManager.putBoolean(AppConstant.IS_LOGIN, false)
        PrefManager.clearKey(AppConstant.CURRENT_USER)
        PrefManager.clearKey(AppConstant.AUTH_TOKEN)

        val builder = AlertDialog.Builder(activity).apply {
            setTitle("Session Expired")
            setMessage("Your session has expired. Please login again to continue.")
            setCancelable(false)
            setPositiveButton("Login") { dialog, _ ->
                dialog.dismiss()
                isDialogShowing = false
                
                val intent = Intent(activity, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                activity.startActivity(intent)
                activity.finish()
            }
        }
        
        val dialog = builder.create().apply {
            setCanceledOnTouchOutside(false)
            setOnKeyListener { _, keyCode, _ ->
                keyCode == KeyEvent.KEYCODE_BACK
            }
        }
        dialog.show()
    }
}
