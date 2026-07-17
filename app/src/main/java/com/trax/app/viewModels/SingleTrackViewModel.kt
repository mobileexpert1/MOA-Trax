package com.trax.app.viewModels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.myoutdoor.agent.retrofit.ApiClient
import com.myoutdoor.agent.retrofit.ResponseHandler
import com.trax.app.models.delete.DeleteTrackResponse
import com.trax.app.models.track.single_track.GetSingleTrackResponse
import kotlinx.coroutines.launch

class SingleTrackViewModel : BaseViewModel() {

    val singleTrackSuccess =
        MutableLiveData<GetSingleTrackResponse?>()

    val deleteTrackSuccess =
        MutableLiveData<DeleteTrackResponse?>()

    fun getSingleTrack(
        token: String,
        trackId: Int
    ) {

        viewModelScope.launch {

            isLoading.value = true

            try {

                val response =
                    ApiClient.getApiClientWithHeader(token)
                        ?.getSingleTrack(trackId)

                isLoading.value = false

                if (response?.isSuccessful == true) {

                    singleTrackSuccess.value =
                        response.body()

                } else {

                    apiError.value =
                        response?.message()
                            ?: "Something went wrong"
                }

            } catch (e: Exception) {

                isLoading.value = false

                apiError.value =
                    ResponseHandler()
                        .handleException<String>(e)
                        .message
            }
        }
    }

    fun deleteTrack(
        token: String,
        trackId: Int
    ) {

        viewModelScope.launch {

            isLoading.value = true

            try {

                val response =
                    ApiClient.getApiClientWithHeader(token)
                        ?.deleteTrack(trackId)

                isLoading.value = false

                if (response?.isSuccessful == true) {

                    deleteTrackSuccess.value =
                        response.body()

                } else {

                    apiError.value =
                        response?.message()
                            ?: "Something went wrong"
                }

            } catch (e: Exception) {

                isLoading.value = false

                apiError.value =
                    ResponseHandler()
                        .handleException<String>(e)
                        .message
            }
        }
    }
}