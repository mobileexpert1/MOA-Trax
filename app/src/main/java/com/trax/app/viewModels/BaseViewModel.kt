package com.trax.app.viewModels

import androidx.lifecycle.ViewModel
import com.trax.app.MyApp

open class BaseViewModel : ViewModel() {

    var appRepo = MyApp.getInstance()!!.getNetworkRepo()
    var appDB = MyApp.getInstance()!!.getAppDatabase()
}