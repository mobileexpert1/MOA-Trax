package com.trax.app.viewModels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.myoutdoor.agent.retrofit.ApiClient
import com.myoutdoor.agent.retrofit.ResponseHandler
import com.trax.app.models.live_track.save_track.SaveTrackRequest
import com.trax.app.models.live_track.save_track.SaveTrackResponse
// OLD IMPLEMENTATION 
// import com.trax.app.models.live_track.coordinates.CoordinateResponse
import kotlinx.coroutines.launch

class MapLiveTrackViewModel : BaseViewModel() {

    // OLD IMPLEMENTATION 
    // val coordinatesSuccess = MutableLiveData<CoordinateResponse?>()
    val saveTrackSuccess = MutableLiveData<SaveTrackResponse?>()

    // OLD IMPLEMENTATION
    // No longer required because the Home API now provides the map data.
    //
    // fun getCoordinates(
    //     token: String,
    //     propertyName: String
    // ) {
    //     viewModelScope.launch {
    //         isLoading.value = true
    //         try {
    //             val response =
    //                 ApiClient.getApiClientWithHeader(token)
    //                     ?.getCoordinates(
    //                         name = propertyName,
    //                         format = "geojson"
    //                     )
    //             isLoading.value = false
    //             if (response?.isSuccessful == true) {
    //                 coordinatesSuccess.value = response.body()
    //             } else {
    //                 apiError.value = response?.message() ?: "Something went wrong"
    //             }
    //         } catch (e: Exception) {
    //             isLoading.value = false
    //             apiError.value = ResponseHandler().handleException<String>(e).message ?: "Network Error"
    //         }
    //     }
    // }

    //** save track **//
    fun saveTrack(
        token: String,
        request: SaveTrackRequest
    ) {

        viewModelScope.launch {

            isLoading.value = true
            try {

                val response =
                    ApiClient
                        .getApiClientWithHeader(token)
                        ?.saveTrack(request)

                isLoading.value = false

                if (response?.isSuccessful == true) {

                    saveTrackSuccess.value =
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
                        .message ?: "Network Error"
            }
        }
    }
}