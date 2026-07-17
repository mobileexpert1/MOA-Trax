package com.myoutdoor.agent.retrofit

import android.widget.Toast
import com.bumptech.glide.load.HttpException
import java.net.SocketTimeoutException


open class ResponseHandler {
    fun <T : Any> handleSuccess(data: T): Resource<T> {
        return Resource.success(data)
    }

    fun <T : Any> handleException(e: Exception): Resource<T> {
        return when (e) {
            is HttpException -> {
            Resource.error(getErrorMessage(e.statusCode), null)

        }

            is SocketTimeoutException -> Resource.error(getErrorMessage(400), null)
            else -> Resource.error(getErrorMessage(Int.MAX_VALUE), null)
        }
    }

    private fun getErrorMessage(code: Int): String {
        return when (code) {
            400 -> "Timeout"
            401 -> "Unauthorised"
            404 -> "Not found"
            else -> "Something went wrong"
        }
    }
}