package com.trax.app.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.gson.JsonElement
import com.mapbox.common.TileRegionLoadOptions
import com.mapbox.common.TileStore
import com.mapbox.geojson.Feature
import com.mapbox.geojson.Point
import com.mapbox.geojson.Polygon
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.CoordinateBounds
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.GlyphsRasterizationMode
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.OfflineManager
import com.mapbox.maps.StylePackLoadOptions
import com.mapbox.maps.TilesetDescriptorOptions
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.fillLayer
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.plugin.animation.flyTo
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createPolylineAnnotationManager
import com.mapbox.maps.plugin.locationcomponent.location
import com.trax.app.R
import com.trax.app.base.BaseActivity
import com.trax.app.databinding.ActivityMapLiveTrackBinding
import com.trax.app.databinding.BottomsheetSaveTrackBinding
import com.trax.app.databinding.BottomsheetStartTrackingBinding
import com.trax.app.databinding.BottomsheetStopTrackingBinding
import com.trax.app.models.home.license.GeoJson
import com.trax.app.models.home.license.Properties
import com.trax.app.models.live_track.save_track.Geometry
import com.trax.app.models.live_track.save_track.OfflineTrack
import com.trax.app.models.live_track.save_track.SaveTrackRequest
import com.trax.app.models.live_track.save_track.TrackLine
import com.trax.app.progressBar.ProgressView
import com.trax.app.services.TrackingForegroundService
import com.trax.app.utils.AppConstant
import com.trax.app.utils.OfflineTrackManager
import com.trax.app.utils.PrefManager
import com.trax.app.viewModels.MapLiveTrackViewModel
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class MapLiveTrackActivity : BaseActivity() {

    //==============================================================================
    // Variables
    //==============================================================================

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

    private var totalDistanceMeters = 0f
    private var finalDistance = "0 m"
    private var lastTrackedLocation: Location? = null
    private var hasTrackingSessionStarted = false

    private lateinit var trackingDotManager: PointAnnotationManager
    private var finalElevation = "0 m"
    private var currentElevationMeters: Double = 0.0

    private var startedAtUtc = ""
    private var endedAtUtc = ""
    private var licenseContractId: Int = 0
    private var trackingStartDate: Date? = null

    private var cachedGeoJson: GeoJson? = null
    private var styleLoadedCalled = false

    private var dynamicSimulationCoordinates: List<Pair<Double, Double>> = emptyList()

    private val defaultCoordinates by lazy {
        listOf(
            Pair(30.6950, 76.6850),
            Pair(30.6980, 76.6880),
            Pair(30.7023, 76.6925)
        )
    }

    private var simulationIndex = 0
    private val simulationHandler = Handler(Looper.getMainLooper())

    private val simulationRunnable = object : Runnable {
        override fun run() {
            if (!isTracking) return
            val coords = getSimulationCoordinates()
            if (simulationIndex < coords.size) {
                val (lat, lng) = coords[simulationIndex]
                val loc = Location("static_sim").apply {
                    latitude = lat
                    longitude = lng
                    altitude = 120.0
                    time = System.currentTimeMillis()
                    accuracy = 1.0f
                }
                processSimulatedLocation(loc)
                simulationIndex++
                if (simulationIndex >= coords.size) {
                    simulationIndex = 0
                }
                if (isTracking) {
                    simulationHandler.postDelayed(this, 1000L)
                }
            }
        }
    }

    private val footstepBitmap by lazy {
        bitmapFromDrawableRes(R.drawable.ic_footstep)
    }

    private val timerRunnable = object : Runnable {
        override fun run() {
            val currentTime = SystemClock.elapsedRealtime()
            elapsedTime = currentTime - startTime

            val hours = elapsedTime / 3600000
            val minutes = (elapsedTime % 3600000) / 60000
            val seconds = (elapsedTime % 60000) / 1000

            finalDuration = String.format("%02d:%02d:%02d", hours, minutes, seconds)
            binding.tvTrackingTime.text = finalDuration

            timerHandler.postDelayed(this, 1000)
        }
    }

    //==============================================================================
    // onCreate / Lifecycle Methods
    //==============================================================================

    //--------------------------------------------------
    // Initializes the activity, view binding, and map controls.
    //--------------------------------------------------
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapLiveTrackBinding.inflate(layoutInflater)
        setContentView(binding.root)

        product_number = intent.getStringExtra("product_number") ?: ""
        countyName = intent.getStringExtra("county_name") ?: ""
        stateName = intent.getStringExtra("state_name") ?: ""

        acres = intent.getDoubleExtra("acres", 0.0)
        if (acres == 0.0) {
            val strAcres = intent.getStringExtra("acres")
            acres = strAcres?.toDoubleOrNull() ?: 0.0
        }

        licenseEndDate = intent.getStringExtra("license_end_date") ?: ""

        licenseContractId = intent.getIntExtra("license_contract_id", 0)
        if (licenseContractId == 0) {
            val strId = intent.getStringExtra("license_contract_id")
            licenseContractId = strId?.toIntOrNull() ?: 0
        }

        Log.e("call", "licenseContractId--  " + licenseContractId)
        Log.e("call", "product_number--  " + product_number)
        Log.e("call", "countyName--  " + countyName)
        Log.e("call", "stateName--  " + stateName)
        Log.e("call", "acres--  " + acres)
        Log.e("call", "licenseEndDate--  " + licenseEndDate)

        if (!isInternetAvailable() && !PrefManager.isProductDownloaded(product_number)) {
            Toast.makeText(this, "Please download this map first to use it in offline mode.", Toast.LENGTH_LONG).show()
            binding.btnPlayPause.isEnabled = false
            binding.btnPlayPause.isClickable = false
            binding.btnPlayPause.alpha = 0.4f
            binding.mapView.visibility = View.INVISIBLE
            setupInsets()
            initViews()
            return
        }

        initializeMap()
        initObservers()
        initMapStyleAndDraw()
        setupInsets()
        initViews()
        initClicks()
        registerNetworkCallback()

        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isTracking || hasTrackingSessionStarted) {
                    showStopTrackingBottomSheet(fromBackButton = true)
                } else {
                    finish()
                }
            }
        })

        updateStopButton(false)
    }

    //--------------------------------------------------
    // Notifies the MapView that the activity is starting.
    //--------------------------------------------------
    override fun onStart() {
        super.onStart()
        binding.mapView.onStart()
    }

    //--------------------------------------------------
    // Notifies the MapView that the activity is stopping.
    //--------------------------------------------------
    override fun onStop() {
        super.onStop()
        binding.mapView.onStop()
    }

    //--------------------------------------------------
    // Cleans up resources, services, and maps when activity is destroyed.
    //--------------------------------------------------
    override fun onDestroy() {
        super.onDestroy()
        unregisterNetworkCallback()
        if (isTracking) {
            TrackingForegroundService.stop(this)
        }
        binding.mapView.onDestroy()
    }

    //--------------------------------------------------
    // Notifies the MapView about low memory conditions.
    //--------------------------------------------------
    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapView.onLowMemory()
    }

    //==============================================================================
    // Initializations
    //==============================================================================

    //--------------------------------------------------
    // Populates the text views and layouts with property info.
    //--------------------------------------------------
    private fun initViews() {
        binding.tvTrackName.text = product_number
        binding.tvTrackLocation.text = stateName + " County, " + countyName
        binding.tvTrackingTime.text = "00:00:00"
        binding.tvDistance.text = "0 km"
        binding.tvAcres.text = "" + acres
        binding.tvValidDate.text = formatLicenseDate(licenseEndDate)

        updateNetworkStatus(this.isInternetAvailable())
    }

    //--------------------------------------------------
    // Initializes click listeners for UI buttons and map controls.
    //--------------------------------------------------
    private fun initClicks() {
        binding.ivMapStyleToggle.setOnClickListener {
            if (currentStyleUri == com.mapbox.maps.Style.STANDARD) {
                currentStyleUri = com.mapbox.maps.Style.SATELLITE
                binding.ivMapStyleToggle.setImageResource(R.drawable.img_topographic)
            } else {
                currentStyleUri = com.mapbox.maps.Style.STANDARD
                binding.ivMapStyleToggle.setImageResource(R.drawable.img_satellite)
            }
            loadMapStyle()
        }

        binding.ivBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.rlZoomIn.setOnClickListener {
            zoomIn()
        }

        binding.rlZoomOut.setOnClickListener {
            zoomOut()
        }

        binding.btnStop.setOnClickListener {
            showStopTrackingBottomSheet(fromBackButton = false)
        }

        binding.btnPlayPause.setOnClickListener {
            if (isTracking) {
                pauseTimer()
                isTracking = false
                TrackingForegroundService.stop(this)
                simulationHandler.removeCallbacks(simulationRunnable)
                binding.btnPlayPause.setImageResource(R.drawable.ic_play)
            } else {
                if (hasTrackingSessionStarted) {
                    resumeTracking()
                } else {
                    showStartTrackingBottomSheet()
                }
            }
        }
    }

    //--------------------------------------------------
    // Initializes observers for live tracking events.
    //--------------------------------------------------
    private fun initObservers() {
        viewModel.saveTrackSuccess.observe(this) { response ->
            response?.let {
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
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

    //--------------------------------------------------
    // Adjusts margins and paddings for status bar and navigation offsets.
    //--------------------------------------------------
    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            binding.statusBarSpace.layoutParams.height = statusBarHeight
            binding.statusBarSpace.requestLayout()

            val bottomInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            val params = binding.bottomTrackingCard.layoutParams as ConstraintLayout.LayoutParams
            params.bottomMargin = bottomInset + 12
            binding.bottomTrackingCard.layoutParams = params

            insets
        }
    }

    //==============================================================================
    // Offline Map Functions
    //==============================================================================

    //--------------------------------------------------
    // Retrieves map boundary data from intent extras or cached preferences.
    //--------------------------------------------------
    private fun getMapDataFromIntentOrPrefs(): com.trax.app.models.home.license.Map? {
        var mapData: com.trax.app.models.home.license.Map? = null
        val mapJson = intent.getStringExtra("map_data")
        if (!mapJson.isNullOrEmpty()) {
            try {
                mapData = com.google.gson.Gson().fromJson(mapJson, com.trax.app.models.home.license.Map::class.java)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (mapData == null) {
            val list = PrefManager.getLicensesList()
            if (list != null) {
                val match = list.find {
                    (!product_number.isNullOrEmpty() && it.propertyDetails?.productNo == product_number) ||
                            (licenseContractId != 0 && it.licenseDetails?.licenseContractID == licenseContractId)
                }
                mapData = match?.map
            }
        }
        return mapData
    }

    //--------------------------------------------------
    // Starts background downloads of Mapbox tile regions for offline usage.
    //--------------------------------------------------
    private fun downloadMapOffline(minLat: Double, minLng: Double, maxLat: Double, maxLng: Double) {
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

            val stylePackLoadOptions = StylePackLoadOptions.Builder()
                .glyphsRasterizationMode(GlyphsRasterizationMode.ALL_GLYPHS_RASTERIZED_LOCALLY)
                .build()

            offlineManager.loadStylePack(
                com.mapbox.maps.Style.STANDARD,
                stylePackLoadOptions,
                { progress ->
                    Log.d("MapboxOffline", "Standard StylePack progress: ${progress.completedResourceCount}/${progress.requiredResourceCount}")
                },
                { expected ->
                    if (expected.isError) {
                        Log.e("MapboxOffline", "Standard StylePack load failed: ${expected.error}")
                    } else {
                        Log.d("MapboxOffline", "Standard StylePack loaded successfully")
                    }
                }
            )

            offlineManager.loadStylePack(
                com.mapbox.maps.Style.SATELLITE,
                stylePackLoadOptions,
                { progress ->
                    Log.d("MapboxOffline", "Satellite StylePack progress: ${progress.completedResourceCount}/${progress.requiredResourceCount}")
                },
                { expected ->
                    if (expected.isError) {
                        Log.e("MapboxOffline", "Satellite StylePack load failed: ${expected.error}")
                    } else {
                        Log.d("MapboxOffline", "Satellite StylePack loaded successfully")
                    }
                }
            )

            offlineManager.loadStylePack(
                com.mapbox.maps.Style.SATELLITE_STREETS,
                stylePackLoadOptions,
                { progress ->
                    Log.d("MapboxOffline", "Satellite Streets StylePack progress: ${progress.completedResourceCount}/${progress.requiredResourceCount}")
                },
                { expected ->
                    if (expected.isError) {
                        Log.e("MapboxOffline", "Satellite Streets StylePack load failed: ${expected.error}")
                    } else {
                        Log.d("MapboxOffline", "Satellite Streets StylePack loaded successfully")
                    }
                }
            )

            val standardDefinition = TilesetDescriptorOptions.Builder()
                .styleURI(com.mapbox.maps.Style.STANDARD)
                .minZoom(0)
                .maxZoom(20)
                .build()

            val satelliteDefinition = TilesetDescriptorOptions.Builder()
                .styleURI(com.mapbox.maps.Style.SATELLITE)
                .minZoom(0)
                .maxZoom(20)
                .build()

            val satelliteStreetsDefinition = TilesetDescriptorOptions.Builder()
                .styleURI(com.mapbox.maps.Style.SATELLITE_STREETS)
                .minZoom(0)
                .maxZoom(20)
                .build()

            val standardDescriptor = offlineManager.createTilesetDescriptor(standardDefinition)
            val satelliteDescriptor = offlineManager.createTilesetDescriptor(satelliteDefinition)
            val satelliteStreetsDescriptor = offlineManager.createTilesetDescriptor(satelliteStreetsDefinition)

            val tileRegionLoadOptions = TileRegionLoadOptions.Builder()
                .geometry(polygon)
                .descriptors(listOf(standardDescriptor, satelliteDescriptor, satelliteStreetsDescriptor))
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

    //==============================================================================
    // Mapbox Functions
    //==============================================================================

    //--------------------------------------------------
    // Initializes Mapbox views, managers, and location clients.
    //--------------------------------------------------
    private fun initializeMap() {
        mapboxMap = binding.mapView.mapboxMap

        polylineAnnotationManager = binding.mapView.annotations.createPolylineAnnotationManager()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        pointAnnotationManager = binding.mapView.annotations.createPointAnnotationManager()
        gateAnnotationManager = binding.mapView.annotations.createPointAnnotationManager()
        textAnnotationManager = binding.mapView.annotations.createPointAnnotationManager()
        trackingDotManager = binding.mapView.annotations.createPointAnnotationManager()

        createLocationRequest()
        createLocationCallback()
        setupMap()
        checkLocationPermission()
    }

    //--------------------------------------------------
    // Configures the initial map camera viewport zoom and center.
    //--------------------------------------------------
    private fun setupMap() {
        val mapData = getMapDataFromIntentOrPrefs()
        val bounds = mapData?.bounds
        val sw = bounds?.southWest
        val ne = bounds?.northEast

        if (sw?.latitude != null && sw.longitude != null && ne?.latitude != null && ne.longitude != null) {
            val centerLat = (sw.latitude + ne.latitude) / 2.0
            val centerLng = (sw.longitude + ne.longitude) / 2.0
            mapboxMap.setCamera(
                CameraOptions.Builder()
                    .center(Point.fromLngLat(centerLng, centerLat))
                    .zoom(14.0)
                    .build()
            )
        } else {
            mapboxMap.setCamera(
                CameraOptions.Builder()
                    .zoom(14.0)
                    .build()
            )
        }
    }

    //--------------------------------------------------
    // Triggers loading of the Mapbox style.
    //--------------------------------------------------
    private fun initMapStyleAndDraw() {
        loadMapStyle()
    }

    private var currentStyleUri: String = com.mapbox.maps.Style.STANDARD

    //--------------------------------------------------
    // Loads the selected style and draws the boundary layout on completion.
    //--------------------------------------------------
    private fun loadMapStyle() {
        styleLoadedCalled = false
        mapboxMap.loadStyle(currentStyleUri) {
            if (!styleLoadedCalled) {
                styleLoadedCalled = true
                Log.d("MapLiveTrack", "Style $currentStyleUri loaded successfully.")
                recreateAnnotationManagers()
                loadAndDrawMapProperty()
            }
        }

        Handler(Looper.getMainLooper()).postDelayed({
            if (!styleLoadedCalled) {
                styleLoadedCalled = true
                Log.w("MapLiveTrack", "Style load timeout. Drawing map property.")
                recreateAnnotationManagers()
                loadAndDrawMapProperty()
            }
        }, 1200)
    }

    //--------------------------------------------------
    // Renders the property boundaries, labels, and begins offline caching.
    //--------------------------------------------------
    private fun loadAndDrawMapProperty() {
        val mapData = getMapDataFromIntentOrPrefs()

        val geoJson = mapData?.geoJson ?: cachedGeoJson
        if (geoJson != null) {
            cachedGeoJson = geoJson
            drawPropertyArea(geoJson)
            boundaryLoaded = true

            val bounds = mapData?.bounds
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

        val bounds = mapData?.bounds
        val sw = bounds?.southWest
        val ne = bounds?.northEast
        if (sw?.latitude != null && sw.longitude != null && ne?.latitude != null && ne.longitude != null) {
            val centerLat = (sw.latitude + ne.latitude) / 2.0
            val centerLng = (sw.longitude + ne.longitude) / 2.0

            val latBuffer = 100.0 / 111320.0
            val cosLat = Math.cos(Math.toRadians(centerLat))
            val lngBuffer = if (cosLat != 0.0) 100.0 / (111320.0 * Math.abs(cosLat)) else latBuffer

            val bufferedMinLat = sw.latitude - latBuffer
            val bufferedMaxLat = ne.latitude + latBuffer
            val bufferedMinLng = sw.longitude - lngBuffer
            val bufferedMaxLng = ne.longitude + lngBuffer

            val bufferedBounds = CoordinateBounds(
                Point.fromLngLat(bufferedMinLng, bufferedMinLat),
                Point.fromLngLat(bufferedMaxLng, bufferedMaxLat)
            )

            binding.mapView.post {
                try {
                    val cameraOptions = mapboxMap.cameraForCoordinateBounds(
                        bufferedBounds,
                        EdgeInsets(150.0, 100.0, 350.0, 100.0),
                        null,
                        null
                    )
                    if (cameraOptions.zoom != null && cameraOptions.zoom!! >= 10.0) {
                        mapboxMap.setCamera(cameraOptions)
                    } else {
                        mapboxMap.setCamera(
                            CameraOptions.Builder()
                                .center(Point.fromLngLat(centerLng, centerLat))
                                .zoom(15.0)
                                .build()
                        )
                    }
                } catch (e: Exception) {
                    mapboxMap.setCamera(
                        CameraOptions.Builder()
                            .center(Point.fromLngLat(centerLng, centerLat))
                            .zoom(15.0)
                            .build()
                    )
                }
            }
        }
    }

    //--------------------------------------------------
    // Parses and draws property polygons, boundaries, and gate markers on the map.
    //--------------------------------------------------
    private fun drawPropertyArea(response: GeoJson) {
        gateAnnotationManager.deleteAll()
        textAnnotationManager.deleteAll()
        polylineAnnotationManager.deleteAll()

        labelIndex = 0
        val allPoints = mutableListOf<Point>()

        response.features?.forEach { feature ->
            when (feature.geometry?.type) {
                "Point" -> {
                    val coordinates = feature.geometry.coordinates?.asJsonArray
                    if (coordinates != null && coordinates.size() >= 2) {
                        val lng = coordinates[0].asDouble
                        val lat = coordinates[1].asDouble
                        val markerColor = feature.properties?.markerColor
                        addGateMarker(lat, lng, markerColor)
                    }
                }
                else -> {
                    val rings = mutableListOf<List<Point>>()
                    extractRings(feature.geometry?.coordinates, rings)

                    if (rings.isNotEmpty()) {
                        val outerRing = rings.first()
                        drawPolygonFill(
                            outerRing = outerRing,
                            holes = rings.drop(1),
                            properties = feature.properties
                        )

                        val rluLabelText = feature.properties?.rluNo ?: product_number
                        if (outerRing.isNotEmpty() && !rluLabelText.isNullOrBlank()) {
                            var sumLat = 0.0
                            var sumLng = 0.0
                            outerRing.forEach { pt ->
                                sumLat += pt.latitude()
                                sumLng += pt.longitude()
                            }
                            val centroidLat = sumLat / outerRing.size
                            val centroidLng = sumLng / outerRing.size
                            addRluLabel(centroidLat, centroidLng, rluLabelText)
                        }

                        rings.forEach { ring ->
                            polylineAnnotationManager.create(
                                PolylineAnnotationOptions()
                                    .withPoints(ring)
                                    .withLineColor(feature.properties?.stroke ?: "#007F56")
                                    .withLineWidth(2.0)
                            )
                            allPoints.addAll(ring)
                        }
                    }
                }
            }
        }
        zoomToEntireProperty(allPoints)
    }

    //--------------------------------------------------
    // Adds a GeoJson source and fill layer to paint polygon colors.
    //--------------------------------------------------
    private fun drawPolygonFill(
        outerRing: List<Point>,
        holes: List<List<Point>>,
        properties: Properties?
    ) {
        val fillColor = properties?.fill ?: return

        val polygon = Polygon.fromLngLats(listOf(outerRing) + holes)
        val feature = Feature.fromGeometry(polygon)

        val sourceId = "fill-source-${System.currentTimeMillis()}"
        val layerId = "fill-layer-${System.currentTimeMillis()}"

        val style = mapboxMap.style
        if (style == null) {
            Log.w("MAP_DRAW", "Style is null, skipping polygon fill layer until style is loaded.")
            return
        }

        style.apply {
            addSource(
                geoJsonSource(sourceId) {
                    feature(feature)
                }
            )

            addLayer(
                fillLayer(layerId, sourceId) {
                    fillColor(fillColor)
                    fillOpacity((properties.fillOpacity ?: 0.3f).toDouble())
                }
            )
        }
    }

    //--------------------------------------------------
    // Adjusts camera view to show the entire property.
    //--------------------------------------------------
    private fun zoomToEntireProperty(points: List<Point>) {
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
            Point.fromLngLat(minLng, minLat),
            Point.fromLngLat(maxLng, maxLat)
        )

        val cameraOptions = mapboxMap.cameraForCoordinateBounds(
            bounds,
            EdgeInsets(150.0, 100.0, 350.0, 100.0),
            null,
            null
        )
        mapboxMap.setCamera(cameraOptions)
    }

    //--------------------------------------------------
    // Recreates annotation managers when the map style transitions.
    //--------------------------------------------------
    private fun recreateAnnotationManagers() {
        try {
            polylineAnnotationManager = binding.mapView.annotations.createPolylineAnnotationManager()
            pointAnnotationManager = binding.mapView.annotations.createPointAnnotationManager()
            gateAnnotationManager = binding.mapView.annotations.createPointAnnotationManager()
            textAnnotationManager = binding.mapView.annotations.createPointAnnotationManager()
            trackingDotManager = binding.mapView.annotations.createPointAnnotationManager()
        } catch (e: Exception) {
            Log.e("MapLiveTrack", "Error recreating annotation managers: ${e.message}")
        }
    }

    //--------------------------------------------------
    // Renders the start flag marker on the map.
    //--------------------------------------------------
    private fun addStartMarker(latitude: Double, longitude: Double) {
        if (startMarkerAdded) return

        val bitmap = bitmapFromDrawableRes(R.drawable.ic_start_marker) ?: return

        val pointAnnotationOptions = PointAnnotationOptions()
            .withPoint(Point.fromLngLat(longitude, latitude))
            .withIconImage(bitmap)
            .withIconSize(1.0)

        pointAnnotationManager.create(pointAnnotationOptions)
        startMarkerAdded = true
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

    //--------------------------------------------------
    // Places a footstep icon overlay at the active point.
    //--------------------------------------------------
    private fun addTrackingDot(point: Point) {
        footstepBitmap?.let { bitmap ->
            trackingDotManager.create(
                PointAnnotationOptions()
                    .withPoint(point)
                    .withIconImage(bitmap)
                    .withIconSize(1.0)
            )
        }
    }

    //--------------------------------------------------
    // Renders a text label indicating the property RLU code.
    //--------------------------------------------------
    private fun addRluLabel(lat: Double, lng: Double, text: String?) {
        if (text.isNullOrBlank()) return

        val offsetLat = lat + (0.00025 * (labelIndex % 4))
        val offsetLng = lng + (0.00015 * (labelIndex % 3))
        labelIndex++

        val bitmap = createTextBitmap(text)

        textAnnotationManager.create(
            PointAnnotationOptions()
                .withPoint(Point.fromLngLat(offsetLng, offsetLat))
                .withIconImage(bitmap)
        )
    }

    //--------------------------------------------------
    // Generates a custom text Bitmap with outlines.
    //--------------------------------------------------
    private fun createTextBitmap(text: String): Bitmap {
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

        val width = fillPaint.measureText(text).toInt() + 40
        val height = 50

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        canvas.drawColor(Color.TRANSPARENT)
        canvas.drawText(text, 10f, 35f, strokePaint)
        canvas.drawText(text, 10f, 35f, fillPaint)

        return bitmap
    }

    //--------------------------------------------------
    // Generates custom circular markers for gates.
    //--------------------------------------------------
    private fun createGateMarker(colorHex: String?): Bitmap {
        val markerColor = try {
            Color.parseColor(colorHex ?: "#EF4444")
        } catch (e: Exception) {
            Color.parseColor("#EF4444")
        }

        val size = 50
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
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

        canvas.drawCircle(size / 2f, size / 2f, 18f, fillPaint)
        canvas.drawCircle(size / 2f, size / 2f, 18f, strokePaint)

        return bitmap
    }

    //--------------------------------------------------
    // Adds a gate marker annotation to the map.
    //--------------------------------------------------
    private fun addGateMarker(lat: Double, lng: Double, markerColor: String?) {
        val bitmap = createGateMarker(markerColor)

        pointAnnotationManager.create(
            PointAnnotationOptions()
                .withPoint(Point.fromLngLat(lng, lat))
                .withIconImage(bitmap)
                .withIconSize(1.0)
        )
    }

    //--------------------------------------------------
    // Increases map zoom by one unit.
    //--------------------------------------------------
    private fun zoomIn() {
        val currentZoom = mapboxMap.cameraState.zoom
        mapboxMap.flyTo(
            CameraOptions.Builder()
                .zoom(currentZoom + 1)
                .build()
        )
    }

    //--------------------------------------------------
    // Decreases map zoom by one unit.
    //--------------------------------------------------
    private fun zoomOut() {
        val currentZoom = mapboxMap.cameraState.zoom
        mapboxMap.flyTo(
            CameraOptions.Builder()
                .zoom(currentZoom - 1)
                .build()
        )
    }

    //==============================================================================
    // Tracking Functions
    //==============================================================================

    //--------------------------------------------------
    // Starts tracking timer, services, and live route simulation.
    //--------------------------------------------------
    private fun startTracking() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        updateStopButton(true)

        trackingStartDate?.let {
            startedAtUtc = isoFormatter.format(it)
        }

        routePoints.clear()
        trackingDotManager.deleteAll()
        pointAnnotationManager.deleteAll()
        startMarkerAdded = false
        totalDistanceMeters = 0f
        lastTrackedLocation = null
        finalDistance = "0 m"
        binding.tvDistance.text = finalDistance

        isTracking = true
        TrackingForegroundService.start(this)

        val startPoint = Point.fromLngLat(currentLongitude, currentLatitude)
        routePoints.add(startPoint)
        addStartMarker(currentLatitude, currentLongitude)

        simulationIndex = 0
        simulationHandler.removeCallbacks(simulationRunnable)
        simulationHandler.post(simulationRunnable)

        startTimer()
    }

    //--------------------------------------------------
    // Stops the tracking timers, foreground services, and simulations.
    //--------------------------------------------------
    @RequiresApi(Build.VERSION_CODES.O)
    private fun stopTracking() {
        endedAtUtc = Instant.now().toString()
        isTracking = false
        TrackingForegroundService.stop(this)
        simulationHandler.removeCallbacks(simulationRunnable)
        stopTimer()
        hasTrackingSessionStarted = false
    }

    //--------------------------------------------------
    // Resumes current paused tracking session.
    //--------------------------------------------------
    private fun resumeTracking() {
        isTracking = true
        TrackingForegroundService.start(this)

        simulationHandler.post(simulationRunnable)
        resumeTimer()

        binding.btnPlayPause.setImageResource(R.drawable.ic_pause)
    }

    //--------------------------------------------------
    // Processes simulated locations, plots footsteps, and pans map.
    //--------------------------------------------------
    private fun processSimulatedLocation(location: Location) {
        if (!isTracking) return

        currentElevationMeters = location.altitude
        finalElevation = String.format(Locale.ENGLISH, "%.1f m", location.altitude)

        lastTrackedLocation?.let { previous ->
            totalDistanceMeters += previous.distanceTo(location)
        }

        lastTrackedLocation = location

        binding.tvDistance.text = formatDistance(totalDistanceMeters)
        finalDistance = formatDistance(totalDistanceMeters)

        val point = Point.fromLngLat(location.longitude, location.latitude)
        routePoints.add(point)
        updateRoute()

        if (!startMarkerAdded) {
            addStartMarker(location.latitude, location.longitude)
        }

        addTrackingDot(point)

        val cameraOptions = CameraOptions.Builder()
            .center(point)
            .zoom(18.0)
            .build()
        mapboxMap.flyTo(cameraOptions)
    }

    //--------------------------------------------------
    // Returns the active simulation route coordinates array.
    //--------------------------------------------------
    private fun getSimulationCoordinates(): List<Pair<Double, Double>> {
        return if (dynamicSimulationCoordinates.isNotEmpty()) dynamicSimulationCoordinates else defaultCoordinates
    }

    //--------------------------------------------------
    // Generates simulated tracking routes points.
    //--------------------------------------------------
    private fun generateSimulationRouteToAmity(startLat: Double, startLng: Double) {
        val destLat = 30.7023
        val destLng = 76.6925
        val points = mutableListOf<Pair<Double, Double>>()
        val steps = 50
        for (i in 0..steps) {
            val fraction = i.toDouble() / steps
            val lat = startLat + (destLat - startLat) * fraction + 0.00015 * Math.sin(fraction * Math.PI * 4)
            val lng = startLng + (destLng - startLng) * fraction + 0.00015 * Math.cos(fraction * Math.PI * 4)
            points.add(Pair(lat, lng))
        }
        dynamicSimulationCoordinates = points
        simulationIndex = 0
    }

    //--------------------------------------------------
    // Pans camera to user position before starting track records.
    //--------------------------------------------------
    private fun startSimulatedTrackingAnimation() {
        mapboxMap.flyTo(
            CameraOptions.Builder()
                .center(Point.fromLngLat(currentLongitude, currentLatitude))
                .zoom(18.0)
                .build()
        )

        binding.mapView.postDelayed({
            if (isTimerRunning) {
                resumeTimer()
            } else {
                trackingStartDate = Date()
                startTracking()
            }
            binding.btnPlayPause.setImageResource(R.drawable.ic_pause)
        }, 1200)
    }

    //--------------------------------------------------
    // Logs the current number of tracked route points.
    //--------------------------------------------------
    private fun updateRoute() {
        if (routePoints.size < 2) return
        Log.e("TRACK", "Points Recorded = ${routePoints.size}")
    }

    //--------------------------------------------------
    // Builds FusedLocationProvider request configs.
    //--------------------------------------------------
    private fun createLocationRequest() {
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
            .setMinUpdateDistanceMeters(1f)
            .build()
    }

    //--------------------------------------------------
    // Binds target GPS coordinates changes callbacks.
    //--------------------------------------------------
    private fun createLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                if (!isTracking) return
                locationResult.lastLocation?.let { location ->
                    currentElevationMeters = location.altitude
                    finalElevation = String.format(Locale.ENGLISH, "%.1f m", location.altitude)

                    lastTrackedLocation?.let { previous ->
                        totalDistanceMeters += previous.distanceTo(location)
                    }

                    lastTrackedLocation = location
                    binding.tvDistance.text = formatDistance(totalDistanceMeters)
                    finalDistance = formatDistance(totalDistanceMeters)

                    val point = Point.fromLngLat(location.longitude, location.latitude)
                    routePoints.add(point)
                    updateRoute()

                    if (!startMarkerAdded) {
                        addStartMarker(location.latitude, location.longitude)
                    }

                    addTrackingDot(point)

                    val cameraOptions = CameraOptions.Builder()
                        .center(point)
                        .zoom(18.0)
                        .build()
                    mapboxMap.flyTo(cameraOptions)
                }
            }
        }
    }

    //--------------------------------------------------
    // Displays bottom sheet requesting confirmation to start tracking.
    //--------------------------------------------------
    private fun showStartTrackingBottomSheet() {
        val dialog = BottomSheetDialog(this)
        val sheetBinding = BottomsheetStartTrackingBinding.inflate(layoutInflater)
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

    //--------------------------------------------------
    // Moves map viewport camera to current location and begins tracking.
    //--------------------------------------------------
    private fun moveCameraToCurrentLocationAndStartTracking() {
        showCurrentLocation()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                val sLat = loc?.latitude ?: currentLatitude.takeIf { it != 0.0 } ?: 30.6950
                val sLng = loc?.longitude ?: currentLongitude.takeIf { it != 0.0 } ?: 76.6850
                currentLatitude = sLat
                currentLongitude = sLng
                generateSimulationRouteToAmity(sLat, sLng)
                startSimulatedTrackingAnimation()
            }.addOnFailureListener {
                val sLat = currentLatitude.takeIf { it != 0.0 } ?: 30.6950
                val sLng = currentLongitude.takeIf { it != 0.0 } ?: 76.6850
                generateSimulationRouteToAmity(sLat, sLng)
                startSimulatedTrackingAnimation()
            }
        } else {
            val sLat = currentLatitude.takeIf { it != 0.0 } ?: 30.6950
            val sLng = currentLongitude.takeIf { it != 0.0 } ?: 76.6850
            generateSimulationRouteToAmity(sLat, sLng)
            startSimulatedTrackingAnimation()
        }
    }

    //--------------------------------------------------
    // Displays bottom sheet to stop or cancel tracking.
    //--------------------------------------------------
    private fun showStopTrackingBottomSheet(fromBackButton: Boolean = false) {
        val wasTrackingBeforeSheet = isTracking
        if (isTracking) {
            pauseTimer()
            isTracking = false
            TrackingForegroundService.stop(this)
            simulationHandler.removeCallbacks(simulationRunnable)
            binding.btnPlayPause.setImageResource(R.drawable.ic_play)
        }

        val dialog = BottomSheetDialog(this)
        val sheetBinding = BottomsheetStopTrackingBinding.inflate(layoutInflater)
        dialog.setContentView(sheetBinding.root)
        dialog.setCancelable(true)

        sheetBinding.tvCancel.setOnClickListener {
            dialog.dismiss()
            if (wasTrackingBeforeSheet && !fromBackButton) {
                resumeTracking()
            }
            if (fromBackButton) {
                finish()
            }
        }

        dialog.setOnCancelListener {
            if (wasTrackingBeforeSheet && !fromBackButton) {
                resumeTracking()
            }
            if (fromBackButton) {
                finish()
            }
        }

        sheetBinding.tvStop.setOnClickListener {
            dialog.dismiss()
            stopTracking()
            showSaveTrackBottomSheet()
        }

        dialog.show()
    }

    //--------------------------------------------------
    // Displays bottom sheet summarizing track stats to save.
    //--------------------------------------------------
    private fun showSaveTrackBottomSheet() {
        val dialog = BottomSheetDialog(this)
        val sheetBinding = BottomsheetSaveTrackBinding.inflate(layoutInflater)
        dialog.setContentView(sheetBinding.root)
        dialog.setCancelable(true)

        val currentDate = SimpleDateFormat("MMM dd", Locale.ENGLISH).format(Date())

        sheetBinding.tvDuration.text = finalDuration
        sheetBinding.tvDistance.text = finalDistance
        sheetBinding.tvTrackName.text = product_number + " — " + currentDate

        val finalPace = calculatePace(totalDistanceMeters, elapsedTime)
        sheetBinding.tvPace.text = finalPace
        sheetBinding.tvElevation.text = finalElevation

        sheetBinding.tvCancel.setOnClickListener {
            dialog.dismiss()
        }

        sheetBinding.tvSave.setOnClickListener {
            dialog.dismiss()
            endedAtUtc = isoFormatter.format(Date())

            val token = PrefManager.getString(AppConstant.AUTH_TOKEN)
            val numericPace = calculatePaceDouble(totalDistanceMeters, elapsedTime)

            val request = SaveTrackRequest(
                licenseContractId = licenseContractId,
                trackName = sheetBinding.tvTrackName.text.toString(),
                notes = sheetBinding.etNotes.text.toString().trim(),
                startedAtUtc = startedAtUtc,
                endedAtUtc = endedAtUtc,
                totalDistanceMeters = totalDistanceMeters.toDouble(),
                trackLine = buildTrackLine(),
                pace = numericPace,
                elevation = currentElevationMeters,
                durationSeconds = elapsedTime / 1000
            )

            resetTimer()
            hasTrackingSessionStarted = false

            if (isInternetAvailable()) {
                viewModel.saveTrack(token, request)
            } else {
                val offlineTrack = OfflineTrack(
                    request = request,
                    countyName = countyName,
                    stateAbbrev = stateName,
                    createdAt = startedAtUtc
                )
                OfflineTrackManager.saveOfflineTrack(this, offlineTrack)

                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("selectFragment", "track")
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        }

        dialog.show()
    }

    //--------------------------------------------------
    // Packages track points into LineString GeoJSON format.
    //--------------------------------------------------
    private fun buildTrackLine(): TrackLine {
        val coordinates = routePoints.map {
            listOf(it.longitude(), it.latitude())
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

    //--------------------------------------------------
    // Starts or resumes the tracking timer.
    //--------------------------------------------------
    private fun startTimer() {
        if (isTimerRunning) return

        startTime = SystemClock.elapsedRealtime() - elapsedTime
        timerHandler.post(timerRunnable)
        isTimerRunning = true
    }

    //--------------------------------------------------
    // Pauses the active tracking timer.
    //--------------------------------------------------
    private fun pauseTimer() {
        if (!isTimerRunning) return

        timerHandler.removeCallbacks(timerRunnable)
        isTimerRunning = false
    }

    //--------------------------------------------------
    // Resumes the tracking timer.
    //--------------------------------------------------
    private fun resumeTimer() {
        startTimer()
    }

    //--------------------------------------------------
    // Stops the tracking timer.
    //--------------------------------------------------
    private fun stopTimer() {
        timerHandler.removeCallbacks(timerRunnable)
        isTimerRunning = false
        binding.tvTrackingTime.text = finalDuration
    }

    //--------------------------------------------------
    // Resets timer parameters and tracked session states.
    //--------------------------------------------------
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

    //--------------------------------------------------
    // Updates UI visibility states of stop tracking buttons.
    //--------------------------------------------------
    private fun updateStopButton(isEnabled: Boolean) {
        binding.btnStop.isEnabled = isEnabled
        binding.btnStop.isClickable = isEnabled
        binding.btnStop.alpha = if (isEnabled) 1f else 0.4f
    }

    //==============================================================================
    // Permission Handling
    //==============================================================================

    //--------------------------------------------------
    // Callback launcher checking runtime locations permissions status.
    //--------------------------------------------------
    private val locationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

            if (granted) {
                showCurrentLocation()
            } else {
                val showRationale = androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
                if (!showRationale) {
                    showPermissionDeniedDialog()
                } else {
                    Toast.makeText(
                        this,
                        "Location permission is required to display your current location.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

    //--------------------------------------------------
    // Evaluates location permission and requests if missing.
    //--------------------------------------------------
    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        ) {
            showCurrentLocation()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    //--------------------------------------------------
    // Displays explanation dialog before opening system settings.
    //--------------------------------------------------
    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("Location Permission Required")
            .setMessage("Location access is required to display your current location on the map. Please enable Location in App Settings.")
            .setPositiveButton("Go to Settings") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.fromParts("package", packageName, null)
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    //--------------------------------------------------
    // Fetches the user's current location and enables location dots.
    //--------------------------------------------------
    private fun showCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) return

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                location?.let {
                    currentLatitude = it.latitude
                    currentLongitude = it.longitude
                    enableLocationComponent()
                }
            }
    }

    //--------------------------------------------------
    // Enables pulsing indicators component on map views.
    //--------------------------------------------------
    private fun enableLocationComponent() {
        binding.mapView.location.updateSettings {
            enabled = true
            pulsingEnabled = true
        }
    }

    //==============================================================================
    // Utility Functions
    //==============================================================================

    //--------------------------------------------------
    // Recursively extracts coordinates rings from GeoJSON arrays.
    //--------------------------------------------------
    private fun extractRings(element: JsonElement?, rings: MutableList<List<Point>>) {
        if (element == null || !element.isJsonArray) return
        val array = element.asJsonArray

        if (array.size() > 0 &&
            array[0].isJsonArray &&
            array[0].asJsonArray.size() >= 2 &&
            array[0].asJsonArray[0].isJsonPrimitive
        ) {
            val ringPoints = mutableListOf<Point>()
            array.forEach { pointElement ->
                val pointArray = pointElement.asJsonArray
                ringPoints.add(Point.fromLngLat(pointArray[0].asDouble, pointArray[1].asDouble))
            }
            rings.add(ringPoints)
            return
        }

        array.forEach {
            extractRings(it, rings)
        }
    }

    //--------------------------------------------------
    // Formats raw meters float into readable distance string.
    //--------------------------------------------------
    private fun formatDistance(meters: Float): String {
        return if (meters < 1000f) {
            "${meters.toInt()} m"
        } else {
            String.format(Locale.ENGLISH, "%.2f km", meters / 1000f)
        }
    }

    //--------------------------------------------------
    // Calculates and formats the user's average pace.
    //--------------------------------------------------
    private fun calculatePace(distanceMeters: Float, elapsedMillis: Long): String {
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

    //--------------------------------------------------
    // Calculates pace values as raw Double structures.
    //--------------------------------------------------
    private fun calculatePaceDouble(distanceMeters: Float, elapsedMillis: Long): Double {
        if (distanceMeters < 1f || elapsedMillis <= 0L) {
            return 0.0
        }
        val distanceKm = distanceMeters / 1000.0
        if (distanceKm <= 0.0) return 0.0
        val totalMinutes = (elapsedMillis / 1000.0) / 60.0
        return totalMinutes / distanceKm
    }

    //--------------------------------------------------
    // Registers connectivity listener checks.
    //--------------------------------------------------
    private var networkCallback: android.net.ConnectivityManager.NetworkCallback? = null

    private fun registerNetworkCallback() {
        val connectivityManager = getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        networkCallback = object : android.net.ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: android.net.Network) {
                super.onAvailable(network)
                runOnUiThread {
                    updateNetworkStatus(true)
                }
            }

            override fun onLost(network: android.net.Network) {
                super.onLost(network)
                runOnUiThread {
                    updateNetworkStatus(false)
                }
            }
        }
        val request = android.net.NetworkRequest.Builder()
            .addCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, networkCallback!!)
    }

    //--------------------------------------------------
    // Unregisters the network callback listener.
    //--------------------------------------------------
    private fun unregisterNetworkCallback() {
        networkCallback?.let {
            val connectivityManager = getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            connectivityManager.unregisterNetworkCallback(it)
        }
    }

    //--------------------------------------------------
    // Renders active offline/online status indicators.
    //--------------------------------------------------
    private fun updateNetworkStatus(isOnline: Boolean) {
        if (isOnline) {
            binding.ivMapStyleToggle.visibility = View.VISIBLE
            binding.tvNetworkStatus.text = "Online"
            binding.ivNetwork.setImageResource(R.drawable.ic_online)
            binding.ivMapStyleToggle.isEnabled = true
            binding.ivMapStyleToggle.isClickable = true
            binding.ivMapStyleToggle.alpha = 1.0f
        } else {
            binding.ivMapStyleToggle.visibility = View.INVISIBLE
            binding.tvNetworkStatus.text = "Offline"
            binding.ivNetwork.setImageResource(R.drawable.ic_offline)
            binding.ivMapStyleToggle.isEnabled = false
            binding.ivMapStyleToggle.isClickable = false
            binding.ivMapStyleToggle.alpha = 0.5f
        }
    }

    private val isoFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
}