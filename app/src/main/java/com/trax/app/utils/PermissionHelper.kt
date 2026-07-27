package com.trax.app.utils

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment

class PermissionHelper {

    //==============================================================================
    // Companion Object
    //==============================================================================

    companion object {

        const val PERMISSION_CODE = 106

        private val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )

        private val storagePermissions = arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )

        private val locationPermissions = arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        //--------------------------------------------------
        // Evaluates if camera, audio, and storage permissions are granted.
        //--------------------------------------------------
        @JvmStatic
        fun hasPermission(activity: Activity): Boolean {
            for (permission in permissions) {
                if (ActivityCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false
                }
            }
            return true
        }

        //--------------------------------------------------
        // Triggers request dialogues for general features permissions.
        //--------------------------------------------------
        @JvmStatic
        fun requestPermission(activity: Activity) {
            ActivityCompat.requestPermissions(activity, permissions, PERMISSION_CODE)
        }

        //--------------------------------------------------
        // Triggers request dialogues for general features permissions from a fragment context.
        //--------------------------------------------------
        @JvmStatic
        @Suppress("DEPRECATION")
        fun requestPermission(fragment: Fragment) {
            fragment.requestPermissions(permissions, PERMISSION_CODE)
        }

        //--------------------------------------------------
        // Evaluates if read/write storage permissions are granted.
        //--------------------------------------------------
        @JvmStatic
        fun hasStoragePermission(activity: Activity): Boolean {
            for (permission in storagePermissions) {
                if (ActivityCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false
                }
            }
            return true
        }

        //--------------------------------------------------
        // Evaluates if coarse/fine foreground location permissions are granted.
        //--------------------------------------------------
        @JvmStatic
        fun hasLocationPermission(activity: Activity): Boolean {
            for (permission in locationPermissions) {
                if (ActivityCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false
                }
            }
            return true
        }

        //--------------------------------------------------
        // Triggers request dialogues for coarse and fine location permissions.
        //--------------------------------------------------
        @JvmStatic
        fun requestLocationPermission(activity: Activity) {
            ActivityCompat.requestPermissions(activity, locationPermissions, PERMISSION_CODE)
        }

        //--------------------------------------------------
        // Evaluates background location permission status.
        //--------------------------------------------------
        @JvmStatic
        fun hasBackgroundLocationPermission(activity: Activity): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ActivityCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        }

        //--------------------------------------------------
        // Triggers request dialogues for background location permission on Android Q+.
        //--------------------------------------------------
        @JvmStatic
        fun requestBackgroundLocationPermission(activity: Activity) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                    PERMISSION_CODE
                )
            }
        }

        //--------------------------------------------------
        // Evaluates if foreground location permissions are granted for tracking.
        //--------------------------------------------------
        @JvmStatic
        fun hasTrackingServicePermissions(activity: Activity): Boolean {
            return hasLocationPermission(activity)
        }

        //--------------------------------------------------
        // Triggers request dialogues for foreground location permissions for tracking.
        //--------------------------------------------------
        @JvmStatic
        fun requestTrackingServicePermissions(activity: Activity) {
            ActivityCompat.requestPermissions(activity, locationPermissions, PERMISSION_CODE)
        }

        //--------------------------------------------------
        // Verifies both foreground and background location permissions status.
        //--------------------------------------------------
        @JvmStatic
        fun hasAllLocationPermissions(activity: Activity): Boolean {
            return hasLocationPermission(activity) && hasBackgroundLocationPermission(activity)
        }

        //--------------------------------------------------
        // Checks if explanation rationale UI should be presented for general permissions.
        //--------------------------------------------------
        @JvmStatic
        fun shouldShowRequestPermissionRationale(activity: Activity): Boolean {
            for (permission in permissions) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                    return true
                }
            }
            return false
        }
    }
}