package appentus.datasource.api.models.home

data class MapBaseBean(
    val productID:Int,
    val productName:String?,
    val productNo:String?,
    val displayName:String?,
    val mapFiles: List<MapFilesDetails>
)
