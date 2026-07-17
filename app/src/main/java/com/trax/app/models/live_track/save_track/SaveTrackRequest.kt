package com.trax.app.models.live_track.save_track

data class SaveTrackRequest(
    val licenseContractId: Int?,
    val trackName: String,
    val notes: String,
    val startedAtUtc: String,
    val endedAtUtc: String,
    val totalDistanceMeters: Double,
    val trackLine: TrackLine
)

data class TrackLine(
    val type: String = "Feature",
    val properties: Map<String, Any> = emptyMap(),
    val geometry: Geometry
)

data class Geometry(
    val type: String = "LineString",
    val coordinates: List<List<Double>>
)
