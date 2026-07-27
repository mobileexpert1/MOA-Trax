package com.trax.app.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.trax.app.R
import com.trax.app.databinding.ItemHomeBinding
import com.trax.app.models.home.license.LicenseModel

class OfflineMapAdapter(
    private val list: MutableList<LicenseModel>,
    private val listener: OfflineMapClickListener
) : RecyclerView.Adapter<OfflineMapAdapter.ViewHolder>() {

    //==============================================================================
    // Interfaces & Inner Classes
    //==============================================================================

    interface OfflineMapClickListener {
        fun onDeleteClick(item: LicenseModel)
        fun onOpenMapClick(item: LicenseModel)
    }

    inner class ViewHolder(
        val binding: ItemHomeBinding
    ) : RecyclerView.ViewHolder(binding.root)

    //==============================================================================
    // Adapter Overrides
    //==============================================================================

    //--------------------------------------------------
    // Inflates the item layout and creates the ViewHolder.
    //--------------------------------------------------
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val binding = ItemHomeBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    //--------------------------------------------------
    // Binds the license data to the item view elements.
    //--------------------------------------------------
    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        val item = list[position]
        val property = item.propertyDetails

        with(holder.binding) {
            tvPropertyName.text = property?.displayName ?: ""

            tvLocation.text = buildString {
                property?.countyName?.takeIf { it.isNotBlank() }?.let {
                    append(it)
                }
                property?.stateName?.takeIf { it.isNotBlank() }?.let {
                    if (isNotEmpty()) {
                        append(" County, ")
                    }
                    append(it)
                }
            }

            tvAcres.text = "${property?.acres ?: 0} acres"

            layoutDownloadMap.visibility = android.view.View.VISIBLE
            layoutDownloadedStatus.visibility = android.view.View.GONE
            pbDownloadLoader.visibility = android.view.View.GONE
            ivDownloadIcon.visibility = android.view.View.VISIBLE
            
            layoutDownloadMap.setBackgroundResource(R.drawable.bg_red_button)
            ivDownloadIcon.setImageResource(R.drawable.ic_delete)
            ivDownloadIcon.imageTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE)
            tvDownloadText.text = "Delete"
            layoutDownloadMap.isEnabled = true
            layoutDownloadMap.isClickable = true

            layoutDownloadMap.setOnClickListener {
                listener.onDeleteClick(item)
            }

            clTop.setOnClickListener {
                listener.onOpenMapClick(item)
            }
        }
    }

    //--------------------------------------------------
    // Returns the total number of items in the list.
    //--------------------------------------------------
    override fun getItemCount() = list.size

    //==============================================================================
    // List Helper Functions
    //==============================================================================

    //--------------------------------------------------
    // Updates the list items data and refreshes UI.
    //--------------------------------------------------
    fun submitList(data: List<LicenseModel>) {
        list.clear()
        list.addAll(data)
        notifyDataSetChanged()
    }
}
