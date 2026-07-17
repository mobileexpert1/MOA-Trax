package com.trax.app.models.login.request

data class SocialLoginRequest(

    val Email: String,

    val AuthorizationKey: String,

    val AuthenticationType: String,

    val deviceToken: String,

    val deviceType: Int
)