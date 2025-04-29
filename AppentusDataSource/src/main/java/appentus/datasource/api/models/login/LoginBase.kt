package appentus.datasource.api.models.login

data class LoginBase(
    val statusCode:Int,
    val message:String,
    val model:UserDetail
)
