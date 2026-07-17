package com.trax.app.models.login.response

data class LoginResponse(

    val statusCode: Int,

    val message: String,

    val model: String?
)