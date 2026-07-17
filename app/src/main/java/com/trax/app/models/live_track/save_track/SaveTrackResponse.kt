package com.trax.app.models.live_track.save_track

data class SaveTrackResponse(
    val success: Boolean,
    val message: String,
    val data: SaveTrackData?
)

data class SaveTrackData(
    val trackId: Int
)
