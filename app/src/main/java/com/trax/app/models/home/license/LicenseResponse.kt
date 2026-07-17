package com.trax.app.models.home.license

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class LicenseResponse(
    val statusCode: Int?,
    val message: String?,
    val model: List<LicenseModel>?
) : Serializable

data class LicenseModel(
    val licenseDetails: LicenseDetails?,
    val propertyDetails: PropertyDetails?,
    val map: Map?
) : Serializable

data class LicenseDetails(
    val licenseContractID: Int?,
    val userAccountID: Int?,
    val licenseStartDate: String?,
    val licenseEndDate: String?,
    val licenseActivityID: Int?,
    val licenseFee: Double?,
    val licenseStatus: String?,
    val contractStatus: String?,
    val licenseState: String?,
    val allowMemberActions: Boolean?,
    val firstName: String?,
    val lastName: String?,
    val phone: String?,
    val email: String?
) : Serializable

data class PropertyDetails(
    val activityNumber: String?,
    val activityType: String?,
    val productType: String?,
    val productTypeID: Int?,
    val productID: Int?,
    val productNo: String?,
    val productName: String?,
    val displayName: String?,
    val acres: Double?,
    val displayDescription: String?,
    val countyName: String?,
    val stateName: String?,
    val propertyName: String?
) : Serializable

data class Map(
    val rluNo: String?,
    val productTypeID: Int?,
    val bounds: Bounds?,
    val geoJson: GeoJson?
) : Serializable

data class Bounds(
    val southWest: SouthWest?,
    val northEast: NorthEast?
) : Serializable

data class SouthWest(
    val longitude: Double?,
    val latitude: Double?
) : Serializable

data class NorthEast(
    val longitude: Double?,
    val latitude: Double?
) : Serializable

data class GeoJson(
    val type: String?,
    val features: List<Feature>?
) : Serializable

data class Feature(
    val type: String?,
    val properties: Properties?,
    val geometry: Geometry?
) : Serializable

data class Properties(
    val name: String?,
    @SerializedName("rluNo")
    val rluNo: String?,
    @SerializedName("gateNo")
    val gateNo: String?,
    @SerializedName("marker-color")
    val markerColor: String?,
    @SerializedName("productType")
    val productType: String?,
    @SerializedName("mapLayer")
    val mapLayer: String?,
    @SerializedName("labelText")
    val labelText: String?,
    @SerializedName("labelColor")
    val labelColor: String?,
    @SerializedName("stroke")
    val stroke: String?,
    @SerializedName("stroke-width")
    val strokeWidth: Float?,
    @SerializedName("stroke-opacity")
    val strokeOpacity: Float?,
    @SerializedName("fill")
    val fill: String?,
    @SerializedName("fill-opacity")
    val fillOpacity: Float?,
    val status: String?,
    val isLicensed: Int?
) : Serializable

data class Geometry(
    val type: String?,
    val coordinates: com.google.gson.JsonElement?
) : Serializable