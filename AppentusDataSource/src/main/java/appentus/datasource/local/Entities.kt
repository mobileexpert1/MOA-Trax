package appentus.datasource.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import appentus.datasource.api.models.home.LatLngData
import org.jetbrains.annotations.NotNull
import java.io.File

/**
 * Status 0 : UPLOADING
 * STATUS 1 : DOWNLOADING
 * STATUS 2 : CONVERTING
 * STATUS 3 : READY
 * */

@Entity(tableName = "geoPDF")
data class EntityGeoPDF(
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0,
    var fileName: String? = "",
    val localTiffPath: String? = "",
    val localPngPath: String? = "",
    val localPdfPath: String? = "",
    var remoteTiffPath: String? = "",
    var created: String = "",
    val status: Int? = 0
)



@Entity(tableName = "remoteKeys")
data class EntityRemoteKeys(
    @PrimaryKey var repoId: Int,
    var apiEndpoint: String,
    var prevKey: Int?,
    var nextKey: Int?
)

@Entity(tableName = "allPdfs")
data class EntitySavePdf(
    @PrimaryKey var productId: Int,
    var productName: String? = "",
    var productNo: String? = "",
    var displayName: String? = "",
    var isSavedTracking: Boolean = false,
     var userIdList: ArrayList<String>?
)
/*var userId: String? =" "*/
@Entity(tableName = "allMapPdfs")
data class EntityMapFile(
    @PrimaryKey var mapId: Int,
    var productId: Int,
    var mapPath: String? = "",
    var mapFileURL: String? = "",
    var mapFileName: String? = "",
    var mapInfoJason: String? = "",
    var status: Int? = 0,
    var latLngJsonString: String? = "",
)

@Entity(tableName = "allTrackingPdfs")
data class EntitySavedTrackingPdf(
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0,
    var productId: Int,
    var productName: String? = "",
    var productNo: String? = "",
    var displayName: String? = "",
    var isSavedTracking: Boolean = false
)

@Entity(tableName = "allMapTrackingPdfs")
data class EntityMapTrackingFile(
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0,
    var mapId: Int,
    var productId: Int,
    var mapPath: String? = "",
    var mapFileURL: String? = "",
    var mapFileName: String? = "",
    var mapInfoJason: String? = "",
    var status: Int? = 0,
    var latLngJsonString: String? = "",
    var userIdList: ArrayList<String>?
)
