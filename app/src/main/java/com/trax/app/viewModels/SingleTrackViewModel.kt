package com.trax.app.viewModels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.myoutdoor.agent.retrofit.ApiClient
import com.myoutdoor.agent.retrofit.ResponseHandler
import com.trax.app.models.delete.DeleteTrackResponse
import com.trax.app.models.track.single_track.GetSingleTrackResponse
import kotlinx.coroutines.launch
import okhttp3.ResponseBody

class SingleTrackViewModel : BaseViewModel() {

    //==============================================================================
    // Variables
    //==============================================================================

    val singleTrackSuccess = MutableLiveData<GetSingleTrackResponse?>()
    val deleteTrackSuccess = MutableLiveData<DeleteTrackResponse?>()
    val pdfTrackSuccess = MutableLiveData<ResponseBody?>()

    //==============================================================================
    // API Calls
    //==============================================================================

    //--------------------------------------------------
    // Dispatches a remote request to fetch coordinates and properties for a single track ID.
    //--------------------------------------------------
    fun getSingleTrack(
        token: String,
        trackId: Int
    ) {
        viewModelScope.launch {
            isLoading.value = true
            try {
                val response = ApiClient.getApiClientWithHeader(token)
                    ?.getSingleTrack(trackId)

                isLoading.value = false
                if (response?.isSuccessful == true) {
                    singleTrackSuccess.value = response.body()
                } else {
                    apiError.value = response?.message() ?: "Something went wrong"
                }
            } catch (e: Exception) {
                isLoading.value = false
                apiError.value = ResponseHandler()
                    .handleException<String>(e)
                    .message
            }
        }
    }

    //--------------------------------------------------
    // Dispatches a delete request to remove the specified track.
    //--------------------------------------------------
    fun deleteTrack(
        token: String,
        trackId: Int
    ) {
        viewModelScope.launch {
            isLoading.value = true
            try {
                val response = ApiClient.getApiClientWithHeader(token)
                    ?.deleteTrack(trackId)

                isLoading.value = false
                if (response?.isSuccessful == true) {
                    deleteTrackSuccess.value = response.body()
                } else {
                    apiError.value = response?.message() ?: "Something went wrong"
                }
            } catch (e: Exception) {
                isLoading.value = false
                apiError.value = ResponseHandler()
                    .handleException<String>(e)
                    .message
            }
        }
    }

    //--------------------------------------------------
    // Requests a PDF / PNG image export stream from the API servers.
    //--------------------------------------------------
    fun pdfTrack(
        token: String,
        trackId: Int
    ) {
        viewModelScope.launch {
            isLoading.value = true
            try {
                val response = ApiClient.getApiClientWithHeader(token)
                    ?.pdfTrack(trackId)

                isLoading.value = false
                if (response?.isSuccessful == true) {
                    pdfTrackSuccess.value = response.body()
                } else {
                    apiError.value = response?.message() ?: "Something went wrong"
                }
            } catch (e: Exception) {
                isLoading.value = false
                apiError.value = ResponseHandler()
                    .handleException<String>(e)
                    .message
            }
        }
    }
}