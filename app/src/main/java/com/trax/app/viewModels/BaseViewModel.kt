package com.trax.app.viewModels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

open class BaseViewModel : ViewModel() {

    //==============================================================================
    // Shared Observables
    //==============================================================================

    // Holds network/API error messages for subscriber activities/fragments
    var apiError = MutableLiveData<String>()

    // Controls progress loader display state visibilities
    var isLoading = MutableLiveData<Boolean>()

    // Flag signifying 401 session expiration occurrences
    val unauthorizedError = MutableLiveData<Boolean>()
}