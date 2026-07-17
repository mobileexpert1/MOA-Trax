package com.trax.app.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.trax.app.R
import com.trax.app.activities.MapLiveTrackActivity
import com.trax.app.activities.ProfileActivity
import com.trax.app.adapters.PropertyAdapter
import com.trax.app.adapters.PropertyTypeAdapter
import com.trax.app.base.BaseFragment
import com.trax.app.databinding.BottomsheetRemoveOfflineMapBinding
import com.trax.app.databinding.BottomsheetSaveTrackBinding
import com.trax.app.databinding.FragmentHomeBinding
import com.trax.app.models.home.PropertyModel
import com.trax.app.models.home.PropertyTypeModel

import androidx.fragment.app.viewModels
import com.trax.app.adapters.HomeAdapter
import com.trax.app.models.home.license.LicenseModel
import com.trax.app.progressBar.ProgressView
import com.trax.app.utils.AppConstant
import com.trax.app.utils.PrefManager
import com.trax.app.viewModels.HomeViewModel
import kotlin.text.isNotEmpty
import android.util.Log
import com.mapbox.geojson.Point
import com.mapbox.geojson.Polygon
import com.mapbox.common.TileStore
import com.mapbox.common.TileRegionLoadOptions
import com.mapbox.maps.OfflineManager
import com.mapbox.maps.StylePackLoadOptions
import com.mapbox.maps.GlyphsRasterizationMode
import com.mapbox.maps.TilesetDescriptorOptions

class HomeFragment : BaseFragment(),
    HomeAdapter.PropertyClickListener {

    private val loader by lazy {
        ProgressView.getLoader(requireActivity())
    }

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()

    private lateinit var adapter: HomeAdapter

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

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()

        initObserver()

        initClicks()

        loadCachedLicenses()

        callLicenseApi()
    }

    private fun setupRecyclerView() {

        adapter = HomeAdapter(
            mutableListOf(),
            this
        )

        binding.rvProperties.layoutManager =
            LinearLayoutManager(requireContext())

        binding.rvProperties.adapter = adapter
    }

    private fun callLicenseApi() {

        val token =
            PrefManager.getString(AppConstant.AUTH_TOKEN)

        viewModel.getLicenses(token)
    }

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

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = requireContext().getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun predownloadOfflineMaps(licenses: List<LicenseModel>) {
        if (!isNetworkAvailable()) {
            Log.d("MapboxOffline", "Offline - skipping preemptive map region download.")
            return
        }
        licenses.forEach { item ->
            val bounds = item.map?.bounds
            val sw = bounds?.southWest
            val ne = bounds?.northEast
            val productNo = item.propertyDetails?.productNo ?: ""
            if (sw?.latitude != null && sw.longitude != null && ne?.latitude != null && ne.longitude != null && productNo.isNotEmpty()) {
                downloadMapOfflineForProduct(productNo, sw.latitude, sw.longitude, ne.latitude, ne.longitude)
            }
        }
    }

    private fun downloadMapOfflineForProduct(
        productNo: String,
        minLat: Double, minLng: Double,
        maxLat: Double, maxLng: Double
    ) {
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
                    Log.d("MapboxOffline", "Product $productNo StylePack progress: ${progress.completedResourceCount}/${progress.requiredResourceCount}")
                },
                { expected ->
                    if (expected.isError) {
                        Log.e("MapboxOffline", "Product $productNo StylePack load failed: ${expected.error}")
                    } else {
                        Log.d("MapboxOffline", "Product $productNo StylePack loaded successfully")
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

            val regionId = "offline-region-$productNo"

            tileStore.loadTileRegion(
                regionId,
                tileRegionLoadOptions,
                { progress ->
                    Log.d("MapboxOffline", "Product $productNo TileRegion progress: ${progress.completedResourceCount}/${progress.requiredResourceCount}")
                },
                { expected ->
                    if (expected.isError) {
                        Log.e("MapboxOffline", "Product $productNo TileRegion load failed: ${expected.error}")
                    } else {
                        Log.d("MapboxOffline", "Product $productNo TileRegion loaded successfully")
                    }
                }
            )
        } catch (e: Exception) {
            Log.e("MapboxOffline", "Failed product $productNo download: ${e.message}")
        }
    }

    private fun initObserver() {

        viewModel.licenseSuccess.observe(viewLifecycleOwner) { response ->

            response?.model?.let {
                PrefManager.saveLicensesList(it)
                binding.tvActiveLeases.text =
                    "${it.size} active lease${if (it.size > 1) "s" else ""}"

                adapter.submitList(it)
                predownloadOfflineMaps(it)
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

        viewModel.isLoading.observe(requireActivity()) {

            if (it == true) {
                loader.show()
            } else {
                loader.dismiss()
            }
        }
    }

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

    override fun onMoreClick(item: LicenseModel) {

        showRemoveMapBottomSheet()
    }

    override fun onOpenMapClick(item: LicenseModel) {

        val intent = Intent(
            requireContext(),
            MapLiveTrackActivity::class.java
        )

        intent.putExtra(
            "license_contract_id",
            item.licenseDetails?.licenseContractID ?: ""
        )

        intent.putExtra(
            "product_number",
            item.propertyDetails?.productNo ?: ""
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
            item.propertyDetails?.acres ?: ""
        )

        intent.putExtra(
            "license_end_date",
            item.licenseDetails?.licenseEndDate ?: ""
        )

        val mapJson = com.google.gson.Gson().toJson(item.map)
        intent.putExtra("map_data", mapJson)

        startActivity(intent)
    }

    private fun showRemoveMapBottomSheet() {

        val dialog =
            BottomSheetDialog(requireContext())

        val sheetBinding =
            BottomsheetRemoveOfflineMapBinding.inflate(layoutInflater)

        dialog.setContentView(sheetBinding.root)

        sheetBinding.tvCancel.setOnClickListener {
            dialog.dismiss()
        }

        sheetBinding.tvRemove.setOnClickListener {

            dialog.dismiss()
        }

        dialog.show()
    }

    override fun onDestroyView() {

        super.onDestroyView()

        _binding = null
    }
}