package com.trax.app.utils

import android.graphics.Rect
import android.text.TextUtils
import android.util.Patterns
import android.view.View
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody

object Utility {

    fun getBoundingViewRect(view : View): Rect {
        val l = IntArray(2)
        view.getLocationOnScreen(l)
        return Rect(
            l[0],
            l[1],
            l[0] + view.width,
            l[1] + view.height
        )
    }

    fun isValidEmail(target: String?): Boolean? {
        return !TextUtils.isEmpty(target) && Patterns.EMAIL_ADDRESS.matcher(target).matches()
    }

    fun convertToRequestBody(params: Map<String, Any>): RequestBody? {
        var requestBody: RequestBody? = null
        try {
            requestBody = Gson().toJson(params)
                .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return requestBody
    }

    fun validEmail(email: String?): Boolean {
        val pattern = Patterns.EMAIL_ADDRESS
        return pattern.matcher(email!!).matches()
    }
}