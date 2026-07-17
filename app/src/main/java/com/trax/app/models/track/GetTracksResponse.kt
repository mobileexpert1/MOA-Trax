package com.trax.app.models.track

import com.google.gson.annotations.SerializedName

data class GetTracksResponse(
    val statusCode: Int,
    val success: Boolean,
    val data: TrackCollection?,
    val message: String?,
    val page: Int,
    val pageSize: Int,
    val totalCount: Int,
    val totalPages: Int
)

data class TrackCollection(
    val type: String?,
    val features: List<TrackFeature>?
)

data class TrackFeature(
    val type: String?,
    val properties: TrackProperties?,
    val geometry: TrackGeometry?
)


data class TrackProperties(

    val trackId: Int,

    val licenseContractId: Int,

    val trackName: String?,

    val notes: String?,

    val countyName: String?,

    val stateAbbrev: String?,

    val startedAtUtc: String?,

    val endedAtUtc: String?,

    val durationSeconds: Int,

    val totalDistanceMeters: Double,

    val totalPoints: Int,

    val stroke: String?,

    @SerializedName("stroke-width")
    val strokeWidth: Int,

    @SerializedName("stroke-opacity")
    val strokeOpacity: Double
)

data class TrackGeometry(

    val type: String?,

    val coordinates: List<List<Double>>?
)
