package com.trax.app.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.trax.app.R
import com.trax.app.activities.MapSavedTrackActivity
import com.trax.app.databinding.ItemTrackBinding
import com.trax.app.databinding.ItemTrackMonthHeaderBinding
import com.trax.app.models.track.TrackFeature
import java.util.Locale

class TrackAdapter(
    private val list: ArrayList<TrackFeature>
) : RecyclerView.Adapter<TrackAdapter.TrackViewHolder>() {

    inner class TrackViewHolder(
        val binding: ItemTrackBinding
    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): TrackViewHolder {

        val binding = ItemTrackBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return TrackViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: TrackViewHolder,
        position: Int
    ) {

        val item = list[position]
        val property = item.properties ?: return

        with(holder.binding) {

            tvTrackName.text = property.trackName ?: ""

            tvLocation.text =
                "${property.countyName ?: ""}, ${property.stateAbbrev ?: ""}"

       //     tvDistance.text = String.format("%.2f km", property.totalDistanceMeters / 1000)
            tvDistance.text = ""+property.totalDistanceMeters+ " km"

            tvDuration.text =
                formatDuration(property.durationSeconds)

            tvTime.text =
                formatTime(property.startedAtUtc)

            tvDayOfWeek.text =
                getDayName(property.startedAtUtc)

            tvDayNumber.text =
                getDayNumber(property.startedAtUtc)

            // Static map image for now
            ivMapThumbnail.setImageResource(R.drawable.map_dummy)

            root.setOnClickListener {

                val intent = Intent(
                    root.context,
                    MapSavedTrackActivity::class.java
                )

                intent.putExtra("trackId", property.trackId)

                root.context.startActivity(intent)
            }
        }
    }

    override fun getItemCount() = list.size

    fun updateList(newList: List<TrackFeature>) {
        list.clear()
        list.addAll(newList)
        notifyDataSetChanged()
    }

    private fun formatDuration(seconds: Int): String {

        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60

        return if (h > 0)
            String.format("%02d:%02d:%02d", h, m, s)
        else
            String.format("%02d:%02d", m, s)
    }

    private fun formatTime(date: String?): String {

        if (date.isNullOrEmpty()) return ""

        return try {

            val input = java.text.SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss",
                Locale.getDefault()
            )

            val output = java.text.SimpleDateFormat(
                "hh:mm a",
                Locale.getDefault()
            )

            output.format(input.parse(date)!!)

        } catch (e: Exception) {
            ""
        }
    }

    private fun getDayName(date: String?): String {

        if (date.isNullOrEmpty()) return ""

        return try {

            val input = java.text.SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss",
                Locale.getDefault()
            )

            val output = java.text.SimpleDateFormat(
                "EEE",
                Locale.getDefault()
            )

            output.format(input.parse(date)!!).uppercase()

        } catch (e: Exception) {
            ""
        }
    }

    private fun getDayNumber(date: String?): String {

        if (date.isNullOrEmpty()) return ""

        return try {

            val input = java.text.SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss",
                Locale.getDefault()
            )

            val output = java.text.SimpleDateFormat(
                "dd",
                Locale.getDefault()
            )

            output.format(input.parse(date)!!)

        } catch (e: Exception) {
            ""
        }
    }
}