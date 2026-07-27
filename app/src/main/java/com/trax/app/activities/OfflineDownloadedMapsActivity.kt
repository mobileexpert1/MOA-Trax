package com.trax.app.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.mapbox.common.TileStore
import com.trax.app.R
import com.trax.app.adapters.OfflineMapAdapter
import com.trax.app.databinding.ActivityOfflineDownloadedMapsBinding
import com.trax.app.databinding.BottomsheetRemoveTrackBinding
import com.trax.app.models.home.license.LicenseModel
import com.trax.app.utils.PrefManager

class OfflineDownloadedMapsActivity : AppCompatActivity(), OfflineMapAdapter.OfflineMapClickListener {

    //==============================================================================
    // Variables
    //==============================================================================

    private lateinit var binding: ActivityOfflineDownloadedMapsBinding
    private lateinit var adapter: OfflineMapAdapter

    //==============================================================================
    // Lifecycle Methods
    //==============================================================================

    //--------------------------------------------------
    // Initializes binding, maps list views, and back clicks.
    //--------------------------------------------------
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOfflineDownloadedMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        loadDownloadedMaps()

        binding.ivBack.setOnClickListener {
            finish()
        }
    }

    //==============================================================================
    // Initializations
    //==============================================================================

    //--------------------------------------------------
    // Configures properties list recycler view adapters.
    //--------------------------------------------------
    private fun setupRecyclerView() {
        adapter = OfflineMapAdapter(mutableListOf(), this)
        binding.rvOfflineMaps.layoutManager = LinearLayoutManager(this)
        binding.rvOfflineMaps.adapter = adapter
    }

    //==============================================================================
    // Offline Functions
    //==============================================================================

    //--------------------------------------------------
    // Removes the downloaded tile region from TileStore and registry cache.
    //--------------------------------------------------
    private fun performDelete(item: LicenseModel) {
        val productNo = item.propertyDetails?.productNo ?: ""
        if (productNo.isEmpty()) return

        try {
            val tileStore = TileStore.create()
            val regionId = "offline-region-$productNo"
            tileStore.removeTileRegion(regionId)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        PrefManager.removeDownloadedProduct(productNo)
        Toast.makeText(this, "Deleted offline map for $productNo", Toast.LENGTH_SHORT).show()
        loadDownloadedMaps()
    }

    //==============================================================================
    // Callbacks & Dialog Functions
    //==============================================================================

    //--------------------------------------------------
    // Displays bottom sheet confirmation to delete offline map.
    //--------------------------------------------------
    override fun onDeleteClick(item: LicenseModel) {
        val dialog = BottomSheetDialog(this)
        val sheetBinding = BottomsheetRemoveTrackBinding.inflate(layoutInflater)
        dialog.setContentView(sheetBinding.root)

        sheetBinding.tvTitle.text = resources.getString(R.string.delete_downloaded_map)
        sheetBinding.tvDescription.text = resources.getString(R.string.are_you_sure_you_want_to_delete_this_downloaded_map)
        sheetBinding.tvRemove.text = resources.getString(R.string.delete)

        sheetBinding.tvCancel.setOnClickListener {
            dialog.dismiss()
        }

        sheetBinding.tvRemove.setOnClickListener {
            dialog.dismiss()
            performDelete(item)
        }

        dialog.show()
    }

    //--------------------------------------------------
    // Handles opening the live tracking activity.
    //--------------------------------------------------
    override fun onOpenMapClick(item: LicenseModel) {
        val productNo = item.propertyDetails?.productNo ?: ""
        
        val intent = Intent(this, MapLiveTrackActivity::class.java).apply {
            putExtra("license_contract_id", item.licenseDetails?.licenseContractID ?: 0)
            putExtra("product_number", productNo)
            putExtra("county_name", item.propertyDetails?.countyName ?: "")
            putExtra("state_name", item.propertyDetails?.stateName ?: "")
            putExtra("acres", item.propertyDetails?.acres ?: 0.0)
            putExtra("license_end_date", item.licenseDetails?.licenseEndDate ?: "")
            val mapJson = com.google.gson.Gson().toJson(item.map)
            putExtra("map_data", mapJson)
        }
        startActivity(intent)
    }

    //==============================================================================
    // Utility Functions
    //==============================================================================

    //--------------------------------------------------
    // Loads offline downloaded licenses maps list from local preferences cache.
    //--------------------------------------------------
    private fun loadDownloadedMaps() {
        val allLicenses = PrefManager.getLicensesList() ?: emptyList()
        val downloadedProductNos = PrefManager.getDownloadedProducts() ?: emptySet()
        val downloadedLicenses = allLicenses.filter { license ->
            val productNo = license.propertyDetails?.productNo ?: ""
            downloadedProductNos.contains(productNo)
        }

        adapter.submitList(downloadedLicenses)

        if (downloadedLicenses.isEmpty()) {
            binding.tvNoDataFound.visibility = View.VISIBLE
            binding.rvOfflineMaps.visibility = View.GONE
        } else {
            binding.tvNoDataFound.visibility = View.GONE
            binding.rvOfflineMaps.visibility = View.VISIBLE
        }
    }
}
