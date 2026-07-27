package com.trax.app.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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

class MainActivity : AppCompatActivity(), LocationProvider.EasyLocationCallback {

    //==============================================================================
    // Variables
    //==============================================================================

    private lateinit var binding: ActivityMainBinding
    private lateinit var locationProvider: LocationProvider

    private companion object {
        const val LOCATION_PERMISSION_REQUEST = 1001
    }

    //==============================================================================
    // Lifecycle Methods
    //==============================================================================

    //--------------------------------------------------
    // Initializes the activity, window insets, default fragments, and click listeners.
    //--------------------------------------------------
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.parseColor("#007F56")
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = false

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupInsets()

        // Setup Default Fragment
        if (intent.getStringExtra("selectFragment") == "track") {
            selectTrack()
            loadFragment(TrackFragment())
        } else {
            selectHome()
            loadFragment(HomeFragment())
        }

        binding.layoutHome.setOnClickListener {
            selectHome()
            loadFragment(HomeFragment())
        }

        binding.layoutTrack.setOnClickListener {
            selectTrack()
            loadFragment(TrackFragment())
        }

        checkLocationPermission()
    }

    //--------------------------------------------------
    // Checks and registers location provider updates when activity resumes.
    //--------------------------------------------------
    override fun onResume() {
        super.onResume()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        ) {
            enableLocationProvider()
        }
    }

    //==============================================================================
    // Initializations
    //==============================================================================

    //--------------------------------------------------
    // Configures status bar and navigation bar layout window insets.
    //--------------------------------------------------
    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            binding.statusBarSpace.layoutParams.height = systemBars.top
            binding.statusBarSpace.requestLayout()

            // Fragment top padding
            binding.fragmentContainer.setPadding(0, systemBars.top, 0, 0)

            // Bottom navigation margin
            val params = binding.bottomNav.layoutParams as ConstraintLayout.LayoutParams
            params.bottomMargin = systemBars.bottom + 20
            binding.bottomNav.layoutParams = params

            insets
        }
    }

    //==============================================================================
    // Location Provider Functions
    //==============================================================================

    //--------------------------------------------------
    // Builds and registers the LocationProvider listener observer.
    //--------------------------------------------------
    private fun enableLocationProvider() {
        if (!::locationProvider.isInitialized) {
            locationProvider = LocationProvider.Builder(this@MainActivity)
                .setInterval(500)
                .setFastestInterval(200)
                .setListener(this)
                .build()
            lifecycle.addObserver(locationProvider)
        }
    }

    //--------------------------------------------------
    // Callback when Google API client is connected or suspended.
    //--------------------------------------------------
    override fun onGoogleAPIClient(googleApiClient: GoogleApiClient?, message: String?) {}

    //--------------------------------------------------
    // Callback triggered when user location coordinates update.
    //--------------------------------------------------
    override fun onLocationUpdated(latitude: Double, longitude: Double) {}

    //--------------------------------------------------
    // Callback triggered when location update requests are removed.
    //--------------------------------------------------
    override fun onLocationUpdateRemoved() {}

    //==============================================================================
    // Permission Handling
    //==============================================================================

    //--------------------------------------------------
    // Checks location permissions and requests them if missing.
    //--------------------------------------------------
    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        ) {
            enableLocationProvider()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST
            )
        }
    }

    //--------------------------------------------------
    // Handles location permission request result callbacks.
    //--------------------------------------------------
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults.any { it == PackageManager.PERMISSION_GRANTED }) {
                enableLocationProvider()
            } else {
                val showRationale = ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
                if (!showRationale) {
                    showPermissionDeniedDialog()
                } else {
                    Toast.makeText(
                        this,
                        "Location permission is required for live tracking features.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    //--------------------------------------------------
    // Displays dialog explaining location requirement before settings redirect.
    //--------------------------------------------------
    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("Location Permission Required")
            .setMessage(getString(R.string.location_permission_is_required_to_use_this_app))
            .setPositiveButton("Go to Settings") { _, _ ->
                openAppSettings()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    //--------------------------------------------------
    // Launches intent details redirecting user to app system settings screen.
    //--------------------------------------------------
    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
    }

    //==============================================================================
    // Navigation & Fragment Management Functions
    //==============================================================================

    //--------------------------------------------------
    // Loads the selected fragment into the layout container.
    //--------------------------------------------------
    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    //--------------------------------------------------
    // Updates UI elements for active Home tab status.
    //--------------------------------------------------
    private fun selectHome() {
        binding.layoutHome.setBackgroundResource(R.drawable.bg_nav_selected)
        binding.layoutTrack.setBackgroundResource(android.R.color.transparent)
        binding.imgHome.setImageResource(R.drawable.ic_home_white)
        binding.imgTrack.setImageResource(R.drawable.ic_track_black)
    }

    //--------------------------------------------------
    // Updates UI elements for active Track tab status.
    //--------------------------------------------------
    private fun selectTrack() {
        binding.layoutTrack.setBackgroundResource(R.drawable.bg_nav_selected)
        binding.layoutHome.setBackgroundResource(android.R.color.transparent)
        binding.imgHome.setImageResource(R.drawable.ic_home_black)
        binding.imgTrack.setImageResource(R.drawable.ic_track_white)
    }
}
