package com.trax.app.retrofit

object Constants {

    const val BASE_URL = "https://datav2.myoutdooragent.com/"

    const val LOGIN = "api/trax/login"
    const val USER_PROFILE = "api/trax/userprofile"
    const val LOGOUT = "api/trax/logout"
    const val LICENSE = "api/trax/licenses"
    const val DELETE_USER = "api/account/DeleteUser"
    // OLD IMPLEMENTATION
    // No longer required because the Home API now provides the map data.
    // const val GET_COORDINATES = "api/trax/getcoordinates"
    const val SAVE_TRACK = "api/trax/savetrack"

    const val GET_TRACKS = "api/trax/gettracks"

    const val GET_TRACK = "api/trax/gettrack/{trackId}"

    const val DELETE_TRACK = "api/trax/deletetrack/{trackId}"

    const val PDF_TRACK = "api/trax/TrackPdf/{trackId}"
}