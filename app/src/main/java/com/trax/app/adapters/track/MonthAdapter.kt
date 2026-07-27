package com.trax.app.adapters.track

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.trax.app.adapters.TrackAdapter
import com.trax.app.databinding.ItemTrackMonthHeaderBinding
import com.trax.app.models.track.TrackMonthModel

class MonthAdapter(
    private val list: ArrayList<TrackMonthModel>
) : RecyclerView.Adapter<MonthAdapter.MonthViewHolder>() {

    //==============================================================================
    // Inner Classes
    //==============================================================================

    inner class MonthViewHolder(
        val binding: ItemTrackMonthHeaderBinding
    ) : RecyclerView.ViewHolder(binding.root)

    //==============================================================================
    // Adapter Overrides
    //==============================================================================

    //--------------------------------------------------
    // Inflates the month category header layout and creates the ViewHolder.
    //--------------------------------------------------
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MonthViewHolder {
        val binding = ItemTrackMonthHeaderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MonthViewHolder(binding)
    }

    //--------------------------------------------------
    // Binds the month label and populates the nested tracks list.
    //--------------------------------------------------
    override fun onBindViewHolder(
        holder: MonthViewHolder,
        position: Int
    ) {
        val item = list[position]

        holder.binding.tvMonth.text = item.month
        holder.binding.rvTracks.layoutManager = LinearLayoutManager(holder.itemView.context)
        holder.binding.rvTracks.adapter = TrackAdapter(item.tracks)
    }

    //--------------------------------------------------
    // Returns the total number of items in the list.
    //--------------------------------------------------
    override fun getItemCount() = list.size

    //==============================================================================
    // List Helper Functions
    //==============================================================================

    //--------------------------------------------------
    // Updates the adapter datasets list and refreshes UI.
    //--------------------------------------------------
    fun updateList(newList: List<TrackMonthModel>) {
        list.clear()
        list.addAll(newList)
        notifyDataSetChanged()
    }
}