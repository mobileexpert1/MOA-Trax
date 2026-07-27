package com.trax.app

import android.app.Application
import android.content.SharedPreferences
import android.util.Log
import appentus.datasource.api.AppNetworkRepository
import appentus.datasource.local.AppDatabase
import com.downloader.PRDownloader
import com.mapbox.common.MapboxOptions
import com.trax.app.utils.NetworkMonitor
import com.trax.app.utils.PrefManager

class MyApp : Application() {

    //==============================================================================
    // Variables
    //==============================================================================

    private lateinit var networkRepository: AppNetworkRepository
    private lateinit var appDatabase: AppDatabase

    //==============================================================================
    // Companion Object
    //==============================================================================

    companion object {
        private var instance: MyApp? = null
        var sharedPreferences: SharedPreferences? = null

        //--------------------------------------------------
        // What the function does: Returns the active single instance of MyApp.
        // When it is called: Triggered across utility helper references.
        // Why it is required: Accesses application context parameters anywhere in code.
        //--------------------------------------------------
        @JvmStatic
        fun getInstance(): MyApp? {
            if (instance == null) {
                instance = MyApp()
            }
            return instance
        }
    }

    //==============================================================================
    // Lifecycle Methods
    //==============================================================================

    //--------------------------------------------------
    // What it does: Configures Mapbox tokens, initializes PRDownloader, database instances, and registers network monitors.
    // When it is called: Automatically by the Android system when the process is created.
    // Why it is required: Sets up global requirements and database dependencies before launching activities.
    //--------------------------------------------------
    override fun onCreate() {
        super.onCreate()

        MapboxOptions.accessToken = BuildConfig.MAPBOX_ACCESS_TOKEN

        instance = this
        PRDownloader.initialize(applicationContext)
        networkRepository = AppNetworkRepository()
        appDatabase = AppDatabase.getDatabase(applicationContext)
        sharedPreferences = PrefManager.init()
        NetworkMonitor.startMonitoring(this)
    }

    //==============================================================================
    // Repository / Database Getters
    //==============================================================================

    //--------------------------------------------------
    // What the function does: Returns the global repository client interface.
    // When it is called: Called when child components access remote APIs.
    // Why it is required: Centrally handles network repo calls.
    //--------------------------------------------------
    fun getNetworkRepo(): AppNetworkRepository {
        return networkRepository
    }

    //--------------------------------------------------
    // What the function does: Returns local SQLite Room database helper.
    // When it is called: Called when database entities query executions start.
    // Why it is required: Accesses local tables data structures.
    //--------------------------------------------------
    fun getAppDatabase(): AppDatabase {
        return appDatabase
    }

    //==============================================================================
    // Logging Functions
    //==============================================================================

    //--------------------------------------------------
    // What the function does: Prints log details to logcat console if debug logs configurations are active.
    // When it is called: Can be called anywhere in code.
    // Why it is required: Provides central safe logger options.
    //--------------------------------------------------
    fun log(msg: String) {
        if (BuildConfig.DEBUG) {
            Log.d("Tag", "Tag: $msg")
        }
    }
}