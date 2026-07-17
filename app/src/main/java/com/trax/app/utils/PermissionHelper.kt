package com.trax.app.utils

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment

/**
 * PermissionHelper — centralised runtime permission management.
 *
 * KEY DESIGN DECISIONS (version-safe):
 * ─────────────────────────────────────
 * 1. FOREGROUND_SERVICE_LOCATION is NOT a runtime permission.
 *    It is a normal (install-time) permission granted automatically
 *    when ACCESS_FINE_LOCATION is granted. Calling checkSelfPermission()
 *    on it returns PERMISSION_DENIED even when the app has full location
 *    access, because Android's runtime permission system does not manage it.
 *
 * 2. Background location (ACCESS_BACKGROUND_LOCATION) must be requested
 *    SEPARATELY and AFTER foreground location is granted. Requesting them
 *    together on API 29+ causes Android to silently ignore the background
 *    request. We expose it as an independent method for callers that need it.
 *
 * Android version compatibility matrix:
 *   API 29 (Android 10): ACCESS_BACKGROUND_LOCATION introduced
 *   API 31 (Android 12): FOREGROUND_SERVICE introduced (install-time only)
 *   API 33 (Android 13): FOREGROUND_SERVICE_LOCATION introduced (install-time only)
 *   API 34 (Android 14): No new location permission changes relevant here
 *   API 35 (Android 15): No new location permission changes relevant here
 */
class PermissionHelper {

    companion object {

        // Request code for location-related runtime permissions
        const val PERMISSION_CODE = 106

        // ── General permissions (camera, storage, audio) ──────────────────────
        private val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )

        @JvmStatic
        fun hasPermission(activity: Activity): Boolean {
            for (permission in permissions) {
                if (
                    ActivityCompat.checkSelfPermission(
                        activity,
                        permission
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return false
                }
            }
            return true
        }

        @JvmStatic
        fun requestPermission(activity: Activity) {
            ActivityCompat.requestPermissions(
                activity,
                permissions,
                PERMISSION_CODE
            )
        }

        @JvmStatic
        fun requestPermission(fragment: Fragment) {
            fragment.requestPermissions(
                permissions,
                PERMISSION_CODE
            )
        }

        // ── Storage permissions ───────────────────────────────────────────────
        private val storagePermissions = arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )

        @JvmStatic
        fun hasStoragePermission(activity: Activity): Boolean {
            for (permission in storagePermissions) {
                if (
                    ActivityCompat.checkSelfPermission(
                        activity,
                        permission
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return false
                }
            }
            return true
        }

        // ── Foreground location permissions ───────────────────────────────────
        private val locationPermissions = arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        @JvmStatic
        fun hasLocationPermission(activity: Activity): Boolean {

            for (permission in locationPermissions) {

                if (
                    ActivityCompat.checkSelfPermission(
                        activity,
                        permission
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return false
                }
            }

            return true
        }

        @JvmStatic
        fun requestLocationPermission(activity: Activity) {

            ActivityCompat.requestPermissions(
                activity,
                locationPermissions,
                PERMISSION_CODE
            )
        }

        // ── Background location permission ────────────────────────────────────
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

        // ── Tracking service permissions ──────────────────────────────────────
        @JvmStatic
        fun hasTrackingServicePermissions(activity: Activity): Boolean {

            return hasLocationPermission(activity)
        }

        @JvmStatic
        fun requestTrackingServicePermissions(activity: Activity) {

            ActivityCompat.requestPermissions(
                activity,
                locationPermissions,
                PERMISSION_CODE
            )
        }

        // ── Combined check ────────────────────────────────────────────────────
        @JvmStatic
        fun hasAllLocationPermissions(activity: Activity): Boolean {

            return hasLocationPermission(activity)
                    && hasBackgroundLocationPermission(activity)
        }

        @JvmStatic
        fun shouldShowRequestPermissionRationale(activity: Activity): Boolean {

            for (permission in permissions) {

                if (
                    ActivityCompat.shouldShowRequestPermissionRationale(
                        activity,
                        permission
                    )
                ) {
                    return true
                }
            }

            return false
        }
    }
}