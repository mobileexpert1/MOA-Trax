package appentus.datasource.api.models.login

data class LoginResponse(
    val statusCode:Int,
    val message:String,
    val model:UserDetail
)
