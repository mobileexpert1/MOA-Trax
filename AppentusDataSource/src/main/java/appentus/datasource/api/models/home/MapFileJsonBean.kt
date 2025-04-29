package appentus.datasource.api.models.home

import com.google.gson.annotations.SerializedName

data class MapFileJsonBean(

    @field:SerializedName("rasterXYsize")
    var rasterXYsize: ArrayList<Int>? = null,

    @field:SerializedName("projection")
    var projection: String? = null,

    @field:SerializedName("geotransform")
    var geotransform: ArrayList<Double>? = null
)
