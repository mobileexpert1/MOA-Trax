package com.trax.app.models.delete

data class DeleteUserResponse(
    var message: String,
    var model: Boolean,
    var statusCode: Int
)
