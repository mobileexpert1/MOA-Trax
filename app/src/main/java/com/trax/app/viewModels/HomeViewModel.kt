package com.trax.app.viewModels

import androidx.lifecycle.ViewModel
import appentus.datasource.api.AppApiService
import com.trax.app.MyApp

class HomeViewModel() : ViewModel() {

    fun getAllMaps(token:String) = MyApp.getInstance()?.getNetworkRepo()?.getAllMaps(
        AppApiService.API_GET_MAPS,token)

}