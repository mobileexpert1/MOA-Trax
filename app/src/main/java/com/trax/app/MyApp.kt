package com.trax.app

import android.app.Application
import android.content.SharedPreferences
import android.util.Log
import appentus.datasource.local.AppDatabase
import appentus.datasource.api.AppNetworkRepository
import com.downloader.PRDownloader
import com.trax.app.utils.PrefManager
import retrofit2.http.Tag

class MyApp: Application() {

    private lateinit var networkRepository: AppNetworkRepository
    private lateinit var appDatabase: AppDatabase

    companion object {
        private var instance: MyApp? = null
        var sharedPreferences: SharedPreferences?=null

        @JvmStatic
        fun getInstance(): MyApp? {
            if (instance == null) {
                instance = MyApp()
            }
            return instance
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        PRDownloader.initialize(applicationContext)
        networkRepository = AppNetworkRepository()
        appDatabase = AppDatabase.getDatabase(applicationContext)
        sharedPreferences= PrefManager.init()

    }

    fun getNetworkRepo(): AppNetworkRepository {
        return networkRepository
    }

    fun getAppDatabase() : AppDatabase {
        return appDatabase
    }

    fun log(msg:String){
        if(BuildConfig.DEBUG){
            Log.d("Tag","Tag: $msg")
        }
    }

}