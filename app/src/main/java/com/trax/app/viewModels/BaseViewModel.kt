package com.trax.app.viewModels


import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myoutdoor.agent.retrofit.ApiClient
import com.myoutdoor.agent.retrofit.ResponseHandler
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import okhttp3.RequestBody

open class BaseViewModel : ViewModel() {

    var apiError= MutableLiveData<String>()
    var isLoading= MutableLiveData<Boolean>()

}