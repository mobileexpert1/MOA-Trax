package com.trax.app.viewModels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.myoutdoor.agent.retrofit.ApiClient
import com.myoutdoor.agent.retrofit.ResponseHandler
import com.trax.app.models.home.license.LicenseResponse
import com.trax.app.models.track.GetTracksResponse
import kotlinx.coroutines.launch

class HomeViewModel : BaseViewModel() {

    //==============================================================================
    // Variables
    //==============================================================================

    val licenseSuccess = MutableLiveData<LicenseResponse?>()
    val trackSuccess = MutableLiveData<GetTracksResponse?>()

    //==============================================================================
    // API Calls
    //==============================================================================

    //--------------------------------------------------
    // Dispatches network getLicenses request to retrieve user property active leases details.
    //--------------------------------------------------
    fun getLicenses(token: String) {
        viewModelScope.launch {
            isLoading.value = true
            try {
                val response = ApiClient.getApiClientWithHeader(token)
                    ?.getLicenses()

                isLoading.value = false
                if (response?.isSuccessful == true) {
                    val body = response.body()
                    if (body?.statusCode == 401) {
                        unauthorizedError.value = true
                    } else if (body?.model != null) {
                        licenseSuccess.value = body
                    } else {
                        apiError.value = body?.message ?: "No licenses found"
                    }
                } else {
                    if (response?.code() == 401) {
                        unauthorizedError.value = true
                    } else {
                        apiError.value = response?.message() ?: "Something went wrong"
                    }
                }
            } catch (e: Exception) {
                isLoading.value = false
                if (e is retrofit2.HttpException && e.code() == 401) {
                    unauthorizedError.value = true
                } else {
                    apiError.value = ResponseHandler()
                        .handleException<String>(e)
                        .message ?: "Network Error"
                }
            }
        }
    }

    //--------------------------------------------------
    // Fetches paginated user recorded tracking routes lists.
    //--------------------------------------------------
    fun getTracks(
        token: String,
        page: Int,
        pageSize: Int
    ) {
        viewModelScope.launch {
            isLoading.value = true
            try {
                val response = ApiClient.getApiClientWithHeader(token)
                    ?.getTracks(page, pageSize)

                isLoading.value = false
                if (response?.isSuccessful == true) {
                    val body = response.body()
                    if (body?.statusCode == 401) {
                        unauthorizedError.value = true
                    } else {
                        trackSuccess.value = body
                    }
                } else {
                    if (response?.code() == 401) {
                        unauthorizedError.value = true
                    } else {
                        apiError.value = response?.message()
                    }
                }
            } catch (e: Exception) {
                isLoading.value = false
                if (e is retrofit2.HttpException && e.code() == 401) {
                    unauthorizedError.value = true
                } else {
                    apiError.value = ResponseHandler()
                        .handleException<String>(e).message
                }
            }
        }
    }
}