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

    inner class ViewHolder(
        val binding: ItemPropertyTypeBinding
    ) : RecyclerView.ViewHolder(binding.root)

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

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {

        val item = list[position]

        holder.binding.tvPropertyType.text =
            item.propertyType

        holder.binding.rvProperties.layoutManager =
            LinearLayoutManager(holder.itemView.context)

        holder.binding.rvProperties.adapter =
            PropertyAdapter(
                item.properties,
                listener
            )
    }

    override fun getItemCount(): Int = list.size
}