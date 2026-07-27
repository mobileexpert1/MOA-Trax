package com.trax.app.models.live_track.save_track

import java.util.UUID

data class OfflineTrack(
    val offlineId: String = UUID.randomUUID().toString(),
    val request: SaveTrackRequest,
    val countyName: String? = null,
    val stateAbbrev: String? = null,
    val createdAt: String = request.startedAtUtc,
    val isOfflineTrack: Boolean = true
)
