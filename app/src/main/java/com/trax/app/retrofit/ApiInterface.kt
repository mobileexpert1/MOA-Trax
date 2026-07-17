package appentus.datasource.api

import appentus.datasource.api.models.home.AllMapBaseResponse
import com.trax.app.models.delete.DeleteTrackResponse
import com.trax.app.models.delete.DeleteUserResponse
import com.trax.app.models.home.license.LicenseResponse
// OLD IMPLEMENTATION 
// import com.trax.app.models.live_track.coordinates.CoordinateResponse
import com.trax.app.models.live_track.save_track.SaveTrackRequest
import com.trax.app.models.live_track.save_track.SaveTrackResponse
import com.trax.app.models.login.request.LoginRequest
import com.trax.app.models.login.request.SocialLoginRequest
import com.trax.app.models.login.response.LoginResponse
import com.trax.app.models.logout.LogoutResponse
import com.trax.app.models.track.GetTracksResponse
import com.trax.app.models.track.single_track.GetSingleTrackResponse
import com.trax.app.models.userprofile.response.ProfileResponse
import com.trax.app.retrofit.Constants
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiInterface {

    @POST(Constants.LOGIN)
    suspend fun loginRequest(
        @Body request: LoginRequest
    ): Response<LoginResponse>

    @POST(Constants.LOGIN)
    suspend fun socialLoginRequest(
        @Body request: SocialLoginRequest
    ): Response<LoginResponse>

    @GET(Constants.USER_PROFILE)
    suspend fun getUserProfile(
    ): Response<ProfileResponse>

    @POST(Constants.LOGOUT)
    suspend fun logout(
        @Header("DeviceToken") deviceToken: String
    ): Response<LogoutResponse>

    @GET(Constants.LICENSE)
    suspend fun getLicenses(): Response<LicenseResponse>

    @POST(Constants.DELETE_USER)
    suspend fun deleteUserRequest(
    ): Response<DeleteUserResponse>

    // OLD IMPLEMENTATION 
    // No longer required because the Home API now provides the map data.
    // @GET(Constants.GET_COORDINATES)
    // suspend fun getCoordinates(
    //     @Query("name") name: String,
    //     @Query("format") format: String = "geojson"
    // ): Response<CoordinateResponse>

    @POST(Constants.SAVE_TRACK)
    suspend fun saveTrack(
        @Body request: SaveTrackRequest
    ): Response<SaveTrackResponse>

    @GET(Constants.GET_TRACKS)
    suspend fun getTracks(
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int
    ): Response<GetTracksResponse>

    @GET(Constants.GET_TRACK)
    suspend fun getSingleTrack(
        @Path("trackId")
        trackId: Int

    ): Response<GetSingleTrackResponse>

    @POST(Constants.DELETE_TRACK)
    suspend fun deleteTrack(
        @Path("trackId") trackId: Int
    ): Response<DeleteTrackResponse>

}