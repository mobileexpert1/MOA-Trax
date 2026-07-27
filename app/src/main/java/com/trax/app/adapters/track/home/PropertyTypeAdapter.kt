package com.trax.app.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.trax.app.databinding.ItemPropertyTypeBinding
import com.trax.app.models.home.PropertyTypeModel

class PropertyTypeAdapter(
    private val list: ArrayList<PropertyTypeModel>,
    private val listener: PropertyAdapter.PropertyClickListener
) : RecyclerView.Adapter<PropertyTypeAdapter.ViewHolder>() {

    //==============================================================================
    // Inner Classes
    //==============================================================================

    inner class ViewHolder(
        val binding: ItemPropertyTypeBinding
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
        return ViewHolder(
            ItemPropertyTypeBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    //--------------------------------------------------
    // Binds property type category labels and setups nested properties lists.
    //--------------------------------------------------
    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        val item = list[position]

        holder.binding.tvPropertyType.text = item.propertyType
        holder.binding.rvProperties.layoutManager = LinearLayoutManager(holder.itemView.context)
        holder.binding.rvProperties.adapter = PropertyAdapter(item.properties, listener)
    }

    //--------------------------------------------------
    // Returns the total number of items in the list.
    //--------------------------------------------------
    override fun getItemCount(): Int = list.size
}