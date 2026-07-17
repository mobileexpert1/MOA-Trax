package com.trax.app.models.live_track.coordinates

// OLD IMPLEMENTATION 
// Previously used to model map coordinates fetched separately.
// No longer required because the Home API response now provides the map data.
//
// import com.google.gson.JsonElement
// import com.google.gson.annotations.SerializedName
//
// data class CoordinateResponse(
//     val type: String?,
//     val features: List<Feature>?
// )
//
// data class Feature(
//     val type: String?,
//     val geometry: Geometry?,
//     val properties: Properties?
// )
//
// data class Geometry(
//     val type: String?,
//     val coordinates: JsonElement?
// )
//
// data class Properties(
//     val name: String?,
//     @SerializedName("rluNo")
//     val rluNo: String?,
//     @SerializedName("gateNo")
//     val gateNo: String?,
//     @SerializedName("marker-color")
//     val markerColor: String?,
//     @SerializedName("productType")
//     val productType: String?,
//     @SerializedName("mapLayer")
//     val mapLayer: String?,
//     @SerializedName("labelText")
//     val labelText: String?,
//     @SerializedName("labelColor")
//     val labelColor: String?,
//     @SerializedName("stroke")
//     val stroke: String?,
//     @SerializedName("stroke-width")
//     val strokeWidth: Float?,
//     @SerializedName("stroke-opacity")
//     val strokeOpacity: Float?,
//     @SerializedName("fill")
//     val fill: String?,
//     @SerializedName("fill-opacity")
//     val fillOpacity: Float?
// )
