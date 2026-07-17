package com.trax.app.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.trax.app.databinding.ItemHomeBinding
import com.trax.app.models.home.license.LicenseModel
import com.trax.app.models.home.license.LicenseResponse

class HomeAdapter(
    private val list: MutableList<LicenseModel>,
    private val listener: PropertyClickListener
) : RecyclerView.Adapter<HomeAdapter.ViewHolder>() {

    interface PropertyClickListener {
        fun onMoreClick(item: LicenseModel)
        fun onOpenMapClick(item: LicenseModel)
    }

    inner class ViewHolder(
        val binding: ItemHomeBinding
    ) : RecyclerView.ViewHolder(binding.root)

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

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {

        val item = list[position]

        val property = item.propertyDetails
        val license = item.licenseDetails

        with(holder.binding) {

            tvPropertyName.text =
                property?.displayName ?: ""

            tvLocation.text = buildString {

                property?.countyName?.takeIf { it.isNotBlank() }?.let {
                    append(it)
                }

                property?.stateName?.takeIf { it.isNotBlank() }?.let {

                    if (isNotEmpty()) {
                        append(", ")
                    }

                    append(it)
                }
            }

            tvAcres.text =
                "${property?.acres ?: 0} acres"

            ivMore.setOnClickListener {
                listener.onMoreClick(item)
            }

            clTop.setOnClickListener {
                listener.onOpenMapClick(item)
            }
        }
    }

    override fun getItemCount() = list.size

    fun submitList(data: List<LicenseModel>) {

        list.clear()
        list.addAll(data)
        notifyDataSetChanged()
    }
}