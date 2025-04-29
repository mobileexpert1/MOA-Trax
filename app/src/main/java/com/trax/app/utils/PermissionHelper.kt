package com.trax.app.utils

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment

class PermissionHelper {
    companion object {
        val PERMISSION_CODE = 106
        private val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        @JvmStatic
        fun hasPermission(activity: Activity): Boolean {
            for (permission in permissions) {
                if (ActivityCompat.checkSelfPermission(activity, permission)
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    return false
                }
            }
            return true
        }

        private val storagePermissions = arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        @JvmStatic
        fun hasStoragePermission(activity: Activity): Boolean {
            for (permission in storagePermissions) {
                if (ActivityCompat.checkSelfPermission(activity, permission)
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    return false
                }
            }
            return true
        }

        private val locationPermission = arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        @JvmStatic
        fun hasLocationPermission(activity: Activity): Boolean {
            for (permission in locationPermission) {
                if (ActivityCompat.checkSelfPermission(activity, permission)
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    return false
                }
            }
            return true
        }

        @JvmStatic
        fun requestLocationPermission(activity: Activity) {
            ActivityCompat.requestPermissions(activity, locationPermission, PERMISSION_CODE)
        }

        @JvmStatic
        fun requestPermission(activity: Activity) {
            ActivityCompat.requestPermissions(activity, permissions, PERMISSION_CODE)
        }

        @JvmStatic
        fun requestPermission(fragment: Fragment) {
            fragment.requestPermissions(permissions, PERMISSION_CODE)
        }

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