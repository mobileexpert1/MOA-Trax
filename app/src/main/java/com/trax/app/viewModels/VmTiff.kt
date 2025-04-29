package com.trax.app.viewModels

import android.util.Log
import appentus.datasource.api.models.home.MapFileJsonBean
import appentus.datasource.local.EntityGeoPDF
import org.gdal.gdal.gdal
import org.gdal.gdalconst.gdalconstConstants
import org.gdal.ogr.ogr
import org.gdal.osr.CoordinateTransformation
import org.gdal.osr.SpatialReference

class VmTiff : BaseViewModel() {

    lateinit var minLatLon : DoubleArray
    lateinit var maxLatLon : DoubleArray

    fun getLocationData(mapbean: MapFileJsonBean) {
        gdal.AllRegister()
        ogr.RegisterAll()

        try {
            var width = mapbean.rasterXYsize!![0]
            var height = mapbean.rasterXYsize!![1]
            var gt = mapbean.geotransform!!
            var minX = gt[0]
            var minY = gt[3] + width * gt[4] + height * gt[5]
            var maxX = gt[0] + width * gt[1] + height * gt[2]
            var maxY = gt[3]

            /**Create the new coordinate system*/
            var wgs84_wkt = """
                    GEOGCS["WGS 84",
                        DATUM["WGS_1984",
                            SPHEROID["WGS 84",6378137,298.257223563,
                            AUTHORITY["EPSG","7030"]],
                            AUTHORITY["EPSG","6326"]],
                            PRIMEM["Greenwich",0,
                            AUTHORITY["EPSG","8901"]],
                            UNIT["degree",0.01745329251994328,
                            AUTHORITY["EPSG","9122"]],
                            AUTHORITY["EPSG","4326"]]"""
            var new_cs = SpatialReference()
            new_cs.ImportFromWkt(wgs84_wkt)

            /**Get the existing coordinate system*/
            var old_cs = SpatialReference()
            old_cs.ImportFromWkt(mapbean.projection!!)

            /**Create a transform object to convert between coordinate systems*/
            var transform = CoordinateTransformation(old_cs, new_cs)

            minLatLon = transform.TransformPoint(minX, minY)
            maxLatLon = transform.TransformPoint(maxX, maxY)
            Log.d("GeoPdfActivity", "getLocationData: Location Data Succesfully recieved")
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("GDAL", "Exception")
        }
        println(gdal.GetLastErrorNo())
        println(gdal.GetLastErrorMsg())
    }

}