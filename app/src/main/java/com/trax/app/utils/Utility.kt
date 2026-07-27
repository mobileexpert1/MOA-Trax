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

    //==============================================================================
    // View Functions
    //==============================================================================

    //--------------------------------------------------
    // Calculates the boundaries rectangle coordinates of a view on the device screen.
    //--------------------------------------------------
    fun getBoundingViewRect(view: View): Rect {
        val l = IntArray(2)
        view.getLocationOnScreen(l)
        return Rect(
            l[0],
            l[1],
            l[0] + view.width,
            l[1] + view.height
        )
    }

    //==============================================================================
    // Validation Functions
    //==============================================================================

    //--------------------------------------------------
    // Validates if the target string matches standard email regex specifications.
    //--------------------------------------------------
    fun isValidEmail(target: String?): Boolean {
        if (target == null) return false
        return !TextUtils.isEmpty(target) && Patterns.EMAIL_ADDRESS.matcher(target).matches()
    }

    //==============================================================================
    // Serialization Functions
    //==============================================================================

    //--------------------------------------------------
    // Serializes parameter maps into HTTP application/json RequestBody entities.
    //--------------------------------------------------
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
}