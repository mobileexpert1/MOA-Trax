package com.trax.app.fragment

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.mapbox.common.TileRegionLoadOptions
import com.mapbox.common.TileStore
import com.mapbox.geojson.Point
import com.mapbox.geojson.Polygon
import com.mapbox.maps.GlyphsRasterizationMode
import com.mapbox.maps.OfflineManager
import com.mapbox.maps.StylePackLoadOptions
import com.mapbox.maps.TilesetDescriptorOptions
import com.trax.app.R
import com.trax.app.activities.MapLiveTrackActivity
import com.trax.app.activities.ProfileActivity
import com.trax.app.adapters.HomeAdapter
import com.trax.app.base.BaseFragment
import com.trax.app.databinding.BottomsheetRemoveOfflineMapBinding
import com.trax.app.databinding.FragmentHomeBinding
import com.trax.app.models.home.license.LicenseModel
import com.trax.app.progressBar.ProgressView
import com.trax.app.utils.AppConstant
import com.trax.app.utils.PrefManager
import com.trax.app.viewModels.HomeViewModel
import kotlin.text.isNotEmpty

class HomeFragment : BaseFragment(), HomeAdapter.PropertyClickListener {

    //==============================================================================
    // Variables
    //==============================================================================

    private val loader by lazy {
        ProgressView.getLoader(requireActivity())
    }

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()

    private lateinit var adapter: HomeAdapter

    //==============================================================================
    // Lifecycle Methods
    //==============================================================================

    //--------------------------------------------------
    // Inflates the fragment layout.
    //--------------------------------------------------
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(
            inflater,
            container,
            false
        )
        return binding.root
    }

    //--------------------------------------------------
    // Initializes the fragment views and data loading.
    //--------------------------------------------------
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        initObservers()
        initClicks()
        loadCachedLicenses()
        callLicenseApi()
    }

    //--------------------------------------------------
    // Refreshes the adapter when the fragment resumes.
    //--------------------------------------------------
    override fun onResume() {
        super.onResume()
        if (::adapter.isInitialized) {
            adapter.notifyDataSetChanged()
        }
    }

    //==============================================================================
    // Initializations
    //==============================================================================

    //--------------------------------------------------
    // Sets up the properties RecyclerView.
    //--------------------------------------------------
    private fun setupRecyclerView() {
        adapter = HomeAdapter(
            mutableListOf(),
            this
        )
        binding.rvProperties.layoutManager =
            LinearLayoutManager(requireContext())
        binding.rvProperties.adapter = adapter
    }

    //--------------------------------------------------
    // Initializes click listeners.
    //--------------------------------------------------
    private fun initClicks() {
        binding.btnSettings.setOnClickListener {
            startActivity(
                Intent(
                    requireContext(),
                    ProfileActivity::class.java
                )
            )
        }
    }

    //--------------------------------------------------
    // Initializes LiveData observers.
    //--------------------------------------------------
    private fun initObservers() {
        viewModel.licenseSuccess.observe(viewLifecycleOwner) { response ->
            response?.model?.let {
                PrefManager.saveLicensesList(it)
                binding.tvActiveLeases.text =
                    "${it.size} active lease${if (it.size > 1) "s" else ""}"
                adapter.submitList(it)
            }
        }

        viewModel.apiError.observe(viewLifecycleOwner) {
            val cached = PrefManager.getLicensesList()
            if (cached.isNullOrEmpty()) {
                binding.tvNoDataFound.visibility = View.VISIBLE
                binding.rvProperties.visibility = View.GONE
            } else {
                binding.tvNoDataFound.visibility = View.GONE
                binding.rvProperties.visibility = View.VISIBLE
            }
        }

        viewModel.unauthorizedError.observe(viewLifecycleOwner) { isUnauthorized ->
            if (isUnauthorized == true) {
                activity?.let { com.trax.app.utils.SessionManager.showSessionExpiredDialog(it) }
            }
        }

        viewModel.isLoading.observe(requireActivity()) {
            if (it == true) {
                loader.show()
            } else {
                loader.dismiss()
            }
        }
    }

    //==============================================================================
    // API Calls
    //==============================================================================

    //--------------------------------------------------
    // Calls the active licenses fetch API.
    //--------------------------------------------------
    private fun callLicenseApi() {
        val token = PrefManager.getString(AppConstant.AUTH_TOKEN)
        viewModel.getLicenses(token)
    }

    //==============================================================================
    // Offline Map Functions
    //==============================================================================

    //--------------------------------------------------
    // Downloads Mapbox style packs and tile regions for offline usage.
    //--------------------------------------------------
    private fun downloadMapOfflineForProduct(
        productNo: String,
        minLat: Double, minLng: Double,
        maxLat: Double, maxLng: Double
    ) {
        try {
            val latBuffer = 100.0 / 111320.0
            val centerLat = (minLat + maxLat) / 2.0
            val cosLat = Math.cos(Math.toRadians(centerLat))
            val lngBuffer = if (cosLat != 0.0) 100.0 / (111320.0 * Math.abs(cosLat)) else latBuffer

            val bufferedMinLat = minLat - latBuffer
            val bufferedMaxLat = maxLat + latBuffer
            val bufferedMinLng = minLng - lngBuffer
            val bufferedMaxLng = maxLng + lngBuffer

            val offlineManager = OfflineManager()
            val tileStore = TileStore.create()

            val polygon = Polygon.fromLngLats(
                listOf(
                    listOf(
                        Point.fromLngLat(bufferedMinLng, bufferedMinLat),
                        Point.fromLngLat(bufferedMaxLng, bufferedMinLat),
                        Point.fromLngLat(bufferedMaxLng, bufferedMaxLat),
                        Point.fromLngLat(bufferedMinLng, bufferedMaxLat),
                        Point.fromLngLat(bufferedMinLng, bufferedMinLat)
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
                    Log.d("MapboxOffline", "Product $productNo Standard StylePack progress: ${progress.completedResourceCount}/${progress.requiredResourceCount}")
                },
                { expected ->
                    if (expected.isError) {
                        Log.e("MapboxOffline", "Product $productNo Standard StylePack load failed: ${expected.error}")
                    } else {
                        Log.d("MapboxOffline", "Product $productNo Standard StylePack loaded successfully")
                    }
                }
            )

            offlineManager.loadStylePack(
                com.mapbox.maps.Style.SATELLITE,
                stylePackLoadOptions,
                { progress ->
                    Log.d("MapboxOffline", "Product $productNo Satellite StylePack progress: ${progress.completedResourceCount}/${progress.requiredResourceCount}")
                },
                { expected ->
                    if (expected.isError) {
                        Log.e("MapboxOffline", "Product $productNo Satellite StylePack load failed: ${expected.error}")
                    } else {
                        Log.d("MapboxOffline", "Product $productNo Satellite StylePack loaded successfully")
                    }
                }
            )

            offlineManager.loadStylePack(
                com.mapbox.maps.Style.SATELLITE_STREETS,
                stylePackLoadOptions,
                { progress ->
                    Log.d("MapboxOffline", "Product $productNo Satellite Streets StylePack progress: ${progress.completedResourceCount}/${progress.requiredResourceCount}")
                },
                { expected ->
                    if (expected.isError) {
                        Log.e("MapboxOffline", "Product $productNo Satellite Streets StylePack load failed: ${expected.error}")
                    } else {
                        Log.d("MapboxOffline", "Product $productNo Satellite Streets StylePack loaded successfully")
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

            val regionId = "offline-region-$productNo"

            tileStore.loadTileRegion(
                regionId,
                tileRegionLoadOptions,
                { progress ->
                    Log.d("MapboxOffline", "Product $productNo TileRegion progress: ${progress.completedResourceCount}/${progress.requiredResourceCount}")
                },
                { expected ->
                    activity?.runOnUiThread {
                        adapter.setDownloading(productNo, false)
                        if (expected.isError) {
                            Log.e("MapboxOffline", "Product $productNo TileRegion load failed: ${expected.error}")
                            Toast.makeText(context, "Download failed for $productNo", Toast.LENGTH_SHORT).show()
                        } else {
                            Log.d("MapboxOffline", "Product $productNo TileRegion loaded successfully")
                            PrefManager.saveDownloadedProduct(productNo)
                            Toast.makeText(context, "Downloaded offline map for $productNo", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        } catch (e: Exception) {
            Log.e("MapboxOffline", "Failed product $productNo download: ${e.message}")
            activity?.runOnUiThread {
                adapter.setDownloading(productNo, false)
            }
        }
    }

    //--------------------------------------------------
    // Displays the bottom sheet to delete downloaded map.
    //--------------------------------------------------
    private fun showRemoveMapBottomSheet(item: LicenseModel) {
        val dialog = BottomSheetDialog(requireContext())
        val sheetBinding = BottomsheetRemoveOfflineMapBinding.inflate(layoutInflater)
        dialog.setContentView(sheetBinding.root)

        sheetBinding.tvCancel.setOnClickListener {
            dialog.dismiss()
        }

        sheetBinding.tvRemove.setOnClickListener {
            val productNo = item.propertyDetails?.productNo ?: ""
            if (productNo.isNotEmpty()) {
                try {
                    val tileStore = TileStore.create()
                    val regionId = "offline-region-$productNo"
                    tileStore.removeTileRegion(regionId)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                PrefManager.removeDownloadedProduct(productNo)
                adapter.notifyDataSetChanged()
            }
            dialog.dismiss()
        }
        dialog.show()
    }

    //==============================================================================
    // Navigation / Callback Interface Overrides
    //==============================================================================

    //--------------------------------------------------
    // Handles click on the more actions button.
    //--------------------------------------------------
    override fun onMoreClick(item: LicenseModel) {
        showRemoveMapBottomSheet(item)
    }

    //--------------------------------------------------
    // Handles map download click actions.
    //--------------------------------------------------
    override fun onDownloadClick(item: LicenseModel) {
        if (!isNetworkAvailable()) {
            Toast.makeText(
                requireContext(),
                "Internet connection required to download map.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val productNo = item.propertyDetails?.productNo ?: ""
        if (productNo.isEmpty()) return

        val bounds = item.map?.bounds
        val sw = bounds?.southWest
        val ne = bounds?.northEast

        if (sw?.latitude != null && sw.longitude != null && ne?.latitude != null && ne.longitude != null) {
            adapter.setDownloading(productNo, true)
            downloadMapOfflineForProduct(productNo, sw.latitude, sw.longitude, ne.latitude, ne.longitude)
        } else {
            Toast.makeText(requireContext(), "Map bounds unavailable for download.", Toast.LENGTH_SHORT).show()
        }
    }

    //--------------------------------------------------
    // Handles clicking the property to open its map.
    //--------------------------------------------------
    override fun onOpenMapClick(item: LicenseModel) {
        val productNo = item.propertyDetails?.productNo ?: ""

        if (!isNetworkAvailable() && !PrefManager.isProductDownloaded(productNo)) {
            Toast.makeText(
                requireContext(),
                "Please download this map first to use it in offline mode.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val intent = Intent(
            requireContext(),
            MapLiveTrackActivity::class.java
        )

        intent.putExtra(
            "license_contract_id",
            item.licenseDetails?.licenseContractID ?: 0
        )

        intent.putExtra(
            "product_number",
            productNo
        )

        intent.putExtra(
            "county_name",
            item.propertyDetails?.countyName ?: ""
        )

        intent.putExtra(
            "state_name",
            item.propertyDetails?.stateName ?: ""
        )

        intent.putExtra(
            "acres",
            item.propertyDetails?.acres ?: 0.0
        )

        intent.putExtra(
            "license_end_date",
            item.licenseDetails?.licenseEndDate ?: ""
        )

        val mapJson = com.google.gson.Gson().toJson(item.map)
        intent.putExtra("map_data", mapJson)

        startActivity(intent)
    }

    //==============================================================================
    // Utility Functions
    //==============================================================================

    //--------------------------------------------------
    // Loads cached license lists from local storage.
    //--------------------------------------------------
    private fun loadCachedLicenses() {
        val cached = PrefManager.getLicensesList()
        if (!cached.isNullOrEmpty()) {
            binding.tvActiveLeases.text =
                "${cached.size} active lease${if (cached.size > 1) "s" else ""}"
            adapter.submitList(cached)
            binding.tvNoDataFound.visibility = View.GONE
            binding.rvProperties.visibility = View.VISIBLE
        }
    }

    //--------------------------------------------------
    // Checks if network connectivity is available.
    //--------------------------------------------------
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = requireContext().getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    //==============================================================================
    // Cleanup Methods
    //==============================================================================

    //--------------------------------------------------
    // Cleans up the view binding references.
    //--------------------------------------------------
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}