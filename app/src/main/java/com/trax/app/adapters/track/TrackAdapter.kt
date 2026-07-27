package com.trax.app.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.trax.app.R
import com.trax.app.activities.MapSavedTrackActivity
import com.trax.app.databinding.ItemTrackBinding
import com.trax.app.models.track.TrackFeature
import java.util.Locale

class TrackAdapter(
    private val list: ArrayList<TrackFeature>
) : RecyclerView.Adapter<TrackAdapter.TrackViewHolder>() {

    //==============================================================================
    // Inner Classes
    //==============================================================================

    inner class TrackViewHolder(
        val binding: ItemTrackBinding
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
    ): TrackViewHolder {
        val binding = ItemTrackBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TrackViewHolder(binding)
    }

    //--------------------------------------------------
    // Binds the track details and click redirects to the view holder.
    //--------------------------------------------------
    override fun onBindViewHolder(
        holder: TrackViewHolder,
        position: Int
    ) {
        val item = list[position]
        val property = item.properties ?: return

        with(holder.binding) {
            tvTrackName.text = property.trackName ?: ""
            tvLocation.text = "${property.countyName ?: ""} County, ${property.stateAbbrev ?: ""}"
            tvDistance.text = formatDistance(property.totalDistanceMeters)
            tvDuration.text = formatDuration(property.durationSeconds)
            tvTime.text = formatTime(property.startedAtUtc)
            tvDayOfWeek.text = getDayName(property.startedAtUtc)
            tvDayNumber.text = getDayNumber(property.startedAtUtc)

            // Static map placeholder image
            ivMapThumbnail.setImageResource(R.drawable.map_dummy)

            root.setOnClickListener {
                val intent = Intent(root.context, MapSavedTrackActivity::class.java).apply {
                    putExtra("trackId", property.trackId)
                    putExtra("isOffline", property.isOfflineTrack)
                    putExtra("trackFeatureJson", com.google.gson.Gson().toJson(item))
                }
                root.context.startActivity(intent)
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
    // Updates the track dataset list and refreshes UI.
    //--------------------------------------------------
    fun updateList(newList: List<TrackFeature>) {
        list.clear()
        list.addAll(newList)
        notifyDataSetChanged()
    }

    //==============================================================================
    // Utility / Format Functions
    //==============================================================================

    //--------------------------------------------------
    // Converts double values into formatted kilometer strings.
    //--------------------------------------------------
    private fun formatDistance(dist: Double): String {
        if (dist <= 0.0) return "0.00 km"
        val km = if (dist >= 100.0) dist / 1000.0 else dist
        return String.format(Locale.US, "%.2f km", km)
    }

    //--------------------------------------------------
    // Formats seconds into HH:MM:SS / MM:SS structures.
    //--------------------------------------------------
    private fun formatDuration(seconds: Int): String {
        if (seconds <= 0) return "00:00"
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60

        return if (h > 0)
            String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
        else
            String.format(Locale.US, "%02d:%02d", m, s)
    }

    //--------------------------------------------------
    // Parses standard date strings into Java Date objects.
    //--------------------------------------------------
    private fun parseDate(dateStr: String?): java.util.Date? {
        if (dateStr.isNullOrEmpty()) return null
        val formats = arrayOf(
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss"
        )
        for (format in formats) {
            try {
                val sdf = java.text.SimpleDateFormat(format, Locale.US)
                sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                val parsed = sdf.parse(dateStr)
                if (parsed != null) return parsed
            } catch (e: Exception) {
                // try next
            }
        }
        return null
    }

    //--------------------------------------------------
    // Extracts the HH:MM AM/PM format from raw date parameters.
    //--------------------------------------------------
    private fun formatTime(date: String?): String {
        val parsed = parseDate(date) ?: return ""
        return try {
            val output = java.text.SimpleDateFormat("hh:mm a", Locale.US)
            output.timeZone = java.util.TimeZone.getDefault()
            output.format(parsed)
        } catch (e: Exception) {
            ""
        }
    }

    //--------------------------------------------------
    // Extracts the short day code from the date string.
    //--------------------------------------------------
    private fun getDayName(date: String?): String {
        val parsed = parseDate(date) ?: return ""
        return try {
            val output = java.text.SimpleDateFormat("EEE", Locale.US)
            output.timeZone = java.util.TimeZone.getDefault()
            output.format(parsed).uppercase()
        } catch (e: Exception) {
            ""
        }
    }

    //--------------------------------------------------
    // Extracts the numerical day from the date string.
    //--------------------------------------------------
    private fun getDayNumber(date: String?): String {
        val parsed = parseDate(date) ?: return ""
        return try {
            val output = java.text.SimpleDateFormat("dd", Locale.US)
            output.timeZone = java.util.TimeZone.getDefault()
            output.format(parsed)
        } catch (e: Exception) {
            ""
        }
    }
}