package com.trax.app.base

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import java.text.SimpleDateFormat
import java.util.Locale
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

open class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
    }

    protected fun applyTopInset(view: View) {

        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->

            val statusBarHeight =
                insets.getInsets(WindowInsetsCompat.Type.statusBars()).top

            v.setPadding(
                v.paddingLeft,
                statusBarHeight + v.paddingTop,
                v.paddingRight,
                v.paddingBottom
            )

            insets
        }
    }


    fun formatLicenseDate(dateString: String?): String {

        if (dateString.isNullOrEmpty()) return ""

        return try {

            val inputFormat =
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())

            val outputFormat =
                SimpleDateFormat("MMM, yyyy", Locale.getDefault())

            val date = inputFormat.parse(dateString)

            outputFormat.format(date!!)

        } catch (e: Exception) {
            dateString
        }
    }


    fun Context.isInternetAvailable(): Boolean {

        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val network = connectivityManager.activeNetwork ?: return false

        val capabilities =
            connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}