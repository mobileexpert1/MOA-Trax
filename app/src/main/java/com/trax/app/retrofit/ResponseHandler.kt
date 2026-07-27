package com.myoutdoor.agent.retrofit

import com.bumptech.glide.load.HttpException
import java.net.SocketTimeoutException

open class ResponseHandler {

    //==============================================================================
    // Resource State Wrapping Functions
    //==============================================================================

    //--------------------------------------------------
    // What the function does: Wraps data structures into standard success Resource objects.
    // When it is called: Can be called on API response success events.
    // Why it is required: Formats payloads to unify view components.
    //--------------------------------------------------
    fun <T : Any> handleSuccess(data: T): Resource<T> {
        return Resource.success(data)
    }

    //--------------------------------------------------
    // What the function does: Evaluates throwing exceptions to wrap them as error Resource containers.
    // When it is called: Called inside ViewModels API try/catch blocks.
    // Why it is required: Normalizes networking exceptions to return clear error strings.
    //--------------------------------------------------
    fun <T : Any> handleException(e: Exception): Resource<T> {
        return when (e) {
            is HttpException -> {
                Resource.error(getErrorMessage(e.statusCode), null)
            }
            is SocketTimeoutException -> Resource.error(getErrorMessage(400), null)
            else -> Resource.error(getErrorMessage(Int.MAX_VALUE), null)
        }
    }

    //==============================================================================
    // Utility / String Translation Functions
    //==============================================================================

    //--------------------------------------------------
    // What the function does: Translates HTTP integer status codes to descriptive error text.
    // When it is called: Inside handleException evaluations.
    // Why it is required: Converts raw numbers to human readable strings.
    //--------------------------------------------------
    private fun getErrorMessage(code: Int): String {
        return when (code) {
            400 -> "Timeout"
            401 -> "Unauthorised"
            404 -> "Not found"
            else -> "Something went wrong"
        }
    }
}