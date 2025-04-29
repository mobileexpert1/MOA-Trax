package appentus.datasource.api.models.home

data class AllMapBaseBean (
    val status:Int,
    val message:String,
    val model:List<MapBaseBean>
    )