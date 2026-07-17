package appentus.datasource.api.models.home

data class AllMapBaseResponse (
    val status:Int,
    val message:String,
    val model:List<MapBaseBean>
    )