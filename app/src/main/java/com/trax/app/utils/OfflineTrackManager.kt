package com.trax.app.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.myoutdoor.agent.retrofit.ApiClient
import com.trax.app.models.live_track.save_track.OfflineTrack
import com.trax.app.models.track.TrackFeature
import com.trax.app.models.track.TrackGeometry
import com.trax.app.models.track.TrackProperties
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicBoolean

object OfflineTrackManager {

    //==============================================================================
    // Variables
    //==============================================================================

    private const val KEY_OFFLINE_TRACKS = "offline_tracks_list"
    private const val TAG = "OfflineTrackManager"

    private val mutex = Mutex()
    private val isSyncing = AtomicBoolean(false)

    //==============================================================================
    // Offline Storage / Database Functions
    //==============================================================================

    //--------------------------------------------------
    // Extracts the array list of cached offline tracks recorded on this device.
    //--------------------------------------------------
    @Synchronized
    fun getOfflineTracks(context: Context): List<OfflineTrack> {
        val json = PrefManager.getString(KEY_OFFLINE_TRACKS)
        if (json.isNullOrEmpty()) {
            return emptyList()
        }
        return try {
            val type = object : TypeToken<List<OfflineTrack>>() {}.type
            Gson().fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing offline tracks: ${e.message}")
            emptyList()
        }
    }

    //--------------------------------------------------
    // Adds a newly recorded tracking session payload locally into caching databases.
    //--------------------------------------------------
    fun saveOfflineTrack(context: Context, track: OfflineTrack) {
        CoroutineScope(Dispatchers.IO).launch {
            mutex.withLock {
                val currentList = getOfflineTracks(context).toMutableList()
                currentList.add(0, track)
                val json = Gson().toJson(currentList)
                PrefManager.putString(KEY_OFFLINE_TRACKS, json)
                Log.d(TAG, "Offline track saved locally. Total offline tracks: ${currentList.size}")
            }
        }
    }

    //--------------------------------------------------
    // Removes specified local cached tracks entries matching the offline UUID tag.
    //--------------------------------------------------
    suspend fun removeOfflineTrack(context: Context, offlineId: String) {
        mutex.withLock {
            val currentList = getOfflineTracks(context).toMutableList()
            val removed = currentList.removeAll { it.offlineId == offlineId }
            if (removed) {
                val json = Gson().toJson(currentList)
                PrefManager.putString(KEY_OFFLINE_TRACKS, json)
                Log.d(TAG, "Removed offline track $offlineId. Remaining: ${currentList.size}")
            }
        }
    }

    //==============================================================================
    // Sync Functions
    //==============================================================================

    //--------------------------------------------------
    // Dispatches a worker coroutine thread to push all saved offline tracks to the API.
    //--------------------------------------------------
    fun syncOfflineTracks(context: Context) {
        if (!isInternetAvailable(context)) {
            Log.d(TAG, "No internet connection available for auto sync.")
            return
        }

        if (!isSyncing.compareAndSet(false, true)) {
            Log.d(TAG, "Sync process is already running.")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                var pendingTracks = getOfflineTracks(context)
                Log.d(TAG, "Starting auto sync for ${pendingTracks.size} offline tracks.")

                while (pendingTracks.isNotEmpty() && isInternetAvailable(context)) {
                    val trackToSync = pendingTracks[0]
                    val token = PrefManager.getString(AppConstant.AUTH_TOKEN)

                    Log.d(TAG, "Attempting to sync offline track: ${trackToSync.offlineId}")
                    val apiInterface = ApiClient.getApiClientWithHeader(token)

                    if (apiInterface != null) {
                        try {
                            val response = apiInterface.saveTrack(trackToSync.request)
                            if (response.isSuccessful && response.body() != null) {
                                Log.d(TAG, "Track ${trackToSync.offlineId} synced successfully.")
                                removeOfflineTrack(context, trackToSync.offlineId)
                            } else {
                                Log.e(TAG, "Failed to sync track ${trackToSync.offlineId}: Code ${response.code()}")
                                break
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Network error while syncing track ${trackToSync.offlineId}: ${e.message}")
                            break
                        }
                    } else {
                        Log.e(TAG, "ApiClient is null. Aborting sync.")
                        break
                    }

                    pendingTracks = getOfflineTracks(context)
                }
            } finally {
                isSyncing.set(false)
                Log.d(TAG, "Offline track sync finished.")
            }
        }
    }

    //==============================================================================
    // Conversions & Formatting Functions
    //==============================================================================

    //--------------------------------------------------
    // Converts a local OfflineTrack data object into a standard TrackFeature JSON model.
    //--------------------------------------------------
    fun convertToTrackFeature(offlineTrack: OfflineTrack): TrackFeature {
        val req = offlineTrack.request
        val properties = TrackProperties(
            trackId = offlineTrack.offlineId.hashCode(),
            licenseContractId = req.licenseContractId ?: 0,
            trackName = req.trackName,
            notes = req.notes,
            countyName = offlineTrack.countyName ?: "",
            stateAbbrev = offlineTrack.stateAbbrev ?: "",
            startedAtUtc = req.startedAtUtc,
            endedAtUtc = req.endedAtUtc,
            durationSeconds = (req.durationSeconds ?: 0L).toInt(),
            totalDistanceMeters = req.totalDistanceMeters,
            totalPoints = req.trackLine.geometry.coordinates.size,
            stroke = null,
            strokeWidth = 2,
            strokeOpacity = 1.0,
            pace = req.pace?.toString(),
            elevation = req.elevation?.toString(),
            isOfflineTrack = true,
            offlineId = offlineTrack.offlineId
        )

        val geometry = TrackGeometry(
            type = req.trackLine.geometry.type,
            coordinates = req.trackLine.geometry.coordinates
        )

        return TrackFeature(
            type = "Feature",
            properties = properties,
            geometry = geometry
        )
    }

    //--------------------------------------------------
    // Returns boolean status of active internet network connectivity.
    //--------------------------------------------------
    private fun isInternetAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
