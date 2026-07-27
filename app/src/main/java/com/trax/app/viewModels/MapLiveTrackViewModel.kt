package com.trax.app.viewModels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.myoutdoor.agent.retrofit.ApiClient
import com.myoutdoor.agent.retrofit.ResponseHandler
import com.trax.app.models.live_track.save_track.SaveTrackRequest
import com.trax.app.models.live_track.save_track.SaveTrackResponse
import kotlinx.coroutines.launch

class MapLiveTrackViewModel : BaseViewModel() {

    //==============================================================================
    // Variables
    //==============================================================================

    val saveTrackSuccess = MutableLiveData<SaveTrackResponse?>()

    //==============================================================================
    // API Calls
    //==============================================================================

    //--------------------------------------------------
    // Dispatches a tracking session save payload to the API server.
    //--------------------------------------------------
    fun saveTrack(
        token: String,
        request: SaveTrackRequest
    ) {
        viewModelScope.launch {
            isLoading.value = true
            try {
                val response = ApiClient.getApiClientWithHeader(token)
                    ?.saveTrack(request)

                isLoading.value = false
                if (response?.isSuccessful == true) {
                    saveTrackSuccess.value = response.body()
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