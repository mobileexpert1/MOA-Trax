package com.trax.app.activities

import android.os.Bundle
import com.google.android.material.bottomsheet.BottomSheetDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.trax.app.base.BaseActivity
import com.trax.app.databinding.ActivityMapLiveTrackBinding
import com.trax.app.databinding.BottomsheetSaveTrackBinding
import com.trax.app.databinding.BottomsheetStartTrackingBinding
import com.trax.app.databinding.BottomsheetStopTrackingBinding
import com.trax.app.R
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.gson.JsonElement
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.plugin.animation.flyTo
import com.mapbox.maps.plugin.locationcomponent.location

import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.maps.extension.style.layers.generated.lineLayer
import com.mapbox.maps.extension.style.layers.properties.generated.LineCap
import com.mapbox.maps.extension.style.layers.properties.generated.LineJoin
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.sources.getSource
import com.mapbox.maps.extension.style.sources.getSourceAs
import com.mapbox.maps.extension.style.style
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPolylineAnnotationManager
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPolylineAnnotationManager
import com.trax.app.models.home.license.GeoJson
// OLD IMPLEMENTATION 
// import com.trax.app.models.live_track.coordinates.CoordinateResponse
import com.trax.app.progressBar.ProgressView
import com.trax.app.utils.AppConstant
import com.trax.app.utils.PrefManager
import com.trax.app.viewModels.MapLiveTrackViewModel
import com.mapbox.maps.CoordinateBounds
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.sources.addSource
import com.trax.app.models.home.license.Properties
// OLD IMPLEMENTATION 
// import com.trax.app.models.live_track.coordinates.Properties
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import com.mapbox.common.TileStore
import com.mapbox.common.TileRegionLoadOptions
import com.mapbox.maps.OfflineManager
import com.mapbox.maps.StylePackLoadOptions
import com.mapbox.maps.GlyphsRasterizationMode
import com.mapbox.maps.TilesetDescriptorOptions

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.location.Location
import android.os.Build
import androidx.annotation.RequiresApi
import com.mapbox.geojson.Polygon
import com.mapbox.maps.extension.style.layers.generated.fillLayer
import com.trax.app.models.live_track.save_track.Geometry
import com.trax.app.models.live_track.save_track.SaveTrackRequest
import com.trax.app.models.live_track.save_track.TrackLine
import java.time.Instant
import java.util.TimeZone


class MapLiveTrackActivity : BaseActivity() {

    private val timerHandler = Handler(Looper.getMainLooper())
    private var startTime = 0L
    private var elapsedTime = 0L
    private var isTimerRunning = false

    private var finalDuration = "00:00:00"

    private lateinit var binding: ActivityMapLiveTrackBinding
    private lateinit var mapboxMap: MapboxMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLatitude = 0.0
    private var currentLongitude = 0.0

    private lateinit var locationRequest: LocationRequest

    private lateinit var locationCallback: LocationCallback

    private val routePoints = mutableListOf<Point>()

    private var isTracking = false

    private lateinit var polylineAnnotationManager: PolylineAnnotationManager

    private var polylineAnnotation =
        null as com.mapbox.maps.plugin.annotation.generated.PolylineAnnotation?

    private lateinit var pointAnnotationManager: PointAnnotationManager

    private var startMarkerAdded = false

    private val viewModel: MapLiveTrackViewModel by viewModels()

    private val loader by lazy {
        ProgressView.getLoader(this)
    }

    private var boundaryLoaded = false

    private lateinit var gateAnnotationManager: PointAnnotationManager

    private lateinit var textAnnotationManager: PointAnnotationManager

    private var product_number = ""

    private var countyName = ""

    private var stateName = ""

    private var labelIndex = 0

    private var acres: Double = 0.00

    private var licenseEndDate = ""

    // Total distance travelled in meters
    private var totalDistanceMeters = 0f

    // Final distance shown after tracking stops
    private var finalDistance = "0 m"

    // Previous GPS location
    private var lastTrackedLocation: Location? = null

    private var hasTrackingSessionStarted = false

    private lateinit var trackingDotManager: PointAnnotationManager

    private var finalElevation = "0 m"

    private var startedAtUtc = ""
    private var endedAtUtc = ""

    private var licenseContractId: Int = 0

    private var trackingStartDate: Date? = null
    val endedAt = Date()

    /**
     * Initializes the activity.
     *
     * - Reads data from Intent.
     * - Initializes Mapbox.
     * - Starts observing API responses.
     * - Loads the map style.
     * - Fetches property coordinates.
     * - Initializes UI and click listeners.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapLiveTrackBinding.inflate(layoutInflater)
        setContentView(binding.root)

        product_number = intent.getStringExtra("product_number") ?: ""

        countyName = intent.getStringExtra("county_name") ?: ""

        stateName = intent.getStringExtra("state_name") ?: ""

        acres = intent.getDoubleExtra("acres", 0.0)

        licenseEndDate = intent.getStringExtra("license_end_date") ?: ""

        licenseContractId = intent.getIntExtra("license_contract_id",0)

        Log.e("call","licenseContractId--  "+licenseContractId)
        Log.e("call","product_number--  "+product_number)
        Log.e("call","countyName--  "+countyName)
        Log.e("call","stateName--  "+stateName)
        Log.e("call","acres--  "+acres)
        Log.e("call","licenseEndDate--  "+licenseEndDate)

        initializeMap()

        observeCoordinates()

        // OLD IMPLEMENTATION 
        // Previously called coordinates API when style loaded.
        // mapboxMap.loadStyle(
        //     com.mapbox.maps.Style.OUTDOORS
        // ) {
        //     callCoordinatesApi()
        // }
        mapboxMap.loadStyle(
            com.mapbox.maps.Style.OUTDOORS
        ) {
            loadAndDrawMapProperty()
        }

        setupInsets()
        initViews()
        clickListeners()

        updateStopButton(false)
    }

    private fun updateStopButton(isEnabled: Boolean) {

        binding.btnStop.isEnabled = isEnabled
        binding.btnStop.isClickable = isEnabled
        binding.btnStop.alpha = if (isEnabled) 1f else 0.4f
    }

    // OLD IMPLEMENTATION 
    // Previously used to call the separate coordinates API.
    // No longer required because map data is now obtained from the Home API licenses response.
    //
    // private fun callCoordinatesApi() {
    //     val token = PrefManager.getString(AppConstant.AUTH_TOKEN)
    //     viewModel.getCoordinates(
    //         token = token,
    //         propertyName = product_number
    //     )
    // }

    private fun loadAndDrawMapProperty() {
        var mapData: com.trax.app.models.home.license.Map? = null

        // 1. Try retrieving from Intent extras
        val mapJson = intent.getStringExtra("map_data")
        if (!mapJson.isNullOrEmpty()) {
            try {
                mapData = com.google.gson.Gson().fromJson(mapJson, com.trax.app.models.home.license.Map::class.java)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 2. Fallback to Shared Preferences
        if (mapData == null) {
            val list = PrefManager.getLicensesList()
            if (list != null) {
                val match = list.find { it.propertyDetails?.productNo == product_number || it.licenseDetails?.licenseContractID == licenseContractId }
                mapData = match?.map
            }
        }

        // 3. Draw the property boundary if geoJson is available
        val geoJson = mapData?.geoJson
        if (geoJson != null) {
            drawPropertyArea(geoJson)
            boundaryLoaded = true

            // Trigger Mapbox offline tile downloading
            val bounds = mapData.bounds
            val sw = bounds?.southWest
            val ne = bounds?.northEast
            if (sw?.latitude != null && sw.longitude != null && ne?.latitude != null && ne.longitude != null) {
                downloadMapOffline(
                    minLat = sw.latitude,
                    minLng = sw.longitude,
                    maxLat = ne.latitude,
                    maxLng = ne.longitude
                )
            }
        } else {
            Log.e("MAP_LOAD", "No Map GeoJSON data found to render.")
        }
    }

    private fun downloadMapOffline(minLat: Double, minLng: Double, maxLat: Double, maxLng: Double) {
        // Only download offline map region if internet is available
        if (!this.isInternetAvailable()) {
            Log.d("MapboxOffline", "No internet connection, skipping tile region download.")
            return
        }

        try {
            val offlineManager = OfflineManager()
            val tileStore = TileStore.create()

            val polygon = Polygon.fromLngLats(
                listOf(
                    listOf(
                        Point.fromLngLat(minLng, minLat),
                        Point.fromLngLat(maxLng, minLat),
                        Point.fromLngLat(maxLng, maxLat),
                        Point.fromLngLat(minLng, maxLat),
                        Point.fromLngLat(minLng, minLat)
                    )
                )
            )

            // 1. Load Style Pack
            val stylePackLoadOptions = StylePackLoadOptions.Builder()
                .glyphsRasterizationMode(GlyphsRasterizationMode.ALL_GLYPHS_RASTERIZED_LOCALLY)
                .build()

            offlineManager.loadStylePack(
                com.mapbox.maps.Style.OUTDOORS,
                stylePackLoadOptions,
                { progress ->
                    Log.d("MapboxOffline", "StylePack progress: ${progress.completedResourceCount}/${progress.requiredResourceCount}")
                },
                { expected ->
                    if (expected.isError) {
                        Log.e("MapboxOffline", "StylePack load failed: ${expected.error}")
                    } else {
                        Log.d("MapboxOffline", "StylePack loaded successfully")
                    }
                }
            )

            // 2. Load Tile Region
            val definition = TilesetDescriptorOptions.Builder()
                .styleURI(com.mapbox.maps.Style.OUTDOORS)
                .minZoom(8)
                .maxZoom(16)
                .build()

            val tilesetDescriptor = offlineManager.createTilesetDescriptor(definition)

            val tileRegionLoadOptions = TileRegionLoadOptions.Builder()
                .geometry(polygon)
                .descriptors(listOf(tilesetDescriptor))
                .build()

            val regionId = "offline-region-${product_number}"

            tileStore.loadTileRegion(
                regionId,
                tileRegionLoadOptions,
                { progress ->
                    Log.d("MapboxOffline", "TileRegion progress: ${progress.completedResourceCount}/${progress.requiredResourceCount}")
                },
                { expected ->
                    if (expected.isError) {
                        Log.e("MapboxOffline", "TileRegion load failed: ${expected.error}")
                    } else {
                        Log.d("MapboxOffline", "TileRegion loaded successfully")
                    }
                }
            )

        } catch (e: Exception) {
            Log.e("MapboxOffline", "Failed to start offline download: ${e.message}")
        }
    }

    private fun observeCoordinates() {

        // OLD IMPLEMENTATION 
        // Previously observed coordinates API success response.
        //
        // viewModel.coordinatesSuccess.observe(this) { response ->
        //     response?.let {
        //         drawPropertyArea(it)
        //         boundaryLoaded = true
        //     }
        // }

        viewModel.saveTrackSuccess.observe(this) { response ->

            response?.let {
                val intent = Intent(
                    this,
                    MainActivity::class.java
                )

                intent.flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK

                startActivity(intent)

                finish()
            }
        }

        viewModel.isLoading.observe(this) { loading ->

            if (isFinishing || isDestroyed) return@observe

            if (loading) {
                if (!loader.isShowing) {
                    loader.show()
                }
            } else {
                if (loader.isShowing) {
                    loader.dismiss()
                }
            }
        }


        viewModel.apiError.observe(this) {

            Log.e("MAP_API", it.toString())
        }
    }

    private fun drawPropertyArea(
        response: GeoJson
    ) {

        gateAnnotationManager.deleteAll()
        textAnnotationManager.deleteAll()

        labelIndex = 0

        val allPoints = mutableListOf<Point>()


        response.features?.forEach { feature ->

            when (feature.geometry?.type) {

                "Point" -> {

                    val coordinates =
                        feature.geometry.coordinates?.asJsonArray

                    if (
                        coordinates != null &&
                        coordinates.size() >= 2
                    ) {

                        val lng = coordinates[0].asDouble
                        val lat = coordinates[1].asDouble

                        val markerColor = feature.properties?.markerColor

                        addGateMarker(
                            lat,
                            lng,
                            markerColor
                        )

//                        addRluLabel(
//                            lat = lat,
//                            lng = lng,
//                            text = feature.properties?.rluNo
//                        )
                    }
                }

                else -> {

                    val rings = mutableListOf<List<Point>>()

                    extractRings(
                        feature.geometry?.coordinates,
                        rings
                    )

                    if (rings.isNotEmpty()) {

                        drawPolygonFill(
                            outerRing = rings.first(),
                            holes = rings.drop(1),
                            properties = feature.properties
                        )

                        rings.forEach { ring ->

                            polylineAnnotationManager.create(
                                PolylineAnnotationOptions()
                                    .withPoints(ring)
                                    .withLineColor(feature.properties?.stroke ?: "#007F56")
                                    .withLineWidth(2.0)
                            )

                            // ADD THIS
                            allPoints.addAll(ring)
                        }
                    }
                }
            }
        }

        zoomToEntireProperty(allPoints)
    }

    private fun drawPolygonFill(
        outerRing: List<Point>,
        holes: List<List<Point>>,
        properties: Properties?
    ) {

        val fillColor =
            properties?.fill ?: return

        val fillOpacity =
            properties?.fillOpacity ?: 0.3

        val polygon = Polygon.fromLngLats(

            listOf(
                outerRing
            ) + holes
        )

        val feature =
            Feature.fromGeometry(polygon)

        val sourceId =
            "fill-source-${System.currentTimeMillis()}"

        val layerId =
            "fill-layer-${System.currentTimeMillis()}"

        mapboxMap.getStyle()?.apply {

            addSource(
                geoJsonSource(sourceId) {
                    feature(feature)
                }
            )

            addLayer(
                fillLayer(layerId, sourceId) {

                    fillColor(fillColor)

                    fillOpacity(
                        (properties?.fillOpacity ?: 0.3f).toDouble()
                    )
                }
            )
        }
    }

    private fun zoomToEntireProperty(
        points: List<Point>
    ) {

        if (points.isEmpty()) return

        var minLat = points.first().latitude()
        var maxLat = points.first().latitude()

        var minLng = points.first().longitude()
        var maxLng = points.first().longitude()

        points.forEach {

            minLat = minOf(minLat, it.latitude())
            maxLat = maxOf(maxLat, it.latitude())

            minLng = minOf(minLng, it.longitude())
            maxLng = maxOf(maxLng, it.longitude())
        }

        val bounds = CoordinateBounds(

            Point.fromLngLat(
                minLng,
                minLat
            ),

            Point.fromLngLat(
                maxLng,
                maxLat
            )
        )

        val cameraOptions =
            mapboxMap.cameraForCoordinateBounds(
                bounds,
                EdgeInsets(
                    150.0, // top
                    100.0, // left
                    350.0, // bottom
                    100.0  // right
                ),
                null,
                null
            )

        mapboxMap.setCamera(cameraOptions)

        /*
        mapboxMap.setCamera(
     cameraOptions.toBuilder()
        .zoom(9.0)
        .build()
)
         */
    }

    //** Initialize Map **//
    private fun initializeMap(){

        mapboxMap = binding.mapView.mapboxMap

        // ========================================================
// Initialize Mapbox annotation managers
// Used to draw the saved track, tracking dots and start marker
// ========================================================

        polylineAnnotationManager =
            binding.mapView.annotations.createPolylineAnnotationManager()

        fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(this)

        pointAnnotationManager =
            binding.mapView.annotations.createPointAnnotationManager()

        gateAnnotationManager =
            binding.mapView.annotations.createPointAnnotationManager()

        textAnnotationManager =
            binding.mapView.annotations.createPointAnnotationManager()

        trackingDotManager =
            binding.mapView.annotations.createPointAnnotationManager()

        createLocationRequest()

        createLocationCallback()

        setupMap()

    //    setupRouteLayer()   // <-- ADD HERE

        checkLocationPermission()
    }

    //** Setup Map **//
    private fun setupMap() {

        mapboxMap.setCamera(
            CameraOptions.Builder()
                .zoom(2.0)
                .build()
        )
    }

    //** Location Permission **//
    private val locationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (granted) {

                showCurrentLocation()
            }
        }

    //** Check Permission **//
    private fun checkLocationPermission() {

        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {

            showCurrentLocation()

        } else {

            locationPermissionLauncher.launch(
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
    }

    //** Show Current Location **//

    private fun showCurrentLocation() {

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->

                location?.let {

                    currentLatitude = it.latitude
                    currentLongitude = it.longitude

                    enableLocationComponent()

                    // DON'T MOVE CAMERA HERE
                }
            }
    }

    //** Enable Blue Dot **//
    private fun enableLocationComponent() {

        binding.mapView.location.updateSettings {

            enabled = true

            pulsingEnabled = true
        }
    }

    //** Move Camera **//
    private fun moveCamera(
        latitude: Double,
        longitude: Double
    ) {
        mapboxMap.flyTo(
            CameraOptions.Builder()
                .center(
                    Point.fromLngLat(
                        longitude,
                        latitude
                    )
                )
                .zoom(15.0)
                .build()
        )
    }

    //** manage topbar overlap with status bar **//
    private fun setupInsets() {

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->

            // Status bar
            val statusBarHeight =
                insets.getInsets(WindowInsetsCompat.Type.statusBars()).top

            binding.statusBarSpace.layoutParams.height =
                statusBarHeight

            binding.statusBarSpace.requestLayout()

            // Bottom navigation / gesture bar
            val bottomInset =
                insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom

            val params =
                binding.bottomTrackingCard.layoutParams
                        as ConstraintLayout.LayoutParams

            params.bottomMargin = bottomInset + 12

            binding.bottomTrackingCard.layoutParams = params

            insets
        }
    }

    //** initViews **//
    private fun initViews() {

        binding.tvTrackName.text = product_number

        binding.tvTrackLocation.text = stateName + ", " + countyName

        binding.tvTrackingTime.text = "00:00:00"

        binding.tvDistance.text = "0 km"

        binding.tvAcres.text  = ""+acres

        binding.tvValidDate.text =  formatLicenseDate(licenseEndDate)

        if (this.isInternetAvailable()) {
            binding.tvNetworkStatus.text = "Online"
            binding.ivNetwork.setImageResource(R.drawable.ic_online)
        } else {
            binding.tvNetworkStatus.text = "Offline"
            binding.ivNetwork.setImageResource(R.drawable.ic_offline)
        }
    }

    //** click listeners **//
    private fun clickListeners() {

        // Back Button

        binding.ivBack.setOnClickListener {

            onBackPressedDispatcher.onBackPressed()
        }

        // Map zoom in

        binding.rlZoomIn.setOnClickListener {

           zoomIn()
        }

        // Map zoom out

        binding.rlZoomOut.setOnClickListener {

          zoomOut()
        }

        // Stop Tracking BottomSheet
        binding.btnStop.setOnClickListener {

            showStopTrackingBottomSheet()
        }

        // Start / Pause BottomSheet

        binding.btnPlayPause.setOnClickListener {

            if (isTracking) {

                // Pause
                pauseTimer()

                isTracking = false

                fusedLocationClient.removeLocationUpdates(locationCallback)

                binding.btnPlayPause.setImageResource(R.drawable.ic_play)

            } else {

                if (hasTrackingSessionStarted) {

                    // Resume directly
                    resumeTracking()

                } else {

                    // First time only
                    showStartTrackingBottomSheet()
                }
            }
        }
    }

    private fun resumeTracking() {

        isTracking = true

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            mainLooper
        )

        resumeTimer()

        binding.btnPlayPause.setImageResource(R.drawable.ic_pause)
    }

    // ================= START TRACKING =================

    private fun showStartTrackingBottomSheet() {

        val dialog = BottomSheetDialog(this)

        val sheetBinding =
            BottomsheetStartTrackingBinding.inflate(layoutInflater)

        dialog.setContentView(sheetBinding.root)

        dialog.setCancelable(true)

        sheetBinding.tvCancel.setOnClickListener {

            dialog.dismiss()
        }

        sheetBinding.tvStart.setOnClickListener {

            dialog.dismiss()

            hasTrackingSessionStarted = true

            moveCameraToCurrentLocationAndStartTracking()
        }

        dialog.show()
    }

    /**
     * Moves the camera to the user's current location.
     * Once the camera animation finishes, live tracking starts.
     */
    private fun moveCameraToCurrentLocationAndStartTracking() {

        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->

            location ?: return@addOnSuccessListener

            currentLatitude = location.latitude
            currentLongitude = location.longitude

            mapboxMap.flyTo(
                CameraOptions.Builder()
                    .center(
                        Point.fromLngLat(
                            currentLongitude,
                            currentLatitude
                        )
                    )
                    .zoom(18.0)
                    .build()
            )

            // Wait until camera animation completes
            binding.mapView.postDelayed({

                if (isTimerRunning) {

                    resumeTimer()

                } else {

                    trackingStartDate = Date()
                    startTracking()
                }

                binding.btnPlayPause.setImageResource(R.drawable.ic_pause)

            }, 1200) // same duration as flyTo animation
        }
    }

    // ================= Stop TRACKING =================

    private fun showStopTrackingBottomSheet() {

        val dialog = BottomSheetDialog(this)

        val sheetBinding =
            BottomsheetStopTrackingBinding.inflate(layoutInflater)

        dialog.setContentView(sheetBinding.root)

        dialog.setCancelable(true)

        sheetBinding.tvCancel.setOnClickListener {

            dialog.dismiss()
        }

        sheetBinding.tvStop.setOnClickListener {

            dialog.dismiss()

            stopTracking()

            showSaveTrackBottomSheet()
        }

        dialog.show()
    }

    // ================= SAVE TRACK =================

    private fun showSaveTrackBottomSheet() {

        val dialog = BottomSheetDialog(this)

        val sheetBinding =
            BottomsheetSaveTrackBinding.inflate(layoutInflater)

        dialog.setContentView(sheetBinding.root)

        dialog.setCancelable(true)

        val currentDate = SimpleDateFormat("MMM dd", Locale.ENGLISH)
            .format(Date())

        sheetBinding.tvDuration.text = finalDuration
        sheetBinding.tvDistance.text = finalDistance

        sheetBinding.tvTrackName.text = product_number + " — " + currentDate

        var finalPace = calculatePace(
            totalDistanceMeters,
            elapsedTime
        )

        sheetBinding.tvPace.text = finalPace

        sheetBinding.tvElevation.text = finalElevation

        sheetBinding.tvCancel.setOnClickListener {

            dialog.dismiss()
        }

        sheetBinding.tvSave.setOnClickListener {

            dialog.dismiss()

            // Save track here

            resetTimer()

            hasTrackingSessionStarted = false


            endedAtUtc = isoFormatter.format(Date())

            //** hit api to save track **//

            val token =
                PrefManager.getString(AppConstant.AUTH_TOKEN)

            val request = SaveTrackRequest(

                licenseContractId = licenseContractId,

                trackName = sheetBinding.tvTrackName.text.toString(),

                notes = sheetBinding.etNotes.text.toString().trim(),

                startedAtUtc = startedAtUtc,

                endedAtUtc = endedAtUtc,

                totalDistanceMeters = totalDistanceMeters.toDouble(),

                trackLine = buildTrackLine()
            )

            viewModel.saveTrack(
                token,
                request
            )

        }

        dialog.show()
    }

    // ========================================================
// Add start marker only once at the beginning of the track
// ========================================================

    private fun addStartMarker(
        latitude: Double,
        longitude: Double
    ) {

        if (startMarkerAdded) return

        val bitmap = bitmapFromDrawableRes(
            R.drawable.ic_start_marker
        ) ?: return

        val pointAnnotationOptions =
            PointAnnotationOptions()
                .withPoint(
                    Point.fromLngLat(
                        longitude,
                        latitude
                    )
                )
                .withIconImage(bitmap)
                .withIconSize(1.0)

        pointAnnotationManager.create(
            pointAnnotationOptions
        )

        startMarkerAdded = true
    }

    // ========================================================
// Convert drawable resource into Bitmap
// Used for custom Mapbox marker icons
// ========================================================

    private fun bitmapFromDrawableRes(
        drawableRes: Int
    ): Bitmap? {

        val drawable =
            ContextCompat.getDrawable(
                this,
                drawableRes
            ) ?: return null

        val bitmap =
            Bitmap.createBitmap(
                drawable.intrinsicWidth,
                drawable.intrinsicHeight,
                Bitmap.Config.ARGB_8888
            )

        val canvas = Canvas(bitmap)

        drawable.setBounds(
            0,
            0,
            canvas.width,
            canvas.height
        )

        drawable.draw(canvas)

        return bitmap
    }


    private fun createLocationRequest() {

        locationRequest =
            LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                3000L
            )
                .setMinUpdateDistanceMeters(3f)
                .build()
    }

    private fun createLocationCallback() {

        locationCallback =
            object : LocationCallback() {

                override fun onLocationResult(
                    locationResult: LocationResult
                ) {

                    if (!isTracking) return

                    locationResult.lastLocation?.let { location ->


                        finalElevation = String.format(
                            Locale.ENGLISH,
                            "%.1f m",
                            location.altitude
                        )

                        // Calculate travelled distance
                        lastTrackedLocation?.let { previous ->

                            totalDistanceMeters += previous.distanceTo(location)
                        }

// Save current location
                        lastTrackedLocation = location

// Update UI continuously
                        binding.tvDistance.text = formatDistance(totalDistanceMeters)

// Save latest value for Save Track screen
                        finalDistance = formatDistance(totalDistanceMeters)

                        val point = Point.fromLngLat(
                            location.longitude,
                            location.latitude
                        )

                        routePoints.add(point)


                        updateRoute()

                        if (!startMarkerAdded) {

                            addStartMarker(
                                location.latitude,
                                location.longitude
                            )
                        }

                        addTrackingDot(point)
                    }
                }
            }
    }

    /**
     * Converts meters into a user friendly format.
     *
     * Examples:
     * 245m
     * 982m
     * 1.23 km
     * 5.87 km
     */
    private fun formatDistance(
        meters: Float
    ): String {

        return if (meters < 1000f) {

            "${meters.toInt()} m"

        } else {

            String.format(
                Locale.ENGLISH,
                "%.2f km",
                meters / 1000f
            )
        }
    }

    private fun updateRoute() {

        if (routePoints.size < 2) return

        polylineAnnotation?.let {
            polylineAnnotationManager.delete(it)
        }

        polylineAnnotation =
            polylineAnnotationManager.create(

                PolylineAnnotationOptions()
                    .withPoints(routePoints)
            )

        Log.e(
            "TRACK",
            "Polyline Updated = ${routePoints.size}"
        )
    }



    private fun startTracking() {

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        // Existing tracking logic

        updateStopButton(true)

        trackingStartDate?.let {
            startedAtUtc = isoFormatter.format(it)
        }

        // Fresh session
        routePoints.clear()

        trackingDotManager.deleteAll()

        pointAnnotationManager.deleteAll()

        startMarkerAdded = false

        totalDistanceMeters = 0f

        lastTrackedLocation = null

        finalDistance = "0 m"

        binding.tvDistance.text = finalDistance

        isTracking = true

        val startPoint = Point.fromLngLat(
            currentLongitude,
            currentLatitude
        )

        routePoints.add(startPoint)

        addStartMarker(
            currentLatitude,
            currentLongitude
        )


        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            mainLooper
        )

        startTimer()

    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun stopTracking() {

        endedAtUtc = Instant.now().toString()

        isTracking = false

        fusedLocationClient.removeLocationUpdates(locationCallback)

        stopTimer()

        hasTrackingSessionStarted = false
    }

    private fun setupRouteLayer() {

        mapboxMap.loadStyle(

            style = com.mapbox.maps.Style.STANDARD
        ) {

            geoJsonSource("route-source") {

                feature(
                    Feature.fromGeometry(
                        LineString.fromLngLats(
                            emptyList()
                        )
                    )
                )
            }

            lineLayer(
                "route-layer",
                "route-source"
            ) {

                lineColor("#FF0000")

                lineWidth(5.0)

                lineCap(LineCap.ROUND)

                lineJoin(LineJoin.ROUND)
            }
        }
    }

    private fun extractPoints(
        coordinates: JsonElement?,
        result: MutableList<Point>
    ) {
        if (coordinates == null) return

        when {

            coordinates.isJsonArray -> {

                val array = coordinates.asJsonArray

                if (
                    array.size() >= 2 &&
                    array[0].isJsonPrimitive &&
                    array[1].isJsonPrimitive
                ) {

                    result.add(
                        Point.fromLngLat(
                            array[0].asDouble,
                            array[1].asDouble
                        )
                    )

                } else {

                    array.forEach {
                        extractPoints(it, result)
                    }
                }
            }
        }
    }

    private fun extractRings(
        element: JsonElement?,
        rings: MutableList<List<Point>>
    ) {

        if (element == null || !element.isJsonArray) return

        val array = element.asJsonArray

        // Ring detected
        if (
            array.size() > 0 &&
            array[0].isJsonArray &&
            array[0].asJsonArray.size() >= 2 &&
            array[0].asJsonArray[0].isJsonPrimitive
        ) {

            val ringPoints = mutableListOf<Point>()

            array.forEach { pointElement ->

                val pointArray = pointElement.asJsonArray

                ringPoints.add(
                    Point.fromLngLat(
                        pointArray[0].asDouble,
                        pointArray[1].asDouble
                    )
                )
            }

            rings.add(ringPoints)

            return
        }

        array.forEach {
            extractRings(it, rings)
        }
    }

    private fun zoomIn() {

        val currentZoom = mapboxMap.cameraState.zoom

        mapboxMap.flyTo(
            CameraOptions.Builder()
                .zoom(currentZoom + 1)
                .build()
        )
    }

    private fun zoomOut() {

        val currentZoom = mapboxMap.cameraState.zoom

        mapboxMap.flyTo(
            CameraOptions.Builder()
                .zoom(currentZoom - 1)
                .build()
        )
    }

    private fun drawGate(
        feature: com.trax.app.models.home.license.Feature
    ) {

        if (feature.geometry?.type != "Point")
            return

        val coordinates =
            feature.geometry.coordinates?.asJsonArray
                ?: return

        if (coordinates.size() < 2)
            return

        val lng = coordinates[0].asDouble
        val lat = coordinates[1].asDouble

        val bitmap =
            bitmapFromDrawableRes(
                R.drawable.ic_gate_marker
            ) ?: return

        gateAnnotationManager.create(

            PointAnnotationOptions()
                .withPoint(
                    Point.fromLngLat(
                        lng,
                        lat
                    )
                )
                .withIconImage(bitmap)
                .withIconSize(0.8)
        )

//        addRluLabel(
//            lat,
//            lng,
//            feature.properties?.rluNo
//        )
    }

    private fun addRluLabel(
        lat: Double,
        lng: Double,
        text: String?
    ) {

        if (text.isNullOrBlank()) return

        val offsetLat =
            lat + (0.00025 * (labelIndex % 4))

        val offsetLng =
            lng + (0.00015 * (labelIndex % 3))

        labelIndex++

        val bitmap = createTextBitmap(text)

        textAnnotationManager.create(
            PointAnnotationOptions()
                .withPoint(
                    Point.fromLngLat(
                        offsetLng,
                        offsetLat
                    )
                )
                .withIconImage(bitmap)
        )
    }

    private fun createTextBitmap(
        text: String
    ): Bitmap {

        val strokePaint = Paint().apply {
            color = Color.WHITE
            textSize = 24f
            style = Paint.Style.STROKE
            strokeWidth = 5f
            isAntiAlias = true
            typeface = Typeface.DEFAULT_BOLD
        }

        val fillPaint = Paint().apply {
            color = Color.BLACK
            textSize = 24f
            style = Paint.Style.FILL
            isAntiAlias = true
            typeface = Typeface.DEFAULT_BOLD
        }

        val width =
            fillPaint.measureText(text).toInt() + 40

        val height = 50

        val bitmap =
            Bitmap.createBitmap(
                width,
                height,
                Bitmap.Config.ARGB_8888
            )

        val canvas = Canvas(bitmap)

        canvas.drawColor(Color.TRANSPARENT)

        canvas.drawText(
            text,
            10f,
            35f,
            strokePaint
        )

        canvas.drawText(
            text,
            10f,
            35f,
            fillPaint
        )

        return bitmap
    }

    private fun createGateMarker(
        colorHex: String?
    ): Bitmap {

        val markerColor = try {
            Color.parseColor(colorHex ?: "#EF4444")
        } catch (e: Exception) {
            Color.parseColor("#EF4444")
        }

        val size = 50

        val bitmap = Bitmap.createBitmap(
            size,
            size,
            Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(bitmap)

        val fillPaint = Paint().apply {
            color = markerColor
            isAntiAlias = true
            style = Paint.Style.FILL
        }

        val strokePaint = Paint().apply {
            color = Color.WHITE
            strokeWidth = 3f
            isAntiAlias = true
            style = Paint.Style.STROKE
        }

        canvas.drawCircle(
            size / 2f,
            size / 2f,
            18f,
            fillPaint
        )

        canvas.drawCircle(
            size / 2f,
            size / 2f,
            18f,
            strokePaint
        )

        return bitmap
    }

    private fun addGateMarker(
        lat: Double,
        lng: Double,
        markerColor: String?
    ) {

        val bitmap = createGateMarker(markerColor)

        pointAnnotationManager.create(
            PointAnnotationOptions()
                .withPoint(
                    Point.fromLngLat(
                        lng,
                        lat
                    )
                )
                .withIconImage(bitmap)
                .withIconSize(1.0)
        )
    }

    private val timerRunnable = object : Runnable {

        override fun run() {

            val currentTime =
                SystemClock.elapsedRealtime()

            elapsedTime =
                currentTime - startTime

            val hours =
                elapsedTime / 3600000

            val minutes =
                (elapsedTime % 3600000) / 60000

            val seconds =
                (elapsedTime % 60000) / 1000

            finalDuration =
                String.format(
                    "%02d:%02d:%02d",
                    hours,
                    minutes,
                    seconds
                )

            binding.tvTrackingTime.text =
                finalDuration

            timerHandler.postDelayed(
                this,
                1000
            )
        }
    }

    private fun startTimer() {

        if (isTimerRunning)
            return

        startTime =
            SystemClock.elapsedRealtime() - elapsedTime

        timerHandler.post(timerRunnable)

        isTimerRunning = true
    }

    private fun pauseTimer() {

        if (!isTimerRunning)
            return

        timerHandler.removeCallbacks(timerRunnable)

        isTimerRunning = false
    }

    private fun resumeTimer() {

        startTimer()
    }

    private fun stopTimer() {

        timerHandler.removeCallbacks(timerRunnable)

        isTimerRunning = false

        binding.tvTrackingTime.text = finalDuration
    }

    private fun resetTimer() {

        timerHandler.removeCallbacks(timerRunnable)

        startTime = 0L

        elapsedTime = 0L

        finalDuration = "00:00:00"

        isTimerRunning = false

        binding.tvTrackingTime.text = finalDuration

        totalDistanceMeters = 0f

        lastTrackedLocation = null

        finalDistance = "0 m"

        binding.tvDistance.text = finalDistance

        hasTrackingSessionStarted = false

        isTracking = false
    }

    private fun calculatePace(
        distanceMeters: Float,
        elapsedMillis: Long
    ): String {

        if (distanceMeters < 1f || elapsedMillis <= 0L) {
            return "--"
        }

        val distanceKm = distanceMeters / 1000f

        if (distanceKm <= 0f) {
            return "--"
        }

        val totalSeconds = elapsedMillis / 1000

        val secondsPerKm = (totalSeconds / distanceKm).toInt()

        val minutes = secondsPerKm / 60
        val seconds = secondsPerKm % 60

        return String.format("%02d:%02d /km", minutes, seconds)
    }

    private fun createTrackingDot(): Bitmap {

        val size = 32

        val bitmap = Bitmap.createBitmap(
            size,
            size,
            Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(bitmap)

        val paint = Paint().apply {
            color = Color.parseColor("#007F56")
            isAntiAlias = true
            style = Paint.Style.FILL
        }

        canvas.drawCircle(
            size / 2f,
            size / 2f,
            10f,
            paint
        )

        return bitmap
    }

    // ========================================================
// Lazy initialization of tracking dot bitmap
// Created only once and reused for all points
// ========================================================

    private val trackingDotBitmap by lazy {
        createTrackingDot()
    }

    private fun addTrackingDot(point: Point) {

        trackingDotManager.create(
            PointAnnotationOptions()
                .withPoint(point)
                .withIconImage(trackingDotBitmap)
                .withIconSize(1.2)
        )
    }

    private fun buildTrackLine(): TrackLine {

        val coordinates =
            routePoints.map {

                listOf(
                    it.longitude(),
                    it.latitude()
                )
            }

        return TrackLine(

            type = "Feature",

            properties = HashMap(),

            geometry = Geometry(

                type = "LineString",

                coordinates = coordinates
            )
        )
    }

    private val isoFormatter =
        SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            Locale.US
        ).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

}