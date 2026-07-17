package com.trax.app.viewModels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.myoutdoor.agent.retrofit.ApiClient
import com.myoutdoor.agent.retrofit.ResponseHandler
import com.trax.app.models.home.license.LicenseResponse
import com.trax.app.models.track.GetTracksResponse
import kotlinx.coroutines.launch

class HomeViewModel : BaseViewModel() {

    val licenseSuccess =
        MutableLiveData<LicenseResponse?>()

    val trackSuccess = MutableLiveData<GetTracksResponse?>()

    //**  get licenses  **//
    fun getLicenses(token: String) {

        viewModelScope.launch {

            isLoading.value = true

            try {

                val response =
                    ApiClient.getApiClientWithHeader(token)
                        ?.getLicenses()

                isLoading.value = false

                if (response?.isSuccessful == true) {

                    val body = response.body()

                    if (body?.model != null) {

                        licenseSuccess.value = body

                    } else {

                        apiError.value =
                            body?.message ?: "No licenses found"
                    }

                } else {

                    apiError.value =
                        response?.message() ?: "Something went wrong"
                }

            } catch (e: Exception) {

                isLoading.value = false

                apiError.value =
                    ResponseHandler()
                        .handleException<String>(e)
                        .message ?: "Network Error"
            }
        }
    }

    //** get tracks api  **//

    fun getTracks(
        token: String,
        page: Int,
        pageSize: Int
    ){

        viewModelScope.launch {

            isLoading.value = true

            try{

                val response =
                    ApiClient.getApiClientWithHeader(token)
                        ?.getTracks(page,pageSize)

                isLoading.value = false

                if(response?.isSuccessful == true){

                    trackSuccess.value = response.body()

                }else{

                    apiError.value = response?.message()
                }

            }catch (e:Exception){

                isLoading.value = false

                apiError.value =
                    ResponseHandler()
                        .handleException<String>(e).message
            }

        }

    }
}