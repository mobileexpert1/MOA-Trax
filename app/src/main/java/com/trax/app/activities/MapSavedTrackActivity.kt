package com.trax.app.activities

import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.mapbox.geojson.Point
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.trax.app.R
import com.trax.app.base.BaseActivity
import com.trax.app.databinding.ActivityMapSavedTrackBinding
import com.trax.app.databinding.BottomsheetRemoveTrackBinding
import com.trax.app.progressBar.ProgressView
import com.trax.app.utils.AppConstant
import com.trax.app.utils.PrefManager
import com.trax.app.viewModels.SingleTrackViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

class MapSavedTrackActivity : BaseActivity() {

    //==============================================================================
    // Variables
    //==============================================================================

    private lateinit var binding: ActivityMapSavedTrackBinding
    private val viewModel: SingleTrackViewModel by viewModels()
    private var trackId = 0
    private var startMarkerAdded = false

    private val loader by lazy {
        ProgressView.getLoader(this)
    }

    private lateinit var trackingDotManager: PointAnnotationManager
    private lateinit var pointAnnotationManager: PointAnnotationManager

    private val footstepBitmap by lazy {
        bitmapFromDrawableRes(R.drawable.ic_footstep)
    }

    private val STORAGE_PERMISSION_REQUEST_CODE = 1007

    //==============================================================================
    // Lifecycle Methods
    //==============================================================================

    //--------------------------------------------------
    // Initializes the activity, configures insets, bindings, and extracts track json parameters.
    //--------------------------------------------------
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapSavedTrackBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupInsets()

        trackingDotManager = binding.mapView.annotations.createPointAnnotationManager()
        pointAnnotationManager = binding.mapView.annotations.createPointAnnotationManager()

        binding.mapView.mapboxMap.loadStyle(com.mapbox.maps.Style.STANDARD)

        trackId = intent.getIntExtra("trackId", 0)
        val isOffline = intent.getBooleanExtra("isOffline", false)
        val trackFeatureJson = intent.getStringExtra("trackFeatureJson")

        initObservers()

        if (!trackFeatureJson.isNullOrEmpty()) {
            try {
                val trackFeature = com.google.gson.Gson().fromJson(
                    trackFeatureJson,
                    com.trax.app.models.track.TrackFeature::class.java
                )
                trackFeature?.properties?.let { property ->
                    binding.tvTitle.text = property.trackName ?: ""
                    binding.tvLocation.text = "${property.countyName ?: ""} County, ${property.stateAbbrev ?: ""}"
                    binding.tvTrackLocation.text = "${property.countyName ?: ""} County, ${property.stateAbbrev ?: ""}"
                    binding.tvDate.text = formatDate(property.startedAtUtc)
                    binding.tvDistanceValue.text = formatDistanceDisplay(property.totalDistanceMeters)
                    binding.tvDurationValue.text = formatDuration(property.durationSeconds)
                    binding.tvPaceValue.text = formatPaceDisplay(property.pace)
                    binding.tvElevationValue.text = property.elevation ?: "--"
                }
                trackFeature?.geometry?.coordinates?.let {
                    drawTrack(it)
                }
            } catch (e: Exception) {
                Log.e("MapSavedTrackActivity", "Error parsing trackFeatureJson: ${e.message}")
            }
        }

        binding.ivBack.setOnClickListener {
            finish()
        }

        val isOfflineMode = isOffline || !isInternetAvailable()
        setupOfflineUI(isOfflineMode)
    }

    //==============================================================================
    // Initializations
    //==============================================================================

    //--------------------------------------------------
    // Initializes LiveData observers.
    //--------------------------------------------------
    private fun initObservers() {
        viewModel.singleTrackSuccess.observe(this) { response ->
            response?.data?.features?.firstOrNull()?.let { feature ->
                binding.tvTitle.text = feature.properties?.trackName
                binding.tvLocation.text = "${feature.properties?.countyName} County, ${feature.properties?.stateAbbrev}"
                binding.tvTrackLocation.text = "${feature.properties?.countyName} County, ${feature.properties?.stateAbbrev}"
                binding.tvDate.text = formatDate(feature.properties?.startedAtUtc)
                binding.tvDistanceValue.text = "${feature.properties?.totalDistanceMeters} km"
                binding.tvDurationValue.text = "${feature.properties?.durationSeconds} sec"
                binding.tvPaceValue.text = "${feature.properties?.pace ?: "--"}"
                binding.tvElevationValue.text = "${feature.properties?.elevation ?: "--"}"

                feature.geometry?.coordinates?.let {
                    drawTrack(it)
                }
            }
        }

        viewModel.deleteTrackSuccess.observe(this) { response ->
            response?.let {
                Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
                if (it.success) {
                    finish()
                }
            }
        }

        viewModel.apiError.observe(this) { errorMsg ->
            Log.e("ERROR", errorMsg.toString())
            errorMsg?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.pdfTrackSuccess.observe(this) { responseBody ->
            if (responseBody != null) {
                lifecycleScope.launch {
                    val isSuccess = saveImageToGallery(responseBody)
                    if (isSuccess) {
                        Toast.makeText(this@MapSavedTrackActivity, "Image saved successfully.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@MapSavedTrackActivity, "Failed to save image.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        viewModel.isLoading.observe(this) { loading ->
            if (loading == true) {
                loader.show()
            } else {
                loader.dismiss()
            }
        }
    }

    //--------------------------------------------------
    // Adjusts margins and paddings for status bar and navigation offsets.
    //--------------------------------------------------
    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            binding.statusBarSpace.layoutParams.height = statusBarHeight
            binding.statusBarSpace.requestLayout()

            val navigationBarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            binding.bottomCard.setPadding(
                binding.bottomCard.paddingLeft,
                binding.bottomCard.paddingTop,
                binding.bottomCard.paddingRight,
                navigationBarHeight + dpToPx(16)
            )

            insets
        }
    }

    //--------------------------------------------------
    // Updates the UI based on offline availability.
    //--------------------------------------------------
    private fun setupOfflineUI(isOffline: Boolean) {
        if (isOffline) {
            binding.rlDelete.isEnabled = false
            binding.rlDelete.isClickable = false
            binding.rlDelete.alpha = 0.4f
            binding.tvDelete.alpha = 0.4f
            binding.rlDelete.setOnClickListener(null)

            binding.btnShare.isEnabled = false
            binding.btnShare.isClickable = false
            binding.btnShare.alpha = 0.4f
            binding.btnShare.setOnClickListener(null)

            binding.btnExport.isEnabled = false
            binding.btnExport.isClickable = false
            binding.btnExport.alpha = 0.4f
            binding.btnExport.setOnClickListener(null)
        } else {
            binding.rlDelete.setOnClickListener {
                showRemoveTrackBottomSheet()
            }
            binding.btnExport.setOnClickListener {
                handleExportClick()
            }
        }
    }

    //==============================================================================
    // API Calls
    //==============================================================================

    //--------------------------------------------------
    // Calls the active track PDF/image export API.
    //--------------------------------------------------
    private fun startPdfTrackExport() {
        val token = PrefManager.getString(AppConstant.AUTH_TOKEN) ?: ""
        viewModel.pdfTrack(token, trackId)
    }

    //--------------------------------------------------
    // Calls the track deletion API.
    //--------------------------------------------------
    private fun callDeleteTrackApi() {
        val token = PrefManager.getString(AppConstant.AUTH_TOKEN)
        viewModel.deleteTrack(token, trackId)
    }

    //==============================================================================
    // Mapbox Functions
    //==============================================================================

    //--------------------------------------------------
    // Draws the track polyline dots and start marker on the map.
    //--------------------------------------------------
    private fun drawTrack(coordinates: List<List<Double>>) {
        if (coordinates.isEmpty()) return

        val points = coordinates.map {
            Point.fromLngLat(it[0], it[1])
        }

        addTrackingDots(points)

        if (points.isNotEmpty()) {
            addStartMarker(points.first())
        }

        binding.mapView.post {
            zoomToTrack(points)
        }
    }

    //--------------------------------------------------
    // Places footstep indicators on the map points.
    //--------------------------------------------------
    private fun addTrackingDots(points: List<Point>) {
        trackingDotManager.deleteAll()
        val bitmap = footstepBitmap ?: return

        points.forEach {
            trackingDotManager.create(
                PointAnnotationOptions()
                    .withPoint(it)
                    .withIconImage(bitmap)
                    .withIconSize(1.0)
            )
        }
    }

    //--------------------------------------------------
    // Renders the start flag marker on the map.
    //--------------------------------------------------
    private fun addStartMarker(point: Point) {
        if (startMarkerAdded) return

        val bitmap = bitmapFromDrawableRes(R.drawable.ic_start_marker) ?: return

        pointAnnotationManager.create(
            PointAnnotationOptions()
                .withPoint(point)
                .withIconImage(bitmap)
                .withIconSize(1.0)
        )

        startMarkerAdded = true
    }

    //--------------------------------------------------
    // Adjusts camera view to show the entire track.
    //--------------------------------------------------
    private fun zoomToTrack(points: List<Point>) {
        if (points.isEmpty()) return

        binding.mapView.post {
            if (points.size == 1 || points.all { it.latitude() == points.first().latitude() && it.longitude() == points.first().longitude() }) {
                val cameraOptions = com.mapbox.maps.CameraOptions.Builder()
                    .center(points.first())
                    .zoom(14.5)
                    .build()
                binding.mapView.mapboxMap.setCamera(cameraOptions)
                return@post
            }

            val camera = binding.mapView.mapboxMap.cameraForCoordinates(
                points,
                EdgeInsets(120.0, 120.0, 120.0, 120.0),
                0.0,
                0.0
            )

            val currentZoom = camera.zoom
            if (currentZoom != null && currentZoom < 11.0) {
                val adjustedCamera = com.mapbox.maps.CameraOptions.Builder()
                    .center(camera.center ?: points.first())
                    .zoom(14.5)
                    .build()
                binding.mapView.mapboxMap.setCamera(adjustedCamera)
            } else {
                binding.mapView.mapboxMap.setCamera(camera)
            }
        }
    }

    //--------------------------------------------------
    // Converts a resource drawable vector into a Bitmap.
    //--------------------------------------------------
    private fun bitmapFromDrawableRes(drawableRes: Int): Bitmap? {
        val drawable = ContextCompat.getDrawable(this, drawableRes) ?: return null
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    //==============================================================================
    // Permission Handling
    //==============================================================================

    //--------------------------------------------------
    // Evaluates write storage permissions for track export actions.
    //--------------------------------------------------
    private fun handleExportClick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startPdfTrackExport()
        } else {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                startPdfTrackExport()
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    STORAGE_PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    //--------------------------------------------------
    // Handles storage permission request results.
    //--------------------------------------------------
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startPdfTrackExport()
            } else {
                Toast.makeText(this, "Storage permission is required to download track image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    //==============================================================================
    // Utility / Format Functions
    //==============================================================================

    //--------------------------------------------------
    // Saves downloaded track export bytes to external gallery.
    //--------------------------------------------------
    private suspend fun saveImageToGallery(responseBody: okhttp3.ResponseBody): Boolean {
        return withContext(Dispatchers.IO) {
            val inputStream = responseBody.byteStream()
            val filename = "track_${trackId}_${System.currentTimeMillis()}.png"
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                        put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                        put(MediaStore.MediaColumns.IS_PENDING, 1)
                    }

                    val imageUri = contentResolver.insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        contentValues
                    )
                    if (imageUri != null) {
                        contentResolver.openOutputStream(imageUri).use { outputStream ->
                            if (outputStream != null) {
                                inputStream.copyTo(outputStream)
                            }
                        }
                        contentValues.clear()
                        contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                        contentResolver.update(imageUri, contentValues, null, null)
                        true
                    } else {
                        false
                    }
                } else {
                    val targetDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                    if (!targetDir.exists()) {
                        targetDir.mkdirs()
                    }
                    val imageFile = File(targetDir, filename)
                    FileOutputStream(imageFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                    android.media.MediaScannerConnection.scanFile(
                        this@MapSavedTrackActivity,
                        arrayOf(imageFile.absolutePath),
                        arrayOf("image/png"),
                        null
                    )
                    true
                }
            } catch (e: Exception) {
                Log.e("MapSavedTrackActivity", "Error saving image to gallery", e)
                false
            } finally {
                try {
                    inputStream.close()
                } catch (ignored: Exception) {
                }
            }
        }
    }

    //--------------------------------------------------
    // Formats seconds into human-readable duration strings.
    //--------------------------------------------------
    private fun formatDuration(seconds: Int): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0)
            String.format("%02d:%02d:%02d", h, m, s)
        else
            String.format("%02d:%02d", m, s)
    }

    //--------------------------------------------------
    // Converts density dp values into pixels.
    //--------------------------------------------------
    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    //--------------------------------------------------
    // Formats numeric distance into readable distance text.
    //--------------------------------------------------
    private fun formatDistanceDisplay(dist: Double): String {
        if (dist <= 0.0) return "0.00 km"
        val km = if (dist >= 100.0) dist / 1000.0 else dist
        return String.format(Locale.US, "%.2f km", km)
    }

    //--------------------------------------------------
    // Calculates and formats the average pace.
    //--------------------------------------------------
    private fun formatPaceDisplay(paceVal: Any?): String {
        if (paceVal == null) return "--"
        val str = paceVal.toString()
        if (str.contains("/km") || str.contains(":")) return str
        val d = str.toDoubleOrNull() ?: return "--"
        if (d <= 0.0) return "--"
        val mins = d.toInt()
        val secs = ((d - mins) * 60).toInt()
        return String.format(Locale.US, "%02d:%02d /km", mins, secs)
    }

    //--------------------------------------------------
    // Formats ISO date strings to readable dates.
    //--------------------------------------------------
    private fun formatDate(dateString: String?): String {
        if (dateString.isNullOrEmpty()) return ""
        val inputFormats = arrayOf(
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd"
        )
        val outputFormat = java.text.SimpleDateFormat("MMM dd, yyyy", Locale.US)
        for (format in inputFormats) {
            try {
                val parser = java.text.SimpleDateFormat(format, Locale.US)
                val date = parser.parse(dateString)
                if (date != null) {
                    return outputFormat.format(date)
                }
            } catch (e: Exception) {
                // Try next
            }
        }
        return dateString
    }

    //--------------------------------------------------
    // Displays bottom sheet confirmation to delete track.
    //--------------------------------------------------
    private fun showRemoveTrackBottomSheet() {
        val dialog = BottomSheetDialog(this)
        val sheetBinding = BottomsheetRemoveTrackBinding.inflate(layoutInflater)
        dialog.setContentView(sheetBinding.root)

        sheetBinding.tvCancel.setOnClickListener {
            dialog.dismiss()
        }

        sheetBinding.tvRemove.setOnClickListener {
            dialog.dismiss()
            callDeleteTrackApi()
        }

        dialog.show()
    }
}