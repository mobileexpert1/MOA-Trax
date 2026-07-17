package com.trax.app.activities

import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.common.api.GoogleApiClient
import com.trax.app.R
import com.trax.app.databinding.ActivityMainBinding
import com.trax.app.fragment.HomeFragment
import com.trax.app.fragment.TrackFragment
import com.trax.app.utils.LocationProvider
import com.trax.app.utils.PermissionHelper
import com.trax.app.viewModels.HomeViewModel
import com.trax.app.viewModels.LoginViewModel

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity(),
    LocationProvider.EasyLocationCallback {

    private lateinit var binding: ActivityMainBinding

    private val viewModel: HomeViewModel by viewModels()
    private val loginViewModel: LoginViewModel by viewModels()

    private lateinit var locationProvider: LocationProvider

    private companion object {
        const val LOCATION_PERMISSION_REQUEST = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupInsets()

        // Default Fragment
        selectHome()
        loadFragment(HomeFragment())

        // Home Click
        binding.layoutHome.setOnClickListener {

            selectHome()
            loadFragment(HomeFragment())
        }

        // Track Click
        binding.layoutTrack.setOnClickListener {

            selectTrack()

            loadFragment(TrackFragment())
        }

        checkLocationPermission()
    }

    // ================= INSETS =================

    private fun setupInsets() {

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->

            val systemBars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
            )

            // Fragment top padding
            binding.fragmentContainer.setPadding(
                0,
                systemBars.top,
                0,
                0
            )

            // Bottom navigation margin
            val params =
                binding.bottomNav.layoutParams as ConstraintLayout.LayoutParams

            params.bottomMargin = systemBars.bottom + 20

            binding.bottomNav.layoutParams = params

            insets
        }
    }

    // ================= FRAGMENT =================

    private fun loadFragment(fragment: Fragment) {

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    // ================= NAVIGATION UI =================

    private fun selectHome() {

        binding.layoutHome.setBackgroundResource(
            R.drawable.bg_nav_selected
        )

        binding.layoutTrack.setBackgroundResource(
            android.R.color.transparent
        )

        binding.imgHome.setImageResource(
            R.drawable.ic_home_white
        )

        binding.imgTrack.setImageResource(
            R.drawable.ic_track_black
        )
    }

    private fun selectTrack() {

        binding.layoutTrack.setBackgroundResource(
            R.drawable.bg_nav_selected
        )

        binding.layoutHome.setBackgroundResource(
            android.R.color.transparent
        )

        binding.imgHome.setImageResource(
            R.drawable.ic_home_black
        )

        binding.imgTrack.setImageResource(
            R.drawable.ic_track_white
        )
    }

    // ================= LOCATION =================

    private fun enableLocationProvider() {

        locationProvider = LocationProvider.Builder(this@MainActivity)
            .setInterval(500)
            .setFastestInterval(200)
            .setListener(this)
            .build()

        lifecycle.addObserver(locationProvider)
    }


    override fun onGoogleAPIClient(
        googleApiClient: GoogleApiClient?,
        message: String?
    ) {

    }

    override fun onLocationUpdated(
        latitude: Double,
        longitude: Double
    ) {

    }

    override fun onLocationUpdateRemoved() {

    }



    // ================= PERMISSIONS =================

    private fun checkLocationPermission() {

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {

            enableLocationProvider()

        } else {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST
            )
        }
    }

    override fun onResume() {
        super.onResume()

        checkLocationPermission()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {

        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        if (requestCode == LOCATION_PERMISSION_REQUEST) {

            if (
                grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {

                enableLocationProvider()

            } else {

                if (
                    ActivityCompat.shouldShowRequestPermissionRationale(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                ) {

                    // User tapped Deny → show system permission again
                    checkLocationPermission()

                } else {

                    // User selected "Don't ask again"

                    openAppSettings()
                }
            }
        }
    }

    private fun openAppSettings() {
        Toast.makeText(this, getString(R.string.location_permission_is_required_to_use_this_app), Toast.LENGTH_LONG).show()

        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)

        intent.data = Uri.fromParts("package", packageName, null)

        startActivity(intent)
    }
}
