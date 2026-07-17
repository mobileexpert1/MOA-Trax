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

    inner class MonthViewHolder(
        val binding: ItemTrackMonthHeaderBinding
    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MonthViewHolder {

        val binding =
            ItemTrackMonthHeaderBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )

        return MonthViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: MonthViewHolder,
        position: Int
    ) {

        val item = list[position]

        holder.binding.tvMonth.text = item.month

        holder.binding.rvTracks.layoutManager =
            LinearLayoutManager(holder.itemView.context)

        holder.binding.rvTracks.adapter =
            TrackAdapter(item.tracks)
    }

    override fun getItemCount() = list.size

    fun updateList(newList: List<TrackMonthModel>) {

        list.clear()

        list.addAll(newList)

        notifyDataSetChanged()
    }
}