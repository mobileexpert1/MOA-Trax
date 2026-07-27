package com.trax.app.models.track.single_track

import com.google.gson.annotations.SerializedName

data class GetSingleTrackResponse(
    val statusCode: Int,
    val success: Boolean,
    val data: SingleTrackData?,
    val message: String?,
    val page: Int?,
    val pageSize: Int?,
    val totalCount: Int?,
    val totalPages: Int?
)

data class SingleTrackData(
    val type: String?,
    val features: List<TrackFeature>?
)

data class TrackFeature(
    val type: String?,
    val properties: SingleTrackProperties?,
    val geometry: SingleTrackGeometry?
)

data class SingleTrackProperties(

    val mapLayer: String?,

    val geometryType: String?,

    val trackId: Int?,

    val licenseContractId: Int?,

    val trackName: String?,

    val notes: String?,

    val countyName: String?,

    val stateAbbrev: String?,

    val startedAtUtc: String?,

    val endedAtUtc: String?,

    val durationSeconds: Double?,

    val totalDistanceMeters: Double?,

    val pace: Double?,

    val elevation: Double?,

    val totalPoints: Int?,

    val stroke: String?,

    @SerializedName("stroke-width")
    val strokeWidth: Int?,

    @SerializedName("stroke-opacity")
    val strokeOpacity: Double?
)

data class SingleTrackGeometry(

    val type: String?,

    val coordinates: List<List<Double>>?
)