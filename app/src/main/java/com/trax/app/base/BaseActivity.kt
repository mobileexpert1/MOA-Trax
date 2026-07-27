package com.trax.app.base

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import java.text.SimpleDateFormat
import java.util.Locale

open class BaseActivity : AppCompatActivity() {

    //==============================================================================
    // Lifecycle Methods
    //==============================================================================

    //--------------------------------------------------
    // Configures status bars and common decorations.
    //--------------------------------------------------
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.parseColor("#007F56")
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = false
    }

    //==============================================================================
    // Utility / Helper Functions
    //==============================================================================

    //--------------------------------------------------
    // Applies status bar top paddings to target views.
    //--------------------------------------------------
    protected fun applyTopInset(view: View) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            v.setPadding(
                v.paddingLeft,
                statusBarHeight + v.paddingTop,
                v.paddingRight,
                v.paddingBottom
            )
            insets
        }
    }

    //--------------------------------------------------
    // Formats date strings into MMM, yyyy format.
    //--------------------------------------------------
    fun formatLicenseDate(dateString: String?): String {
        if (dateString.isNullOrEmpty()) return ""
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("MMM, yyyy", Locale.getDefault())
            val date = inputFormat.parse(dateString)
            outputFormat.format(date!!)
        } catch (e: Exception) {
            dateString
        }
    }

    //--------------------------------------------------
    // Evaluates network internet connectivity.
    //--------------------------------------------------
    fun Context.isInternetAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}