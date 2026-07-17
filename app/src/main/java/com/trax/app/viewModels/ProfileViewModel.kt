package com.trax.app.viewModels


import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.myoutdoor.agent.retrofit.ApiClient
import com.myoutdoor.agent.retrofit.ResponseHandler
import com.trax.app.models.delete.DeleteUserResponse
import com.trax.app.models.logout.LogoutResponse
import com.trax.app.models.userprofile.response.ProfileResponse
import kotlinx.coroutines.launch

class ProfileViewModel : BaseViewModel() {

    val profileSuccess = MutableLiveData<ProfileResponse?>()
    val logoutSuccess = MutableLiveData<LogoutResponse?>()

    val deleteSuccess = MutableLiveData<DeleteUserResponse?>()

    //** get profile api
    fun getProfile(token: String) {

        Log.e("call","TOKEN:: "+token)


        viewModelScope.launch {

            isLoading.value = true

            try {

                val response =
                    ApiClient.getApiClientWithHeader(token)?.getUserProfile()

                isLoading.value = false

                if (response!!.isSuccessful) {

                    val body = response.body()

                    if (body?.model != null) {

                        profileSuccess.value = body

                    } else {

                        apiError.value = body?.message ?: "Something went wrong"
                    }

                } else {

                    apiError.value = response.message()
                }

            } catch (e: Exception) {

                Log.e("call","Exception:: "+e.toString())

                isLoading.value = false

                apiError.value =
                    ResponseHandler()
                        .handleException<String>(e)
                        .message ?: "Network Error"
            }
        }
    }

    //** logout api

    fun logoutApi(token: String, deviceToken: String) {

        viewModelScope.launch {

            isLoading.value = true

            try {

                val response = ApiClient.getApiClientWithHeader(token)?.logout(
                    deviceToken
                )

                isLoading.value = false

                if (response?.isSuccessful == true) {

                    logoutSuccess.value = response.body()

                } else {

                    apiError.value = response?.message() ?: "Something went wrong"
                }

            } catch (e: Exception) {

                isLoading.value = false

                apiError.value = ResponseHandler()
                    .handleException<String>(e)
                    .message ?: "Network Error"
            }
        }
    }

    //** delete api

    fun deleteApi(token: String) {

        viewModelScope.launch {

            isLoading.value = true

            try {

                val response = ApiClient.getApiClientWithHeader(token)?.deleteUserRequest()

                isLoading.value = false

                if (response?.isSuccessful == true) {

                    deleteSuccess.value = response.body()

                } else {

                    apiError.value = response?.message() ?: "Something went wrong"
                }

            } catch (e: Exception) {

                isLoading.value = false

                apiError.value = ResponseHandler()
                    .handleException<String>(e)
                    .message ?: "Network Error"
            }
        }
    }
}