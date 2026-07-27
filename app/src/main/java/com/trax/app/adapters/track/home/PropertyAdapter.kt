package com.trax.app.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.trax.app.databinding.ItemPropertyBinding
import com.trax.app.models.home.PropertyModel

class PropertyAdapter(
    private val list: ArrayList<PropertyModel>,
    private val listener: PropertyClickListener
) : RecyclerView.Adapter<PropertyAdapter.ViewHolder>() {

    //==============================================================================
    // Interfaces & Inner Classes
    //==============================================================================

    interface PropertyClickListener {
        fun onMoreClick(item: PropertyModel)
        fun onOpenMapClick(item: PropertyModel)
        fun onSaveClick(item: PropertyModel)
    }

    inner class ViewHolder(
        val binding: ItemPropertyBinding
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
        val binding = ItemPropertyBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    //--------------------------------------------------
    // Binds property names, locations, image resources, and click listener actions.
    //--------------------------------------------------
    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        val item = list[position]

        with(holder.binding) {
            tvPropertyName.text = item.propertyName
            tvLocation.text = item.location
            tvAcres.text = item.acres
            ivProperty.setImageResource(item.image)

            ivMore.setOnClickListener {
                listener.onMoreClick(item)
            }

            layoutOpenMap.setOnClickListener {
                listener.onOpenMapClick(item)
            }

            llSave.setOnClickListener {
                listener.onSaveClick(item)
            }
        }
    }

    //--------------------------------------------------
    // Returns the total number of items in the list.
    //--------------------------------------------------
    override fun getItemCount(): Int = list.size
}