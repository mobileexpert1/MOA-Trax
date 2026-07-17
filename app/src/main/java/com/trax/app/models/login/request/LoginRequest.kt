package com.trax.app.models.login.request

data class LoginRequest(

    val Email: String,

    val Password: String,

    val AuthenticationType: String,

    val deviceToken: String,

    val deviceType: Int
)